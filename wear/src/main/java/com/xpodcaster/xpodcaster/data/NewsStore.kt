package com.xpodcaster.xpodcaster.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

/** Notícias guardadas no armazenamento interno do relógio (news.json). */
class NewsStore(context: Context, private val scope: CoroutineScope) {
    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, "news.json")

    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<NewsItem>> = _items.asStateFlow()

    fun get(index: Int): NewsItem? = _items.value.getOrNull(index)

    /** Substitui tudo pelo que veio do celular. */
    fun replaceAll(payload: NewsPayload) {
        save(payload)
        _items.value = payload.items
        publishStatus()
    }

    fun clear() {
        file.delete()
        _items.value = emptyList()
        publishStatus()
    }

    /**
     * Publica um DataItem com a quantidade de notícias. Serve de "anúncio": o celular
     * vê de qual nó ele veio e passa a saber que aquele relógio tem o app.
     */
    fun publishStatus() {
        scope.launch(Dispatchers.IO) {
            try {
                val request = PutDataMapRequest.create(Protocol.PATH_STATUS).apply {
                    dataMap.putInt(Protocol.KEY_COUNT, _items.value.size)
                    dataMap.putLong(Protocol.KEY_TIME, System.currentTimeMillis())
                }.asPutDataRequest().setUrgent()
                Wearable.getDataClient(appContext).putDataItem(request).await()
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao publicar status", e)
            }
        }
    }

    private fun load(): List<NewsItem> = try {
        if (file.exists()) Protocol.json.decodeFromString<NewsPayload>(file.readText()).items else emptyList()
    } catch (e: Exception) {
        Log.w(TAG, "news.json inválido", e)
        emptyList()
    }

    private fun save(payload: NewsPayload) {
        val tmp = File(file.parentFile, "news.json.tmp")
        tmp.writeText(Protocol.json.encodeToString(NewsPayload.serializer(), payload))
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }

    companion object {
        private const val TAG = "NewsStore"
    }
}
