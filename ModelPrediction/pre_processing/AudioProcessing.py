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
