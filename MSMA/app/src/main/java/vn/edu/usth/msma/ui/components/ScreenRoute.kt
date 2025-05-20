package vn.edu.usth.msma.ui.components

import kotlinx.serialization.Serializable
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Serializable
sealed class ScreenRoute(val route: String) {
    // Auth
    object Login : ScreenRoute("login")
    object Register : ScreenRoute("register")
    object ForgotPassword : ScreenRoute("forgot_password")
    object Otp : ScreenRoute("otp/{email}/{sessionId}/{otpDueDate}") {
        fun createRoute(email: String, sessionId: String, otpDueDate: String) =
            "otp/$email/$sessionId/$otpDueDate"
    }
    object ResetPassword : ScreenRoute("reset_password/{sessionId}") {
        fun createRoute(sessionId: String) = "reset_password/$sessionId"
    }

    // Main
    @Serializable
    object NotificationScreen : ScreenRoute("notification_screen")

    object Home : ScreenRoute("home")
    object Search : ScreenRoute("search")
    object Library : ScreenRoute("library")
    object Settings : ScreenRoute("settings")

    object SongDetails : ScreenRoute("songDetails/{songJson}?fromMiniPlayer={fromMiniPlayer}") {
        fun createRoute(songJson: String, fromMiniPlayer: Boolean): String {
            val encodedJson = URLEncoder.encode(songJson, StandardCharsets.UTF_8.toString())
            return "songDetails/$encodedJson?fromMiniPlayer=$fromMiniPlayer"
        }
    }

    object Genre : ScreenRoute("genre/{genreJson}") {
        fun createRoute(genreJson: String): String {
            val encodedJson = URLEncoder.encode(genreJson, StandardCharsets.UTF_8.toString())
            return "genre/$encodedJson"
        }
    }

    object FavoriteSongs : ScreenRoute("favorite_songs")
    object Top10TrendingSongs : ScreenRoute("top10_trending")
    object Top15DownloadedSongs : ScreenRoute("top15_downloaded")

    object ArtistDetails : ScreenRoute("artist/{artistDetailsJson}") {
        fun createRoute(artistDetailsJson: String): String {
            val encodedJson = URLEncoder.encode(artistDetailsJson, StandardCharsets.UTF_8.toString())
            return "artist/$encodedJson"
        }
    }

    object PlaylistDetails : ScreenRoute("playlist/{playlistJson}") {
        fun createRoute(playlistJson: String): String {
            val encodedJson = URLEncoder.encode(playlistJson, StandardCharsets.UTF_8.toString())
            return "playlist/$encodedJson"
        }
    }

    object AlbumDetails : ScreenRoute("album/{albumJson}") {
        fun createRoute(albumJson: String): String {
            val encodedJson = URLEncoder.encode(albumJson, StandardCharsets.UTF_8.toString())
            return "album/$encodedJson"
        }
    }

    object ViewProfile: ScreenRoute("view_profile")
    object EditProfile: ScreenRoute("edit_profile")
    object ChangePasswordScreen: ScreenRoute("change_password_screen")
    object ViewHistoryListen: ScreenRoute("view_history_listen")
}