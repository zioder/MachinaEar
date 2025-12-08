# mlops/training/model.py
"""
CNN Autoencoder avec PyTorch Lightning et MLflow tracking
"""

import torch
from torch import nn
import torch.nn.functional as F
import pytorch_lightning as pl
from sklearn.metrics import roc_auc_score, f1_score, recall_score, precision_score, confusion_matrix
import numpy as np
import mlflow
import mlflow.pytorch


class CNNAutoEncoder(nn.Module):
    """CNN Autoencoder pour la détection d'anomalies"""
    
    def __init__(self, z_dim=40):
        super().__init__()

        # Encoder
        self.conv1 = nn.Sequential(
            nn.ZeroPad2d((1, 2, 1, 2)),
            nn.Conv2d(1, 32, kernel_size=5, stride=2),
            nn.ReLU()
        )
        self.conv2 = nn.Sequential(
            nn.ZeroPad2d((1, 2, 1, 2)),
            nn.Conv2d(32, 64, kernel_size=5, stride=2),
            nn.ReLU(),
            nn.Dropout(0.2)
        )
        self.conv3 = nn.Sequential(
            nn.Conv2d(64, 128, kernel_size=3, stride=2, padding=0),
            nn.ReLU(),
            nn.Dropout(0.3)
        )
        self.conv4 = nn.Sequential(
            nn.Conv2d(128, 256, kernel_size=3, stride=2, padding=0),
            nn.ReLU(),
            nn.Dropout(0.3)
        )
        self.fc1 = nn.Conv2d(256, z_dim, kernel_size=3)

        # Decoder
        self.fc2 = nn.Sequential(
            nn.ConvTranspose2d(z_dim, 256, kernel_size=3),
            nn.ReLU(),
            nn.Dropout(0.3)
        )
        self.conv4d = nn.Sequential(
            nn.ConvTranspose2d(256, 128, kernel_size=3, stride=2, padding=0),
            nn.ReLU(),
            nn.Dropout(0.3)
        )
        self.conv3d = nn.Sequential(
            nn.ConvTranspose2d(128, 64, kernel_size=3, stride=2, padding=0),
            nn.ReLU(),
            nn.Dropout(0.2)
        )
        self.conv2d = nn.Sequential(
            nn.ConvTranspose2d(64, 32, kernel_size=5, stride=2),
            nn.ReLU()
        )
        self.conv1d = nn.ConvTranspose2d(32, 1, kernel_size=5, stride=2)

    def forward(self, x):
        # Sauvegarder la taille d'entrée pour la reconstruction exacte
        input_size = x.shape
        
        # Encoder
        encoded = self.fc1(self.conv4(self.conv3(self.conv2(self.conv1(x)))))

        # Decoder
        decoded = self.fc2(encoded)
        decoded = self.conv4d(decoded)
        decoded = self.conv3d(decoded)
        decoded = self.conv2d(decoded)[:, :, 1:-1, 1:-1]
        decoded = self.conv1d(decoded)[:, :, 0:-1, 0:-1]
        
        # Ajuster la taille pour matcher l'input exactement
        if decoded.shape != input_size:
            # Utiliser interpolate pour ajuster à la taille exacte
            decoded = F.interpolate(decoded, size=input_size[2:], mode='bilinear', align_corners=False)
        
        decoded = torch.sigmoid(decoded)

        return decoded


class LitAutoEncoder(pl.LightningModule):
    """PyTorch Lightning wrapper pour l'autoencoder avec MLflow tracking"""
    
    def __init__(
        self,
        z_dim: int = 40,
        learning_rate: float = 1e-3,
        threshold_percentile: float = 95.0
    ):
        super().__init__()
        self.save_hyperparameters()
        
        self.model = CNNAutoEncoder(z_dim=z_dim)
        self.learning_rate = learning_rate
        self.threshold_percentile = threshold_percentile
        self.threshold = None
        
        # Pour stocker les prédictions du test
        self.test_reconstruction_errors = []
        self.test_labels = []
    
    def forward(self, x):
        return self.model(x)
    
    def compute_reconstruction_error(self, x, x_hat):
        """Calcule l'erreur de reconstruction (MSE par sample)"""
        # Erreur par sample (moyenne sur les dimensions spatial)
        mse = F.mse_loss(x_hat, x, reduction='none')
        mse = mse.view(mse.size(0), -1).mean(dim=1)
        return mse
    
    def training_step(self, batch, batch_idx):
        x, _ = batch
        x_hat = self(x)
        
        # Loss globale pour la backprop
        loss = F.mse_loss(x_hat, x)
        
        # Log metrics
        self.log('train_loss', loss, on_step=False, on_epoch=True, prog_bar=True)
        
        return loss
    
    def validation_step(self, batch, batch_idx):
        x, _ = batch
        x_hat = self(x)
        
        loss = F.mse_loss(x_hat, x)
        
        # Calculer les erreurs de reconstruction
        reconstruction_errors = self.compute_reconstruction_error(x, x_hat)
        
        self.log('val_loss', loss, on_step=False, on_epoch=True, prog_bar=True)
        self.log('val_mean_reconstruction_error', reconstruction_errors.mean(), 
                 on_step=False, on_epoch=True)
        
        return {'val_loss': loss, 'reconstruction_errors': reconstruction_errors}
    
    def on_validation_epoch_end(self):
        """Calcule le threshold basé sur les erreurs de validation"""
        # Récupérer toutes les erreurs de reconstruction de validation
        if hasattr(self.trainer, 'val_dataloaders'):
            all_errors = []
            
            self.eval()
            with torch.no_grad():
                for batch in self.trainer.val_dataloaders:
                    x, _ = batch
                    x = x.to(self.device)
                    x_hat = self(x)
                    errors = self.compute_reconstruction_error(x, x_hat)
                    all_errors.extend(errors.cpu().numpy())
            
            # Calculer le threshold (ex: 95ème percentile)
            self.threshold = np.percentile(all_errors, self.threshold_percentile)
            self.log('threshold', self.threshold, prog_bar=False)
    
    def test_step(self, batch, batch_idx):
        x, labels = batch
        x_hat = self(x)
        
        # Calculer les erreurs de reconstruction
        reconstruction_errors = self.compute_reconstruction_error(x, x_hat)
        
        # Stocker pour calcul des métriques à la fin
        self.test_reconstruction_errors.extend(reconstruction_errors.cpu().numpy())
        self.test_labels.extend(labels.cpu().numpy().flatten())
        
        return {'reconstruction_errors': reconstruction_errors, 'labels': labels}
    
    def on_test_epoch_end(self):
        """Calcule les métriques finales sur le test set"""
        errors = np.array(self.test_reconstruction_errors)
        labels = np.array(self.test_labels)
        
        # Prédictions (anomaly si erreur > threshold)
        if self.threshold is None:
            self.threshold = np.percentile(errors[labels == 0], self.threshold_percentile)
        
        predictions = (errors > self.threshold).astype(int)
        
        # Calculer les métriques
        auc = roc_auc_score(labels, errors)
        f1 = f1_score(labels, predictions)
        recall = recall_score(labels, predictions)
        precision = precision_score(labels, predictions)
        
        # Confusion matrix
        tn, fp, fn, tp = confusion_matrix(labels, predictions).ravel()
        
        # Log dans MLflow et Lightning
        metrics = {
            'test_auc': auc,
            'test_f1_score': f1,
            'test_recall': recall,
            'test_precision': precision,
            'test_accuracy': (tp + tn) / (tp + tn + fp + fn),
            'test_threshold': self.threshold,
            'test_true_positives': tp,
            'test_false_positives': fp,
            'test_true_negatives': tn,
            'test_false_negatives': fn
        }
        
        for key, value in metrics.items():
            self.log(key, value, prog_bar=True)
        
        # Log dans MLflow
        if mlflow.active_run():
            mlflow.log_metrics(metrics)
        
        # Clear pour le prochain test
        self.test_reconstruction_errors = []
        self.test_labels = []
        
        return metrics
    
    def configure_optimizers(self):
        optimizer = torch.optim.Adam(self.parameters(), lr=self.learning_rate)
        
        # Learning rate scheduler (sans verbose pour compatibilité)
        scheduler = torch.optim.lr_scheduler.ReduceLROnPlateau(
            optimizer,
            mode='min',
            factor=0.5,
            patience=5
        )
        
        return {
            'optimizer': optimizer,
            'lr_scheduler': {
                'scheduler': scheduler,
                'monitor': 'train_loss'  # Monitor train_loss au lieu de val_loss
            }
        }
    
    def predict_anomaly(self, x):
        """Prédiction d'anomalie pour l'inférence"""
        self.eval()
        with torch.no_grad():
            x_hat = self(x)
            error = self.compute_reconstruction_error(x, x_hat)
            is_anomaly = error > self.threshold
        
        return is_anomaly, error