# 3CNN Autoencoder for Anomaly Detection

This project implements a 3-layer Convolutional Autoencoder for unsupervised anomaly detection in audio data, developed for the DCASE 2020 Challenge Task 2.

## Project Overview

The model uses a convolutional autoencoder architecture to learn normal audio patterns from machine sounds. It detects anomalies by measuring reconstruction error - higher errors indicate potential anomalies.

**Key Features:**
- 3-layer convolutional encoder-decoder architecture
- Mel spectrogram feature extraction
- PyTorch Lightning for training
- Automated model checkpointing
- Comprehensive testing and evaluation


## Dataset Structure

Your data should be organized as follows:
```
../data/
├── train/
│   ├── normal_00.wav
│   ├── normal_01.wav
│   └── ...
└── test/
    ├── normal_00.wav
    ├── anomaly_00.wav
    └── ...
```

The data directory should be one level up from the ML folder (i.e., `../data/` relative to the ML folder).

## Configuration

Edit [config.yaml](config.yaml) to adjust model parameters:

```yaml
# Feature extraction parameters
feature:
  n_mels: 128          # Number of mel frequency bands
  frames: 5            # Number of frames
  n_fft: 1024          # FFT window size
  hop_length: 512      # Hop length for STFT
  power: 2.0           # Power for mel spectrogram

# Training parameters
fit:
  lr: 0.001            # Learning rate
  epochs: 100          # Number of training epochs
  batch_size: 256      # Batch size
  validation_split: 0.1 # Validation split ratio
```

## Project Structure

```
ML/
├── notebooks/              # Jupyter notebooks for EDA and experiments
│   └── data_eda.ipynb     # Exploratory data analysis
├── utils/                  # Utility modules
│   ├── __init__.py
│   ├── common.py          # Common utilities (yaml, file I/O, logging)
│   ├── pytorch_utils.py   # PyTorch utilities (datasets, Lightning)
│   └── preprocessing.py   # Data preprocessing (spectrogram conversion)
├── models/                 # Model definitions
│   ├── __init__.py
│   └── cnn_autoencoder.py # CNN Autoencoder architecture
├── training/               # Training scripts
│   ├── __init__.py
│   └── train.py           # Main training script
├── testing/                # Testing scripts
│   ├── __init__.py
│   └── test.py            # Main testing script
├── saved_models/           # Model outputs (auto-generated)
│   ├── checkpoints/       # Model checkpoints (.ckpt files)
│   ├── lightning_logs/    # PyTorch Lightning training logs
│   └── preprocessed/      # Cached preprocessed data
├── results/                # Test results and evaluation outputs
├── config.yaml             # Configuration file
├── requirements.txt        # Python dependencies
└── README.md              # This file
```

## Usage

### 1. Exploratory Data Analysis

Run the [notebooks/data_eda.ipynb](notebooks/data_eda.ipynb) notebook to explore your dataset:
```bash
jupyter notebook notebooks/data_eda.ipynb
```

This notebook provides:
- Dataset statistics
- Audio file properties
- Mel spectrogram visualizations
- Feature distribution analysis

### 2. Training

Train the model using:
```bash
cd training
python train.py
```

Or from the ML root directory:
```bash
python -m training.train
```

The script will:
- Preprocess audio files into mel spectrograms
- Cache preprocessed data for faster subsequent runs
- Train the autoencoder with validation split
- Save checkpoints automatically in `saved_models/checkpoints/`
- Keep the top 3 best models based on validation loss

**Training Output:**
- Model checkpoints: `saved_models/checkpoints/model-epoch=XX-val_loss=X.XXXX.ckpt`
- Training logs: `saved_models/lightning_logs/`
- Preprocessed data cache: `saved_models/preprocessed/preprocessed_train.npy`

### 3. Testing

Test the trained model:
```bash
cd testing
python test.py
```

Or from the ML root directory:
```bash
python -m testing.test
```

Optional arguments:
```bash
# Use specific checkpoint
python test.py --checkpoint ../saved_models/checkpoints/model-epoch=10-val_loss=0.0009.ckpt

# Use custom test directory
python test.py --test-dir /path/to/test/data
```

**Test Output:**
- `results/anomaly_scores.csv` - Per-file anomaly scores
- `results/test_results.csv` - Overall metrics (AUC, pAUC)
- Console output with detailed statistics

## Model Architecture

The CNNAutoEncoder consists of:

**Encoder:**
- Conv2D (1→32, kernel=5, stride=2) + ReLU
- Conv2D (32→64, kernel=5, stride=2) + ReLU + Dropout(0.2)
- Conv2D (64→128, kernel=3, stride=2) + ReLU + Dropout(0.3)
- Conv2D (128→256, kernel=3, stride=2) + ReLU + Dropout(0.3)
- Conv2D (256→z_dim, kernel=3) - Latent representation (z_dim=40)

**Decoder:**
- ConvTranspose2D (z_dim→256, kernel=3) + ReLU + Dropout(0.3)
- ConvTranspose2D (256→128, kernel=3, stride=2) + ReLU + Dropout(0.3)
- ConvTranspose2D (128→64, kernel=3, stride=2) + ReLU + Dropout(0.2)
- ConvTranspose2D (64→32, kernel=5, stride=2) + ReLU
- ConvTranspose2D (32→1, kernel=5, stride=2) + Sigmoid

## Module Description

### Core Modules

- **training/** - Contains training scripts
  - `train.py` - Main training script with data preprocessing and model training

- **testing/** - Contains testing and evaluation scripts
  - `test.py` - Testing script with evaluation metrics (AUC, pAUC)

- **models/** - Model architecture definitions
  - `cnn_autoencoder.py` - CNN Autoencoder implementation

- **utils/** - Utility functions organized by purpose
  - `common.py` - Common utilities (YAML loading, file I/O, logging)
  - `pytorch_utils.py` - PyTorch utilities (datasets, Lightning module, normalization)
  - `preprocessing.py` - Audio preprocessing (mel spectrogram conversion, image datasets)

- **notebooks/** - Jupyter notebooks for exploration
  - `data_eda.ipynb` - Exploratory data analysis and visualization

- **saved_models/** - Auto-generated directory for model outputs
  - `checkpoints/` - Model checkpoint files
  - `lightning_logs/` - Training logs and metrics
  - `preprocessed/` - Cached preprocessed data

- **results/** - Test results and evaluation outputs

## Evaluation Metrics

The model is evaluated using:

- **AUC (Area Under ROC Curve)**: Measures overall classification performance
  - Range: 0.5 (random) to 1.0 (perfect)
  - Target: > 0.75 for acceptable, > 0.85 for good performance

- **pAUC (Partial AUC)**: Focuses on low false positive rate region
  - Configured via `max_fpr` in config.yaml (default: 0.1)
  - Important for real-world applications where false alarms are costly

