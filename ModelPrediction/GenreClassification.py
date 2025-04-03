import os
import re
import contractions
import spacy
import joblib
import inflect
import whisper
import subprocess
from collections import defaultdict
from lingua import Language, LanguageDetectorBuilder


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


def pre_processing(text):
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


def preprocess_lyrics(text, multilingual):
    if multilingual == "No":
        lyrics_test = pre_processing(text)
        return lyrics_test
    return "Không hợp lệ: Văn bản chứa nhiều ngôn ngữ."


def tfidf_lyrics(test_lyrics):
    """
        Chuyển đổi lyrics thành vector TF-IDF.
    """
    if not test_lyrics:
        print("🚫 Không có lyrics.")
        return None

    lyrics_data = detect_languages_for_lyrics(test_lyrics)
    preprocessed_lyrics = preprocess_lyrics(test_lyrics, lyrics_data["multilingual"])

    if preprocessed_lyrics != "Không hợp lệ: Văn bản chứa nhiều ngôn ngữ.":
        # Biến lyrics thành vector TF-IDF
        song_tfidf = vectorizer_tfidf.transform([preprocessed_lyrics])
        return song_tfidf

    return "Error"


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


def predict_genre(model_prediction, tfidf_vector):
    """Dự đoán thể loại từ mô hình đã huấn luyện."""
    predicted_genre = model_prediction.predict(tfidf_vector)[0]
    predicted_genre_name = label.inverse_transform([predicted_genre])[0]
    return predicted_genre_name


def main():
    """Hàm chính để nhập lời bài hát và dự đoán thể loại."""

    # lyrics_input = input("Nhập lời bài hát hoặc đường dẫn file: ").strip()
    lyrics_input = """Baby, I'm preying on you tonight
Hunt you down eat you alive
Just like animals
Animals
Like animals-mals
Maybe you think that you can hide
I can smell your scent for miles
Just like animals
Animals
Like animals-mals
Baby I'm
So what you trying to do to me
It's like we can't stop, we're enemies
But we get along when I'm inside you, eh
You're like a drug that's killing me
I cut you out entirely
But I get so high when I'm inside you
Yeah, you can start over you can run free
You can find other fish in the sea
You can pretend it's meant to be
But you can't stay away from me
I can still hear you making that sound
Taking me down rolling on the ground
You can pretend that it was me
But no, oh
Baby, I'm preying on you tonight
Hunt you down eat you alive
Just like animals
Animals
Like animals
Maybe you think that you can hide
I can smell your scent for miles
Just like animals
Animals
Like animals-mals
Baby, I'm
So if I run it's not enough
You're still in my head forever stuck
So you can do what you wanna do, eh
I love your lies I'll eat 'em up
But don't deny the animal
That comes alive when I'm inside you
Yeah, you can start over you can run free
You can find other fish in the sea
You can pretend it's meant to be
But you can't stay away from me
I can still hear you making that sound
Taking me down rolling on the ground
You can pretend that it was me
But no, oh
Baby I'm preying on you tonight
Hunt you down eat you alive
Just like animals
Animals
Like animals-mals
Maybe you think that you can hide
I can smell your scent for miles
Just like animals
Animals
Like animals-mals
Baby, I'm
Don't tell no lie, lie, lie, lie
You can't deny-ny-ny-ny
The beast inside-side-side-side
Yeah yeah yeah
No girl, don't lie, lie, lie, lie (no girl don't lie)
You can't deny, ny-ny-ny (you can't deny)
The beast inside-side-side-side
Yeah, yeah, yeah
Yo
Whoa
Whoa
Just like animals
Animals
Like animals-mals
Just like animals (yeah)
Animals (yeah)
Like animals-mals (yeah)
Ow
Baby I'm preying on you tonight
Hunt you down eat you alive
Just like animals
Animals
Like animals-mals
Maybe you think that you can hide
I can smell your scent for miles
Just like animals
Animals
Like animals-mals
Baby I'm
Don't tell no lie, lie, lie, lie
You can't deny-ny-ny-ny
The beast inside-side-side-side
Yeah yeah yeah
No girl, don't lie, lie, lie, lie (no, girl don't lie)
You can't deny-ny-ny-ny (you can't deny)
The beast inside-side-side-side
Yeah, yeah, yeah"""    # Animals
    lyrics_input = """
Girl, you know I want your love
Your love was handmade for somebody like me
Well, come on now, follow my lead
I may be crazy, don't mind me
Say, "Boy, let's not talk too much"
Grab on my waist and put that body on me
Well, come on now, follow my lead
Come, come on now, follow my lead, mm

I'm in love with the shape of you
We push and pull like a magnet do
Although my heart is fallin' too
I'm in love with your body
And last night, you were in my room
And now my bed sheets smell like you
Every day, discoverin' somethin' brand new
Well, I'm in love with your body

Oh, I, oh, I, oh, I, oh, I
Well, I'm in love with your body
Oh, I, oh, I, oh, I, oh, I
Well, I'm in love with your body
Oh, I, oh, I, oh, I, oh, I
Well, I'm in love with your body
Every day, discoverin' somethin' brand new
I'm in love with the shape of you

One week in, we let the story begin
We're goin' out on our first date (Mm)
You and me are thrifty, so go all-you-can-eat
Fill up your bag and I fill up a plate (Mm)
We talk for hours and hours about the sweet and the sour
And how your family is doin' okay (Mm)
And leave and get in a taxi, and kiss in the back seat
Tell the driver make the radio play and I'm singin' like

Girl, you know I want your love
Your love was handmade for somebody like me
Well, come on now, follow my lead
I may be crazy, don't mind me
Say, "Boy, let's not talk too much"
Grab on my waist and put that body on me
Well, come on now, follow my lead
Come, come on now, follow my lead, mm

Come on, be my baby, come on
Come on, be my baby, come on
I'm in love with your body
Come on, be my baby, come on
Come on, be my baby, come on
I'm in love with your body
Come on, be my baby, come on
Come on, be my baby, come on
I'm in love with your body
Every day, discoverin' somethin' brand new
I'm in love with the shape of you
"""    # Shape of you
    lyrics_input = """
    Days like this don't get much better
    Top down, good fam, good weather
    Days like this, we don't miss, never
    Yeah, yeah
    Last night, things got a little bit crazy
    Pulled up doin' 'bout 180
    Life goes fast, so there ain't no maybes
    Yeah, yeah

    Just give me that sunset, give me that ride
    Give me them stars out, four-wheel drive
    Takin' that back road, yeah, no lie
    Man, that sure feels nice
    Give me them real ones, ones I know
    Do a little two-step, dosido
    Turn it up loud, bring in that crowd
    Take it high, then drop it low

    We do this everywhere we go
    I think you know
    This ain't no one horse rodeo
    I think you know
    When we pull up, green lights our show
    I think you know
    That we gon' run it, run it, run it
    'Til we runnin' outta road (Oh, yeah)

    Run it, run it, run it
    'Til we runnin' outta road (Oh, yeah)
    (Oh, yeah)
    Run it, run it, run it
    'Til we runnin' outta road

    Wastin' time ain't overrated
    Not when you're celebrated
    Raise that glass, we're all here waitin'
    Yeah, yeah

    Just give me that sunset, give me that ride
    Give me that truck, that four-wheel drive
    Takin' that back road, yeah, no lie
    Man, that sure feels nice
    Give me them real ones, ones I know
    Do a little two-step, dosido
    Turn it up loud, bring in that crowd
    Take it up high, then drop it low

    We do this everywhere we go
    I think you know
    This ain't no one horse rodeo
    I think you know
    When we pull up, green lights our show
    I think you know
    That we gon' run it, run it, run it
    'Til we runnin' outta road (Oh, yeah)

    Run it, run it, run it
    'Til we runnin' outta road (Oh, yeah)
    (Oh, yeah)
    Run it, run it, run it
    'Til we runnin' outta road (Oh, yeah)
"""    # Run It
    lyrics_input = """Ooh
I, I just woke up from a dream
Where you and I had to say goodbye
And I don't know what it all means
But since I survived, I realized
Wherever you go, that's where I'll follow
Nobody's promised tomorrow
So I'ma love you every night like it's the last night
Like it's the last night
If the world was ending, I'd wanna be next to you
If the party was over and our time on Earth was through
I'd wanna hold you just for a while and die with a smile
If the world was ending, I'd wanna be next to you
Ooh
Ooh, lost, lost in the words that we scream
I don't even wanna do this anymore
'Cause you already know what you mean to me
And our love's the only war worth fighting for
Wherever you go, that's where I'll follow
Nobody's promised tomorrow
So I'ma love you every night like it's the last night
Like it's the last night
If the world was ending, I'd wanna be next to you
If the party was over and our time on Earth was through
I'd wanna hold you just for a while and die with a smile
If the world was ending, I'd wanna be next to you
Right next to you
Next to you
Right next to you
Oh-oh, oh
If the world was ending, I'd wanna be next to you
If the party was over and our time on Earth was through
I'd wanna hold you just for a while and die with a smile
If the world was ending, I'd wanna be next to you
If the world was ending, I'd wanna be next to you
Ooh
I'd wanna be next to you
"""    # Die with a smile

    if lyrics_input.startswith(
            "D:\\Pycharm_Projects\\Python_Projects\\ModelPrediction\\Music-Streaming-and-Management-Application\\ModelPrediction\\songs_input"):
        # Nếu nhập đường dẫn file audio
        if os.path.exists(lyrics_input):  # Kiểm tra file có tồn tại không
            audio = process_audio(lyrics_input)
            lyrics_whisper = transcribe_lyric_by_whisper(audio)
            lyrics_processed = tfidf_lyrics(lyrics_whisper)
        else:
            print("⚠️ Lỗi: Tệp tin không tồn tại.")
            return
    else:
        # Nếu nhập trực tiếp lời bài hát
        lyrics_processed = tfidf_lyrics(lyrics_input)

    # Dự đoán thể loại
    predicted_genre = predict_genre(rf_model_train, lyrics_processed)
    print(f"\n🎵 Thể loại dự đoán: {predicted_genre}")


if __name__ == "__main__":
    custom_contractions = load_contractions("custom/custom_contractions.txt")
    custom_remove = load_remove("custom/custom_remove.txt")

    vectorizer_tfidf = joblib.load('vectorizer_tfidf.pkl')
    label = joblib.load('label_encoder.pkl')
    rf_model_train = joblib.load('rf_model_tf_idf.pkl')

    main()
