package com.xpodcaster.xpodcaster.data

import kotlinx.serialization.Serializable

@Serializable
enum class ResumeMode { LISTEN, READ }

/** Onde você parou. Aponta para a notícia pelo [itemId], porque a lista é trocada a cada envio. */
@Serializable
data class ResumePoint(
    val itemId: String,
    val mode: ResumeMode,
    /** Ouvindo: trecho do TextChunker. Lendo: parágrafo. */
    val position: Int = 0,
    /** A notícia foi até o fim: continuar vai para a próxima. */
    val finished: Boolean = false,
    val savedAt: Long = 0,
)

/** O que o chip "Continuar" abre: notícia [index] da lista atual, no [mode], a partir de [position]. */
data class ResumeTarget(val index: Int, val mode: ResumeMode, val position: Int)

object Resume {
    /** Depois de uma pausa longa, a fala volta um trecho para recuperar o contexto. */
    const val LONG_PAUSE_MS = 2 * 60_000L

    fun resolve(point: ResumePoint?, items: List<NewsItem>, now: Long): ResumeTarget? {
        if (point == null) return null
        val i = items.indexOfFirst { it.id == point.itemId }
        if (i < 0) return null
        if (point.finished) {
            return if (i + 1 < items.size) ResumeTarget(i + 1, point.mode, 0) else null
        }
        val rewind = point.mode == ResumeMode.LISTEN && now - point.savedAt > LONG_PAUSE_MS
        val position = if (rewind) point.position - 1 else point.position
        return ResumeTarget(i, point.mode, position.coerceAtLeast(0))
    }
}
