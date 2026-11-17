package com.xpodcaster.xpodcaster

import android.app.Application
import com.xpodcaster.xpodcaster.data.NewsStore
import com.xpodcaster.xpodcaster.playback.SpeechController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class WatchApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    lateinit var store: NewsStore
        private set
    lateinit var speech: SpeechController
        private set

    override fun onCreate() {
        super.onCreate()
        store = NewsStore(this, appScope)
        speech = SpeechController(this, store)
        store.publishStatus()
    }
}
