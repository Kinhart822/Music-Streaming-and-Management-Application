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

# Additional
conn = psycopg2.connect(
    dbname="music_management",
    user="postgres",
    password="kinhart822",
    host="localhost",
    port="5432"
)

nlp = spacy.load('en_core_web_sm')

p = inflect.engine()  # Tạo engine để chuyển số thành chữ

directories = [
    r"D:\Pycharm_Projects\Python_Projects\music\copyrighted",
    r"D:\Pycharm_Projects\Python_Projects\music\no_copyrighted"
]

engine = create_engine("postgresql+psycopg2://postgres:kinhart822@localhost:5432/music_management")


def fetch_songs():
    query = "SELECT * FROM public.songs ORDER BY id ASC;"
    return pd.read_sql_query(query, conn)


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


df_results = detect_languages_for_dataframe(df)
non_english_songs = df_results[df_results["multilingual"] == "Yes"]
non_english_ids = non_english_songs["id"].tolist()

if non_english_ids:
    with engine.connect() as conn:
        for song_id in non_english_ids:
            query = text("DELETE FROM song WHERE id = :id")
            conn.execute(query, {"id": song_id})
        conn.commit()
else:
    print("✅ Dữ liệu đã chỉ chứa bài hát tiếng Anh, không cần xóa.")

columns_to_keep = ["id", "title", "lyrics", "artist", "label_copyrighted"]
df = df[columns_to_keep]


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


def hash_column(series):
    """Tạo hash từ dữ liệu của cột để kiểm tra thay đổi"""
    return hashlib.md5(series.to_string().encode()).hexdigest()


def check_exist(df):
    if not os.path.exists("clean_lyrics_stopwords_lemmatizer.csv"):
        print("📂 Lần đầu chạy: Tạo mới file CSV...")
    else:
        # Đọc dữ liệu từ CSV
        df_old = pd.read_csv("clean_lyrics_stopwords_lemmatizer.csv")

        # Kiểm tra xem dữ liệu có thay đổi không
        if "clean_lyrics" in df_old and hash_column(df["lyrics"]) == hash_column(df_old["lyrics"]):
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


# Train the model prediction
df = check_exist(df)
df_train = df
X_train, X_test, y_train, y_test = train_test_split(df_train["clean_lyrics"], df_train["label_copyrighted"],
                                                    test_size=0.2, shuffle=True, random_state=42)
vectorizer_tfidf = TfidfVectorizer(use_idf=True)
X_train_vectors_tfidf = vectorizer_tfidf.fit_transform(X_train)
X_test_vectors_tfidf = vectorizer_tfidf.transform(X_test)



def load_hyperparams():
    """Load hyperparameters từ file JSON"""
    try:
        with open("best_hyperparams_stopwords_lemmatizer.json", "r") as f:
            return json.load(f)
    except FileNotFoundError:
        return None


def save_hyperparams(best_params):
    """Lưu hyperparameters vào file JSON"""
    best_params = {key: int(value) if isinstance(value, np.integer) else value for key, value in best_params.items()}
    with open("best_hyperparams_stopwords_lemmatizer.json", "w") as f:
        json.dump(best_params, f, indent=4)


def find_best_hyperparams(X_train, y_train):
    print("🔍 Tìm hyperparameters tối ưu với GridSearchCV...")

    param_grid = {
        'n_estimators': np.arange(50, 500, 50),
        'max_depth': [5, 10, 15, 20, None],
        'min_samples_split': [2, 5, 10],
        'min_samples_leaf': [1, 2, 4],
        'max_features': ['sqrt', 'log2'],
        'bootstrap': [True, False],
    }

    rf = RandomForestClassifier()
    grid_search = GridSearchCV(
        estimator=rf,
        param_grid=param_grid,
        cv=5,
        verbose=2,
        n_jobs=-1
    )
    grid_search.fit(X_train, y_train)

    best_params = grid_search.best_params_
    print("🎯 Best Parameters:", best_params)

    # Lưu hyperparameters vào JSON
    save_hyperparams(best_params)
    return best_params


def check_and_update_hyperparams(X_train, y_train, df):
    best_params = load_hyperparams()

    if best_params:
        print("✅ Hyperparameters đã có sẵn, kiểm tra dữ liệu...")
        df_old = pd.read_csv("clean_lyrics_stopwords_lemmatizer.csv")

        if hash_column(df["lyrics"]) == hash_column(df_old["lyrics"]):
            print("✅ Dữ liệu không đổi, giữ nguyên best_params.")
            return best_params

        print("🔄 Dữ liệu thay đổi, tính lại hyperparameters...")

    else:
        print("📂 Lần đầu chạy: Tạo mới file JSON...")

    return find_best_hyperparams(X_train, y_train)


best_params_for_rf = check_and_update_hyperparams(X_train_vectors_tfidf, y_train, df)
rf = RandomForestClassifier(**best_params_for_rf, class_weight="balanced", random_state=42)


def transcribe_lyric_by_whisper(audio_path):
    try:
        print("Đang chuyển đổi audio thành text bằng Whisper...")
        model_to_load = whisper.load_model("large")
        result = model_to_load.transcribe(audio_path, fp16=False)
        lyrics = result["text"]

        print("🎤 Lời bài hát được trích xuất thành công!")
        return lyrics
    except Exception as e:
        print(f"❌ Lỗi khi trích xuất lời bài hát: {e}")
        return None


def separate_vocals(input_file, output_folder="demucs_output"):
    """Chạy Demucs để tách giọng hát từ file MP3 và đổi tên file kết quả."""

    # Kiểm tra file đầu vào có tồn tại không
    if not os.path.exists(input_file):
        print(f"❌ Lỗi: File đầu vào '{input_file}' không tồn tại!")
        return None

    try:
        # Tạo thư mục output nếu chưa tồn tại
        os.makedirs(output_folder, exist_ok=True)

        filename = os.path.splitext(os.path.basename(input_file))[0]
        vocals_dir = os.path.join(output_folder, "mdx_extra_q", filename)
        vocals_path = os.path.join(vocals_dir, "vocals.mp3")
        new_vocals_path = os.path.join(vocals_dir, f"{filename}_vocals.mp3")

        if os.path.exists(new_vocals_path):
            print(f"✅ File vocals đã tồn tại: {new_vocals_path}")
            return new_vocals_path

        # Chạy Demucs để tách giọng hát
        command = [
            "demucs", "-o", output_folder, "--mp3",
            "-n", "mdx_extra_q", input_file
        ]
        subprocess.run(command, check=True)

        print(f"✅ Tách giọng hát thành công! Kết quả lưu tại: {output_folder}")

        # Kiểm tra file vocals.mp3 có được tạo ra không
        if not os.path.exists(vocals_path):
            print(f"❌ Lỗi: File vocals.mp3 không tồn tại trong thư mục {vocals_dir}")
            return None

        # Đổi tên file
        os.rename(vocals_path, new_vocals_path)
        print(f"✅ Đã đổi tên file thành: {new_vocals_path}")

        return new_vocals_path

    except subprocess.CalledProcessError as e:
        print(f"❌ Lỗi khi chạy Demucs: {e}")
        return None


def slow_down_audio_ffmpeg(input_file, output_folder="final_output", speed_factor=0.85):
    """Làm chậm audio bằng FFmpeg và loại bỏ khoảng lặng"""
    try:
        # Tách tên file không có phần mở rộng
        filename = os.path.splitext(os.path.basename(input_file))[0]

        # Tạo thư mục output nếu chưa tồn tại
        os.makedirs(output_folder, exist_ok=True)

        # Tạo đường dẫn cho file đã làm chậm
        slowed_audio = os.path.join(output_folder, f"{filename}_slowdown.wav")

        # Lệnh FFmpeg làm chậm audio
        command = [
            "ffmpeg", "-i", input_file,
            "-filter:a", f"atempo={speed_factor}", "-vn",
            "-y", slowed_audio  # Ghi đè file nếu đã tồn tại
        ]
        subprocess.run(command, check=True)

        # Gọi hàm remove_silence để xử lý file đã làm chậm
        final_output = os.path.join(output_folder, f"{filename}_final.wav")
        result = remove_silence(slowed_audio, final_output)

        if os.path.exists(slowed_audio):
            os.remove(slowed_audio)

        return result
    except subprocess.CalledProcessError as e:
        print(f"❌ Lỗi khi chạy FFmpeg: {e}")
        return None


def remove_silence(input_audio, output_audio, silence_threshold="-40dB", min_silence_duration="0.5"):
    """Loại bỏ khoảng lặng trong file audio bằng FFmpeg"""
    try:
        command = [
            "ffmpeg",
            "-i", input_audio,
            "-af",
            f"silenceremove=stop_periods=-1:stop_duration={min_silence_duration}:stop_threshold={silence_threshold}",
            "-y",
            output_audio
        ]
        subprocess.run(command, check=True)
        return output_audio
    except subprocess.CalledProcessError as e:
        print(f"❌ Lỗi khi loại bỏ khoảng lặng: {e}")
        return None


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





def find_similar_lyrics_with_tf_idf(lyrics, vectorizer, tfidf_matrix):
    if not lyrics:
        print("🚫 Không thể so sánh do lỗi khi trích xuất lyrics.")
        return None

    lyrics_data = detect_languages_for_lyrics(lyrics)
    preprocessed_lyrics = preprocess_lyrics(lyrics, lyrics_data["multilingual"])

    if preprocessed_lyrics != "Không hợp lệ: Văn bản chứa nhiều ngôn ngữ.":
        vector = vectorizer.transform([preprocessed_lyrics])
        similarity_scores = cosine_similarity(vector, tfidf_matrix)[0]

        # Tìm index có độ tương đồng cao nhất
        best_index = similarity_scores.argmax()
        best_score = similarity_scores[best_index]

        # Lấy thông tin bài hát tương ứng
        best_match = df.iloc[best_index]

        result = {
            "Title": best_match["title"],
            "Lyrics": best_match["lyrics"],
            "Similarity": best_score
        }

        return result

    return "Không hợp lệ: Văn bản chứa nhiều ngôn ngữ."


def process_audio(input_file):
    # Tạo file tạm để lưu dữ liệu từ file object
    with tempfile.NamedTemporaryFile(delete=False, suffix=".wav") as temp_audio:
        temp_audio.write(input_file.read())  # Ghi dữ liệu vào file tạm
        temp_audio_path = temp_audio.name  # Lưu đường dẫn file tạm

    print(f"🔄 Đã lưu file tạm: {temp_audio_path}")

    # Gọi hàm để tách vocals từ file tạm
    vocals_path = separate_vocals(temp_audio_path)

    if vocals_path and os.path.exists(vocals_path):
        processed_audio = slow_down_audio_ffmpeg(vocals_path)

        # Xóa file tạm sau khi xử lý xong
        os.remove(temp_audio_path)
        os.remove(vocals_path)

        if processed_audio:
            print(f"🎵 Audio đã xử lý thành công: {processed_audio}")
            return processed_audio
        else:
            print(f"❌ Lỗi khi làm chậm audio cho {vocals_path}")
            return None

    print(f"❌ Lỗi: Không tìm thấy file vocals tại {vocals_path}")
    os.remove(temp_audio_path)
    return None


def predict_copyright(model_prediction, tfidf_vector):
    prob = model_prediction.predict_proba(tfidf_vector)[0][1]
    prediction_label = "⚠️ Copyrighted" if prob >= 0.5 else "✅ No Copyrighted"

    return prediction_label


def tfidf_lyrics(lyrics):
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
    df = fetch_songs()

    vectorizer = TfidfVectorizer(use_idf=True)
    tfidf_matrix = vectorizer.fit_transform(df["clean_lyrics"])

    lyrics_provided = input()
    tf_lyric = tfidf_lyrics(lyrics_provided)
    result_prediction_provided_lyrics = predict_copyright(rf, tf_lyric)
    result_similarity_provided_lyrics = find_similar_lyrics_with_tf_idf(lyrics_provided, vectorizer, tfidf_matrix)
    if result_prediction_provided_lyrics == "✅ No Copyrighted":
        if result_similarity_provided_lyrics and result_similarity_provided_lyrics["Similarity"] >= 0.5:
            print("🚫 Bài hát đã tồn tại!")
        else:
            print("✅ Bài hát chưa tồn tại, bạn có thể đăng nhạc của mình lên!")
    elif result_prediction_provided_lyrics == "⚠️ Copyrighted" and result_similarity_provided_lyrics["Similarity"] < 0.5:
        print("✅ Bài hát ko vi phạm bản quyền và chưa tồn tại, bạn có thể đăng nhạc của mình lên!")
    else:
        print("✅ Bài hát đã vi phạm bản quyền!")


    # input_file =
    # processed_audio = process_audio(input_file)
    # lyrics_whisper = transcribe_lyric_by_whisper(processed_audio)
    # tf_lyric_audio = tfidf_lyrics(lyrics_whisper)
    # result_prediction_audio_lyrics = predict_copyright(rf_best, lyrics_whisper)
    # if result_prediction_audio_lyrics == "✅ No Copyrighted":
    #     result_similarity_audio_lyrics = find_similar_lyrics_with_tf_idf(lyrics_whisper, vectorizer, tfidf_matrix)
    #     if result_similarity_audio_lyrics and result_similarity_audio_lyrics["Similarity"] >= 0.5:
    #         print("🚫 Bài hát đã tồn tại!")
    #     else:
    #         print("✅ Bài hát chưa tồn tại, bạn có thể đăng nhạc của mình lên!")


if __name__ == "__main__":
    main()
