import pandas as pd
from feast import FeatureStore
from datetime import datetime
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class FeastFeatureManager:
    """
    Gestionnaire pour les opérations Feast
    """
    
    def __init__(self, repo_path: str = "./feast_config"):
        """
        Initialise le gestionnaire Feast
        
        Args:
            repo_path: Chemin vers le dossier contenant feature_store.yaml
        """
        self.store = FeatureStore(repo_path=repo_path)
        
    def materialize_features(self, start_date: datetime, end_date: datetime):
        """
        Matérialise les features dans le online store
        
        Args:
            start_date: Date de début
            end_date: Date de fin
        """
        try:
            logger.info(f"Matérialisation des features de {start_date} à {end_date}...")
            self.store.materialize(start_date=start_date, end_date=end_date)
            logger.info("✓ Matérialisation terminée avec succès")
        except Exception as e:
            logger.error(f"Erreur lors de la matérialisation: {e}")
            raise
    
    def get_online_features(self, machine_ids: list) -> pd.DataFrame:
        """
        Récupère les 13 features online pour des machines spécifiques
        
        Args:
            machine_ids: Liste des IDs de machines
            
        Returns:
            DataFrame avec les 13 features
        """
        try:
            entity_rows = [{"machine_id": mid} for mid in machine_ids]
            
            feature_vector = self.store.get_online_features(
                features=[
                    "machine_audio_features:rms_mean",
                    "machine_audio_features:rms_std",
                    "machine_audio_features:zcr_mean",
                    "machine_audio_features:spectral_centroid",
                    "machine_audio_features:spectral_bandwidth",
                    "machine_audio_features:spectral_rolloff",
                    "machine_audio_features:spectral_contrast",
                    "machine_audio_features:mfcc_0",
                    "machine_audio_features:mfcc_1",
                    "machine_audio_features:mfcc_2",
                    "machine_audio_features:mfcc_3",
                    "machine_audio_features:mfcc_4",
                    "machine_audio_features:mfcc_5",
                ],
                entity_rows=entity_rows
            ).to_df()
            
            return feature_vector
            
        except Exception as e:
            logger.error(f"Erreur lors de la récupération des features online: {e}")
            raise
    
    def get_historical_features(
        self,
        entity_df: pd.DataFrame,
        features: list = None
    ) -> pd.DataFrame:
        """
        Récupère les 13 features historiques pour l'entraînement
        
        Args:
            entity_df: DataFrame avec machine_id et event_timestamp
            features: Liste des features à récupérer (None = toutes les 13)
            
        Returns:
            DataFrame avec les features historiques
        """
        try:
            if features is None:
                features = [
                    "machine_audio_features:rms_mean",
                    "machine_audio_features:rms_std",
                    "machine_audio_features:zcr_mean",
                    "machine_audio_features:spectral_centroid",
                    "machine_audio_features:spectral_bandwidth",
                    "machine_audio_features:spectral_rolloff",
                    "machine_audio_features:spectral_contrast",
                    "machine_audio_features:mfcc_0",
                    "machine_audio_features:mfcc_1",
                    "machine_audio_features:mfcc_2",
                    "machine_audio_features:mfcc_3",
                    "machine_audio_features:mfcc_4",
                    "machine_audio_features:mfcc_5",
                ]
            
            training_df = self.store.get_historical_features(
                entity_df=entity_df,
                features=features
            ).to_df()
            
            return training_df
            
        except Exception as e:
            logger.error(f"Erreur lors de la récupération des features historiques: {e}")
            raise


# ================================
# Script d'initialisation Feast
# ================================

def setup_feast():
    """
    Configure et initialise Feast avec PostgreSQL
    """
    import subprocess
    import os
    from pathlib import Path
    
    feast_dir = Path("./mlops/feature_store/feast_config")
    feast_dir.mkdir(exist_ok=True)
    
    # Changer vers le dossier feast
    os.chdir(feast_dir)
    
    print("Initialisation du projet Feast...")
    
    # Apply des définitions de features
    try:
        subprocess.run(["feast", "apply"], check=True)
        print(" Features enregistrées dans le registry")
    except subprocess.CalledProcessError as e:
        print(f" Erreur lors de feast apply: {e}")
        return False
    
    os.chdir("..")
    return True


def materialize_initial_features():
    """
    Matérialise les features initiales dans le online store
    """
    from datetime import datetime, timedelta
    import pandas as pd
    
    manager = FeastFeatureManager()
    
    # Lire les données pour obtenir la vraie plage de dates
    df = pd.read_parquet("./features/normal_features.parquet")
    
    # Utiliser les vraies dates min/max des données
    start_date = df['event_timestamp'].min()
    end_date = df['event_timestamp'].max() + timedelta(hours=1)
    
    print(f"\n Matérialisation des features...")
    print(f"   Période réelle des données: {start_date} -> {end_date}")
    print(f"   Nombre d'enregistrements: {len(df)}")
    
    manager.materialize_features(start_date, end_date)
    print(" Features matérialisées dans le online store")


def test_feature_retrieval():
    """
    Test de récupération des features avec vérification
    """
    import pandas as pd
    manager = FeastFeatureManager()
    
    # Charger les données pour obtenir des machine_ids
    df = pd.read_parquet("./features/normal_features.parquet")
    sample_machines = df['machine_id'].head(3).tolist()
    
    print(f"\n Test de récupération des features pour: {sample_machines}")
    
    # Récupérer les features online
    features_df = manager.get_online_features(sample_machines)
    print("\n Features online récupérées:")
    print(features_df)
    
    # Vérifier si les valeurs sont None
    null_count = features_df.isnull().sum().sum()
    total_values = features_df.shape[0] * (features_df.shape[1] - 1)  # -1 pour machine_id
    
    if null_count == total_values:
        print("\n  ATTENTION: Toutes les features online sont NULL!")
        print("   Causes possibles:")
        print("   1. La période de matérialisation ne correspond pas aux timestamps")
        print("   2. Les machine_id ne matchent pas")
        print("   3. Problème de connexion PostgreSQL")
        
        # Vérifier dans PostgreSQL
        print("\n Vérification dans PostgreSQL...")
        check_postgres_data()
    else:
        print(f"\n {total_values - null_count}/{total_values} valeurs récupérées avec succès")
    
    # Récupérer les features historiques
    entity_df = df[['machine_id', 'event_timestamp']].head(5)
    historical_df = manager.get_historical_features(entity_df)
    print("\n Features historiques récupérées:")
    print(historical_df.head())


def check_postgres_data():
    """
    Vérifie directement les données dans PostgreSQL
    """
    try:
        import psycopg2
        import pandas as pd
        from sqlalchemy import create_engine
        
        # Configuration PostgreSQL
        conn_string = "postgresql://postgres:Chiheb12345@localhost:5432/feast_online"
        
        engine = create_engine(conn_string)
        
        # Lister les tables
        query = """
        SELECT table_name 
        FROM information_schema.tables 
        WHERE table_schema = 'public'
        ORDER BY table_name
        """
        tables = pd.read_sql(query, engine)
        print(f"\n Tables disponibles dans feast_online:")
        for table in tables['table_name'].tolist():
            print(f"   - {table}")
        
        # Chercher la table des features (le nom peut varier)
        feature_tables = [t for t in tables['table_name'].tolist() if 'machine_audio' in t.lower() or 'mlops' in t.lower()]
        
        if feature_tables:
            for table in feature_tables:
                print(f"\n Table: {table}")
                count_query = f"SELECT COUNT(*) as count FROM {table}"
                count_df = pd.read_sql(count_query, engine)
                print(f"   Enregistrements: {count_df['count'].iloc[0]}")
                
                # Afficher quelques lignes
                sample_query = f"SELECT * FROM {table} LIMIT 5"
                sample_df = pd.read_sql(sample_query, engine)
                print(f"\n   Colonnes: {sample_df.columns.tolist()}")
                print(f"\n   Échantillon:")
                print(sample_df)
        else:
            print("\n  Aucune table de features trouvée!")
            print("   Tables disponibles:", tables['table_name'].tolist())
        
        engine.dispose()
        
    except Exception as e:
        print(f" Erreur lors de la vérification PostgreSQL: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    print("=" * 60)
    print("Configuration Feast pour Détection d'Anomalies")
    print("=" * 60)
    
    # Étape 1: Setup Feast
    if setup_feast():
        print("\n Feast configuré avec succès")
        
        # Étape 2: Matérialiser les features
        materialize_initial_features()
        
        # Étape 3: Test de récupération
        test_feature_retrieval()
        
        print("\n" + "=" * 60)
        print(" Configuration terminée avec succès!")
        print("=" * 60)
    else:
        print("\n Erreur lors de la configuration")