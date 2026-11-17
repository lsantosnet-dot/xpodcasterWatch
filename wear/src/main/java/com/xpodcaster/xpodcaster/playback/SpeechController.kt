package com.xpodcaster.xpodcaster.playback

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.xpodcaster.xpodcaster.data.NewsItem
import com.xpodcaster.xpodcaster.data.NewsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

data class SpeechState(
    /** Notícia atual (índice na lista); -1 = nenhuma. */
    val index: Int = -1,
    val speaking: Boolean = false,
    /** 0..1 dentro da notícia atual. */
    val progress: Float = 0f,
    val error: String? = null,
)

/**
 * Lê as notícias em voz alta, uma depois da outra, com o TextToSpeech do relógio.
 * Antes de cada uma anuncia "Notícia 2 de 5." em português e depois troca para o idioma da notícia.
 */
class SpeechController(context: Context, private val store: NewsStore) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pendingIndex: Int? = null

    /** Muda a cada notícia iniciada, para ignorar callbacks atrasados da anterior. */
    @Volatile
    private var session = 0
    @Volatile
    private var chunkCount = 1

    private val _state = MutableStateFlow(SpeechState())
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    private fun engine(): TextToSpeech {
        tts?.let { return it }
        val t = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                pendingIndex?.let { i ->
                    pendingIndex = null
                    speakItem(i)
                }
            } else {
                _state.update { it.copy(speaking = false, error = "Sem voz instalada no relógio") }
            }
        }
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) = Unit

            override fun onDone(utteranceId: String) {
                val (s, item, chunk) = parse(utteranceId) ?: return
                if (s != session) return
                if (chunk >= 0) {
                    _state.update { it.copy(progress = (chunk + 1f) / chunkCount) }
                }
                if (chunk == chunkCount - 1) next(fromEngine = item)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                Log.w(TAG, "Erro no TTS: $utteranceId")
                val (s, item, chunk) = parse(utteranceId) ?: return
                if (s == session && chunk == chunkCount - 1) next(fromEngine = item)
            }
        })
        tts = t
        return t
    }

    /** Começa a ler a partir da notícia [index] e segue até o fim da lista. */
    fun playFrom(index: Int) {
        val items = store.items.value
        if (items.isEmpty()) return
        val i = index.coerceIn(0, items.size - 1)
        _state.value = SpeechState(index = i, speaking = true)
        engine()
        if (!ready) pendingIndex = i else speakItem(i)
    }

    fun toggle() {
        val s = _state.value
        when {
            s.speaking -> pause()
            s.index >= 0 -> playFrom(s.index)
            else -> playFrom(0)
        }
    }

    /** Pausa; ao tocar de novo, a notícia atual recomeça do início. */
    fun pause() {
        session++
        pendingIndex = null
        tts?.stop()
        _state.update { it.copy(speaking = false, progress = 0f) }
    }

    fun skip(delta: Int) {
        val s = _state.value
        val items = store.items.value
        if (items.isEmpty()) return
        val i = (s.index.coerceAtLeast(0) + delta).coerceIn(0, items.size - 1)
        if (s.speaking) playFrom(i) else _state.value = SpeechState(index = i)
    }

    /** Para tudo e esquece a posição (lista nova ou limpa). */
    fun stop() {
        session++
        pendingIndex = null
        tts?.stop()
        _state.value = SpeechState()
    }

    private fun next(fromEngine: Int) {
        val items = store.items.value
        val nextIndex = fromEngine + 1
        if (nextIndex < items.size && _state.value.speaking) {
            _state.update { it.copy(index = nextIndex, progress = 0f) }
            speakItem(nextIndex)
        } else {
            _state.update { it.copy(speaking = false, progress = 1f) }
        }
    }

    private fun speakItem(index: Int) {
        val t = tts ?: return
        val items = store.items.value
        val item = items.getOrNull(index) ?: return
        session++
        val s = session
        t.stop()
        val chunks = TextChunker.split(speechOf(item))
        chunkCount = chunks.size.coerceAtLeast(1)
        _state.update { it.copy(index = index, progress = 0f, error = null) }

        setLanguage(t, PT_BR)
        t.speak("Notícia ${index + 1} de ${items.size}.", TextToSpeech.QUEUE_FLUSH, Bundle(), id(s, index, -1))
        setLanguage(t, localeOf(item.languageCode))
        if (chunks.isEmpty()) {
            t.speak(" ", TextToSpeech.QUEUE_ADD, Bundle(), id(s, index, 0))
        } else {
            chunks.forEachIndexed { c, text ->
                t.speak(text, TextToSpeech.QUEUE_ADD, Bundle(), id(s, index, c))
            }
        }
    }

    private fun speechOf(item: NewsItem): String {
        val body = item.speechText.ifBlank { item.text }
        val title = item.title.trim().trimEnd('.', '!', '?')
        return if (title.isEmpty()) body else "$title. $body"
    }

    private fun setLanguage(t: TextToSpeech, locale: Locale) {
        val r = t.setLanguage(locale)
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            t.setLanguage(PT_BR)
        }
    }

    private fun localeOf(code: String): Locale = when (code.lowercase()) {
        "", "und", "pt" -> PT_BR
        "en" -> Locale.US
        "es" -> Locale("es", "ES")
        else -> Locale(code)
    }

    private fun id(session: Int, item: Int, chunk: Int) = "$session:$item:$chunk"

    private fun parse(id: String): Triple<Int, Int, Int>? {
        val parts = id.split(':')
        if (parts.size != 3) return null
        return Triple(parts[0].toIntOrNull() ?: return null, parts[1].toIntOrNull() ?: return null, parts[2].toIntOrNull() ?: return null)
    }

    companion object {
        private const val TAG = "SpeechController"
        private val PT_BR = Locale("pt", "BR")
    }
}
