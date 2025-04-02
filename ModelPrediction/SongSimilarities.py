import os
import re
import contractions
import pandas as pd
import spacy
import inflect
import whisper
import subprocess
from collections import defaultdict
from lingua import Language, LanguageDetectorBuilder
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.feature_extraction.text import TfidfVectorizer

# Initialize variables
vectorizer_tfidf = TfidfVectorizer(use_idf=True)


# Functions
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


# Remove stopwords with spaCy
def remove_stopwords(text, nlp):
    doc = nlp(text)
    return " ".join([token.text for token in doc if not token.is_stop])


# Lemmatization with spaCy
def lemmatize(text, nlp):
    doc = nlp(text)
    return " ".join([token.lemma_ for token in doc])


def pre_processing(custom_contractions, custom_remove, text):
    if text is None:
        return

    # Load spaCy English model
    nlp = spacy.load('en_core_web_sm')

    # Tạo engine để chuyển số thành chữ
    p = inflect.engine()

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
    lyrics_preprocess = re.sub(r"\[.*?\]", "", lyrics_preprocess)
    lyrics_preprocess = re.sub(r"\b(chorus|verse)\b", "", lyrics_preprocess, flags=re.IGNORECASE)
    lyrics_preprocess = re.sub(r"[^\w\s]", "", lyrics_preprocess)
    lyrics_preprocess = re.sub(r"[,:?\[\]{}\-+\\/|@#$*^&%~!();\"]", "", lyrics_preprocess)
    lyrics_preprocess = re.sub(r"x[1-9]", "", lyrics_preprocess)
    lyrics_preprocess = re.sub(r"\s+", " ", lyrics_preprocess).strip()

    lyrics_preprocess = remove_stopwords(lyrics_preprocess, nlp)
    lyrics_preprocess = lemmatize(lyrics_preprocess, nlp)

    return lyrics_preprocess


def detect_languages(text, detector):
    """Phát hiện ngôn ngữ và xác suất của đoạn văn bản."""
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


def preprocess_lyrics(text, custom_contractions, custom_remove, multilingual):
    if multilingual == "No":
        lyrics_test = pre_processing(custom_contractions, custom_remove, text)
        return lyrics_test
    return "Không hợp lệ: Văn bản chứa nhiều ngôn ngữ."


def transcribe_lyric_by_whisper(audio_path):
    """Dùng Whisper để trích xuất lyric từ file nhạc"""
    try:
        print("Đang chuyển đổi audio thành text bằng Whisper...")
        model_to_load = whisper.load_model("large")
        result = model_to_load.transcribe(audio_path, fp16=False)
        lyrics_transcribe = result["text"]

        print("🎤 Lời bài hát được trích xuất thành công!")
        return lyrics_transcribe
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


def process_audio(input_file):
    filename = os.path.splitext(os.path.basename(input_file))[0]

    vocals_path = separate_vocals(input_file)  # Tách vocal từ file gốc

    if vocals_path and os.path.exists(vocals_path):
        final_output_path = rf"D:\Pycharm_Projects\Python_Projects\ModelPrediction\Music-Streaming-and-Management-Application\ModelPrediction\final_output\{filename}_vocals_final.wav"

        if os.path.exists(final_output_path):
            print(f"✅ File đã tồn tại, không cần xử lý lại: {final_output_path}")
            return final_output_path

        processed_audio = slow_down_audio_ffmpeg(vocals_path)
        if processed_audio:
            print(f"🎵 Audio đã xử lý thành công: {processed_audio}")
            return processed_audio
        else:
            print(f"❌ Lỗi khi processing audio cho {vocals_path}")
            return None
    print(f"❌ Lỗi: Không tìm thấy file vocals tại {vocals_path}")
    return None


def check_similarity(lyrics_testing, custom_contractions, custom_remove):
    df = pd.read_csv("data/filtered_lyrics_5000_each_genre.csv", encoding='utf-8')
    df_cosine_similarity = df

    documents = df_cosine_similarity["clean_lyrics"]
    tfidf_matrix = vectorizer_tfidf.fit_transform(documents)

    if not lyrics_testing:
        print("🚫 Không thể so sánh do lỗi khi trích xuất lyrics.")
        return

    lyrics_data = detect_languages_for_lyrics(lyrics_testing)
    preprocessed_lyrics = preprocess_lyrics(lyrics_testing, custom_contractions, custom_remove,
                                            lyrics_data["multilingual"])

    if preprocessed_lyrics != "Không hợp lệ: Văn bản chứa nhiều ngôn ngữ.":
        vector = vectorizer_tfidf.transform([preprocessed_lyrics])

        similarity_scores = cosine_similarity(vector, tfidf_matrix)[0]

        df_sim_whisper = pd.DataFrame({
            "Song": df_cosine_similarity["song"].values,
            "Lyrics": df_cosine_similarity["clean_lyrics"].values,
            "Similarity": similarity_scores
        })
        df_sim_whisper = df_sim_whisper.sort_values(by="Similarity", ascending=False)

        return df_sim_whisper.head(5)
    return "Không hợp lệ: Văn bản chứa nhiều ngôn ngữ."


def main():
    """Hàm chính để nhập lời bài hát và dự đoán thể loại."""
    custom_contractions = load_contractions("custom/custom_contractions.txt")
    custom_remove = load_remove("custom/custom_remove.txt")

    lyrics_input = input("Nhập lời bài hát hoặc đường dẫn file: ").strip()

    if lyrics_input.startswith(
            "D:\\Pycharm_Projects\\Python_Projects\\ModelPrediction\\Music-Streaming-and-Management-Application\\ModelPrediction\\songs_input"):
        # Nếu nhập đường dẫn file audio
        if os.path.exists(lyrics_input):  # Kiểm tra file có tồn tại không
            audio = process_audio(lyrics_input)
            lyrics_whisper = transcribe_lyric_by_whisper(audio)
            df_result = check_similarity(lyrics_whisper, custom_contractions, custom_remove)
            if df_result is not None and not df_result.empty:
                if df_result.iloc[0]["Similarity"] >= 0.75:
                    print(f"=> Thể loại của bài hát: {df_result.iloc[0]['Song']}")
                else:
                    print("=> Không có bài hát nào bị trùng với bài hát này.")
            else:
                print("⚠️ Không thể tìm thấy bài hát phù hợp.")
        else:
            print("⚠️ Lỗi: Tệp tin không tồn tại.")
            return
    else:
        # Nếu nhập trực tiếp lời bài hát
        df_result = check_similarity(lyrics_input, custom_contractions, custom_remove)
        if df_result is not None and not df_result.empty:
            if df_result.iloc[0]["Similarity"] >= 0.75:
                print(f"=> Thể loại của bài hát: {df_result.iloc[0]['Song']}")
            else:
                print("=> Không có bài hát nào bị trùng với bài hát này.")
        else:
            print("⚠️ Không thể tìm thấy bài hát phù hợp.")


if __name__ == "__main__":
    main()
