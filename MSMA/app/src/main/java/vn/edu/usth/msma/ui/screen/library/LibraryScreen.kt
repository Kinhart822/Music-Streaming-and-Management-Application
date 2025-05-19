package vn.edu.usth.msma.ui.screen.library

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.gson.Gson
import kotlinx.coroutines.launch
import vn.edu.usth.msma.R
import vn.edu.usth.msma.data.Song
import vn.edu.usth.msma.ui.components.ScreenRoute
import vn.edu.usth.msma.ui.components.SongItem
import vn.edu.usth.msma.ui.screen.songs.MusicPlayerViewModel
import vn.edu.usth.msma.utils.eventbus.Event
import vn.edu.usth.msma.utils.eventbus.EventBus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    navController: NavHostController
) {
    val musicPlayerViewModel: MusicPlayerViewModel = hiltViewModel()
    val context = LocalContext.current
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val tabs = listOf("All", "Artists", "Playlists", "Albums")
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f,
        pageCount = { tabs.size }
    )
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Sort by Recently Added") }
    val filters = listOf("Sort by Recently Added", "Sort by Title", "Sort by Artist")
    val interactionSource = remember { MutableInteractionSource() }
    val focusManager = LocalFocusManager.current

    DisposableEffect(context) {
        musicPlayerViewModel.registerMusicEventReceiver(context)
        onDispose {
            musicPlayerViewModel.unregisterMusicEventReceiver(context)
        }
    }

    LaunchedEffect(Unit) {
        musicPlayerViewModel.refreshCurrentSongData(context)
        coroutineScope.launch {
            EventBus.publish(Event.InitializeDataLibrary)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        when (pagerState.currentPage) {
            0 -> {
                viewModel.loadFavoriteSongs()
                viewModel.loadArtists()
                viewModel.loadPlaylists()
                viewModel.loadAlbums()
            }
            1 -> viewModel.loadArtists()
            2 -> viewModel.loadPlaylists()
            3 -> viewModel.loadAlbums()
        }
    }

    LaunchedEffect(searchQuery) {
        viewModel.searchContent(searchQuery)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = "Your Library",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Search bar with Cancel button and Filter
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search in Your Library") },
                leadingIcon = {
                    var lastClickTime by remember { mutableStateOf(0L) }
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable {
                            val currentTime = System.currentTimeMillis()
                            val timeSinceLastClick = currentTime - lastClickTime

                            if (timeSinceLastClick < 500) { // double click detected
                                searchQuery = ""
                                isSearchFocused = false
                                viewModel.searchContent("")
                                focusManager.clearFocus()
                            } else {
                                isSearchFocused = true
                            }

                            lastClickTime = currentTime
                        }
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 16.dp),
                singleLine = true,
                interactionSource = interactionSource,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { viewModel.searchContent(searchQuery) }
                ),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            if (searchQuery.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                FilterButton(
                    selectedFilter = selectedFilter,
                    filters = filters,
                    onFilterSelected = { filter ->
                        selectedFilter = filter
                        viewModel.applyFilter(filter)
                    }
                )
            }
        }

        // Content
        if (searchQuery.isNotEmpty()) {
            // Search Results
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    Text(
                        text = "Searching...",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (searchResults.isEmpty()) {
                    Text(
                        text = "No results found",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(searchResults) { item ->
                            when (item) {
                                is LibraryItem.SongItem -> {
                                    SongItem(
                                        song = item.song,
                                        onSongClick = {
                                            val songJson = Gson().toJson(item.song)
                                            navController.navigate(
                                                ScreenRoute.SongDetails.createRoute(songJson, true)
                                            )
                                        }
                                    )
                                }
                                is LibraryItem.ArtistItem -> {
                                    CategoryItem(
                                        title = item.name,
                                        subtitle = "Artist",
                                        imageUrl = item.imageUrl,
                                        onClick = {
//                                            navController.navigate(ScreenRoute.ArtistProfile.createRoute(item.id))
                                        }
                                    )
                                }
                                is LibraryItem.PlaylistItem -> {
                                    CategoryItem(
                                        title = item.name,
                                        subtitle = "Playlist",
                                        imageUrl = item.imageUrl,
                                        onClick = {
//                                            navController.navigate(ScreenRoute.Playlist.createRoute(item.id))
                                        }
                                    )
                                }
                                is LibraryItem.AlbumItem -> {
                                    CategoryItem(
                                        title = item.name,
                                        subtitle = "Album • ${item.artistName}",
                                        imageUrl = item.imageUrl,
                                        onClick = {
//                                            navController.navigate(ScreenRoute.Album.createRoute(item.id))
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        } else {
            // Tabs
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 0.dp,
                divider = {},
                containerColor = Color.Transparent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Text(
                                text = tab,
                                color = if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            // Pager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> AllTab(
                        favoriteSongs = favoriteSongs,
                        artists = artists,
                        playlists = playlists,
                        albums = albums,
                        onCategoryClick = { category ->
                            when (category) {
                                "Favourite Songs" -> navController.navigate(ScreenRoute.FavoriteSongs.route)
                                "Artists" -> coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                "Playlists" -> coroutineScope.launch { pagerState.animateScrollToPage(2) }
                                "Albums" -> coroutineScope.launch { pagerState.animateScrollToPage(3) }
                            }
                        }
                    )
                    1 -> ArtistsTab(artists = artists, navController = navController)
                    2 -> PlaylistsTab(playlists = playlists, navController = navController)
                    3 -> AlbumsTab(albums = albums, navController = navController)
                }
            }
        }
    }
}

@Composable
fun AllTab(
    favoriteSongs: List<Song>,
    artists: List<LibraryItem.ArtistItem>,
    playlists: List<LibraryItem.PlaylistItem>,
    albums: List<LibraryItem.AlbumItem>,
    onCategoryClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Liked Songs Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp)
                    .clickable { onCategoryClick("Favourite Songs") },
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.liked_song),
                        contentDescription = "Liked Songs",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Liked Songs",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Artists Section
        item {
            Text(
                text = "Artists",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        if (artists.isEmpty()) {
            item {
                Text(
                    text = "No artists found",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                )
            }
        } else {
            items(artists.take(3)) { artist ->
                CategoryItem(
                    title = artist.name,
                    subtitle = "Artist",
                    imageUrl = artist.imageUrl,
                    onClick = {
//                        navController.navigate(ScreenRoute.ArtistProfile.createRoute(artist.id))
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (artists.size > 3) {
                item {
                    TextButton(
                        onClick = { onCategoryClick("Artists") },
                        modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                    ) {
                        Text(
                            text = "See more..",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Playlists Section
        item {
            Text(
                text = "Playlists",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        if (playlists.isEmpty()) {
            item {
                Text(
                    text = "No playlists found",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                )
            }
        } else {
            items(playlists.take(3)) { playlist ->
                CategoryItem(
                    title = playlist.name,
                    subtitle = "Playlist",
                    imageUrl = playlist.imageUrl,
                    onClick = {
//                        navController.navigate(ScreenRoute.Playlist.createRoute(playlist.id))
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (playlists.size > 3) {
                item {
                    TextButton(
                        onClick = { onCategoryClick("Playlists") },
                        modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                    ) {
                        Text(
                            text = "See more..",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Albums Section
        item {
            Text(
                text = "Albums",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        if (albums.isEmpty()) {
            item {
                Text(
                    text = "No albums found",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                )
            }
        } else {
            items(albums.take(3)) { album ->
                CategoryItem(
                    title = album.name,
                    subtitle = "Album • ${album.artistName}",
                    imageUrl = album.imageUrl,
                    onClick = {
//                        navController.navigate(ScreenRoute.Album.createRoute(album.id))
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (albums.size > 3) {
                item {
                    TextButton(
                        onClick = { onCategoryClick("Albums") },
                        modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                    ) {
                        Text(
                            text = "See more..",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryItem(
    title: String,
    subtitle: String,
    imageUrl: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                modifier = Modifier
                    .size(48.dp)
                    .aspectRatio(1f)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                onError = { Log.e("LibraryScreen", "Failed to load image for $title") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun FilterButton(
    selectedFilter: String,
    filters: List<String>,
    onFilterSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = "Filter",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            filters.forEach { filter ->
                DropdownMenuItem(
                    text = { Text(filter, color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        onFilterSelected(filter)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ArtistsTab(artists: List<LibraryItem.ArtistItem>, navController: NavHostController) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (artists.isEmpty()) {
            Text(
                text = "No artists found",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(artists) { artist ->
                    CategoryItem(
                        title = artist.name,
                        subtitle = "Artist",
                        imageUrl = artist.imageUrl,
                        onClick = {
//                            navController.navigate(ScreenRoute.ArtistProfile.createRoute(artist.id))
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun PlaylistsTab(playlists: List<LibraryItem.PlaylistItem>, navController: NavHostController) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (playlists.isEmpty()) {
            Text(
                text = "No playlists found",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(playlists) { playlist ->
                    CategoryItem(
                        title = playlist.name,
                        subtitle = "Playlist",
                        imageUrl = playlist.imageUrl,
                        onClick = {
//                            navController.navigate(ScreenRoute.PlaylistDetails.createRoute(playlist.id))
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun AlbumsTab(albums: List<LibraryItem.AlbumItem>, navController: NavHostController) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (albums.isEmpty()) {
            Text(
                text = "No albums found",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(albums) { album ->
                    CategoryItem(
                        title = album.name,
                        subtitle = "Album • ${album.artistName}",
                        imageUrl = album.imageUrl,
                        onClick = {
//                            navController.navigate(ScreenRoute.Album.createRoute(album.id))
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}