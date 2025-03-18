import os
import re
import shutil
import psycopg2
import lyricsgenius
import requests
import yt_dlp
import urllib
import urllib.parse
import urllib.request

from mutagen.easyid3 import EasyID3
from rich.console import Console

# Genius API (Lyrics)
GENIUS_ACCESS_TOKEN = "xdBn9BO3O6qSRZdz2zUB5BKzA1SdNL8JR7KQ37VN12xFVlJkoUFWhrlH1QsAR5d0"
console = Console()

# PostgreSQL
conn = psycopg2.connect(
    dbname="copyrighted_check",
    user="postgres",
    password="kinhart822",
    host="localhost",
    port="5432"
)
cursor = conn.cursor()


# TODO: Get the lyric
def transcribe_lyric(artist_name, song_title, access_token):
    try:
        genius = lyricsgenius.Genius(access_token)
        genius.timeout = 15
        genius.verbose = False

        song = genius.search_song(song_title, artist_name)

        if song is not None:
            return song.lyrics
        else:
            print("\n❌ Không tìm thấy bài hát trên Genius!")

            # Thử lấy lyrics từ Lyrics.ovh
            az_lyrics = get_lyrics_from_lyrics_ovh(artist_name, song_title)
            if az_lyrics:
                return az_lyrics
    except Exception as e:
        print(f"\n❌ Lỗi khi trích xuất lời bài hát: {e}")


# TODO: Get the lyric by Lyrics.ovh
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


# TODO: Split the lyrics into different parts
def split_lyrics(lyrics):
    try:
        if not lyrics or lyrics.strip() == "":
            print("❌ Lời bài hát trống!")
            return None

        sections = re.split(r"\[(.*?)\]", lyrics)
        song_parts = {
            "intro": "",
            "body_1": "",
            "body": "",
            "body_2": "",
            "outro": ""
        }
        current_section = "body"
        has_intro = False
        has_outro = False
        collected_body = []

        if len(sections) > 1:
            for i in range(1, len(sections), 2):
                header = sections[i].strip().lower()
                content = sections[i + 1].strip() if i + 1 < len(sections) else ""

                if "intro" in header:
                    song_parts["intro"] = content
                    has_intro = True
                    current_section = "intro"
                elif "outro" in header or "ending" in header:
                    song_parts["outro"] = content
                    has_outro = True
                    current_section = "outro"
                elif "verse 1" in header or "chorus" in header:
                    collected_body.append(content)
                    current_section = "body"
                elif "verse" in header or "chorus" in header or "bridge" in header:
                    collected_body.append(content)
                    current_section = "body"
                else:
                    if current_section == "body":
                        collected_body.append(content)
                    else:
                        song_parts[current_section] += "\n\n" + content

            if not collected_body:
                parts = lyrics.strip().split("\n")

                if len(parts) <= 3:
                    song_parts["body"] = "\n".join(parts).strip()
                else:
                    if not has_intro:
                        song_parts["body_1"] = parts[0].strip()
                    song_parts["body"] = "\n".join(parts[1:-1]).strip()
                    if not has_outro:
                        song_parts["body_2"] = parts[-1].strip()
            else:
                if not has_intro:
                    song_parts["body_1"] = collected_body[0]
                song_parts["body"] = "\n".join(collected_body[1:-1])
                if not has_outro:
                    song_parts["body_2"] = collected_body[-1]

        return song_parts
    except Exception as e:
        print(f"❌ Lỗi khi tách phần câu hát: {e}")
        return None


# TODO: Get data from 'songs' table
def get_songs_from_db():
    try:
        query = "SELECT id, title, artist FROM public.songs ORDER BY id ASC;"
        cursor.execute(query)
        return cursor.fetchall()
    except Exception as e:
        print(f"❌ Lỗi khi lấy dữ liệu từ database: {e}")
        return []


# TODO: Get lyrics & save into DB
def process_lyrics(access_token):
    songs = get_songs_from_db()

    for song_id, title, artist in songs:
        print(f"\n🔎 Đang xử lý: {artist} - {title}")
        lyrics = transcribe_lyric(artist, title, access_token)

        if lyrics:
            parts = split_lyrics(lyrics)
            if parts:
                insert_lyrics_to_db(song_id, parts)
            else:
                print(f"❌ Không thể tách lyrics cho {artist} - {title}")
        else:
            print(f"❌ Không có lyrics cho {artist} - {title}")


# TODO: Insert lyrics into song_specifications
def insert_lyrics_to_db(song_id, parts):
    try:
        query = """
            INSERT INTO song_specifications (song_id, intro, body_1, body, body_2, outro) 
            VALUES (%s, %s, %s, %s, %s, %s)
            ON CONFLICT (song_id) 
            DO UPDATE SET 
                intro = EXCLUDED.intro, 
                body_1 = EXCLUDED.body_1, 
                body = EXCLUDED.body, 
                body_2 = EXCLUDED.body_2, 
                outro = EXCLUDED.outro;
        """
        cursor.execute(query, (
            song_id,
            parts.get("intro", ""),
            parts.get("body_1", ""),
            parts.get("body", ""),
            parts.get("body_2", ""),
            parts.get("outro", "")
        ))
        conn.commit()
        print(f"✅ Đã chèn lyrics cho bài hát ID {song_id}")

    except Exception as e:
        print(f"❌ Lỗi khi chèn dữ liệu vào DB: {e}")
        conn.rollback()


# TODO: Get empty song_specifications
def get_empty_songs():
    try:
        query = """
            SELECT s.id, s.title, s.artist
            FROM songs s
            JOIN song_specifications ss ON s.id = ss.song_id
            WHERE (ss.intro IS NULL OR ss.intro = '') 
            AND (ss.body_1 IS NULL OR ss.body_1 = '') 
            AND (ss.body IS NULL OR ss.body = '') 
            AND (ss.body_2 IS NULL OR ss.body_2 = '') 
            AND (ss.outro IS NULL OR ss.outro = '');
        """

        cursor.execute(query)
        empty_songs = cursor.fetchall()

        if empty_songs:
            print("Songs with all empty fields:")
            for song in empty_songs:
                print(f"Song ID: {song[0]} - Song title: {song[1]} - Song artist: {song[2]}")
                delete_track(song[1], song[2])
        else:
            print("No songs found with all fields empty.")
    except Exception as e:
        print(f"Error: {e}")


# TODO: Delete track that have lyric null or instrumental only
def delete_track(title, artist):
    # Directories where the music files are stored
    directories = [
        r"D:\Pycharm_Projects\Python_Projects\music\copyrighted",
        r"D:\Pycharm_Projects\Python_Projects\music\no_copyrighted"
    ]

    try:
        # Delete from database
        query = "DELETE FROM songs WHERE title = %s AND artist = %s"
        cursor.execute(query, (title, artist))
        conn.commit()

        # Remove corresponding files
        filename = f"{title}.mp3"
        for directory in directories:
            file_path = os.path.join(directory, filename)
            if os.path.exists(file_path):
                os.remove(file_path)
                print(f"🗑️ Deleted: {file_path}")
            else:
                print(f"⚠️ File not found: {file_path}")

        print(f"✅ Deleted from database: {title} - {artist}")

    except Exception as e:
        print(f"❌ Error deleting from database: {e}")
        conn.rollback()


# TODO: Delete track duplicates
def delete_track_duplicates():
    # Directories where the music files are stored
    directories = [
        r"D:\Pycharm_Projects\Python_Projects\music\copyrighted",
        r"D:\Pycharm_Projects\Python_Projects\music\no_copyrighted"
    ]

    try:
        # Delete from database
        query_find_duplicates = """
                    SELECT TITLE 
                    FROM public.songs
                    GROUP BY TITLE
                    HAVING COUNT(*) > 1;
                """
        cursor.execute(query_find_duplicates)
        duplicate_titles = cursor.fetchall()

        if not duplicate_titles:
            print("✅ Không có bài hát trùng lặp để xóa.")
            return

        for (title,) in duplicate_titles:
            query_delete = "DELETE FROM public.songs WHERE TITLE = %s"
            cursor.execute(query_delete, (title,))
            conn.commit()

            # Xóa file MP3 tương ứng
            filename = f"{title}.mp3"
            for directory in directories:
                file_path = os.path.join(directory, filename)
                if os.path.exists(file_path):
                    os.remove(file_path)
                    print(f"🗑️ Đã xóa file: {file_path}")
                else:
                    print(f"⚠️ Không tìm thấy file: {file_path}")

            print(f"✅ Đã xóa bài hát trùng lặp: {title}")

    except Exception as e:
        print(f"❌ Lỗi khi xóa bài hát: {e}")
        conn.rollback()


# TODO: Check .mp3 files existence
def check_song_files():
    directories = {
        1: r"D:\Pycharm_Projects\Python_Projects\music\copyrighted",
        0: r"D:\Pycharm_Projects\Python_Projects\music\no_copyrighted"
    }
    try:
        # Query to get all songs with their label_copyrighted status
        query = "SELECT title, artist, label_copyrighted FROM songs"
        cursor.execute(query)
        songs = cursor.fetchall()

        missing_files = []

        for title, artist, label in songs:
            formatted_title = title.replace("/", "-").replace("?", " ")
            filename = f"{formatted_title}.mp3"

            if label in directories:
                file_path = os.path.join(directories[label], filename)
                if not os.path.exists(file_path):
                    missing_files.append((title, artist, file_path))

        # Output missing files
        if missing_files:
            print("\n❌ Missing files:")
            for title, artist, path in missing_files:
                print(f"* {title} - {artist} not found at {path}")
        else:
            print("\n✅ All songs have corresponding files.")

    except Exception as e:
        print(f"❌ Error checking song files: {e}")


# TODO: Tìm kiếm bài hát dựa trên metadata
def find_youtube(query):
    try:
        search_url = f"https://www.youtube.com/results?search_query={urllib.parse.quote(query)}"
        response = urllib.request.urlopen(search_url)
        search_results = re.findall(r"watch\?v=(\S{11})", response.read().decode("utf-8"))

        if search_results:
            return f"https://www.youtube.com/watch?v={search_results[0]}"
        else:
            return None

    except Exception as e:
        print(f"❌ Failed to find YouTube video: {e}")
        return None


# TODO: Tải bài hát về dưới dạng .mp3 file
def download_yt(yt_link, track_title, track_artist):
    try:
        output_dir = "../music/tmp"
        os.makedirs(output_dir, exist_ok=True)

        ydl_opts = {
            "format": "bestaudio/best",
            "outtmpl": f"{output_dir}/%(title)s.%(ext)s",
            "quiet": True,
            "ffmpeg_location": "C:/Users/ADMIN/Downloads/ffmpeg/bin",
            "postprocessors": [{
                "key": "FFmpegExtractAudio",
                "preferredcodec": "mp3",
                "preferredquality": "192",
            }],
        }

        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            ydl.extract_info(yt_link, download=True)

        # Kiểm tra file tải về
        downloaded_files = os.listdir(output_dir)
        for file in downloaded_files:
            if file.endswith(".mp3"):
                old_path = os.path.join(output_dir, file)
                new_path = os.path.join(output_dir, f"{track_title} - {track_artist}.mp3")
                os.rename(old_path, new_path)
                print(f"✅ Đã tải: {new_path}")
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


# TODO: Download missing tracks
def download_missing_tracks():
    """Tìm kiếm và tải về những bài hát bị thiếu."""
    directories = {
        1: r"D:\Pycharm_Projects\Python_Projects\music\copyrighted",
        0: r"D:\Pycharm_Projects\Python_Projects\music\no_copyrighted"
    }

    query = "SELECT title, artist, label_copyrighted FROM songs"
    cursor.execute(query)
    songs = cursor.fetchall()

    for title, artist, label in songs:
        formatted_title = title.replace("/", "-").replace("?", " ")
        filename = f"{formatted_title}.mp3"

        if label in directories:
            file_path = os.path.join(directories[label], filename)
            if not os.path.exists(file_path):
                print(f"\n🚀 Đang tải {title} - {artist}...")
                search_term = f"{artist} {title} lyrics"
                yt_link = find_youtube(search_term)

                if yt_link:
                    downloaded_file = download_yt(yt_link, title, artist)
                    if downloaded_file:
                        set_metadata({"artist_name": artist, "track_title": title}, downloaded_file)

                        # Di chuyển file về thư mục đích
                        os.makedirs(directories[label], exist_ok=True)
                        final_path = os.path.join(directories[label], filename)
                        os.replace(downloaded_file, final_path)

                        print(f"✅ Hoàn thành tải: {title} -> {final_path}")
                else:
                    print(f"❌ Không tìm thấy video cho {title}")

    # Dọn dẹp thư mục tạm
    clean_temp_folder("../music/tmp")


# TODO: Clean temporary folder
def clean_temp_folder(folder):
    if os.path.exists(folder):
        shutil.rmtree(folder)


def main():
    # process_lyrics(GENIUS_ACCESS_TOKEN)
    # get_empty_songs()
    # check_song_files()
    # download_missing_tracks()
    delete_track_duplicates()


if __name__ == "__main__":
    main()
