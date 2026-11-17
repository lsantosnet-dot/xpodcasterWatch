package com.xpodcaster.xpodcaster.playback

/**
 * Divide um texto em trechos para o TextToSpeech: o motor tem limite de tamanho por fala
 * e trechos menores dão um progresso mais fino. Corta preferindo fim de frase.
 */
object TextChunker {
    fun split(text: String, maxLen: Int = 400): List<String> {
        val normalized = text.replace(Regex("\\s+"), " ").trim()
        if (normalized.isEmpty()) return emptyList()
        val sentences = Regex("(?<=[.!?…])\\s+").split(normalized)
        val out = mutableListOf<String>()
        val current = StringBuilder()
        fun flush() {
            if (current.isNotBlank()) out += current.toString().trim()
            current.clear()
        }
        for (sentence in sentences) {
            if (sentence.length > maxLen) {
                flush()
                sentence.chunked(maxLen).forEach { out += it.trim() }
                continue
            }
            if (current.length + sentence.length + 1 > maxLen) flush()
            if (current.isNotEmpty()) current.append(' ')
            current.append(sentence)
        }
        flush()
        return out.filter { it.isNotEmpty() }
    }
}
