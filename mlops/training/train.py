# mlops/training/train.py
"""
Script d'entraînement du CNN Autoencoder avec MLflow tracking
"""

import torch
import pytorch_lightning as pl
from pytorch_lightning.callbacks import ModelCheckpoint, EarlyStopping
from pytorch_lightning.loggers import MLFlowLogger
import mlflow
import mlflow.pytorch
from pathlib import Path
import sys

# Ajouter le chemin du projet
sys.path.append(str(Path(__file__).parent.parent.parent))

from mlops.training.model import LitAutoEncoder
from mlops.training.dataset import SpectrogramDataModule
from mlops.training.mlflow_config import MLflowConfig


def train_model(
    # Hyperparamètres du modèle
    z_dim: int = 40,
    learning_rate: float = 1e-3,
    threshold_percentile: float = 95.0,
    
    # Paramètres d'entraînement
    max_epochs: int = 1,  # 1 epoch pour test rapide
    batch_size: int = 32,
    num_workers: int = 4,
    
    # Chemins des données
    train_data_path: str = "./mlops/feature_store/features/normal_spectrograms.npy",
    test_data_path: str = "./mlops/data/test",
    
    # Configuration MLflow
    experiment_name: str = "anomaly_detection_autoencoder",
    run_name: str = None,
    
    # Autres
    accelerator: str = "auto",
    devices: int = 1,
    seed: int = 42
):
    """
    Entraîne le modèle CNN Autoencoder avec tracking MLflow
    
    Args:
        z_dim: Dimension du latent space
        learning_rate: Learning rate
        threshold_percentile: Percentile pour définir le seuil d'anomalie
        max_epochs: Nombre d'epochs
        batch_size: Taille des batchs
        num_workers: Nombre de workers pour le DataLoader
        train_data_path: Chemin vers les spectrogrammes d'entraînement
        test_data_path: Chemin vers les données de test
        experiment_name: Nom de l'expérience MLflow
        run_name: Nom du run MLflow
        accelerator: Type d'accélérateur (auto, gpu, cpu)
        devices: Nombre de devices
        seed: Seed pour la reproductibilité
    """
    
    print("=" * 100)
    print("ENTRAÎNEMENT CNN AUTOENCODER - DÉTECTION D'ANOMALIES")
    print("=" * 100)
    
    # 1. Configuration de la reproductibilité
    pl.seed_everything(seed)
    
    # 2. Configuration MLflow
    print("\n Configuration MLflow...")
    mlflow_config = MLflowConfig(experiment_name=experiment_name)
    experiment_id = mlflow_config.setup_mlflow()
    
    # 3. Créer le DataModule
    print("\n Chargement des données...")
    data_module = SpectrogramDataModule(
        train_data_path=train_data_path,
        test_data_path=test_data_path,
        batch_size=batch_size,
        num_workers=num_workers
    )
    
    # Setup pour voir les statistiques
    data_module.setup("fit")
    data_module.setup("test")
    
    print(f"    Train size: {len(data_module.train_dataset)} (all normal data)")
    print(f"    Val/Test size: {len(data_module.val_dataset)} (normal + anomaly)")
    print(f"   Note: Validation = Test set for early evaluation")
    
    # 4. Créer le modèle
    print("\n Création du modèle...")
    model = LitAutoEncoder(
        z_dim=z_dim,
        learning_rate=learning_rate,
        threshold_percentile=threshold_percentile
    )
    
    # Afficher l'architecture
    sample_batch = next(iter(data_module.train_dataloader()))
    sample_input = sample_batch[0][:1]
    print(f"    Input shape: {sample_input.shape}")
    print(f"    Model parameters: {sum(p.numel() for p in model.parameters()):,}")
    
    # 5. Callbacks
    print("\n Configuration des callbacks...")
    
    checkpoint_callback = ModelCheckpoint(
        dirpath='./checkpoints',
        filename='autoencoder-{epoch:02d}-{val_loss:.4f}',
        monitor='val_loss',
        mode='min',
        save_top_k=3,
        save_last=True,
        verbose=True
    )
    
    early_stopping = EarlyStopping(
        monitor='val_loss',
        patience=10,
        mode='min',
        verbose=True
    )
    
    # 6. MLflow Logger
    mlflow_logger = MLFlowLogger(
        experiment_name=experiment_name,
        tracking_uri=mlflow_config.tracking_uri,
        run_name=run_name
    )
    
    # 7. Trainer
    print("\n Configuration du Trainer...")
    trainer = pl.Trainer(
        max_epochs=max_epochs,
        accelerator=accelerator,
        devices=devices,
        callbacks=[checkpoint_callback, early_stopping],
        logger=mlflow_logger,
        enable_progress_bar=True,
        log_every_n_steps=10,
        deterministic=True
    )
    
    # 8. Démarrer le run MLflow
    with mlflow.start_run(run_name=run_name) as run:
        print(f"\n MLflow Run ID: {run.info.run_id}")
        
        # Log des hyperparamètres
        mlflow.log_params({
            'z_dim': z_dim,
            'learning_rate': learning_rate,
            'threshold_percentile': threshold_percentile,
            'max_epochs': max_epochs,
            'batch_size': batch_size,
            'seed': seed,
            'model_type': 'CNNAutoEncoder',
            'optimizer': 'Adam',
            'train_size': len(data_module.train_dataset),
            'val_test_size': len(data_module.val_dataset)
        })
        
        # 9. Entraînement
        print("\n" + "=" * 100)
        print(" DÉBUT DE L'ENTRAÎNEMENT")
        print("=" * 100)
        
        trainer.fit(model, data_module)
        
        print("\n" + "=" * 100)
        print(" ENTRAÎNEMENT TERMINÉ")
        print("=" * 100)
        
        # 10. Test
        print("\n" + "=" * 100)
        print(" ÉVALUATION SUR LE TEST SET")
        print("=" * 100)
        
        test_results = trainer.test(model, data_module)
        
        print("\n RÉSULTATS DU TEST:")
        print("-" * 100)
        for key, value in test_results[0].items():
            print(f"   {key}: {value:.4f}")
        print("-" * 100)
        
        # 11. Sauvegarder le modèle dans MLflow
        print("\n Sauvegarde du modèle dans MLflow...")
        
        # Log du modèle PyTorch
        mlflow.pytorch.log_model(
            model,
            "model",
            registered_model_name="anomaly_detection_autoencoder"
        )
        
        # Log des artifacts supplémentaires
        mlflow.log_artifact(checkpoint_callback.best_model_path, "best_checkpoint")
        
        print(f"    Modèle sauvegardé dans MLflow")
        print(f"    Best checkpoint: {checkpoint_callback.best_model_path}")
        
        # 12. Log des métriques finales
        best_metrics = {
            'best_val_loss': checkpoint_callback.best_model_score.item(),
            **test_results[0]
        }
        
        print("\n" + "=" * 100)
        print(" MÉTRIQUES FINALES")
        print("=" * 100)
        print(f"   Best Validation Loss: {best_metrics['best_val_loss']:.4f}")
        print(f"   Test AUC: {best_metrics['test_auc']:.4f}")
        print(f"   Test F1 Score: {best_metrics['test_f1_score']:.4f}")
        print(f"   Test Recall: {best_metrics['test_recall']:.4f}")
        print(f"   Test Precision: {best_metrics['test_precision']:.4f}")
        print(f"   Test Accuracy: {best_metrics['test_accuracy']:.4f}")
        print("=" * 100)
        
        return run.info.run_id, best_metrics


if __name__ == "__main__":
    print("LANCEMENT DE L'ENTRAÎNEMENT - TEST RAPIDE (1 EPOCH)")
     
    # Configuration pour test rapide
    run_id, metrics = train_model(
        z_dim=40,
        learning_rate=1e-3,
        threshold_percentile=95.0,
        max_epochs=1,  # 1 epoch pour test
        batch_size=32,
        num_workers=4,
        train_data_path="./mlops/feature_store/features/normal_spectrograms.npy",
        test_data_path="./mlops/data/test",
        experiment_name="anomaly_detection_autoencoder",
        run_name="test_run_1_epoch",
        seed=42
    )
    
    print(f"\n Run ID: {run_id}")
    print("\n Pour voir les résultats dans MLflow UI:")
    print("   mlflow ui --backend-store-uri postgresql://postgres:Chiheb12345@localhost:5432/mlflow_db")
    print("   Puis ouvrir: http://localhost:5000")