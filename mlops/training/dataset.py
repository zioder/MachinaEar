# mlops/training/dataset.py
"""
Dataset et DataModule pour l'entraînement de l'autoencoder
"""

import torch
from torch.utils.data import Dataset, DataLoader
import pytorch_lightning as pl
import numpy as np
import librosa
from pathlib import Path
from typing import Optional, Tuple
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class SpectrogramDataset(Dataset):
    """Dataset pour les spectrogrammes"""
    
    def __init__(
        self,
        data_path: str,
        mode: str = "train",  # "train" ou "test"
        sample_rate: int = 22050,
        n_mels: int = 128,
        n_fft: int = 2048,
        hop_length: int = 512,
        duration: float = 5.0,
        target_length: int = None,  # Longueur cible pour uniformiser
        transform=None
    ):
        self.data_path = Path(data_path)
        self.mode = mode
        self.sample_rate = sample_rate
        self.n_mels = n_mels
        self.n_fft = n_fft
        self.hop_length = hop_length
        self.duration = duration
        self.target_length = target_length
        self.transform = transform
        
        # Charger les données
        if mode == "train":
            # Pour train: charger le fichier .npy des spectrogrammes normaux
            self.spectrograms = np.load(data_path)
            self.labels = np.zeros(len(self.spectrograms))  # Tous normaux
            
            # Définir la longueur cible basée sur les données d'entraînement
            if self.target_length is None:
                self.target_length = self.spectrograms.shape[2]
            
            logger.info(f"Train data loaded: {self.spectrograms.shape}")
            logger.info(f"Target length set to: {self.target_length}")
            
        elif mode == "test":
            # Pour test: charger les fichiers audio de normal/ et anomaly/
            self.spectrograms, self.labels = self._load_test_data()
            logger.info(f"Test data loaded: {len(self.spectrograms)} samples")
    
    def _load_test_data(self) -> Tuple[np.ndarray, np.ndarray]:
        """Charge les données de test depuis un dossier unique avec labels dans les noms"""
        spectrograms = []
        labels = []
        
        # Extensions audio supportées
        audio_extensions = {'.wav', '.mp3', '.flac', '.ogg'}
        
        # Lister tous les fichiers audio dans data/test/
        audio_files = [f for f in self.data_path.glob('*') if f.suffix.lower() in audio_extensions]
        
        logger.info(f"Found {len(audio_files)} audio files in {self.data_path}")
        
        normal_count = 0
        anomaly_count = 0
        
        for audio_file in audio_files:
            # Déterminer le label depuis le nom du fichier
            filename = audio_file.stem.lower()  # Nom sans extension, en minuscules
            
            # Détecter si c'est normal ou anomaly depuis le nom
            if 'normal' in filename:
                label = 0  # Normal
                normal_count += 1
            elif 'anomaly' in filename or 'abnormal' in filename:
                label = 1  # Anomaly
                anomaly_count += 1
            else:
                # Si pas de mot-clé, essayer de détecter par pattern (ex: "id_00" = normal, "id_01" = anomaly)
                # Vous pouvez adapter cette logique selon votre convention de nommage
                logger.warning(f"Cannot determine label for {audio_file.name}, skipping...")
                continue
            
            # Extraire le spectrogramme
            spec = self._extract_mel_spectrogram(str(audio_file))
            if spec is not None:
                spectrograms.append(spec)
                labels.append(label)
        
        logger.info(f"Loaded: {normal_count} normal, {anomaly_count} anomaly files")
        
        if len(spectrograms) == 0:
            raise ValueError(f"No valid audio files found in {self.data_path}. "
                           "Make sure filenames contain 'normal' or 'anomaly'.")
        
        return np.array(spectrograms), np.array(labels)
    
    def _extract_mel_spectrogram(self, audio_path: str) -> Optional[np.ndarray]:
        """Extrait le spectrogramme Mel d'un fichier audio"""
        try:
            y, sr = librosa.load(audio_path, sr=self.sample_rate, duration=self.duration)
            
            mel_spec = librosa.feature.melspectrogram(
                y=y,
                sr=sr,
                n_mels=self.n_mels,
                n_fft=self.n_fft,
                hop_length=self.hop_length
            )
            
            mel_spec_db = librosa.power_to_db(mel_spec, ref=np.max)
            mel_spec_normalized = (mel_spec_db - mel_spec_db.min()) / (mel_spec_db.max() - mel_spec_db.min())
            
            # Uniformiser la longueur temporelle si target_length est défini
            if self.target_length is not None and mel_spec_normalized.shape[1] != self.target_length:
                if mel_spec_normalized.shape[1] > self.target_length:
                    # Tronquer
                    mel_spec_normalized = mel_spec_normalized[:, :self.target_length]
                else:
                    # Padder avec des zéros
                    pad_width = self.target_length - mel_spec_normalized.shape[1]
                    mel_spec_normalized = np.pad(mel_spec_normalized, ((0, 0), (0, pad_width)), mode='constant')
            
            return mel_spec_normalized
            
        except Exception as e:
            logger.error(f"Error extracting spectrogram from {audio_path}: {e}")
            return None
    
    def __len__(self):
        return len(self.spectrograms)
    
    def __getitem__(self, idx):
        spectrogram = self.spectrograms[idx]
        label = self.labels[idx]
        
        # Ajouter dimension channel (batch, channel, height, width)
        spectrogram = torch.FloatTensor(spectrogram).unsqueeze(0)
        
        if self.transform:
            spectrogram = self.transform(spectrogram)
        
        return spectrogram, torch.FloatTensor([label])


class SpectrogramDataModule(pl.LightningDataModule):
    """DataModule PyTorch Lightning pour gérer les données"""
    
    def __init__(
        self,
        train_data_path: str = "./mlops/feature_store/features/normal_spectrograms.npy",
        test_data_path: str = "./mlops/data/test",
        batch_size: int = 32,
        num_workers: int = 4,
        **dataset_kwargs
    ):
        super().__init__()
        self.train_data_path = train_data_path
        self.test_data_path = test_data_path
        self.batch_size = batch_size
        self.num_workers = num_workers
        self.dataset_kwargs = dataset_kwargs
        self.target_length = None  # Sera défini après chargement des données train
        
    def setup(self, stage: Optional[str] = None):
        """Setup datasets"""
        
        # Toujours charger le train dataset en premier pour obtenir target_length
        if not hasattr(self, 'train_dataset'):
            self.train_dataset = SpectrogramDataset(
                data_path=self.train_data_path,
                mode="train",
                **self.dataset_kwargs
            )
            self.target_length = self.train_dataset.target_length
            logger.info(f"Train size: {len(self.train_dataset)} (all normal data)")
        
        if stage == "fit" or stage is None:
            # Train dataset déjà créé ci-dessus
            
            # Utiliser le test set comme validation
            self.val_dataset = SpectrogramDataset(
                data_path=self.test_data_path,
                mode="test",
                target_length=self.target_length,
                **self.dataset_kwargs
            )
            
            logger.info(f"Val size: {len(self.val_dataset)} (test data for validation)")
        
        if stage == "test" or stage is None:
            # Dataset de test (normal + anomaly) - même que validation
            self.test_dataset = SpectrogramDataset(
                data_path=self.test_data_path,
                mode="test",
                target_length=self.target_length,
                **self.dataset_kwargs
            )
            
            logger.info(f"Test size: {len(self.test_dataset)}")
    
    def train_dataloader(self):
        return DataLoader(
            self.train_dataset,
            batch_size=self.batch_size,
            shuffle=True,
            num_workers=self.num_workers,
            pin_memory=True
        )
    
    def val_dataloader(self):
        return DataLoader(
            self.val_dataset,
            batch_size=self.batch_size,
            shuffle=False,
            num_workers=self.num_workers,
            pin_memory=True
        )
    
    def test_dataloader(self):
        return DataLoader(
            self.test_dataset,
            batch_size=self.batch_size,
            shuffle=False,
            num_workers=self.num_workers,
            pin_memory=True
        )


# Test du dataset
if __name__ == "__main__":
    print("=" * 80)
    print("TEST DU DATASET")
    print("=" * 80)
    
    # Test du datamodule
    dm = SpectrogramDataModule(
        train_data_path="./mlops/feature_store/features/normal_spectrograms.npy",
        test_data_path="./mlops/data/test",
        batch_size=8
    )
    
    # Setup
    dm.setup("fit")
    dm.setup("test")
    
    # Test train loader
    train_loader = dm.train_dataloader()
    batch = next(iter(train_loader))
    spectrograms, labels = batch
    
    print(f"\n Train batch shape: {spectrograms.shape}")
    print(f" Labels shape: {labels.shape}")
    print(f" Labels: {labels.squeeze()}")
    
    # Test test loader
    test_loader = dm.test_dataloader()
    batch = next(iter(test_loader))
    spectrograms, labels = batch
    
    print(f"\n Test batch shape: {spectrograms.shape}")
    print(f" Labels shape: {labels.shape}")
    print(f" Labels distribution: Normal={(labels==0).sum()}, Anomaly={(labels==1).sum()}")