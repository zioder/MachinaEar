"""
Plot ROC curve from test results
"""
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from sklearn.metrics import roc_curve, auc
import os

def plot_roc_curve(anomaly_scores_path, output_path='results/roc_curve.png'):
    """
    Plot ROC curve from anomaly scores
    
    Args:
        anomaly_scores_path: Path to CSV file with anomaly scores
        output_path: Path to save the ROC curve plot
    """
    # Read anomaly scores
    df = pd.read_csv(anomaly_scores_path)
    
    # Extract labels from filenames
    # Files with 'anomaly' in name are anomalies (label=1)
    # Files with 'normal' in name are normal (label=0)
    df['true_label'] = df['filename'].apply(
        lambda x: 1 if 'anomaly' in x.lower() else 0
    )
    
    # Get predictions and labels
    y_true = df['true_label'].values
    y_scores = df['anomaly_score'].values
    
    # Calculate ROC curve
    fpr, tpr, thresholds = roc_curve(y_true, y_scores)
    roc_auc = auc(fpr, tpr)
    
    # Calculate pAUC (p=0.1)
    # pAUC is the area under ROC curve for FPR in [0, 0.1]
    p = 0.1
    mask = fpr <= p
    if mask.any():
        fpr_p = fpr[mask]
        tpr_p = tpr[mask]
        # Add the point at p if it's not already there
        if fpr_p[-1] < p:
            # Interpolate
            idx = np.where(fpr > p)[0][0]
            alpha = (p - fpr[idx-1]) / (fpr[idx] - fpr[idx-1])
            tpr_at_p = tpr[idx-1] + alpha * (tpr[idx] - tpr[idx-1])
            fpr_p = np.append(fpr_p, p)
            tpr_p = np.append(tpr_p, tpr_at_p)
        pauc = auc(fpr_p, tpr_p)
    else:
        pauc = 0
    
    # Normalize pAUC to [0, 1] range
    pauc_normalized = pauc / p
    
    # Create plot
    plt.figure(figsize=(10, 8))
    plt.plot(fpr, tpr, color='darkorange', lw=2, 
             label=f'ROC curve (AUC = {roc_auc:.4f})')
    plt.plot([0, 1], [0, 1], color='navy', lw=2, linestyle='--', 
             label='Random classifier')
    
    # Highlight pAUC region
    if mask.any():
        plt.fill_between(fpr_p, 0, tpr_p, alpha=0.2, color='darkorange',
                        label=f'pAUC (p={p}) = {pauc_normalized:.4f}')
        plt.axvline(x=p, color='red', linestyle=':', lw=1, 
                   label=f'FPR = {p}')
    
    plt.xlim([0.0, 1.0])
    plt.ylim([0.0, 1.05])
    plt.xlabel('False Positive Rate', fontsize=12)
    plt.ylabel('True Positive Rate', fontsize=12)
    plt.title('ROC Curve - Anomaly Detection', fontsize=14, fontweight='bold')
    plt.legend(loc="lower right", fontsize=10)
    plt.grid(True, alpha=0.3)
    
    # Add text box with statistics
    stats_text = f'AUC: {roc_auc:.4f}\npAUC (p={p}): {pauc_normalized:.4f}\n'
    stats_text += f'Normal samples: {(y_true == 0).sum()}\n'
    stats_text += f'Anomaly samples: {(y_true == 1).sum()}'
    plt.text(0.6, 0.2, stats_text, fontsize=10, 
             bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.5))
    
    plt.tight_layout()
    
    # Save plot
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    plt.savefig(output_path, dpi=300, bbox_inches='tight')
    print(f"ROC curve saved to: {output_path}")
    
    # Print statistics
    print(f"\n{'='*50}")
    print(f"ROC Curve Statistics")
    print(f"{'='*50}")
    print(f"AUC: {roc_auc:.4f}")
    print(f"pAUC (p={p}): {pauc_normalized:.4f}")
    print(f"Normal samples: {(y_true == 0).sum()}")
    print(f"Anomaly samples: {(y_true == 1).sum()}")
    print(f"Total samples: {len(y_true)}")
    print(f"{'='*50}\n")
    
    # Also show the plot
    plt.show()
    
    return roc_auc, pauc_normalized

if __name__ == '__main__':
    # Path to anomaly scores
    scores_path = 'results/anomaly_scores.csv'
    output_path = 'results/roc_curve.png'
    
    # Plot ROC curve
    auc_score, pauc_score = plot_roc_curve(scores_path, output_path)
