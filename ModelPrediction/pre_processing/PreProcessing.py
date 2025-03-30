import csv

import pandas as pd
import json
import numpy as np
import subprocess
import whisper
import hashlib
import os
import re
import contractions
import spacy
import inflect
import tempfile
from io import BytesIO
from collections import defaultdict
from lingua import Language, LanguageDetectorBuilder
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import GridSearchCV
from sqlalchemy import create_engine, text
import psycopg2
from sklearn.model_selection import train_test_split
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

nlp = spacy.load('en_core_web_sm')

p = inflect.engine()


def import_dataset():
    with open('lyrics.csv', 'r', newline='', encoding='utf-8') as inp, open('lyrics_out.csv', 'w', newline='',
                                                                            encoding='utf-8') as out:
        reader = csv.reader(inp)
        writer = csv.writer(out)

        for row in reader:
            if len(row) > 5 and row[4] not in ["Alkebulan", "Other", "", "Not Available", "zora sourit"] and row[
                5] != "":
                writer.writerow(row)


def detect_languages(text, detector):
    """Phát hiện ngôn ngữ và xác suất của đoạn văn bản."""
    if isinstance(text, str) and text.strip():
        confidences = detector.compute_language_confidence_values(text)
        return {lang.language.name: lang.value for lang in confidences}
    return {}


def detect_languages_for_dataframe(df):
    languages = [
        Language.ENGLISH, Language.VIETNAMESE, Language.KOREAN, Language.CHINESE, Language.JAPANESE,
        Language.FRENCH, Language.GERMAN, Language.SPANISH, Language.ITALIAN, Language.RUSSIAN,
        Language.PORTUGUESE, Language.POLISH
    ]

    detector = LanguageDetectorBuilder.from_languages(*languages).build()
    results = []

    for _, row in df.dropna().iterrows():
        text = row["lyrics"]
        segments = [line for line in text.strip().splitlines() if line.strip()]

        language_totals = defaultdict(float)
        segment_count = 0

        for segment in segments:
            detected_languages = detect_languages(segment, detector)
            for lang, value in detected_languages.items():
                if lang != "UNKNOWN":
                    language_totals[lang] += value
            segment_count += 1

        if not language_totals:
            results.append({
                "id": row["id"],
                "title": row["title"],
                "artist": row["artist"],
                "lyrics": text,
                "detected_languages": "Unknown",
                "multilingual": "Unknown"
            })
            continue

        average_languages = {lang: round(value / segment_count * 100, 2) for lang, value in language_totals.items()}

        multilingual = any(
            lang in {"VIETNAMESE", "KOREAN", "CHINESE", "JAPANESE", "FRENCH", "GERMAN", "SPANISH", "ITALIAN", "RUSSIAN",
                     "PORTUGUESE", "POLISH"} and value > 20
            for lang, value in average_languages.items()
        )

        results.append({
            "id": row["id"],
            "title": row["title"],
            "artist": row["artist"],
            "lyrics": text,
            "detected_languages": average_languages,
            "multilingual": "Yes" if multilingual else "No"
        })

    return pd.DataFrame(results)


# df_results = detect_languages_for_dataframe(df)
# non_english_songs = df_results[df_results["multilingual"] == "Yes"]
# non_english_ids = non_english_songs["id"].tolist()
#
# if non_english_ids:
#     with engine.connect() as conn:
#         for song_id in non_english_ids:
#             query = text("DELETE FROM song WHERE id = :id")
#             conn.execute(query, {"id": song_id})
#         conn.commit()
# else:
#     print("✅ Dữ liệu đã chỉ chứa bài hát tiếng Anh, không cần xóa.")
#
# columns_to_keep = ["id", "title", "lyrics", "artist", "label_copyrighted"]
# df = df[columns_to_keep]


def load_contractions(file_path):
    contractions_dict = {}
    with open(file_path, "r", encoding="utf-8") as file:
        for line in file:
            short, full = line.strip().split("=")
            contractions_dict[short] = full
    return contractions_dict


def load_remove(file_path):
    remove_list = set()
    with open(file_path, "r", encoding="utf-8") as file:
        for line in file:
            word = line.strip()
            if word:
                remove_list.add(re.escape(word))
    return remove_list


# Convert to lowercase, strip and remove punctuations
def preprocess(text):
    if pd.isna(text):
        return ""

    lyrics = text.lower().strip()

    lyrics = re.sub(r'\d+', lambda x: p.number_to_words(x.group()), lyrics)

    custom_remove = load_remove("custom_remove.txt")
    pattern = r"\b(" + "|".join(custom_remove) + r")\b"
    lyrics = re.sub(pattern, "", lyrics, flags=re.IGNORECASE)

    lyrics = lyrics.replace("in'", "ing").replace("in '", "ing")
    lyrics = lyrics.replace("in’", "ing").replace("in ’", "ing")

    custom_contractions = load_contractions("custom_contractions.txt")
    for short, full in custom_contractions.items():
        lyrics = lyrics.replace(short, full)

    lyrics = contractions.fix(lyrics)
    lyrics = lyrics.replace("'", "").replace("`", "").replace("’", "")
    lyrics = re.sub(r'<.*?>', '', lyrics)
    lyrics = re.sub(r'\[[0-9]*\]', '', lyrics)
    lyrics = re.sub(r"[,.:?\[\]{}\-+\\/|@#$*^&%\~!()\";]", "", lyrics)
    lyrics = re.sub(r"\s+", " ", lyrics).strip()

    return lyrics


def check_exist(df):
    if not os.path.exists("clean_lyrics_stopwords_lemmatizer.csv"):
        print("📂 Lần đầu chạy: Tạo mới file CSV...")
    else:
        # Đọc dữ liệu từ CSV
        df_old = pd.read_csv("clean_lyrics_stopwords_lemmatizer.csv")

        # Kiểm tra xem dữ liệu có thay đổi không
        if "clean_lyrics" in df_old:
            print("✅ Không có thay đổi, tải dữ liệu từ CSV...")
            df["clean_lyrics"] = df_old["clean_lyrics"]
            return df

        print("🔄 Dữ liệu thay đổi, xóa CSV cũ và tạo lại...")
        os.remove("clean_lyrics_stopwords_lemmatizer.csv")

    # Chạy preprocessing và lưu vào CSV
    print("🚀 Đang xử lý lyrics...")
    df["clean_lyrics"] = df["lyrics"].apply(preprocess)
    df.to_csv("clean_lyrics_stopwords_lemmatizer.csv", index=False)
    return df


def detect_languages(text, detector):
    """Phát hiện ngôn ngữ và xác suất của đoạn văn bản."""
    if isinstance(text, str) and text.strip():
        confidences = detector.compute_language_confidence_values(text)
        return {lang.language.name: lang.value for lang in confidences}
    return {}


def detect_languages_for_lyrics(lyrics):
    languages = [
        Language.ENGLISH, Language.VIETNAMESE, Language.KOREAN, Language.CHINESE, Language.JAPANESE, Language.FRENCH,
        Language.GERMAN, Language.SPANISH, Language.ITALIAN, Language.RUSSIAN, Language.PORTUGUESE, Language.POLISH,
    ]

    detector = LanguageDetectorBuilder.from_languages(*languages).build()

    text = lyrics.strip()
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

    # Calculate the average confidence percentages
    average_languages = {lang: round(value / segment_count * 100, 2) for lang, value in language_totals.items()}

    # Check if text is multilingual (if any language has > 20% confidence)
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
        lyrics = preprocess(text)

        return lyrics
    return "Không hợp lệ: Văn bản chứa nhiều ngôn ngữ."


def predict_copyright(model_prediction, tfidf_vector):
    prob = model_prediction.predict_proba(tfidf_vector)[0][1]
    prediction_label = "⚠️ Copyrighted" if prob >= 0.5 else "✅ No Copyrighted"

    return prediction_label


def tfidf_lyrics(lyrics, vectorizer_tfidf):
    if not lyrics:
        print("🚫 Không có lyrics.")
        return None

    lyrics_data = detect_languages_for_lyrics(lyrics)
    preprocessed_lyrics = preprocess_lyrics(lyrics, lyrics_data["multilingual"])

    if preprocessed_lyrics != "Không hợp lệ: Văn bản chứa nhiều ngôn ngữ.":
        # Biến lyrics thành vector TF-IDF
        remix_song_tfidf = vectorizer_tfidf.transform([preprocessed_lyrics])
        return remix_song_tfidf

    return "Không hợp lệ: Văn bản chứa nhiều ngôn ngữ."


def main():
    import_dataset()


if __name__ == "__main__":
    main()
