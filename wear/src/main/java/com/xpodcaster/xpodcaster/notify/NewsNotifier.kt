package com.xpodcaster.xpodcaster.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.xpodcaster.xpodcaster.R
import com.xpodcaster.xpodcaster.WatchApp
import com.xpodcaster.xpodcaster.data.Resume
import com.xpodcaster.xpodcaster.ui.MainActivity

/** Monta e mostra a notificação "próxima notícia para ler". */
object NewsNotifier {
    private const val TAG = "NewsNotifier"
    const val CHANNEL_ID = "next_news"
    private const val NOTIFICATION_ID = 1

    const val EXTRA_OPEN = "open"
    const val EXTRA_INDEX = "index"
    const val EXTRA_POSITION = "position"
    const val EXTRA_ITEM_ID = "item_id"
    const val OPEN_READ = "read"
    const val OPEN_LISTEN = "listen"

    /** Quantos caracteres do texto aparecem na notificação expandida. */
    private const val PREVIEW_CHARS = 500

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Próxima notícia",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Lembrete periódico com a próxima notícia do Perplexity para ler" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED && NotificationManagerCompat.from(context).areNotificationsEnabled()

    /**
     * Mostra a próxima não lida. Devolve false quando não há o que mostrar
     * (lista vazia, tudo lido ou sem permissão).
     */
    fun showNext(context: Context): Boolean {
        val app = context.applicationContext as WatchApp
        val items = app.store.items.value
        val target = Resume.nextToRead(app.resume.point.value, items) ?: run {
            cancel(context)
            return false
        }
        if (!canNotify(context)) {
            Log.i(TAG, "Sem permissão de notificação")
            return false
        }
        val item = items[target.index]
        val position = "${target.index + 1} de ${items.size}"
        val preview = item.text.trim().let { if (it.length > PREVIEW_CHARS) it.take(PREVIEW_CHARS).trimEnd() + "…" else it }

        val open = activityIntent(context, 10, OPEN_READ, target.index, target.position)
        val listen = activityIntent(context, 11, OPEN_LISTEN, target.index, 0)
        val markRead = PendingIntent.getBroadcast(
            context,
            12,
            Intent(context, MarkReadReceiver::class.java).putExtra(EXTRA_ITEM_ID, item.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_read)
            .setContentTitle(item.title)
            .setContentText(if (target.position > 0) "Continuar lendo · $position" else "Perplexity · $position")
            .setStyle(NotificationCompat.BigTextStyle().bigText(preview).setSummaryText(position))
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_read, "Ler", open)
            .addAction(R.drawable.ic_check, "Lida", markRead)
            .addAction(R.drawable.ic_headphones, "Ouvir", listen)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            app.notifySettings.shownItemId = item.id
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "Notificação recusada", e)
            false
        }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        (context.applicationContext as WatchApp).notifySettings.shownItemId = null
    }

    private fun activityIntent(context: Context, code: Int, mode: String, index: Int, position: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_OPEN, mode)
            .putExtra(EXTRA_INDEX, index)
            .putExtra(EXTRA_POSITION, position)
        return PendingIntent.getActivity(
            context,
            code,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
