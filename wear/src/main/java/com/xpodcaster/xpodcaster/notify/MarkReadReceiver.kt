package com.xpodcaster.xpodcaster.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xpodcaster.xpodcaster.WatchApp

/** Botão "Lida" da notificação: marca como lida sem abrir o app; o "Continuar" passa para a próxima. */
class MarkReadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as WatchApp
        val id = intent.getStringExtra(NewsNotifier.EXTRA_ITEM_ID) ?: return
        val item = app.store.items.value.firstOrNull { it.id == id }
        if (item != null) {
            val paragraphs = item.text.split('\n').count { it.isNotBlank() }
            app.resume.saveReading(id, (paragraphs - 1).coerceAtLeast(0), finished = true)
        }
        NewsNotifier.cancel(context)
    }
}
