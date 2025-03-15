import re

import lyricsgenius
import spotipy
from spotipy import SpotifyClientCredentials

GENIUS_ACCESS_TOKEN = "O25c0048MGusObOSDAcJ6PqlLT1p6vTo4w3de-YdvklrZNeUDzNjDr0A_HQwIbNO"

# Spotify API
CLIENT_ID = "43a1e91076d24a48b2a8c1a3cb6c8f91"
CLIENT_SECRET = "97f5a6f636004e6b973d158d266d227a"

sp = spotipy.Spotify(auth_manager=SpotifyClientCredentials(client_id=CLIENT_ID, client_secret=CLIENT_SECRET))


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
    except Exception as e:
        print(f"\n❌ Lỗi khi trích xuất lời bài hát: {e}")


track = get_track_info("0C1dOJr5ae2tJSUGxMXTcC")
track_title = track.get('track_title', 'Unknown')
match = re.match(r'^[a-zA-Z0-9\s]+', track_title)
title_part = match.group(0).strip() if match else track_title
lyric = transcribe_lyric(track.get('artist_name', 'Unknown'), title_part, GENIUS_ACCESS_TOKEN)
print(lyric)
