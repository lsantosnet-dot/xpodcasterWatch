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

    /**
     * A próxima notícia para ler (a da notificação). Diferente de [resolve], nunca fica sem
     * resposta enquanto houver notícia nova: sem marcador, ou com a lista trocada pelo celular,
     * começa da primeira. Só devolve null quando a última da lista já foi até o fim.
     */
    fun nextToRead(point: ResumePoint?, items: List<NewsItem>): ResumeTarget? {
        if (items.isEmpty()) return null
        val i = point?.let { p -> items.indexOfFirst { it.id == p.itemId } } ?: -1
        if (i < 0) return ResumeTarget(0, ResumeMode.READ, 0)
        if (point!!.finished) {
            return if (i + 1 < items.size) ResumeTarget(i + 1, ResumeMode.READ, 0) else null
        }
        // Parou ouvindo no meio: a frase não corresponde a um parágrafo, então abre do começo.
        val position = if (point.mode == ResumeMode.READ) point.position else 0
        return ResumeTarget(i, ResumeMode.READ, position)
    }
}
