package com.xpodcaster.xpodcaster.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.Executors

/**
 * Guarda no relógio (resume.json) a última notícia lida ou ouvida, para o chip "Continuar".
 * Só existe uma posição: a da última coisa que você fez.
 */
class ResumeStore(context: Context) {
    private val file = File(context.applicationContext.filesDir, "resume.json")
    /** Grava fora da thread principal e na ordem em que as posições chegam. */
    private val writer = Executors.newSingleThreadExecutor()

    private val _point = MutableStateFlow(load())
    val point: StateFlow<ResumePoint?> = _point.asStateFlow()

    fun saveListening(itemId: String, chunk: Int, finished: Boolean = false) =
        save(ResumePoint(itemId, ResumeMode.LISTEN, chunk, finished))

    fun saveReading(itemId: String, paragraph: Int, finished: Boolean) =
        save(ResumePoint(itemId, ResumeMode.READ, paragraph, finished))

    @Synchronized
    private fun save(point: ResumePoint) {
        val current = _point.value
        if (current != null && current.copy(savedAt = 0) == point) return
        val stamped = point.copy(savedAt = System.currentTimeMillis())
        _point.value = stamped
        writer.execute { write(stamped) }
    }

    @Synchronized
    fun clear() {
        _point.value = null
        writer.execute { file.delete() }
    }

    private fun write(point: ResumePoint) {
        try {
            val tmp = File(file.parentFile, "resume.json.tmp")
            tmp.writeText(Protocol.json.encodeToString(ResumePoint.serializer(), point))
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao gravar resume.json", e)
        }
    }

    private fun load(): ResumePoint? = try {
        if (file.exists()) Protocol.json.decodeFromString<ResumePoint>(file.readText()) else null
    } catch (e: Exception) {
        Log.w(TAG, "resume.json inválido", e)
        null
    }

    companion object {
        private const val TAG = "ResumeStore"
    }
}
