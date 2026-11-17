package com.xpodcaster.xpodcaster.data

import kotlinx.serialization.json.Json

/**
 * Caminhos da Wearable Data Layer entre o XPodcaster (celular) e o XPodcaster Watch.
 * O lado do celular tem uma cópia destas constantes em WatchBridge.kt — mantenha as duas iguais.
 */
object Protocol {
    /** Canal (ChannelClient) celular → relógio: JSON UTF-8 com a lista completa de notícias. */
    const val PATH_NEWS = "/xpodcaster/perplexity/news"

    /** Mensagem relógio → celular: notícias recebidas e salvas (payload = quantidade). */
    const val PATH_NEWS_RECEIVED = "/xpodcaster/perplexity/received"

    /** Mensagem celular → relógio: "você está aí?". */
    const val PATH_PING = "/xpodcaster/ping"

    /** Mensagem relógio → celular: resposta ao ping (payload = quantidade de notícias guardadas). */
    const val PATH_PONG = "/xpodcaster/pong"

    /** DataItem publicado pelo relógio anunciando que o app está instalado. */
    const val PATH_STATUS = "/xpodcaster/watch"

    const val KEY_COUNT = "count"
    const val KEY_TIME = "t"

    /** Capability declarada pelo app do relógio (res/values/wear.xml). */
    const val CAP_WATCH = "xpodcaster_watch"

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}
