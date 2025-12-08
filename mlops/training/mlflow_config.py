# mlops/training/mlflow_config.py
"""
Configuration MLflow avec PostgreSQL comme backend
"""

import mlflow
import psycopg2
from pathlib import Path
import os


class MLflowConfig:
    """Configuration centralisée pour MLflow"""
    
    def __init__(
        self,
        experiment_name: str = "anomaly_detection_autoencoder",
        tracking_uri: str = "postgresql://postgres:Chiheb12345@localhost:5432/mlflow_db",
        artifact_location: str = "./mlruns"
    ):
        self.experiment_name = experiment_name
        self.tracking_uri = tracking_uri
        self.artifact_location = Path(artifact_location)
        
        # Créer le dossier des artifacts
        self.artifact_location.mkdir(parents=True, exist_ok=True)
        
    def setup_mlflow(self):
        """Configure MLflow avec PostgreSQL backend"""
        
        # Configurer le tracking URI
        mlflow.set_tracking_uri(self.tracking_uri)
        
        # Créer ou récupérer l'expérience
        try:
            experiment = mlflow.get_experiment_by_name(self.experiment_name)
            if experiment is None:
                experiment_id = mlflow.create_experiment(
                    self.experiment_name,
                    artifact_location=str(self.artifact_location)
                )
                print(f" Expérience créée: {self.experiment_name} (ID: {experiment_id})")
            else:
                experiment_id = experiment.experiment_id
                print(f" Expérience existante: {self.experiment_name} (ID: {experiment_id})")
            
            mlflow.set_experiment(self.experiment_name)
            return experiment_id
            
        except Exception as e:
            print(f"❌ Erreur lors de la configuration MLflow: {e}")
            raise
    
    @staticmethod
    def create_mlflow_database():
        """Crée la base de données MLflow dans PostgreSQL"""
        try:
            # Se connecter à la base postgres par défaut
            conn = psycopg2.connect(
                host="localhost",
                port=5432,
                database="postgres",
                user="postgres",
                password="Chiheb12345"
            )
            conn.autocommit = True
            cursor = conn.cursor()
            
            # Vérifier si la DB existe
            cursor.execute("SELECT 1 FROM pg_database WHERE datname='mlflow_db'")
            exists = cursor.fetchone()
            
            if not exists:
                cursor.execute("CREATE DATABASE mlflow_db")
                print(" Base de données mlflow_db créée")
            else:
                print(" Base de données mlflow_db existe déjà")
            
            cursor.close()
            conn.close()
            
        except Exception as e:
            print(f" Erreur lors de la création de la DB: {e}")
            raise


# Script d'initialisation
if __name__ == "__main__":
    print("=" * 80)
    print("CONFIGURATION MLFLOW")
    print("=" * 80)
    
    # 1. Créer la base de données
    print("\n Création de la base de données MLflow...")
    MLflowConfig.create_mlflow_database()
    
    # 2. Configurer MLflow
    print("\n Configuration de MLflow...")
    mlflow_config = MLflowConfig()
    mlflow_config.setup_mlflow()
    
    print("\n" + "=" * 80)
    print(" MLflow configuré avec succès!")
    print("=" * 80)
    print(f"\n Tracking URI: {mlflow_config.tracking_uri}")
    print(f" Artifact Location: {mlflow_config.artifact_location}")
    print(f" Experiment: {mlflow_config.experiment_name}")
    print("\n Pour lancer l'interface web:")
    print(f"   mlflow ui --backend-store-uri {mlflow_config.tracking_uri}")