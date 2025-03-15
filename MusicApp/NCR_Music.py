import csv
import requests
import spotipy
from spotipy.oauth2 import SpotifyClientCredentials
import lyricsgenius
import whisper
import psycopg2
import urllib
import urllib.parse
import urllib.request
import yt_dlp
from mutagen.easyid3 import EasyID3
from rich.console import Console
import warnings
import re
import os
import torch
import shutil
import time
from datetime import datetime
import pytz

# Spotify API
CLIENT_ID = "43a1e91076d24a48b2a8c1a3cb6c8f91"
CLIENT_SECRET = "97f5a6f636004e6b973d158d266d227a"

# Genius API (Lyrics)
GENIUS_ACCESS_TOKEN = "O25c0048MGusObOSDAcJ6PqlLT1p6vTo4w3de-YdvklrZNeUDzNjDr0A_HQwIbNO"

# PostgreSQL
conn = psycopg2.connect(
    dbname="copyrighted_check",
    user="postgres",
    password="kinhart822",
    host="localhost",
    port="5432"
)
cursor = conn.cursor()

# Khởi tạo Spotify API client
sp = spotipy.Spotify(auth_manager=SpotifyClientCredentials(client_id=CLIENT_ID, client_secret=CLIENT_SECRET))
console = Console()
warnings.filterwarnings("ignore", category=UserWarning)


# TODO: Fetch Playlist (Tracks Information)
def fetch_playlist_tracks_info(playlist_url):
    """Lấy toàn bộ bài hát từ playlist trên Spotify (không giới hạn 100 bài)"""
    tracks = fetch_playlist_tracks(playlist_url)

    print(f"📀 Total Tracks: {len(tracks)}\n")

    # Hiển thị danh sách bài hát
    for idx, item in enumerate(tracks, start=1):
        if item["track"]:  # Kiểm tra track có tồn tại không
            track = item["track"]
            artists = ', '.join(artist['name'] for artist in track['artists'])
            print(f"{idx}. {track['name']} - {artists}")
            print(f"   🎧 Spotify URL: {track['external_urls']['spotify']}\n")


# TODO: Fetch Playlist Tracks
def fetch_playlist_tracks(playlist_url):
    """Lấy toàn bộ bài hát từ playlist trên Spotify (không giới hạn 100 bài)"""
    # Lấy ID playlist từ URL
    playlist_id = playlist_url.split("/")[-1].split("?")[0]

    # Gọi API để lấy dữ liệu playlist
    playlist_data = sp.playlist(playlist_id)

    # Lấy tất cả bài hát từ playlist bằng pagination
    tracks = []
    results = playlist_data["tracks"]

    while results:
        tracks.extend(results["items"])  # Lưu các bài hát vào danh sách
        results = sp.next(results)  # Lấy trang tiếp theo (nếu có)

    return tracks


# TODO: Lấy thông tin chi tiết bài hát ()
def get_track_info(track_id):
    """Lấy thông tin chi tiết bài hát bằng Spotipy"""
    try:
        track = sp.track(track_id)
        if not track:
            print(f"❌ Không tìm thấy thông tin cho track_id: {track_id}")
            return None

        main_artist = track.get("artists", [{}])[0].get("name", "Unknown Artist")
        additional_artists = ", ".join(artist["name"] for artist in track["artists"][1:]) if len(
            track["artists"]) > 1 else None
        album_images = track["album"]["images"]
        art_small = album_images[2]["url"] if len(album_images) > 2 else None
        art_medium = album_images[1]["url"] if len(album_images) > 1 else None
        art_big = album_images[0]["url"] if len(album_images) > 0 else None

        return {
            "artist_name": main_artist,
            "additional_artists": additional_artists,
            "track_title": track.get("name", "Unknown Track"),
            "track_number": track.get("track_number", 0),
            "duration_ms": track.get("duration_ms", 0),
            "album_name": track.get("album", {}).get("name", "Unknown Album"),
            "release_date": track.get("album", {}).get("release_date", "Unknown Date"),
            "spotify_url": track.get("external_urls", {}).get("spotify", ""),
            "album_art_small": art_small,
            "album_art_medium": art_medium,
            "album_art_big": art_big
        }
    except Exception as e:
        raise ValueError(f"Failed to fetch track info: {e}")


# TODO: Tìm kiếm bài hát dựa trên metadata
def find_youtube(query):
    try:
        search_url = f"https://www.youtube.com/results?search_query={urllib.parse.quote(query)}"
        response = urllib.request.urlopen(search_url)
        search_results = re.findall(r"watch\?v=(\S{11})", response.read().decode('utf-8'))
        return f"https://www.youtube.com/watch?v={search_results[0]}"
    except Exception as e:
        raise ValueError(f"Failed to find YouTube video: {e}")


# TODO: Tải bài hát về dưới dạng .mp3 file
def download_yt(yt_link, track_title):
    try:
        output_dir = "../music/tmp11"
        os.makedirs(output_dir, exist_ok=True)

        ydl_opts = {
            'format': 'bestaudio/best',
            'outtmpl': f"{output_dir}/%(title)s.%(ext)s",
            'quiet': True,
            'ffmpeg_location': 'C:/Users/ADMIN/Downloads/ffmpeg/bin',
            'postprocessors': [{
                'key': 'FFmpegExtractAudio',
                'preferredcodec': 'mp3',
                'preferredquality': '192',
            }],
        }

        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            ydl.extract_info(yt_link, download=True)

            downloaded_files = os.listdir(output_dir)
            for file in downloaded_files:
                if file.endswith(".mp3"):  # Đảm bảo chỉ xử lý file MP3
                    old_path = os.path.join(output_dir, file)
                    new_path = os.path.join(output_dir, f"{track_title}.mp3")
                    os.rename(old_path, new_path)
                    print(f"✅ Đã đổi tên: {old_path} -> {new_path}")
                    return new_path

    except Exception as e:
        print(f"[ERROR] Exception: {e}")
        return None


# TODO: Set metadata to .mp3 file
def set_metadata(metadata, file_path):
    try:
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"File not found: {file_path}")

        # Thiết lập metadata cơ bản
        mp3file = EasyID3(file_path)
        mp3file["albumartist"] = metadata.get("artist_name", "Unknown Artist")
        if metadata.get("additional_artists"):
            mp3file["artist"] = metadata["additional_artists"]
        else:
            mp3file["artist"] = metadata.get("artist_name", "Unknown Artist")
            mp3file["album"] = metadata.get("album_name", "Unknown Album")

            track_title = metadata.get("track_title", "Unknown Title")
            match = re.match(r'^[^-()]+', track_title)
            title_part = match.group(0).strip() if match else track_title
            mp3file["title"] = title_part

            mp3file["date"] = metadata.get("release_date", "Unknown Date")
            mp3file["tracknumber"] = str(metadata.get("track_number", "1"))
            mp3file.save()

        print(f"✅ Đã gán metadata cho file: {file_path}")

    except Exception as e:
        raise ValueError(f"❌ Lỗi khi gán metadata: {e}")


# TODO: Transcribe the lyric
def transcribe_lyric(artist_name, song_title, access_token):
    try:
        genius = lyricsgenius.Genius(access_token)
        genius.timeout = 15  # Tăng thời gian timeout lên 15 giây
        genius.verbose = False  # Tắt log không cần thiết
        genius.remove_section_headers = True  # Xóa tiêu đề như [Chorus], [Verse 1]

        song = genius.search_song(song_title, artist_name)

        if song is not None:
            print("✅ Lời bài hát tìm thấy!")
            return song.lyrics
        else:
            print("\n❌ Không tìm thấy bài hát trên Genius!")

            # Thử lấy lyrics từ Lyrics.ovh
            az_lyrics = get_lyrics_from_lyrics_ovh(artist_name, song_title)
            if az_lyrics:
                print("✅ Lời bài hát tìm thấy trên Lyrics.ovh!")
                return az_lyrics
            else:
                return
    except Exception as e:
        print(f"\n❌ Lỗi khi trích xuất lời bài hát: {e}")


# TODO: Transcribe the lyric by Lyrics.ovh
def get_lyrics_from_lyrics_ovh(artist, title):
    try:
        url = f"https://api.lyrics.ovh/v1/{artist}/{title}"
        response = requests.get(url)

        if response.status_code == 200:
            data = response.json()
            return data.get("lyrics", "❌ Không tìm thấy lời bài hát!")
        else:
            print("❌ Không tìm thấy bài hát trên Lyrics.ovh!")

    except Exception as e:
        return f"❌ Lỗi khi lấy lyrics: {e}"


# TODO: Transcribe the lyric by whisper
def transcribe_lyric_by_whisper(audio_path):
    """Dùng Whisper để trích xuất lyric từ file nhạc"""
    try:
        console.print("[yellow]Đang chuyển đổi audio thành text bằng Whisper...[/yellow]")
        device = "cuda" if torch.cuda.is_available() else "cpu"
        model = whisper.load_model("medium", device=device)
        result = model.transcribe(audio_path)
        lyrics = result["text"]

        console.print("[green]🎤 Lời bài hát được trích xuất thành công![/green]")
        return lyrics
    except Exception as e:
        console.print(f"[red]Lỗi khi trích xuất lời bài hát: {e}[/red]")
        return None


# TODO: Download track
def log_missing_lyric(track_info):
    file_path = "../music/no_copyrighted/no_lyric_tracks.csv"
    os.makedirs(os.path.dirname(file_path), exist_ok=True)
    file_exists = os.path.isfile(file_path)

    existing_tracks = set()
    if file_exists:
        with open(file_path, mode="r", encoding="utf-8") as file:
            reader = csv.reader(file)
            next(reader, None)
            for row in reader:
                existing_tracks.add(tuple(row))

    track_title = track_info.get('track_title', 'Unknown')
    match = re.match(r'^[^-()]+', track_title)
    title_part = match.group(0).strip() if match else track_title
    new_entry = (track_info.get('artist_name', 'Unknown'), title_part)

    if new_entry not in existing_tracks:
        with open(file_path, mode="a", newline="", encoding="utf-8") as file:
            writer = csv.writer(file)
            if not file_exists:
                writer.writerow(["Artist", "Track Title"])
            writer.writerow(new_entry)


def download_track(track_info):
    start_time = time.time()
    track_title = track_info.get('track_title', 'Unknown')

    # Lọc bỏ ký tự không hợp lệ
    match = re.match(r'^[^-()]+', track_title)
    title_part = match.group(0).strip() if match else track_title
    title_part = re.sub(r'[:*?"<>|]', " ", title_part)
    title_part = title_part.replace("/", "-").replace("\\", "-")

    search_term = f"{track_info.get('artist_name', 'Unknown')} {title_part} lyrics"

    try:
        video_link = find_youtube(search_term)
        if not video_link:
            console.print(f"[red]Không tìm thấy video cho {title_part}[/red]")
            return

        console.print(f"Downloading '[cyan]{track_info.get('artist_name', 'Unknown')}: {title_part}[/cyan]'...")
        audio = download_yt(video_link, title_part)

        if not audio:
            print("[ERROR] Không tìm thấy file tải về!")
            return

        # Gán metadata kèm lyrics
        set_metadata(track_info, audio)

        destination = os.path.join("../music/no_copyrighted", f"{title_part}.mp3")
        os.makedirs(os.path.dirname(destination), exist_ok=True)

        if os.path.exists(destination):
            console.print("[yellow]File already exists. Updating database...[/yellow]")
            delete_track_from_db(track_info)
            os.remove(destination)

        # Di chuyển file mới vào thư mục lưu trữ
        os.replace(audio, destination)

        # Gọi hàm insert vào SQL
        lyric_text = transcribe_lyric(track_info.get('artist_name', 'Unknown'), title_part, GENIUS_ACCESS_TOKEN)

        if lyric_text:
            insert_track_into_db(track_info, lyric_text)
            clean_temp_folder("../music/tmp11")
            console.print(f"\nDOWNLOAD COMPLETED")
            console.print(f"Total time taken: {round(time.time() - start_time)} seconds", style="on white")
        else:
            console.print("[yellow]Không tìm thấy lyrics.[/yellow]")
            clean_temp_folder("../music/tmp11")
            log_missing_lyric(track_info)
            os.remove(destination)
            return

    except Exception as e:
        console.print(f"[red]Error downloading {title_part}: {e}[/red]")


# TODO: Download all tracks
def download_all_tracks(tracks):
    downloaded = 0

    for idx, item in enumerate(tracks, start=1):
        if item["track"]:
            track = item["track"]

            spotify_url = track.get('external_urls', {}).get('spotify', '')

            track_id = spotify_url.split("/")[-1].split("?")[0]

            track_info = get_track_info(track_id)
            if download_track(track_info):
                downloaded += 1


# TODO: Clean temporary folder
def clean_temp_folder(folder):
    if os.path.exists(folder):
        shutil.rmtree(folder)


# TODO: Insert to PostgreSQL Database
def insert_track_into_db(track_info, lyric_text):
    try:
        query = """
        INSERT INTO songs (title, release_date, lyrics, duration, art_small_url, art_medium_url, 
                art_big_url, album_name, artist, additional_artists, count_listen, history_listen, label_copyrighted)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """

        timezone = pytz.timezone("Asia/Ho_Chi_Minh")
        current_time = datetime.now(timezone).strftime("%d/%m/%Y %H:%M:%S"),

        track_title = track_info.get('track_title', 'Unknown')
        match = re.match(r'^[^-()]+', track_title)
        title_part = match.group(0).strip() if match else track_title
        data = (
            title_part,
            track_info.get("release_date"),
            lyric_text,
            track_info.get("duration_ms"),
            track_info.get("album_art_small"),
            track_info.get("album_art_medium"),
            track_info.get("album_art_big"),
            track_info.get("album_name"),
            track_info.get("artist_name"),
            track_info.get("additional_artists"),
            0,
            current_time,
            0
        )

        cursor.execute(query, data)
        conn.commit()

        track_title = track_info.get('track_title', 'Unknown')
        match = re.match(r'^[^-()]+', track_title)
        title_part = match.group(0).strip() if match else track_title
        print(f"✅ Đã lưu vào database: {title_part} - {track_info.get('artist_name')}")
    except Exception as e:
        print(f"❌ Lỗi khi lưu vào database: {e}")


# TODO: Delete track from DB
def delete_track_from_db(track_info):
    try:
        query = """
        DELETE FROM songs WHERE title = %s AND artist = %s
        """

        track_title = track_info.get('track_title', 'Unknown')
        match = re.match(r'^[^-()]+', track_title)
        title_part = match.group(0).strip() if match else track_title
        data = (title_part, track_info.get("artist_name"))

        cursor.execute(query, data)
        conn.commit()

        print(f"🗑️ Đã xóa khỏi database: {title_part} - {track_info.get('artist_name')}")
    except Exception as e:
        print(f"❌ Lỗi khi xóa khỏi database: {e}")


# TODO: Chương trình chính
def main():
    while True:
        user_input = input("Enter 'c' - to continue or 'n' - to out: ").strip().lower()
        if "n" in user_input:
            break
        elif "c" in user_input:
            fetch_playlist_tracks_info("https://open.spotify.com/playlist/3BCmzJOAYUapYwNIt8CAHN")
            while True:
                tracks = fetch_playlist_tracks("https://open.spotify.com/playlist/3BCmzJOAYUapYwNIt8CAHN")
                track_choice = input(
                    "\n'1...n' - Nhập số bài hát để xem chi tiết\n'D' - Download all the songs'\n'SD' - Specified Download\n'B' - Back\nChoose: ").strip().lower()

                if track_choice == 'b':
                    break

                if track_choice == 'sd':
                    start = int(input("Nhập số thứ tự bài hát muốn bắt đầu: "))
                    end = int(input("Nhập số thứ tự bài hát muốn kết thúc: "))
                    download_all_tracks(tracks[start - 1:end])

                if track_choice == 'd':
                    confirm = input("Bạn có chắc chắn muốn tải tất cả bài hát? (Y/N): ").strip().lower()
                    if confirm == 'y':
                        download_all_tracks(tracks)
                    else:
                        print("❌ Đã hủy tải tất cả bài hát.")
                    continue

                if track_choice.isdigit():
                    track_choice = int(track_choice)

                    if 1 <= track_choice <= len(tracks):
                        selected_track = tracks[track_choice - 1]["track"]
                        track_id = selected_track["id"]
                        track_info = get_track_info(track_id)

                        print("\n🎵 Thông tin bài hát:")
                        print(f"🎤 Nghệ sĩ: {track_info['artist_name']}")
                        print(f"💿 Album: {track_info['album_name']}")
                        print(f"📅 Ngày phát hành: {track_info['release_date']}")
                        print(
                            f"⏱️ Thời lượng: {track_info['duration_ms'] // 60000}:{(track_info['duration_ms'] // 1000) % 60:02d} phút")
                        print(f"🔗 Nghe trên Spotify: {track_info['spotify_url']}")

                        while True:
                            choice = input("'D' - Tải bài hát, 'B' - Quay lại: ").strip().lower()

                            if choice == 'b':
                                break
                            elif choice == 'd':
                                download_track(track_info)
                            else:
                                print("⚠️ Vui lòng nhập 'D' để tải hoặc 'B' để quay lại.")
                    else:
                        print("⚠️ Lựa chọn không hợp lệ, vui lòng thử lại.")
                else:
                    print("⚠️ Vui lòng nhập số hợp lệ hoặc 'N' để quay lại.")


if __name__ == "__main__":
    main()
