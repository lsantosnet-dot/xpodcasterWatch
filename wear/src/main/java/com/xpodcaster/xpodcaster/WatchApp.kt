package com.xpodcaster.xpodcaster

import android.app.Application
import com.xpodcaster.xpodcaster.data.NewsStore
import com.xpodcaster.xpodcaster.data.NotifySettings
import com.xpodcaster.xpodcaster.data.NotifySettingsStore
import com.xpodcaster.xpodcaster.data.ResumeStore
import com.xpodcaster.xpodcaster.notify.NewsNotifier
import com.xpodcaster.xpodcaster.notify.NotifyWorker
import com.xpodcaster.xpodcaster.playback.SpeechController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WatchApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    lateinit var store: NewsStore
        private set
    lateinit var resume: ResumeStore
        private set
    lateinit var speech: SpeechController
        private set
    lateinit var notifySettings: NotifySettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        store = NewsStore(this, appScope)
        resume = ResumeStore(this)
        speech = SpeechController(this, store, resume)
        notifySettings = NotifySettingsStore(this)
        NewsNotifier.createChannel(this)
        NotifyWorker.schedule(this, notifySettings.settings.value, restart = false)
        store.publishStatus()

        // Terminou de ler ou ouvir a notícia da notificação: ela sai da tela.
        appScope.launch {
            resume.point.collect { p ->
                if (p != null && p.finished && p.itemId == notifySettings.shownItemId) {
                    NewsNotifier.cancel(this@WatchApp)
                }
            }
        }
    }

    fun updateNotifySettings(transform: (NotifySettings) -> NotifySettings) {
        val before = notifySettings.settings.value
        notifySettings.update(transform)
        val after = notifySettings.settings.value
        if (after == before) return
        // O horário de silêncio é conferido a cada disparo; só intervalo e liga/desliga reagendam.
        if (after.enabled != before.enabled || after.intervalMinutes != before.intervalMinutes) {
            NotifyWorker.schedule(this, after, restart = true)
            if (!after.enabled) NewsNotifier.cancel(this)
        }
    }
}
