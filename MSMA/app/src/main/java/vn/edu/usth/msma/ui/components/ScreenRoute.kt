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
    object ArtistDetails : ScreenRoute("artist")
    object PlaylistDetails : ScreenRoute("playlist")
    object AlbumDetails : ScreenRoute("album")

    object ViewProfile: ScreenRoute("view_profile")
    object EditProfile: ScreenRoute("edit_profile")
    object ChangePasswordScreen: ScreenRoute("change_password_screen")
    object ViewHistoryListen: ScreenRoute("view_history_listen")
}