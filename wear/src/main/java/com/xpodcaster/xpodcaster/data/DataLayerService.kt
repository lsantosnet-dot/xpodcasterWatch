package com.xpodcaster.xpodcaster.data

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.xpodcaster.xpodcaster.WatchApp
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Recebe as notícias e os pings do celular, mesmo com o app fechado.
 * Os callbacks já rodam numa thread de fundo.
 */
class DataLayerService : WearableListenerService() {
    private val app get() = application as WatchApp

    override fun onChannelOpened(channel: ChannelClient.Channel) {
        if (channel.path != Protocol.PATH_NEWS) return
        val client = Wearable.getChannelClient(this)
        try {
            val input = Tasks.await(client.getInputStream(channel), 30, TimeUnit.SECONDS)
            val bytes = input.use { it.readBytes() }
            val payload = Protocol.json.decodeFromString<NewsPayload>(bytes.decodeToString())
            app.appScope.launch { app.speech.stop() }
            app.store.replaceAll(payload)
            Log.i(TAG, "Recebidas ${payload.items.size} notícias")
            reply(channel.nodeId, Protocol.PATH_NEWS_RECEIVED, payload.items.size)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao receber notícias", e)
        } finally {
            client.close(channel)
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            Protocol.PATH_PING -> {
                reply(event.sourceNodeId, Protocol.PATH_PONG, app.store.items.value.size)
                app.store.publishStatus()
            }
        }
    }

    private fun reply(nodeId: String, path: String, count: Int) {
        try {
            Tasks.await(
                Wearable.getMessageClient(this).sendMessage(nodeId, path, count.toString().toByteArray()),
                10,
                TimeUnit.SECONDS,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao responder $path", e)
        }
    }

    companion object {
        private const val TAG = "DataLayerService"
    }
}
