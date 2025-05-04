package vn.edu.usth.msma.ui.screen.library

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LibraryScreen() {
    val musicItems = listOf(
        "Song 1 - Artist A",
        "Song 2 - Artist B",
        "Song 3 - Artist C",
        "Song 4 - Artist D"
    )

    LazyColumn(
        modifier = Modifier.padding(16.dp)
    ) {
        items(musicItems) { item ->
            Text(
                text = item,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}