"""
Training script for 3CNN Autoencoder
Adapted from 00-train-with-visual.ipynb
"""

import sys
import os
from pathlib import Path
import random
import numpy as np
import torch
import pytorch_lightning as pl
from pytorch_lightning.callbacks import ModelCheckpoint
from argparse import Namespace

# Import local modules
import sys
sys.path.insert(0, str(Path(__file__).parent.parent))

from utils import common as com
from utils.preprocessing import Task2ImageDataset, file_to_vector_array_2d
from utils.pytorch_utils import ToTensor1ch
from models.cnn_autoencoder import CNNAutoEncoder


def deterministic_everything(seed=2020):
    """Set seeds for reproducibility"""
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)
    torch.cuda.manual_seed_all(seed)
    torch.backends.cudnn.deterministic = True
    torch.backends.cudnn.benchmark = False


def preprocess_data(data_dir, output_file, params, data_type='train'):
    """
    Preprocess WAV files to numpy arrays

    Args:
        data_dir: Directory containing WAV files (e.g., 'data/train')
        output_file: Path to save the preprocessed .npy file
        params: Configuration parameters
        data_type: 'train' or 'test'
    """
    if Path(output_file).exists():
        print(f"Loading cached preprocessed data from {output_file}")
        return np.load(output_file)

    print(f"Preprocessing {data_type} data from {data_dir}...")

    # Get all WAV files
    wav_files = sorted(Path(data_dir).glob('**/*.wav'))

    if len(wav_files) == 0:
        raise ValueError(f"No WAV files found in {data_dir}")

    print(f"Found {len(wav_files)} WAV files")

    # Process each file
    all_vectors = []
    for i, wav_file in enumerate(wav_files):
        if i % 100 == 0:
            print(f"Processing file {i+1}/{len(wav_files)}")

        vectors = file_to_vector_array_2d(
            str(wav_file),
            n_mels=params.feature.n_mels,
            steps=20,  # step size for sliding window
            n_fft=params.feature.n_fft,
            hop_length=params.feature.hop_length,
            power=params.feature.power
        )

        if len(vectors) > 0:
            all_vectors.append(vectors)

    # Concatenate all vectors
    data = np.concatenate(all_vectors, axis=0)
    print(f"Preprocessed data shape: {data.shape}")

    # Save to file
    np.save(output_file, data)
    print(f"Saved preprocessed data to {output_file}")

    return data


class Task2ImageLightning(pl.LightningModule):
    """Task2 PyTorch Lightning class for image-based training"""

    def __init__(self, device, model, params, preprocessed_file, normalize=True):
        super().__init__()
        self.device_type = device
        self.params = params
        self.normalize = normalize
        self.model = model
        self.mseloss = torch.nn.MSELoss()

        # Create datasets
        to_tensor = ToTensor1ch(device=device, image=True)
        self.trn_ds = Task2ImageDataset(
            preprocessed_file,
            transform=to_tensor,
            normalize=normalize,
            random=True
        )
        self.val_ds = Task2ImageDataset(
            preprocessed_file,
            transform=to_tensor,
            normalize=normalize,
            random=False
        )

        # Split data
        train_index = self.trn_ds.get_index_by_pct(self.params.fit.validation_split)
        self.trn_ds.train_split(train_index)
        self.val_ds.val_split(train_index)

    def forward(self, x):
        return self.model(x)

    def training_step(self, batch, batch_idx):
        x, y = batch
        y_hat = self.forward(x)
        loss = self.mseloss(y_hat, y)
        self.log('train_loss', loss, prog_bar=True)
        return loss

    def validation_step(self, batch, batch_idx):
        x, y = batch
        y_hat = self.forward(x)
        loss = self.mseloss(y_hat, y)
        self.log('val_loss', loss, prog_bar=True)
        return loss

    def configure_optimizers(self):
        return torch.optim.Adam(
            self.parameters(),
            lr=self.params.fit.lr,
            betas=(self.params.fit.b1, self.params.fit.b2),
            weight_decay=self.params.fit.weight_decay
        )

    def train_dataloader(self):
        return torch.utils.data.DataLoader(
            self.trn_ds,
            batch_size=self.params.fit.batch_size,
            shuffle=self.params.fit.shuffle,
            num_workers=0  # Set to 0 for Windows compatibility
        )

    def val_dataloader(self):
        return torch.utils.data.DataLoader(
            self.val_ds,
            batch_size=self.params.fit.batch_size,
            shuffle=False,
            num_workers=0  # Set to 0 for Windows compatibility
        )


def main():
    """Main training function"""

    # Load configuration
    config_path = Path(__file__).parent.parent / 'config.yaml'
    params = Namespace(**com.yaml_load(str(config_path)))

    # Convert dict to Namespace for nested attributes
    params.feature = Namespace(**params.feature)
    params.fit = Namespace(**params.fit)

    # Set random seeds for reproducibility
    deterministic_everything(2020)

    # Setup device
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    if device.type == 'cuda':
        print(f"Using GPU: {torch.cuda.get_device_name(0)}")
        print(f"CUDA version: {torch.version.cuda}")
    else:
        print("WARNING: CUDA not available, using CPU")

    # Setup directories
    ml_root = Path(__file__).parent.parent
    preprocessed_dir = ml_root / 'saved_models' / 'preprocessed'
    preprocessed_dir.mkdir(parents=True, exist_ok=True)

    # Data directory (using the actual data structure)
    train_data_dir = ml_root / 'data' / 'dcase2025t2' / 'dev_data' / 'raw' / 'CustomMachine' / 'train'

    if not train_data_dir.exists():
        raise ValueError(f"Training data directory not found: {train_data_dir}")

    # Preprocessed data file
    preprocessed_file = preprocessed_dir / 'preprocessed_train.npy'

    # Preprocess data if needed
    data = preprocess_data(
        train_data_dir,
        preprocessed_file,
        params,
        data_type='train'
    )

    print(f"\nDataset info:")
    print(f"  Total samples: {len(data)}")
    print(f"  Sample shape: {data[0].shape}")
    print(f"  Data type: {data.dtype}")

    # Create model
    print("\nInitializing model...")
    model = CNNAutoEncoder(z_dim=40)

    # Create Lightning module
    lightning_model = Task2ImageLightning(
        device=device,
        model=model,
        params=params,
        preprocessed_file=preprocessed_file,
        normalize=True
    )

    print(f"\nTraining dataset size: {len(lightning_model.trn_ds)}")
    print(f"Validation dataset size: {len(lightning_model.val_ds)}")

    # Setup callbacks for checkpointing
    # Combined checkpoint: saves top 3 models
    # PyTorch Lightning will automatically save them with their metrics in the filename
    checkpoint_dir = ml_root / 'saved_models' / 'checkpoints'
    checkpoint_dir.mkdir(parents=True, exist_ok=True)

    checkpoint_callback = ModelCheckpoint(
        dirpath=checkpoint_dir,
        filename='model-{epoch:02d}-{val_loss:.4f}',
        monitor='val_loss',
        mode='min',
        save_top_k=3,
        save_last=True  # Also save the last model
    )

    # Setup trainer
    lightning_logs_dir = ml_root / 'saved_models' / 'lightning_logs'
    lightning_logs_dir.mkdir(parents=True, exist_ok=True)

    trainer = pl.Trainer(
        max_epochs=params.fit.epochs,
        accelerator='gpu' if torch.cuda.is_available() else 'cpu',
        devices=1,
        callbacks=[checkpoint_callback],
        default_root_dir=lightning_logs_dir,
        log_every_n_steps=10,
        enable_progress_bar=True,
        enable_model_summary=True
    )

    # Train model
    print("\n" + "="*80)
    print("Starting training...")
    print("="*80 + "\n")

    trainer.fit(lightning_model)

    print("\n" + "="*80)
    print("Training completed!")
    print(f"Best model saved at: {checkpoint_callback.best_model_path}")
    print(f"Checkpoints saved in: {checkpoint_callback.dirpath}")
    print(f"Top 3 models with lowest val_loss are kept")
    print("="*80)


if __name__ == '__main__':
    main()
