package com.xpodcaster.xpodcaster.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.xpodcaster.xpodcaster.WatchApp
import com.xpodcaster.xpodcaster.data.NotifySettings
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Roda a cada intervalo configurado e, fora do horário de silêncio, mostra a próxima notícia. */
class NotifyWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as WatchApp
        val settings = app.notifySettings.settings.value
        if (!settings.enabled) return Result.success()
        if (settings.isQuiet(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))) return Result.success()
        NewsNotifier.showNext(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "next_news"

        /**
         * Agenda (ou desagenda) conforme a configuração. [restart] recomeça a contagem do
         * intervalo, para quando você muda a configuração; na abertura do app mantém o que já existe.
         */
        fun schedule(context: Context, settings: NotifySettings, restart: Boolean) {
            val wm = WorkManager.getInstance(context)
            if (!settings.enabled) {
                wm.cancelUniqueWork(WORK_NAME)
                return
            }
            val minutes = settings.intervalMinutes.toLong().coerceAtLeast(15)
            val request = PeriodicWorkRequestBuilder<NotifyWorker>(minutes, TimeUnit.MINUTES)
                // A primeira vem depois de um intervalo, não na hora em que você salva.
                .setInitialDelay(minutes, TimeUnit.MINUTES)
                .addTag(WORK_NAME)
                .build()
            wm.enqueueUniquePeriodicWork(
                WORK_NAME,
                if (restart) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
