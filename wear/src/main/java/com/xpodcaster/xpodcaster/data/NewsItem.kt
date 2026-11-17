package com.xpodcaster.xpodcaster.data

import kotlinx.serialization.Serializable

/** O que o celular envia: a lista inteira, que substitui a que está no relógio. */
@Serializable
data class NewsPayload(
    val sentAt: Long = 0,
    val items: List<NewsItem> = emptyList(),
)

/** Uma notícia do Perplexity, como está na fila do XPodcaster no celular. */
@Serializable
data class NewsItem(
    val id: String,
    /** Título da notícia (no XPodcaster fica no campo "author"). */
    val title: String,
    /** Data como aparece no Perplexity ("há 2 horas"), ou formatada pelo celular. */
    val dateLabel: String? = null,
    /** Texto original, para ler na tela. */
    val text: String = "",
    /** Texto já tratado para a fala (sem URLs, emojis etc.). */
    val speechText: String = "",
    /** Idioma detectado no celular (ex.: "pt", "en"); "und" se desconhecido. */
    val languageCode: String = "und",
)
