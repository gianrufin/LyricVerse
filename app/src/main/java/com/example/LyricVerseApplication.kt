package com.example

import android.app.Application
import com.example.data.local.LyricVerseDatabase
import com.example.data.local.PreferencesDataStore
import com.example.data.lyrics.DemoLyricsProvider
import com.example.data.lyrics.LrclibApiProvider
import com.example.data.lyrics.LyricsRepository
import com.example.media.DemoPlaybackController
import com.example.media.LyricSyncCoordinator
import com.example.media.MediaSessionTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(application: Application) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database = LyricVerseDatabase.getInstance(application)
    val preferencesDataStore = PreferencesDataStore(application)
    val demoLyricsProvider = DemoLyricsProvider()
    val lrclibApiProvider = LrclibApiProvider()
    val lyricsRepository = LyricsRepository(
        cacheDao = database.lyricsCacheDao(),
        demoProvider = demoLyricsProvider,
        remoteProvider = lrclibApiProvider,
    )
    val mediaSessionTracker = MediaSessionTracker(application)
    val demoController = DemoPlaybackController(
        demoProvider = demoLyricsProvider,
        scope = appScope,
    )
    val syncCoordinator = LyricSyncCoordinator(
        mediaSessionTracker = mediaSessionTracker,
        demoController = demoController,
        lyricsRepository = lyricsRepository,
        preferencesDataStore = preferencesDataStore,
        scope = appScope,
    )
}

class LyricVerseApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
