package vn.edu.usth.msma.utils

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import vn.edu.usth.msma.data.PreferencesManager
import vn.edu.usth.msma.repository.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideSongRepository(): SongRepository = SongRepository()

    @Provides
    @Singleton
    fun provideArtistRepository(): ArtistRepository = ArtistRepository()

    @Provides
    @Singleton
    fun providePlaylistRepository(): PlaylistRepository = PlaylistRepository()

    @Provides
    @Singleton
    fun provideAlbumRepository(): AlbumRepository = AlbumRepository()

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }
}