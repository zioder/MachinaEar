"""
Test script for 3CNN Autoencoder using Lightning checkpoint
Tests on data/test directory
"""

import os
import sys
import csv
from pathlib import Path
import numpy as np
import torch
from tqdm import tqdm
from sklearn import metrics
from argparse import Namespace, ArgumentParser

# Import local modules
sys.path.insert(0, str(Path(__file__).parent.parent))

from utils import common as com
from utils.preprocessing import file_to_vector_array_2d
from utils.pytorch_utils import ToTensor1ch, normalize_0to1
from models.cnn_autoencoder import CNNAutoEncoder


def load_lightning_checkpoint(checkpoint_path, device):
    """
    Load model from PyTorch Lightning checkpoint

    Args:
        checkpoint_path: Path to .ckpt file
        device: torch device

    Returns:
        Loaded model in eval mode
    """
    print(f"Loading checkpoint: {checkpoint_path}")

    # Load checkpoint
    checkpoint = torch.load(checkpoint_path, map_location=device)

    # Create model
    model = CNNAutoEncoder(z_dim=40).to(device)

    # Load state dict (Lightning saves model state in 'state_dict' key)
    # The keys have 'model.' prefix in Lightning, so we need to handle that
    state_dict = checkpoint['state_dict']

    # Remove 'model.' prefix from keys
    new_state_dict = {}
    for key, value in state_dict.items():
        if key.startswith('model.'):
            new_key = key[6:]  # Remove 'model.' prefix
            new_state_dict[new_key] = value

    model.load_state_dict(new_state_dict)
    model.eval()

    print(f"✓ Model loaded successfully")
    print(f"  Epoch: {checkpoint.get('epoch', 'N/A')}")

    return model


def get_test_files(test_dir):
    """
    Get test files and labels from test directory

    Args:
        test_dir: Path to test directory

    Returns:
        test_files: List of file paths
        labels: List of labels (0=normal, 1=anomaly) or None if labels can't be determined
    """
    test_path = Path(test_dir)

    if not test_path.exists():
        raise ValueError(f"Test directory not found: {test_dir}")

    # Get all WAV files
    wav_files = sorted(test_path.glob('**/*.wav'))

    if len(wav_files) == 0:
        raise ValueError(f"No WAV files found in {test_dir}")

    # Try to extract labels from filenames (normal vs anomaly)
    files = []
    labels = []
    has_labels = True

    for wav_file in wav_files:
        files.append(str(wav_file))

        # Check if filename contains 'normal' or 'anomaly'
        filename = wav_file.name.lower()
        if 'normal' in filename:
            labels.append(0)
        elif 'anomaly' in filename:
            labels.append(1)
        else:
            has_labels = False
            labels.append(-1)  # Unknown

    if not has_labels:
        print("Warning: Could not determine labels from filenames")
        labels = None

    return files, labels


def save_csv(save_file_path, save_data):
    """Save data to CSV file"""
    with open(save_file_path, "w", newline="") as f:
        writer = csv.writer(f, lineterminator='\n')
        writer.writerows(save_data)


def test_model(model, test_files, params, device):
    """
    Test model on files and compute anomaly scores

    Args:
        model: Trained model
        test_files: List of file paths
        params: Configuration parameters
        device: torch device

    Returns:
        y_pred: List of anomaly scores
        anomaly_score_list: List of [filename, score] pairs
    """
    to_tensor = ToTensor1ch(device=device, image=True)

    y_pred = []
    anomaly_score_list = []

    print(f"\nTesting on {len(test_files)} files...")

    for file_path in tqdm(test_files):
        try:
            # Load and preprocess data
            data = file_to_vector_array_2d(
                file_path,
                n_mels=params.feature.n_mels,
                steps=20,
                n_fft=params.feature.n_fft,
                hop_length=params.feature.hop_length,
                power=params.feature.power
            )

            # Normalize
            data = normalize_0to1(data)

            # Get predictions
            with torch.no_grad():
                yhat = model(to_tensor(data)).cpu().detach().numpy().reshape(data.shape)

                # Calculate reconstruction error
                errors = np.mean(np.square(data - yhat), axis=1)

            # Average error is the anomaly score
            anomaly_score = np.mean(errors)
            y_pred.append(anomaly_score)
            anomaly_score_list.append([os.path.basename(file_path), anomaly_score])

        except Exception as e:
            print(f"Error processing {file_path}: {e}")
            y_pred.append(0.0)
            anomaly_score_list.append([os.path.basename(file_path), 0.0])

    return y_pred, anomaly_score_list


def main():
    """Main test function"""

    parser = ArgumentParser(description='Test 3CNN Autoencoder')
    parser.add_argument('--checkpoint', type=str, default=None,
                        help='Path to checkpoint file (if not specified, uses best model from lightning_logs)')
    parser.add_argument('--test-dir', type=str, default=None,
                        help='Path to test directory (default: ../data/test)')
    args = parser.parse_args()

    # Load configuration
    config_path = Path(__file__).parent.parent / 'config.yaml'
    params = Namespace(**com.yaml_load(str(config_path)))
    params.feature = Namespace(**params.feature)
    params.fit = Namespace(**params.fit)

    # Setup device
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print(f"Using device: {device}")

    # Find checkpoint if not specified
    if args.checkpoint is None:
        ml_root = Path(__file__).parent.parent
        checkpoint_dir = ml_root / 'saved_models' / 'checkpoints'

        if not checkpoint_dir.exists():
            print("Error: saved_models/checkpoints directory not found")
            print("Please train the model first or specify --checkpoint")
            return

        # Look for checkpoint files
        checkpoints = list(checkpoint_dir.glob('model-*.ckpt'))

        if not checkpoints:
            checkpoints = list(checkpoint_dir.glob('*.ckpt'))

        if not checkpoints:
            print("Error: No checkpoint files found in saved_models/checkpoints/")
            print("Please train the model first or specify --checkpoint")
            return

        # Sort by val_loss in filename (lowest first)
        # Filenames like: model-epoch=XX-val_loss=0.XXXX.ckpt
        checkpoint_path = sorted(checkpoints)[0]
        print(f"Using checkpoint: {checkpoint_path}")
    else:
        checkpoint_path = Path(args.checkpoint)
        if not checkpoint_path.exists():
            print(f"Error: Checkpoint not found: {checkpoint_path}")
            return

    # Load model
    model = load_lightning_checkpoint(checkpoint_path, device)

    # Setup test directory
    if args.test_dir:
        test_dir = Path(args.test_dir)
    else:
        ml_root = Path(__file__).parent.parent
        test_dir = ml_root.parent / 'data' / 'test'

    print(f"\nTest directory: {test_dir}")

    # Get test files
    try:
        test_files, labels = get_test_files(test_dir)
        print(f"Found {len(test_files)} test files")

        if labels is not None:
            n_normal = sum(1 for l in labels if l == 0)
            n_anomaly = sum(1 for l in labels if l == 1)
            print(f"  Normal: {n_normal}")
            print(f"  Anomaly: {n_anomaly}")
    except ValueError as e:
        print(f"Error: {e}")
        return

    # Test model
    print("\n" + "="*80)
    print("Starting testing...")
    print("="*80)

    y_pred, anomaly_score_list = test_model(model, test_files, params, device)

    # Setup result directory
    ml_root = Path(__file__).parent.parent
    result_dir = ml_root / 'results'
    result_dir.mkdir(exist_ok=True)

    # Save anomaly scores
    anomaly_score_file = result_dir / 'anomaly_scores.csv'
    save_csv(anomaly_score_file, [['filename', 'anomaly_score']] + anomaly_score_list)
    print(f"\n✓ Anomaly scores saved to: {anomaly_score_file}")

    # Calculate metrics if labels are available
    if labels is not None:
        print("\n" + "="*80)
        print("Performance Metrics:")
        print("="*80)

        try:
            auc = metrics.roc_auc_score(labels, y_pred)
            print(f"AUC: {auc:.4f}")

            # Calculate pAUC (partial AUC with max_fpr)
            p_auc = metrics.roc_auc_score(labels, y_pred, max_fpr=params.max_fpr)
            print(f"pAUC (max_fpr={params.max_fpr}): {p_auc:.4f}")

            # Save results to CSV
            result_file = result_dir / 'test_results.csv'
            result_data = [
                ['Metric', 'Value'],
                ['AUC', auc],
                ['pAUC', p_auc],
                ['Checkpoint', str(checkpoint_path)],
                ['Test Directory', str(test_dir)],
                ['Number of Test Files', len(test_files)]
            ]
            save_csv(result_file, result_data)
            print(f"\n✓ Test results saved to: {result_file}")

        except Exception as e:
            print(f"Error calculating metrics: {e}")
    else:
        print("\nNote: Labels not available, skipping metric calculation")

    # Print statistics
    print("\n" + "="*80)
    print("Anomaly Score Statistics:")
    print("="*80)
    scores = np.array(y_pred)
    print(f"Mean:   {scores.mean():.6f}")
    print(f"Std:    {scores.std():.6f}")
    print(f"Min:    {scores.min():.6f}")
    print(f"Max:    {scores.max():.6f}")
    print(f"Median: {np.median(scores):.6f}")

    # If labels available, show score distribution
    if labels is not None:
        normal_scores = scores[np.array(labels) == 0]
        anomaly_scores = scores[np.array(labels) == 1]

        print("\nNormal samples:")
        print(f"  Mean: {normal_scores.mean():.6f} ± {normal_scores.std():.6f}")
        print(f"  Range: [{normal_scores.min():.6f}, {normal_scores.max():.6f}]")

        print("\nAnomaly samples:")
        print(f"  Mean: {anomaly_scores.mean():.6f} ± {anomaly_scores.std():.6f}")
        print(f"  Range: [{anomaly_scores.min():.6f}, {anomaly_scores.max():.6f}]")

    print("\n" + "="*80)
    print("Testing completed!")
    print("="*80)


if __name__ == '__main__':
    main()
