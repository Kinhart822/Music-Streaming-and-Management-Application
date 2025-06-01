import asyncio
import sys
import subprocess
import hashlib
import logging
import re
import time
from uuid import uuid4
import contractions
import inflect
import spacy
from fastapi import FastAPI, Form, UploadFile
from fastapi.responses import JSONResponse
from contextlib import asynccontextmanager
from collections import defaultdict
from lingua import Language, LanguageDetectorBuilder
# import whisper
import os
import shutil
import joblib
# import uvicorn
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import pandas as pd
from sqlalchemy import create_engine
import aiofiles
from pydub import AudioSegment
from concurrent.futures import ProcessPoolExecutor
import multiprocessing

# Force ProactorEventLoop on Windows
if sys.platform == "win32":
    asyncio.set_event_loop_policy(asyncio.WindowsProactorEventLoopPolicy())

# PostgreSQL
# DATABASE_URL = "postgresql://postgres:kinhart822@localhost:5432/msma_database"
DATABASE_URL = os.getenv("DATABASE_URL")
# DATABASE_URL = "postgresql://postgres:kinhart822@host.docker.internal:5433/msma_database"
engine = create_engine(DATABASE_URL)

# Paths
TFIDF_PICKLE_PATH = "pkl/tfidf_matrix.pkl"
HASH_PICKLE_PATH = "pkl/lyrics_hash.pkl"

# Global variables
vectorizer_tfidf = None
rf_model_train = None
vectorizer_tfidf_similarity = TfidfVectorizer(use_idf=True)
tfidf_matrix = None
lyrics_hash = None
process_pool = None  # ProcessPoolExecutor
# whisper_model = None  # Global Whisper model

# Load spaCy English model
nlp = spacy.load('en_core_web_sm')
p = inflect.engine()

# Semaphore to limit concurrent requests
REQUEST_SEMAPHORE = asyncio.Semaphore(2)

# Configure logging
logging.basicConfig(level=logging.INFO)


# Lifespan event handler
@asynccontextmanager
async def lifespan(app: FastAPI):
    global vectorizer_tfidf, rf_model_train, tfidf_matrix, lyrics_hash, process_pool
    logging.info("Starting up: Loading models and TF-IDF matrix...")
    # Log ffmpeg version
    try:
        ffmpeg_version = subprocess.run(
            ["ffmpeg", "-version"],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=30
        ).stdout
        logging.info(f"FFmpeg version: {ffmpeg_version.splitlines()[0]}")
    except Exception as e:
        logging.error(f"Failed to get FFmpeg version: {e}")

    vectorizer_tfidf = joblib.load('pkl/vectorizer_tfidf.pkl')
    rf_model_train = joblib.load('pkl/rf_model_tf_idf.pkl')
    tfidf_matrix = processing_database()
    try:
        lyrics_hash = joblib.load(HASH_PICKLE_PATH)
    except FileNotFoundError:
        lyrics_hash = None

    # Initialize ProcessPoolExecutor
    max_workers = min(2, multiprocessing.cpu_count())
    process_pool = ProcessPoolExecutor(max_workers=max_workers)
    logging.info(f"Initialized ProcessPoolExecutor with {max_workers} workers")

    logging.info("Application startup completed: Loaded models and TF-IDF matrix.")
    yield

    logging.info("Shutting down: Cleaning up resources...")
    shutil.rmtree("temp", ignore_errors=True)
    process_pool.shutdown(wait=True)
    whisper_model = None  # Clear model reference
    logging.info("Shutdown complete.")


# Initialize FastAPI with lifespan
app = FastAPI(lifespan=lifespan)


def validate_audio_file(file_path):
    """Validate if an audio file is non-empty and has sufficient duration."""
    try:
        if not os.path.exists(file_path):
            logging.error(f"Audio file does not exist: {file_path}")
            return False
        file_size = os.path.getsize(file_path)
        if file_size < 1024:  # Less than 1KB
            logging.error(f"Audio file is too small: {file_size} bytes at {file_path}")
            return False
        audio = AudioSegment.from_file(file_path)
        duration_ms = len(audio)
        if duration_ms < 1000:  # Less than 1 second
            logging.error(f"Audio duration too short: {duration_ms}ms at {file_path}")
            return False
        logging.info(f"Validated audio: {file_path}, size={file_size} bytes, duration={duration_ms}ms")
        return True
    except Exception as e:
        logging.error(f"Error validating audio file {file_path}: {type(e).__name__}: {str(e)}")
        return False


def safe_decode(output_bytes):
    """Safely decode subprocess output, replacing invalid characters."""
    return output_bytes.decode("utf-8", errors="replace")


# Define module-level functions for ProcessPoolExecutor
def run_whisper_transcription(audio_path):
    """Run Whisper transcription in a separate process (load model inside process)."""
    try:
        import whisper

        logging.info("Loading Whisper model 'small.en' inside subprocess...")
        start_time = time.time()
        logging.info("Loading Whisper model 'small.en'...")
        model = whisper.load_model("small.en")
        logging.info("Whisper model loaded successfully.")
        logging.info(f"Whisper model loaded in {time.time() - start_time:.2f} seconds")

        logging.info(f"Starting transcription for {audio_path}")
        result = model.transcribe(audio_path, fp16=False)
        lyrics = result["text"]

        if not lyrics.strip():
            logging.warning(f"Transcription produced empty lyrics: {audio_path}")
            return None

        logging.info(f"Transcription successful for {audio_path}, lyrics length: {len(lyrics)} characters")
        return lyrics

    except Exception as e:
        logging.error(f"Whisper transcription error: {type(e).__name__}: {str(e)}")
        return None

# def run_whisper_transcription(audio_path):
#     """Run Whisper transcription in a separate process using a global model or load if necessary."""
#     global whisper_model
#     try:
#         # Check if the model is available; if not, load it (for a new worker process)
#         if whisper_model is None:
#             logging.info("Whisper model not found in process. Loading 'base.en'...")
#             start_time = time.time()
#             try:
#                 whisper_model = whisper.load_model("base.en")
#                 logging.info(f"Whisper model loaded successfully in {time.time() - start_time:.2f} seconds")
#             except RuntimeError as e:
#                 if "checksum does not match" in str(e):
#                     logging.warning("Whisper model checksum mismatch. Clearing cache and retrying...")
#                     # cache_dir = os.path.expanduser("~/.cache/whisper")
#                     # if os.path.exists(cache_dir):
#                     #     shutil.rmtree(cache_dir, ignore_errors=True)
#                     #     logging.info(f"Cleared Whisper cache at {cache_dir}")
#                     start_time = time.time()
#                     whisper_model = whisper.load_model("base.en")
#                     logging.info(f"Whisper model loaded successfully after cache clear in {time.time() - start_time:.2f} seconds")
#                 else:
#                     raise e
#         logging.info(f"Starting transcription for {audio_path}")
#         start_time = time.time()
#         result = whisper_model.transcribe(audio_path, fp16=False)
#         logging.info(f"Transcription completed in {time.time() - start_time:.2f} seconds")
#         lyrics = result["text"]
#         if not lyrics.strip():
#             logging.warning(f"Transcription produced empty lyrics: {audio_path}")
#             return None
#         logging.info(f"Transcription successful for {audio_path}, lyrics length: {len(lyrics)} characters")
#         return lyrics
#     except Exception as e:
#         logging.error(f"Whisper transcription error: {type(e).__name__}: {str(e)}")
#         return None


def run_demucs_separation(input_file, output_folder, filename):
    """Run Demucs vocal separation in a separate process."""
    try:
        os.makedirs(output_folder, exist_ok=True)
        vocals_dir = os.path.join(output_folder, "htdemucs", filename)
        vocals_path = os.path.join(vocals_dir, "vocals.mp3")
        new_vocals_path = os.path.join(vocals_dir, f"{filename}_vocals.mp3")

        if os.path.exists(new_vocals_path) and validate_audio_file(new_vocals_path):
            logging.info(f"Vocals file already exists and is valid: {new_vocals_path}")
            return new_vocals_path

        if os.path.exists(new_vocals_path):
            os.remove(new_vocals_path)

        command = ["demucs", "-o", output_folder, "--mp3", "-n", "htdemucs", "--device=cpu", input_file]
        logging.info(f"Running demucs command: {' '.join(command)}")
        result = subprocess.run(
            command,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=False,
            timeout=600
        )

        if result.returncode != 0:
            stderr = safe_decode(result.stderr)
            logging.error(f"Demucs failed with exit code {result.returncode}: {stderr}")
            shutil.rmtree(output_folder, ignore_errors=True)
            return None

        stdout = safe_decode(result.stdout)
        logging.info(f"Demucs stdout: {stdout}")
        if not os.path.exists(vocals_path):
            logging.error(f"Vocals file not found in {vocals_dir}")
            return None

        os.rename(vocals_path, new_vocals_path)
        if not validate_audio_file(new_vocals_path):
            logging.error(f"Generated vocals file is invalid: {new_vocals_path}")
            return None

        logging.info(f"Renamed vocals file to: {new_vocals_path}")
        return new_vocals_path
    except Exception as e:
        logging.error(f"Demucs error: {type(e).__name__}: {str(e)}")
        return None


def run_ffmpeg_slowdown(input_file, output_file, speed_factor):
    """Run FFmpeg slowdown in a separate process."""
    try:
        command = [
            "ffmpeg", "-i", input_file,
            "-filter:a", f"atempo={speed_factor}", "-vn",
            "-loglevel", "info", "-y", output_file
        ]
        logging.info(f"Running ffmpeg command: {' '.join(command)}")
        result = subprocess.run(
            command,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=False,
            timeout=300
        )

        if result.returncode != 0:
            stderr = safe_decode(result.stderr)
            logging.error(f"FFmpeg failed with exit code {result.returncode}: {stderr}")
            return None

        stdout = safe_decode(result.stdout)
        logging.info(f"FFmpeg stdout: {stdout}")
        if not os.path.exists(output_file):
            logging.error(f"FFmpeg output file not created: {output_file}")
            return None

        if not validate_audio_file(output_file):
            logging.error(f"Slowed audio is invalid: {output_file}")
            return None

        return output_file
    except Exception as e:
        logging.error(f"FFmpeg slowdown error: {type(e).__name__}: {str(e)}")
        return None


def run_ffmpeg_silence_removal(input_audio, output_audio, silence_threshold, min_silence_duration):
    """Run FFmpeg silence removal in a separate process."""
    try:
        command = [
            "ffmpeg", "-i", input_audio,
            "-af",
            f"silenceremove=stop_periods=-1:stop_duration={min_silence_duration}:stop_threshold={silence_threshold}",
            "-loglevel", "info", "-y", output_audio
        ]
        logging.info(f"Running ffmpeg silence removal command: {' '.join(command)}")
        result = subprocess.run(
            command,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=False,
            timeout=300
        )

        if result.returncode != 0:
            stderr = safe_decode(result.stderr)
            logging.error(f"FFmpeg silence removal failed with exit code {result.returncode}: {stderr}")
            return None

        stdout = safe_decode(result.stdout)
        logging.info(f"FFmpeg silence removal stdout: {stdout}")
        if not os.path.exists(output_audio):
            logging.error(f"Silence removal output file not created: {output_audio}")
            return None

        if not validate_audio_file(output_audio):
            logging.warning(f"Silence removal produced invalid output: {output_audio}. Falling back to input audio.")
            return input_audio

        return output_audio
    except Exception as e:
        logging.error(f"FFmpeg silence removal error: {type(e).__name__}: {str(e)}")
        return None


async def transcribe_lyric_by_whisper(audio_path):
    try:
        if not validate_audio_file(audio_path):
            logging.error(f"Cannot transcribe invalid audio: {audio_path}")
            return None
        logging.info(f"Transcribing audio: {audio_path}")
        loop = asyncio.get_running_loop()
        try:
            start_time = time.time()
            result = await asyncio.wait_for(
                loop.run_in_executor(process_pool, run_whisper_transcription, audio_path),
                timeout=600  # 10-minute timeout
            )
            logging.info(f"Total transcription process took {time.time() - start_time:.2f} seconds")
            if result is None:
                logging.warning(f"Transcription failed for {audio_path}")
                return None
            logging.info("Lyrics transcribed successfully.")
            return result
        except asyncio.TimeoutError:
            logging.error(f"Transcription timed out after 10 minutes for {audio_path}")
            return None
    except Exception as e:
        logging.error(f"Error transcribing lyrics: {type(e).__name__}: {str(e)}", exc_info=True)
        return None


async def separate_vocals(input_file, output_folder="demucs_output"):
    if not os.path.exists(input_file):
        logging.error(f"Input file '{input_file}' does not exist!")
        return None
    if not validate_audio_file(input_file):
        logging.error(f"Input audio is invalid: {input_file}")
        return None
    try:
        filename = os.path.splitext(os.path.basename(input_file))[0]
        loop = asyncio.get_running_loop()
        result = await loop.run_in_executor(
            process_pool, run_demucs_separation, input_file, output_folder, filename
        )
        if result is None:
            logging.error(f"Demucs vocal separation failed for {input_file}")
            return None
        logging.info(f"Vocals separated successfully: {result}")
        return result
    except Exception as e:
        logging.error(f"Unexpected error running Demucs: {type(e).__name__}: {str(e)}", exc_info=True)
        return None


async def slow_down_audio_ffmpeg(input_file, output_folder="final_output", speed_factor=0.85):
    try:
        if not os.path.exists(input_file):
            logging.error(f"Input file '{input_file}' does not exist!")
            return None
        if not validate_audio_file(input_file):
            logging.error(f"Input audio is invalid: {input_file}")
            return None
        filename = os.path.splitext(os.path.basename(input_file))[0]
        os.makedirs(output_folder, exist_ok=True)
        slowed_audio = os.path.join(output_folder, f"{filename}_slowdown.wav")

        loop = asyncio.get_running_loop()
        result = await loop.run_in_executor(
            process_pool, run_ffmpeg_slowdown, input_file, slowed_audio, speed_factor
        )
        if result is None:
            logging.error(f"FFmpeg slowdown failed for {input_file}")
            return None

        final_output = os.path.join(output_folder, f"{filename}_final.wav")
        result = await remove_silence(slowed_audio, final_output)
        if os.path.exists(slowed_audio):
            os.remove(slowed_audio)
        return result
    except Exception as e:
        logging.error(f"Error running FFmpeg: {type(e).__name__}: {str(e)}", exc_info=True)
        return None


async def remove_silence(input_audio, output_audio, silence_threshold="-30dB", min_silence_duration="1.0"):
    try:
        if not validate_audio_file(input_audio):
            logging.error(f"Input audio for silence removal is invalid: {input_audio}")
            return None
        loop = asyncio.get_running_loop()
        result = await loop.run_in_executor(
            process_pool, run_ffmpeg_silence_removal, input_audio, output_audio, silence_threshold, min_silence_duration
        )
        if result is None:
            logging.error(f"Silence removal failed for {input_audio}")
            return None
        return result
    except Exception as e:
        logging.error(f"Error removing silence: {type(e).__name__}: {str(e)}", exc_info=True)
        return None


def load_remove(path_to_load):
    remove_list = set()
    with open(path_to_load, "r", encoding="utf-8") as file:
        for line in file:
            word = line.strip()
            if word:
                remove_list.add(re.escape(word))
    return remove_list


def load_contractions(path_to_load):
    contractions_dict = {}
    with open(path_to_load, "r", encoding="utf-8") as file:
        for line in file:
            short, full = line.strip().split("=")
            contractions_dict[short] = full
    return contractions_dict


def remove_stopwords(text):
    doc = nlp(text)
    return " ".join([token.text for token in doc if not token.is_stop])


def lemmatize(text):
    doc = nlp(text)
    return " ".join([token.lemma_ for token in doc])


def pre_processing(text):
    if text is None:
        return None
    custom_contractions = load_contractions("custom/custom_contractions.txt")
    custom_remove = load_remove("custom/custom_remove.txt")
    lyrics_preprocess = text.lower().strip()
    lyrics_preprocess = re.sub(r'\d+', lambda x: p.number_to_words(x.group()), lyrics_preprocess)
    pattern = r"\b(" + "|".join(custom_remove) + r")\b"
    lyrics_preprocess = re.sub(pattern, "", lyrics_preprocess, flags=re.IGNORECASE)
    lyrics_preprocess = lyrics_preprocess.replace("in'", "ing").replace("in '", "ing")
    lyrics_preprocess = lyrics_preprocess.replace("in’", "ing").replace("in ’", "ing")
    for short, full in custom_contractions.items():
        lyrics_preprocess = lyrics_preprocess.replace(short, full)
    lyrics_preprocess = contractions.fix(lyrics_preprocess)
    lyrics_preprocess = re.sub(r"[`’']", "", lyrics_preprocess)
    lyrics_preprocess = re.sub(r"<.*?>", "", lyrics_preprocess)
    lyrics_preprocess = re.sub(r"\[[0-9]*\]", "", lyrics_preprocess)
    lyrics_preprocess = re.sub(r"\b(chorus|verse)\b", "", lyrics_preprocess, flags=re.IGNORECASE)
    lyrics_preprocess = re.sub(r"[^\w\s]", "", lyrics_preprocess)
    lyrics_preprocess = re.sub(r"[,:?\[\]{}\-+\\/|@#$*^&%~!();\"]", "", lyrics_preprocess)
    lyrics_preprocess = re.sub(r"x[1-9]", "", lyrics_preprocess)
    lyrics_preprocess = re.sub(r"\s+", " ", lyrics_preprocess).strip()
    lyrics_preprocess = remove_stopwords(lyrics_preprocess)
    lyrics_preprocess = lemmatize(lyrics_preprocess)
    return lyrics_preprocess


def detect_languages(text, detector):
    if isinstance(text, str) and text.strip():
        confidences = detector.compute_language_confidence_values(text)
        return {lang.language.name: lang.value for lang in confidences}
    return {}


def detect_languages_for_lyrics(lyrics_languages):
    languages = [
        Language.ENGLISH, Language.VIETNAMESE, Language.KOREAN, Language.CHINESE, Language.JAPANESE, Language.FRENCH,
        Language.GERMAN, Language.SPANISH, Language.ITALIAN, Language.RUSSIAN, Language.PORTUGUESE, Language.POLISH,
    ]
    detector = LanguageDetectorBuilder.from_languages(*languages).build()
    text = lyrics_languages.strip()
    segments = [line for line in text.splitlines() if line.strip()]
    if not segments:
        return {"detected_languages": "Unknown", "multilingual": "Unknown"}
    language_totals = defaultdict(float)
    segment_count = len(segments)
    for segment in segments:
        detected_languages = detect_languages(segment, detector)
        for lang, value in detected_languages.items():
            if lang != "UNKNOWN":
                language_totals[lang] += value
    if not language_totals:
        return {"detected_languages": "Unknown", "multilingual": "Unknown"}
    average_languages = {lang: round(value / segment_count * 100, 2) for lang, value in language_totals.items()}
    multilingual = any(
        lang in {"VIETNAMESE", "KOREAN", "CHINESE", "JAPANESE", "FRENCH", "GERMAN", "SPANISH", "ITALIAN", "RUSSIAN",
                 "PORTUGUESE", "POLISH"} and value > 20
        for lang, value in average_languages.items()
    )
    return {
        "detected_languages": average_languages,
        "multilingual": "Yes" if multilingual else "No"
    }


def preprocess_lyrics(text, multilingual):
    if multilingual == "No":
        lyrics_test = pre_processing(text)
        return lyrics_test
    return None


def tfidf_lyrics(test_lyrics):
    if not test_lyrics:
        logging.warning("No lyrics provided.")
        return None
    lyrics_data = detect_languages_for_lyrics(test_lyrics)
    preprocessed_lyrics = preprocess_lyrics(test_lyrics, lyrics_data["multilingual"])
    if not preprocessed_lyrics:
        return None
    song_tfidf = vectorizer_tfidf.transform([preprocessed_lyrics])
    return song_tfidf


async def process_audio(input_file, request_id):
    temp_dir = f"temp/{request_id}"
    demucs_dir = f"{temp_dir}/demucs_output"
    final_output_dir = f"{temp_dir}/final_output"
    vocals_path = await separate_vocals(input_file, demucs_dir)
    if not vocals_path or not os.path.exists(vocals_path):
        logging.error(f"No vocals file found at {vocals_path}")
        return None
    processed_audio = await slow_down_audio_ffmpeg(vocals_path, final_output_dir)
    if not processed_audio:
        logging.error(f"Error processing audio for {vocals_path}")
        return None
    if not validate_audio_file(processed_audio):
        logging.error(f"Processed audio is invalid: {processed_audio}")
        return None
    logging.info(f"Audio processed successfully: {processed_audio}")
    return processed_audio


async def handle_audio_upload(audio_file: UploadFile):
    request_id = str(uuid4())
    temp_dir = f"temp/{request_id}"
    os.makedirs(temp_dir, exist_ok=True)
    file_location = f"temp/{request_id}/{audio_file.filename}"
    logging.info(f"File location: {file_location}")
    if not audio_file.filename.lower().endswith(('.mp3', '.wav')):
        logging.error(f"Unsupported file format: {audio_file.filename}")
        return None, None, None
    file_size = audio_file.size
    logging.info(f"File size: {file_size}")
    if file_size > 100 * 1024 * 1024:
        logging.error(f"File too large: {file_size} bytes")
        return None, None, None
    async with aiofiles.open(file_location, "wb") as buffer:
        content = await audio_file.read()
        bytes_written = await buffer.write(content)
        logging.info(f"Wrote {bytes_written} bytes to {file_location}")
    audio = await process_audio(file_location, request_id)
    return request_id, temp_dir, audio


def get_lyrics_hash(lyrics_list):
    all_lyrics = "".join(lyrics_list)
    return hashlib.md5(all_lyrics.encode("utf-8")).hexdigest()


def processing_database():
    query = "SELECT * FROM songs WHERE status = 'ACCEPTED'"
    df = pd.read_sql(query, engine)
    current_hash = get_lyrics_hash(df["lyrics"].tolist())
    global lyrics_hash, tfidf_matrix
    if lyrics_hash and current_hash == lyrics_hash:
        logging.info("No change in lyrics. Using cached TF-IDF matrix.")
        return tfidf_matrix
    logging.info("Lyrics changed. Rebuilding TF-IDF matrix.")
    df["clean_lyrics"] = df["lyrics"].apply(pre_processing)
    documents = df["clean_lyrics"]
    tfidf_matrix_new = vectorizer_tfidf_similarity.fit_transform(documents)
    joblib.dump(vectorizer_tfidf_similarity, "pkl/tfidf_similarity_vectorizer.pkl")
    joblib.dump(tfidf_matrix_new, TFIDF_PICKLE_PATH)
    joblib.dump(current_hash, HASH_PICKLE_PATH)
    lyrics_hash = current_hash
    tfidf_matrix = tfidf_matrix_new
    return tfidf_matrix_new


def tfidf_lyrics_similarity(test_lyrics):
    if not test_lyrics:
        logging.warning("No lyrics provided.")
        return None
    lyrics_data = detect_languages_for_lyrics(test_lyrics)
    preprocessed_lyrics = preprocess_lyrics(test_lyrics, lyrics_data["multilingual"])
    if not preprocessed_lyrics:
        return None
    song_tfidf = joblib.load("pkl/tfidf_similarity_vectorizer.pkl").transform([preprocessed_lyrics])
    return song_tfidf


def check_similarity(tfidf_vector, tfidf_matrix_vector):
    if tfidf_vector is None:
        return None
    similarity_scores = cosine_similarity(tfidf_vector, tfidf_matrix_vector)[0]
    return similarity_scores.max()


def tfidf_lyrics_similarity_compare_lyric_with_audio(lyrics_list):
    vectorizer = joblib.load("pkl/tfidf_similarity_vectorizer.pkl")
    processed_lyrics = [preprocess_lyrics(lyric, "No") for lyric in lyrics_list if lyric]
    processed_lyrics = [lyric for lyric in processed_lyrics if lyric]
    if not processed_lyrics:
        return []
    tfidf_matrix_similarity = vectorizer.fit_transform(processed_lyrics)
    return tfidf_matrix_similarity.toarray()


def check_similarity_between_lyrics(v1, v2):
    return cosine_similarity([v1], [v2])[0][0]


def predict_genre(model_prediction, tfidf_vector):
    logging.info(f"Making genre prediction.")
    predicted_genre = model_prediction.predict(tfidf_vector)[0]
    return predicted_genre


@app.get("/")
async def root():
    return {"message": "FastAPI is running"}


@app.post("/check-similarity")
async def similarity_api(lyrics: str = Form(None), audio_file: UploadFile = None):
    async with REQUEST_SEMAPHORE:
        logging.info("Starting check-similarity processing")
        try:
            if audio_file:
                logging.info("Processing audio file upload")
                result = await handle_audio_upload(audio_file)
                logging.info(f"handle_audio_upload result: {result}")
                request_id, temp_dir, audio = result
                logging.info(f"Finished handling audio upload: {audio}")
                if not audio:
                    shutil.rmtree(temp_dir, ignore_errors=True)
                    return JSONResponse(status_code=400,
                                        content={"error": "Audio processing failed. Check logs for details."})
                logging.info("Starting transcription")
                lyrics_transcribe = await transcribe_lyric_by_whisper(audio)
                logging.info(f"Transcription result: {lyrics_transcribe}")
                if not lyrics_transcribe:
                    shutil.rmtree(temp_dir, ignore_errors=True)
                    return JSONResponse(status_code=400,
                                        content={"error": "Lyrics transcription failed. Check audio file."})
                lyrics_file = f"{temp_dir}/lyrics_transcribe.pkl"
                loop = asyncio.get_running_loop()
                await loop.run_in_executor(None, lambda: joblib.dump(lyrics_transcribe, lyrics_file))
                logging.info(f"Saved transcribed lyrics to {lyrics_file}")
                lyrics_processed = tfidf_lyrics_similarity(lyrics_transcribe)
                logging.info("Finished processing lyrics to TF-IDF")
                similarity_score = check_similarity(lyrics_processed, tfidf_matrix)
                logging.info(f"Similarity score: {similarity_score}")
                match = bool(similarity_score and similarity_score >= 0.75)
                return JSONResponse(
                    status_code=200,
                    content={
                        "request_id": request_id,
                        "lyrics": lyrics_transcribe,
                        "match": match,
                        "similarity_score": float(round(similarity_score or 0, 4)),
                        "message": "Bài hát đã bị trùng." if match else "Không có bài hát nào trùng khớp!"
                    }
                )
            elif lyrics:
                lyrics_processed = tfidf_lyrics_similarity(lyrics)
                logging.info("Finished processing lyrics to TF-IDF")
                similarity_score = check_similarity(lyrics_processed, tfidf_matrix)
                logging.info("Finished checking similarity")
                match = bool(similarity_score and similarity_score >= 0.75)
                return JSONResponse(
                    status_code=200,
                    content={
                        "match": match,
                        "similarity_score": float(round(similarity_score or 0, 4)),
                        "message": "Bài hát đã bị trùng." if match else "Không có bài hát nào trùng khớp!"
                    }
                )
            else:
                return JSONResponse(
                    status_code=400,
                    content={"error": "Bạn phải cung cấp audio hoặc lyrics."}
                )
        except Exception as e:
            shutil.rmtree(temp_dir, ignore_errors=True)
            logging.error(f"Error in similarity_api: {type(e).__name__}: {str(e)}", exc_info=True)
            return JSONResponse(status_code=500, content={"error": f"Server error: {str(e)}"})


@app.post("/predict-genre")
async def predict_genre_api(request_id: str = Form(None), lyrics: str = Form(None), audio_file: UploadFile = None):
    async with REQUEST_SEMAPHORE:
        try:
            logging.info("Starting predict-genre processing")
            if audio_file:
                if request_id:
                    temp_dir = f"temp/{request_id}"
                    lyrics_file = f"{temp_dir}/lyrics_transcribe.pkl"
                    if not os.path.exists(lyrics_file):
                        shutil.rmtree(temp_dir, ignore_errors=True)
                        return JSONResponse(status_code=400, content={"error": "Lyrics file not found."})
                    loop = asyncio.get_running_loop()
                    lyrics_transcribe = await loop.run_in_executor(None, lambda: joblib.load(lyrics_file))
                    lyrics_processed = tfidf_lyrics(lyrics_transcribe)
                    logging.info("Finished processing lyrics to TF-IDF")
                    predicted_genre = predict_genre(rf_model_train, lyrics_processed)
                    if predicted_genre:
                        shutil.rmtree(temp_dir, ignore_errors=True)
                        logging.info(f"Cleaned up temp directory: {temp_dir}")
                    logging.info("Finished predicting genre")
                    return {"genre": predicted_genre}
                return None
            elif lyrics:
                lyrics_processed = tfidf_lyrics(lyrics)
                logging.info("Finished processing lyrics to TF-IDF")
                predicted_genre = predict_genre(rf_model_train, lyrics_processed)
                logging.info("Finished predicting genre")
                return {"genre": predicted_genre}
            else:
                return JSONResponse(
                    status_code=400,
                    content={"error": "Bạn phải cung cấp audio hoặc lyrics."}
                )
        except Exception as e:
            shutil.rmtree(temp_dir, ignore_errors=True)
            logging.error(f"Error in predict_genre_api: {type(e).__name__}: {str(e)}", exc_info=True)
            return JSONResponse(status_code=500, content={"error": f"Server error: {str(e)}"})


@app.post("/transcribe-lyrics")
async def transcribe_lyrics(audio_file: UploadFile = None):
    async with REQUEST_SEMAPHORE:
        try:
            logging.info("Starting transcribe-lyrics processing")
            if audio_file:
                request_id, temp_dir, audio = await handle_audio_upload(audio_file)
                logging.info(f"Finished handling audio upload: {audio}")
                if not audio:
                    shutil.rmtree(temp_dir, ignore_errors=True)
                    return JSONResponse(status_code=400,
                                        content={"error": "Audio processing failed. Check logs for details."})
                lyrics_transcribe = await transcribe_lyric_by_whisper(audio)
                logging.info("Finished transcribing lyrics")
                if not lyrics_transcribe:
                    shutil.rmtree(temp_dir, ignore_errors=True)
                    return JSONResponse(status_code=400,
                                        content={"error": "Lyrics transcription failed. Check audio file."})
                lyrics_file = f"{temp_dir}/lyrics_transcribe.pkl"
                loop = asyncio.get_running_loop()
                await loop.run_in_executor(None, lambda: joblib.dump(lyrics_transcribe, lyrics_file))
                logging.info(f"Saved transcribed lyrics to {lyrics_file}")
                return JSONResponse(
                    status_code=200,
                    content={
                        "request_id": request_id,
                        "lyrics": lyrics_transcribe
                    }
                )
            else:
                return JSONResponse(
                    status_code=400,
                    content={"error": "Bạn phải cung cấp audio hoặc lyrics."}
                )
        except Exception as e:
            shutil.rmtree(temp_dir, ignore_errors=True)
            logging.error(f"Error in transcribe_lyrics: {type(e).__name__}: {str(e)}", exc_info=True)
            return JSONResponse(status_code=500, content={"error": f"Server error: {str(e)}"})


@app.post("/check-similar-between-input-and-audio")
async def check_similar_api(lyrics: str = Form(None), lyrics_audio: str = Form(None)):
    async with REQUEST_SEMAPHORE:
        try:
            logging.info("Starting check-similar-between-input-and-audio processing")
            if not lyrics or not lyrics_audio:
                return JSONResponse(
                    status_code=400,
                    content={"error": "Bạn phải cung cấp cả audio và lyrics để so sánh."}
                )
            lyrics_pair = [lyrics, lyrics_audio]
            processed_vectors = tfidf_lyrics_similarity_compare_lyric_with_audio(lyrics_pair)
            logging.info("Finished processing lyrics to TF-IDF")
            if len(processed_vectors) != 2:
                return JSONResponse(
                    status_code=400,
                    content={"error": "Không thể xử lý lyrics để so sánh."}
                )
            similarity_score = check_similarity_between_lyrics(processed_vectors[0], processed_vectors[1])
            logging.info("Finished checking similarity")
            isNotMatch = bool(similarity_score <= 0.5)
            return JSONResponse(
                status_code=200,
                content={
                    "isNotMatch": isNotMatch,
                    "similarity_score": float(round(similarity_score, 4)),
                    "message": "Lời bài hát không trùng khớp, not ok!" if isNotMatch else "Lời bài hát trùng khớp, ok!"
                }
            )
        except Exception as e:
            logging.error(f"Error in check_similar_api: {type(e).__name__}: {str(e)}", exc_info=True)
            return JSONResponse(status_code=500, content={"error": f"Server error: {str(e)}"})

# if __name__ == "__main__":
#     uvicorn.run("Backend:app", host="0.0.0.0", port=8000, reload=True)