#  Détection d'Anomalies dans les Machines - Projet MLOps

Système de détection d'anomalies en temps réel basé sur l'analyse des vibrations de machines industrielles utilisant des techniques d'apprentissage profond non supervisé.

##  Table des Matières

- [Vue d'Ensemble](#vue-densemble)
- [Architecture](#architecture)
- [Installation](#installation)
- [Partie 1 : Feature Engineering & Feature Store](#partie-1--feature-engineering--feature-store)
- [Partie 2 : Training & MLflow](#partie-2--training--mlflow)
- [Structure du Projet](#structure-du-projet)
- [Utilisation](#utilisation)
- [Configuration](#configuration)
- [Troubleshooting](#troubleshooting)

---

##  Vue d'Ensemble

Ce projet implémente un pipeline MLOps complet pour la détection d'anomalies dans les machines industrielles à partir de signaux audio de vibrations.

### Approche Technique

- **Méthode** : Apprentissage non supervisé (Autoencoder)
- **Features** : Spectrogrammes Mel + Features statistiques
- **Feature Store** : Feast avec PostgreSQL
- **Training** : PyTorch Lightning + MLflow
- **Détection** : Erreur de reconstruction > seuil

### Cas d'Usage

 Maintenance prédictive  
 Détection de pannes  
 Surveillance en temps réel  
 Réduction des temps d'arrêt  

---

##  Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    PIPELINE MLOPS COMPLET                       │
└─────────────────────────────────────────────────────────────────┘

┌──────────────────────┐
│  1. DATA INGESTION   │
│  Fichiers Audio .wav │
│  Vibrations machines │
└──────────┬───────────┘
           ↓
┌──────────────────────────────────────────────────────────────┐
│  2. FEATURE ENGINEERING                                      │
│                                                              │
│  A. Spectrogrammes Mel (128 bandes)                         │
│     → Sauvegardés en .npy pour training                     │
│                                                              │
│  B. 13 Features Statistiques                                │
│     → rms_mean, spectral_centroid, mfcc_0-5, etc.          │
│     → Sauvegardés en Parquet                                │
└──────────┬───────────────────────────────────────────────────┘
           ↓
┌──────────────────────────────────────────────────────────────┐
│  3. FEATURE STORE (FEAST + POSTGRESQL)                       │
│                                                              │
│  Offline Store (Parquet)     Online Store (PostgreSQL)      │
│  ┌──────────────────┐        ┌──────────────────┐          │
│  │ Données historiques│  ══►  │ Latence < 10ms   │          │
│  │ Pour training     │ Matér.│ Pour production  │          │
│  └──────────────────┘        └──────────────────┘          │
└──────────┬───────────────────────────────────────────────────┘
           ↓
┌──────────────────────────────────────────────────────────────┐
│  4. MODEL TRAINING (CNN AUTOENCODER)                         │
│                                                              │
│  PyTorch Lightning + MLflow                                  │
│  ┌────────┐   ┌──────────┐   ┌────────┐                    │
│  │Encoder │ → │  Latent  │ → │Decoder │                    │
│  │        │   │  Space   │   │        │                    │
│  └────────┘   └──────────┘   └────────┘                    │
│                                                              │
│  Loss: MSE(input, reconstruction)                            │
│  Métriques: AUC, F1, Recall, Precision                      │
└──────────┬───────────────────────────────────────────────────┘
           ↓
┌──────────────────────────────────────────────────────────────┐
│  5. MODEL REGISTRY (MLFLOW)                                  │
│                                                              │
│  PostgreSQL Backend                                          │
│  ├─ Experiments tracking                                     │
│  ├─ Hyperparameters logging                                  │
│  ├─ Metrics comparison                                       │
│  └─ Model versioning                                         │
└──────────┬───────────────────────────────────────────────────┘
           ↓
┌──────────────────────────────────────────────────────────────┐
│  6. INFERENCE (TEMPS RÉEL)                                   │
│                                                              │
│  Nouveau signal → Features → Feast → Model → Anomalie?      │
│                      (< 10ms)         (< 50ms)              │
└──────────────────────────────────────────────────────────────┘
```

---

##  Installation

### Prérequis

- Python 3.10+
- PostgreSQL 12+
- 8GB RAM minimum
- GPU recommandé (optionnel)

 

### 2. Créer l'Environnement Virtuel

```bash
python -m venv venv

# Windows
venv\Scripts\activate

# Linux/Mac
source venv/bin/activate
```

### 3. Installer les Dépendances

```bash
pip install -r requirements.txt
```

### 4. Configurer PostgreSQL

```sql
-- Se connecter à PostgreSQL
psql -U postgres

-- Créer les bases de données
CREATE DATABASE feast_online;
CREATE DATABASE mlflow_db;

-- Vérifier
\l
```

---

##  Partie 1 : Feature Engineering & Feature Store

### Objectif

Extraire les features audio et les stocker dans un Feature Store pour un accès rapide.

### 1.1 Feature Engineering

**Script** : `mlops/feature_store/feature_engineering.py`

**Ce qu'il fait** :
- Lit les fichiers audio normaux (`data/normal/*.wav`)
- Extrait des spectrogrammes Mel (128 bandes)
- Calcule 13 features statistiques
- Sauvegarde tout dans `mlops/feature_store/features/`

**Exécution** :

```bash
# Placer vos fichiers audio dans data/normal/
python mlops/feature_store/feature_engineering.py
```

**Output** :
```
features/
├── normal_spectrograms.npy    # (N, 128, temps) pour l'autoencoder
├── normal_features.parquet    # 13 features pour Feast
└── normal_features.csv        # Backup
```

### 1.2 Configuration Feast

**Script** : `mlops/feature_store/feast_config/features.py`

**Configuration** : `mlops/feature_store/feast_config/feature_store.yaml`

```yaml
project: mlops
registry: registry.db
provider: local

online_store:
  type: postgres
  host: localhost
  port: 5432
  database: feast_online
  user: postgres
  password: VOTRE_PASSWORD

offline_store:
  type: file
```

**Initialisation** :

```bash
cd mlops/feature_store/feast_config
feast apply
cd ../../..
```

### 1.3 Matérialisation des Features

**Script** : `mlops/feature_store/feast_operations.py`

```bash
python mlops/feature_store/feast_operations.py
```

**Résultat** :
- 13 features × N machines dans PostgreSQL (online store)
- Accessibles en < 10ms pour l'inférence

### 1.4 Les 13 Features

| # | Feature | Description |
|---|---------|-------------|
| 1-2 | `rms_mean`, `rms_std` | Énergie du signal |
| 3 | `zcr_mean` | Taux de passage par zéro |
| 4 | `spectral_centroid` | Centre de masse spectral |
| 5 | `spectral_bandwidth` | Largeur de bande |
| 6 | `spectral_rolloff` | Concentration d'énergie |
| 7 | `spectral_contrast` | Contraste spectral |
| 8-13 | `mfcc_0` à `mfcc_5` | Coefficients MFCC |

### 1.5 Utilisation du Feature Store

```python
from feast import FeatureStore

store = FeatureStore(repo_path="./mlops/feature_store/feast_config")

# Online features (temps réel)
features = store.get_online_features(
    features=["machine_audio_features:rms_mean", ...],
    entity_rows=[{"machine_id": "machine_001"}]
).to_df()

# Historical features (training)
historical = store.get_historical_features(
    entity_df=entity_df,
    features=[...]
).to_df()
```

---

##  Partie 2 : Training & MLflow

### Objectif

Entraîner un CNN Autoencoder pour apprendre les patterns normaux et détecter les anomalies.

### 2.1 Configuration MLflow

**Script** : `mlops/training/mlflow_config.py`

```bash
# Créer la base de données MLflow
python mlops/training/mlflow_config.py
```

### 2.2 Préparation des Données de Test

**Structure requise** :

```
data/test/
├── normal_001.wav
├── normal_002.wav
├── anomaly_001.wav
├── anomaly_002.wav
└── ...
```

**Important** : Les fichiers doivent contenir "normal" ou "anomaly" dans leur nom pour la détection automatique des labels.

### 2.3 Entraînement

**Script** : `mlops/training/train.py`

```bash
# Test rapide (1 epoch)
python mlops/training/train.py

# Production (50 epochs)
# Modifier max_epochs=50 dans train.py
```

**Hyperparamètres configurables** :

```python
train_model(
    z_dim=40,                    # Dimension du latent space
    learning_rate=1e-3,          # Learning rate
    threshold_percentile=95.0,   # Seuil de détection (95ème percentile)
    max_epochs=50,               # Nombre d'epochs
    batch_size=32,               # Taille des batchs
    accelerator="auto",          # "gpu" ou "cpu"
)
```

### 2.4 Architecture du Modèle

```
Input: (batch, 1, 128, temps)
    ↓
┌─────────────────────┐
│ Encoder             │
│ Conv2D(1→32)        │
│ Conv2D(32→64)       │
│ Conv2D(64→128)      │
│ Conv2D(128→256)     │
│ Conv2D(256→z_dim)   │
└──────────┬──────────┘
           ↓
    [Latent Space]
    (compressed)
           ↓
┌──────────┴──────────┐
│ Decoder             │
│ ConvT2D(z_dim→256)  │
│ ConvT2D(256→128)    │
│ ConvT2D(128→64)     │
│ ConvT2D(64→32)      │
│ ConvT2D(32→1)       │
└─────────────────────┘
    ↓
Output: (batch, 1, 128, temps)
```

### 2.5 Métriques Trackées

**Pendant Training** :
- `train_loss` : MSE de reconstruction
- `val_loss` : Loss sur test set
- `threshold` : Seuil calculé

**Test Final** :
- **AUC** : Area Under ROC Curve (0-1)
- **F1 Score** : Balance Precision/Recall
- **Recall** : Taux de détection des anomalies
- **Precision** : Taux de vraies détections
- **Accuracy** : Précision globale
- Confusion Matrix (TP, FP, TN, FN)

### 2.6 MLflow UI

```bash
mlflow ui --backend-store-uri postgresql://postgres:PASSWORD@localhost:5432/mlflow_db
```

Ouvrir : **http://localhost:5000**

**Fonctionnalités** :
- Comparaison des runs
- Visualisation des métriques
- Recherche par hyperparamètres
- Model Registry
- Tagging et versioning
 

---

##  Structure du Projet

```
mlops/
├── feature_store/
│   ├── features/
│   │   ├── normal_spectrograms.npy      # Spectrogrammes pour training
│   │   ├── normal_features.parquet      # Features pour Feast
│   │   └── normal_features.csv
│   ├── feast_config/
│   │   ├── feature_store.yaml           # Config Feast
│   │   ├── features.py                  # Définition des features
│   │   └── registry.db                  # Registry SQLite
│   ├── feature_engineering.py           # Extraction features
│   └── feast_operations.py              # Opérations Feast
├── training/
│   ├── mlflow_config.py                 # Configuration MLflow
│   ├── dataset.py                       # Dataset PyTorch
│   ├── model.py                         # CNN Autoencoder
│   └── train.py                         # Script training
data/
├── normal/                              # Fichiers audio normaux (training)
│   ├── normal_001.wav
│   └── ...
└── test/                                # Fichiers audio test (normal + anomaly)
    ├── normal_test_001.wav
    ├── anomaly_test_001.wav
    └── ...
checkpoints/                             # Checkpoints PyTorch Lightning
mlruns/                                  # Artifacts MLflow
requirements.txt                         # Dépendances Python
README.md                                # Ce fichier
```

---

## 🚀Utilisation

### Workflow Complet

```bash
# 1. Feature Engineering
python mlops/feature_store/feature_engineering.py

# 2. Configuration Feast
cd mlops/feature_store/feast_config
feast apply
cd ../../..

# 3. Matérialisation
python mlops/feature_store/feast_operations.py

# 4. Configuration MLflow
python mlops/training/mlflow_config.py

# 5. Training (test rapide)
python mlops/training/train.py

# 6. MLflow UI
mlflow ui --backend-store-uri postgresql://postgres:PASSWORD@localhost:5432/mlflow_db
```

 

##  Configuration

### PostgreSQL

**Modifier les mots de passe** :

1. `mlops/feature_store/feast_config/feature_store.yaml`
2. `mlops/training/mlflow_config.py`

### Hyperparamètres

**Fichier** : `mlops/training/train.py`

```python
# Ajuster selon vos besoins
z_dim = 40                      # Compression (20-60)
learning_rate = 1e-3            # LR (1e-4 à 1e-2)
threshold_percentile = 95.0     # Sensibilité (90-99)
max_epochs = 50                 # Durée training
batch_size = 32                 # Selon RAM/GPU
```

 

##  Prochaines Étapes

- [ ] API FastAPI pour inférence temps réel
- [ ] Dashboard de monitoring (Grafana)
- [ ] Pipeline CI/CD (GitHub Actions)
- [ ] Détection de drift
- [ ] Retraining automatique
- [ ] Déploiement Docker/Kubernetes

---

##  Ressources

- [Feast Documentation](https://docs.feast.dev/)
- [MLflow Documentation](https://mlflow.org/docs/latest/index.html)
- [PyTorch Lightning](https://lightning.ai/docs/pytorch/stable/)
- [Librosa Audio Processing](https://librosa.org/)

---
 