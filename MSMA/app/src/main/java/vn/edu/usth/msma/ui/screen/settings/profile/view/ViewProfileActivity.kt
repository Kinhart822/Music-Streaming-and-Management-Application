package vn.edu.usth.msma.ui.screen.settings.profile.view

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
import vn.edu.usth.msma.ui.screen.settings.profile.edit.EditProfileViewModel
import vn.edu.usth.msma.ui.theme.MSMATheme

@AndroidEntryPoint
class ViewProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ViewProfileActivity", "onCreate called")

        setContent {
            MSMATheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Log.d("ViewProfileActivity", "Setting ViewProfileScreen content")
                    ViewProfileScreen(
                        viewModel = hiltViewModel<EditProfileViewModel>(),
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}