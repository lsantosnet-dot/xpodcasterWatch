package com.xpodcaster.xpodcaster.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Como o relógio avisa da próxima notícia: de quanto em quanto tempo e em que horário fica quieto.
 * Muda na tela Notificações do relógio; não depende do celular nem de um build novo.
 */
data class NotifySettings(
    val enabled: Boolean = true,
    val intervalMinutes: Int = 60,
    /** Hora (0..23) em que o silêncio começa. Igual a [quietEnd] = sem silêncio. */
    val quietStart: Int = 22,
    /** Hora (0..23) em que o silêncio termina. */
    val quietEnd: Int = 8,
) {
    /** O silêncio pode atravessar a meia-noite (22h → 8h). */
    fun isQuiet(hour: Int): Boolean = when {
        quietStart == quietEnd -> false
        quietStart < quietEnd -> hour in quietStart until quietEnd
        else -> hour >= quietStart || hour < quietEnd
    }

    companion object {
        /** O WorkManager não repete em menos de 15 minutos. */
        val INTERVALS = listOf(15, 30, 60, 90, 120, 180, 240, 360)

        fun intervalLabel(minutes: Int): String = when {
            minutes < 60 -> "$minutes min"
            minutes % 60 == 0 -> "${minutes / 60} h"
            else -> "${minutes / 60} h ${minutes % 60} min"
        }

        fun hourLabel(hour: Int) = "${hour}h"
    }
}

/** Guarda [NotifySettings] em SharedPreferences. */
class NotifySettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("notify", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<NotifySettings> = _settings.asStateFlow()

    fun update(transform: (NotifySettings) -> NotifySettings) {
        val next = transform(_settings.value)
        if (next == _settings.value) return
        prefs.edit()
            .putBoolean(KEY_ENABLED, next.enabled)
            .putInt(KEY_INTERVAL, next.intervalMinutes)
            .putInt(KEY_QUIET_START, next.quietStart)
            .putInt(KEY_QUIET_END, next.quietEnd)
            .apply()
        _settings.value = next
    }

    /** Id da notícia que está na notificação agora (para tirá-la quando você terminar de ler). */
    var shownItemId: String?
        get() = prefs.getString(KEY_SHOWN, null)
        set(value) = prefs.edit().putString(KEY_SHOWN, value).apply()

    private fun load(): NotifySettings {
        val d = NotifySettings()
        return NotifySettings(
            enabled = prefs.getBoolean(KEY_ENABLED, d.enabled),
            intervalMinutes = prefs.getInt(KEY_INTERVAL, d.intervalMinutes),
            quietStart = prefs.getInt(KEY_QUIET_START, d.quietStart),
            quietEnd = prefs.getInt(KEY_QUIET_END, d.quietEnd),
        )
    }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_INTERVAL = "interval_min"
        const val KEY_QUIET_START = "quiet_start"
        const val KEY_QUIET_END = "quiet_end"
        const val KEY_SHOWN = "shown_item"
    }
}
