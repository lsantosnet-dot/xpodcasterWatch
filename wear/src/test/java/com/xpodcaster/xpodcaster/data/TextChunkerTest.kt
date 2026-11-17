package com.xpodcaster.xpodcaster.data

import com.xpodcaster.xpodcaster.playback.TextChunker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextChunkerTest {
    @Test
    fun empty() = assertEquals(emptyList<String>(), TextChunker.split("   "))

    @Test
    fun keepsSentencesTogetherUnderLimit() {
        assertEquals(listOf("Uma frase. Outra frase."), TextChunker.split("Uma frase.\n Outra frase."))
    }

    @Test
    fun splitsAtSentenceBoundaries() {
        val text = "Primeira frase aqui. Segunda frase aqui. Terceira frase aqui."
        val chunks = TextChunker.split(text, maxLen = 25)
        assertEquals(listOf("Primeira frase aqui.", "Segunda frase aqui.", "Terceira frase aqui."), chunks)
    }

    @Test
    fun hardSplitsHugeSentence() {
        val chunks = TextChunker.split("a".repeat(1000), maxLen = 400)
        assertEquals(3, chunks.size)
        assertTrue(chunks.all { it.length <= 400 })
    }

    @Test
    fun decodesPayloadIgnoringUnknownFields() {
        val json = """{"sentAt":1,"items":[{"id":"x","title":"T","text":"Corpo","extra":true}]}"""
        val p = Protocol.json.decodeFromString<NewsPayload>(json)
        assertEquals("T", p.items.single().title)
        assertEquals("und", p.items.single().languageCode)
    }
}
