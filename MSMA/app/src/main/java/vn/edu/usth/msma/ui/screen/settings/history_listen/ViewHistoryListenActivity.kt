package vn.edu.usth.msma.ui.screen.settings.history_listen

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import vn.edu.usth.msma.ui.theme.MSMATheme

@AndroidEntryPoint
class ViewHistoryListenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ViewHistoryListenActivity", "onCreate called")

        setContent {
            MSMATheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Log.d("ViewHistoryListenActivity", "Setting ViewHistoryListenScreen content")
                    ViewHistoryListenScreen(
                        viewModel = hiltViewModel(),
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}