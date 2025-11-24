
import glob
import argparse
import sys
import os
from pathlib import Path
import pandas as pd

import numpy as np
import librosa
import librosa.core
import librosa.feature
import yaml
from tqdm.auto import tqdm
import torch

import logging

logging.basicConfig(level=logging.DEBUG, filename="baseline.log")
logger = logging.getLogger(' ')
handler = logging.StreamHandler()
formatter = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')
handler.setFormatter(formatter)
logger.addHandler(handler)


def command_line_chk(args=None, return_args=False):
    parser = argparse.ArgumentParser(description='Without option argument, it will not run properly.')
    parser.add_argument('-v', '--version', action='store_true', help="show application version")
    parser.add_argument('-e', '--eval', action='store_true', help="run mode Evaluation")
    parser.add_argument('-d', '--dev', action='store_true', help="run mode Development")
    parser.add_argument('--mode', type=str, default='baseline', help='chooses which model to use. [baseline | vae | vae_r2]')
    args = parser.parse_args(args=args)
    if args.version:
        print("===============================")
        print("DCASE 2020 task 2 baseline\nversion {}".format(__versions__))
        print("===============================\n")
    if args.eval ^ args.dev:
        if args.dev:
            flag = True
        else:
            flag = False
    else:
        flag = None
        print("incorrect argument")
        print("please set option argument '--dev' or '--eval'")
    return (flag, args) if return_args else flag


def yaml_load(yaml_file="baseline.yaml"):
    """Load parameters from YAML configuration file."""
    with open(yaml_file) as stream:
        param = yaml.safe_load(stream)
    return param


def file_load(wav_name, mono=False):
    """
    Load WAV audio file using librosa.

    Args:
        wav_name: Path to the target WAV file
        mono: If True, convert multi-channel audio to mono

    Returns:
        Tuple of (audio_data, sample_rate)
    """
    try:
        return librosa.load(wav_name, sr=None, mono=mono)
    except:
        logger.error("file_broken or not exists!! : {}".format(wav_name))


def file_to_vector_array(file_name,
                         n_mels=64,
                         frames=5,
                         n_fft=1024,
                         hop_length=512,
                         power=2.0):
    """
    Convert audio file to mel spectrogram feature vector array.

    Args:
        file_name: Path to the WAV file
        n_mels: Number of mel frequency bands
        frames: Number of frames to concatenate
        n_fft: FFT window size
        hop_length: Hop length for STFT
        power: Power for mel spectrogram

    Returns:
        Feature vector array of shape (dataset_size, feature_vector_length)
    """
    # Calculate the number of dimensions
    dims = n_mels * frames

    # Generate melspectrogram using librosa
    y, sr = file_load(file_name)
    mel_spectrogram = librosa.feature.melspectrogram(y=y,
                                                     sr=sr,
                                                     n_fft=n_fft,
                                                     hop_length=hop_length,
                                                     n_mels=n_mels,
                                                     power=power)

    # Convert melspectrogram to log mel energy
    log_mel_spectrogram = 20.0 / power * np.log10(mel_spectrogram + sys.float_info.epsilon)

    # Calculate total vector size
    vector_array_size = len(log_mel_spectrogram[0, :]) - frames + 1

    # Skip too short clips
    if vector_array_size < 1:
        return np.empty((0, dims))

    # Generate feature vectors by concatenating multiframes
    vector_array = np.zeros((vector_array_size, dims))
    for t in range(frames):
        vector_array[:, n_mels * t: n_mels * (t + 1)] = log_mel_spectrogram[:, t: t + vector_array_size].T

    return vector_array


def select_dirs(param, mode):
    """
    Select directories based on development or evaluation mode.

    Args:
        param: Configuration dictionary
        mode: If True, load development data; if False, load evaluation data

    Returns:
        List of directory paths
    """
    if mode:
        logger.info("load_directory <- development")
        dir_path = os.path.abspath("{base}/*".format(base=param["dev_directory"]))
        dirs = sorted(glob.glob(dir_path))
    else:
        logger.info("load_directory <- evaluation")
        dir_path = os.path.abspath("{base}/*".format(base=param["eval_directory"]))
        dirs = sorted(glob.glob(dir_path))
    dirs = [d for d in dirs if os.path.isdir(d)]

    if 'target' in param:
        def is_one_of_in(substrs, full_str):
            for s in substrs:
                if s in full_str: return True
            return False
        dirs = [d for d in dirs if is_one_of_in(param["target"], str(d))]

    return dirs


def list_to_vector_array(file_list,
                         msg="calc...",
                         n_mels=64,
                         frames=5,
                         n_fft=1024,
                         hop_length=512,
                         power=2.0):
    """
    Convert list of audio files to concatenated feature vector array.

    Args:
        file_list: List of WAV file paths
        msg: Description message for progress bar
        n_mels: Number of mel frequency bands
        frames: Number of frames to concatenate
        n_fft: FFT window size
        hop_length: Hop length for STFT
        power: Power for mel spectrogram

    Returns:
        Concatenated feature array of shape (total_vectors, feature_dimensions)
    """
    # Calculate the number of dimensions
    dims = n_mels * frames

    # Iterate through files and extract features
    for idx in tqdm(range(len(file_list)), desc=msg):
        vector_array = file_to_vector_array(file_list[idx],
                                            n_mels=n_mels,
                                            frames=frames,
                                            n_fft=n_fft,
                                            hop_length=hop_length,
                                            power=power)
        if idx == 0:
            dataset = np.zeros((vector_array.shape[0] * len(file_list), dims), float)
            logger.info((f'Creating data for {len(file_list)} files: size={dataset.shape[0]}'
                         f', shape={dataset.shape[1:]}'))
        dataset[vector_array.shape[0] * idx: vector_array.shape[0] * (idx + 1), :] = vector_array

    return dataset


def file_list_generator(target_dir,
                        dir_name="train",
                        ext="wav"):
    """
    Generate list of audio files from target directory.

    Args:
        target_dir: Base directory path
        dir_name: Subdirectory name containing data files
        ext: File extension to search for

    Returns:
        Sorted list of file paths
    """
    logger.info("target_dir : {}".format('/'.join(str(target_dir).split('/')[-2:])))

    # Generate file list
    training_list_path = os.path.abspath("{dir}/{dir_name}/*.{ext}".format(dir=target_dir, dir_name=dir_name, ext=ext))
    files = sorted(glob.glob(training_list_path))
    if len(files) == 0:
        logger.exception(f"{training_list_path} -> no_wav_file!!")

    logger.info("# of training samples : {num}".format(num=len(files)))
    return files
