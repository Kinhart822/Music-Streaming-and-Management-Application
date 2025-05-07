package vn.edu.usth.msma.ui.screen.search.genres

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import vn.edu.usth.msma.data.PreferencesManager
import vn.edu.usth.msma.data.dto.response.management.GenreResponse
import vn.edu.usth.msma.network.ApiService
import vn.edu.usth.msma.network.CustomAuthenticator
import vn.edu.usth.msma.ui.theme.MSMATheme
import javax.inject.Inject

@AndroidEntryPoint
class GenreActivity : ComponentActivity() {
    private val viewModel: GenreViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val genreJson = intent.getStringExtra("genre")
        val genre = genreJson?.let { Gson().fromJson(it, GenreResponse::class.java) }
            ?: throw IllegalArgumentException("Genre data is required")

        // Initialize ViewModel with genre data
        viewModel.initialize(genre)

        setContent {
            MSMATheme {
                GenreScreen(
                    genre = genre,
                    onBack = { finish() },
                    viewModel = viewModel
                )
            }
        }
    }
}