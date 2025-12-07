# feast_config/feature_store.yaml
# Sauvegarder ce fichier dans le dossier feast_config/

"""
project: machine_anomaly_detection
registry: postgresql://postgres:password@localhost:5432/feast_registry
provider: local
online_store:
  type: postgres
  host: localhost
  port: 5432
  database: feast_online
  db_schema: public
  user: postgres
  password: password

offline_store:
  type: file
"""

# ================================
# feast_config/features.py
# Définitions des features pour Feast (Syntaxe moderne)
# ================================

from feast import Entity, FeatureView, Field, FileSource, ValueType
from feast.types import Float32, String, Int64
from datetime import timedelta


# Définir l'entité Machine
machine = Entity(
    name="machine_id",
    value_type=ValueType.STRING,  # Utiliser ValueType.STRING
    description="Identifiant unique de la machine"
)

# Source de données (fichier Parquet généré par le feature engineering)
machine_vibration_source = FileSource(
    path="../features/normal_features.parquet",
    timestamp_field="event_timestamp",
    created_timestamp_column="created_timestamp"
)

# Feature View avec les 13 features audio
machine_audio_features = FeatureView(
    name="machine_audio_features",
    entities=[machine],
    ttl=timedelta(days=365),
    schema=[
        Field(name="rms_mean", dtype=Float32),
        Field(name="rms_std", dtype=Float32),
        Field(name="zcr_mean", dtype=Float32),
        Field(name="spectral_centroid", dtype=Float32),
        Field(name="spectral_bandwidth", dtype=Float32),
        Field(name="spectral_rolloff", dtype=Float32),
        Field(name="spectral_contrast", dtype=Float32),
        Field(name="mfcc_0", dtype=Float32),
        Field(name="mfcc_1", dtype=Float32),
        Field(name="mfcc_2", dtype=Float32),
        Field(name="mfcc_3", dtype=Float32),
        Field(name="mfcc_4", dtype=Float32),
        Field(name="mfcc_5", dtype=Float32),
    ],
    online=True,
    source=machine_vibration_source,
    tags={"team": "mlops", "project": "anomaly_detection"}
)