import librosa
import librosa.display
import numpy as np
import pandas as pd
from pathlib import Path
from datetime import datetime, timedelta
import json
from typing import List, Tuple
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class AudioFeatureExtractor:
    """
    Extracteur de features audio pour la détection d'anomalies
    Génère des spectrogrammes Mel et d'autres features temporelles
    """
    
    def __init__(
        self,
        sample_rate: int = 22050,
        n_mels: int = 128,
        n_fft: int = 2048,
        hop_length: int = 512,
        duration: float = 5.0
    ):
        self.sample_rate = sample_rate
        self.n_mels = n_mels
        self.n_fft = n_fft
        self.hop_length = hop_length
        self.duration = duration
        
    def extract_mel_spectrogram(self, audio_path: str) -> np.ndarray:
        """
        Extrait le spectrogramme Mel d'un fichier audio
        
        Args:
            audio_path: Chemin vers le fichier audio
            
        Returns:
            Spectrogramme Mel normalisé
        """
        try:
            # Charger l'audio
            y, sr = librosa.load(audio_path, sr=self.sample_rate, duration=self.duration)
            
            # Générer le spectrogramme Mel
            mel_spec = librosa.feature.melspectrogram(
                y=y,
                sr=sr,
                n_mels=self.n_mels,
                n_fft=self.n_fft,
                hop_length=self.hop_length
            )
            
            # Convertir en échelle logarithmique (dB)
            mel_spec_db = librosa.power_to_db(mel_spec, ref=np.max)
            
            # Normaliser entre 0 et 1
            mel_spec_normalized = (mel_spec_db - mel_spec_db.min()) / (mel_spec_db.max() - mel_spec_db.min())
            
            return mel_spec_normalized
            
        except Exception as e:
            logger.error(f"Erreur lors de l'extraction du spectrogramme pour {audio_path}: {e}")
            return None
    
    def extract_statistical_features(self, audio_path: str) -> dict:
        """
        Extrait 13 features statistiques pour Feast
        
        Args:
            audio_path: Chemin vers le fichier audio
            
        Returns:
            Dictionnaire de 13 features
        """
        try:
            y, sr = librosa.load(audio_path, sr=self.sample_rate, duration=self.duration)
            
            # 13 features sélectionnées
            features = {
                'rms_mean': float(np.mean(librosa.feature.rms(y=y))),
                'rms_std': float(np.std(librosa.feature.rms(y=y))),
                'zcr_mean': float(np.mean(librosa.feature.zero_crossing_rate(y))),
                'spectral_centroid': float(np.mean(librosa.feature.spectral_centroid(y=y, sr=sr))),
                'spectral_bandwidth': float(np.mean(librosa.feature.spectral_bandwidth(y=y, sr=sr))),
                'spectral_rolloff': float(np.mean(librosa.feature.spectral_rolloff(y=y, sr=sr))),
                'spectral_contrast': float(np.mean(librosa.feature.spectral_contrast(y=y, sr=sr))),
            }
            
            # Ajouter les 6 premiers MFCC moyens (pour avoir 13 features au total)
            mfccs = librosa.feature.mfcc(y=y, sr=sr, n_mfcc=6)
            for i in range(6):
                features[f'mfcc_{i}'] = float(np.mean(mfccs[i]))
            
            return features
            
        except Exception as e:
            logger.error(f"Erreur lors de l'extraction des features pour {audio_path}: {e}")
            return None
    
    def process_normal_data(
        self,
        audio_dir: str,
        output_dir: str = './features'
    ) -> Tuple[np.ndarray, pd.DataFrame]:
        """
        Traite tous les fichiers audio normaux et crée les datasets
        
        Args:
            audio_dir: Dossier contenant les fichiers audio normaux
            output_dir: Dossier de sortie pour les features
            
        Returns:
            Tuple (spectrogrammes concatenés, dataframe de features)
        """
        audio_dir = Path(audio_dir)
        output_dir = Path(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)
        
        spectrograms = []
        features_list = []
        
        # Extensions audio supportées
        audio_extensions = {'.wav', '.mp3', '.flac', '.ogg'}
        audio_files = [f for f in audio_dir.glob('*') if f.suffix.lower() in audio_extensions]
        
        logger.info(f"Traitement de {len(audio_files)} fichiers audio...")
        
        for idx, audio_file in enumerate(audio_files):
            logger.info(f"Traitement {idx+1}/{len(audio_files)}: {audio_file.name}")
            
            # Extraire le spectrogramme Mel
            mel_spec = self.extract_mel_spectrogram(str(audio_file))
            if mel_spec is not None:
                spectrograms.append(mel_spec)
            
            # Extraire les features statistiques
            stats_features = self.extract_statistical_features(str(audio_file))
            if stats_features is not None:
                stats_features.update({
                    'machine_id': audio_file.stem,
                    'filename': audio_file.name,
                    'timestamp': datetime.now() - timedelta(hours=len(audio_files)-idx),
                    'label': 'normal'
                })
                features_list.append(stats_features)
        
        # Sauvegarder les spectrogrammes concatenés
        if spectrograms:
            spectrograms_array = np.array(spectrograms)
            np.save(output_dir / 'normal_spectrograms.npy', spectrograms_array)
            logger.info(f"Spectrogrammes sauvegardés: shape {spectrograms_array.shape}")
        
        # Créer le DataFrame de features
        if features_list:
            features_df = pd.DataFrame(features_list)
            features_df['event_timestamp'] = features_df['timestamp']
            features_df['created_timestamp'] = datetime.now()
            
            # Sauvegarder le DataFrame
            features_df.to_parquet(output_dir / 'normal_features.parquet', index=False)
            features_df.to_csv(output_dir / 'normal_features.csv', index=False)
            logger.info(f"Features sauvegardées: {len(features_df)} enregistrements")
        
        return spectrograms_array if spectrograms else None, features_df if features_list else None


if __name__ == "__main__":
    # Configuration
    AUDIO_DIR = "./mlops/data/normal"  # Dossier contenant vos fichiers audio normaux
    OUTPUT_DIR = "./mlops/feature_store/features"
    
    # Créer l'extracteur
    extractor = AudioFeatureExtractor(
        sample_rate=22050,
        n_mels=128,
        n_fft=2048,
        hop_length=512,
        duration=5.0
    )
    
    # Traiter les données
    spectrograms, features_df = extractor.process_normal_data(AUDIO_DIR, OUTPUT_DIR)
    
    if spectrograms is not None:
        print(f"\n✓ Spectrogrammes générés: {spectrograms.shape}")
    if features_df is not None:
        print(f"✓ Features extraites: {features_df.shape}")
        print(f"\nColonnes disponibles:\n{features_df.columns.tolist()}")