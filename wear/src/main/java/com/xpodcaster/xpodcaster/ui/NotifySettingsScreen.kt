package com.xpodcaster.xpodcaster.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import com.xpodcaster.xpodcaster.R
import com.xpodcaster.xpodcaster.WatchApp
import com.xpodcaster.xpodcaster.data.NotifySettings
import com.xpodcaster.xpodcaster.notify.NewsNotifier

/** Liga/desliga o lembrete, escolhe o intervalo e o horário de silêncio. Vale na hora. */
@Composable
fun NotifySettingsScreen(app: WatchApp, onRequestPermission: () -> Unit) {
    val settings by app.notifySettings.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()
    var testResult by remember { mutableStateOf<String?>(null) }

    fun change(transform: (NotifySettings) -> NotifySettings) = app.updateNotifySettings(transform)

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
            item { ListHeader { Text("Notificações", color = Teal, fontWeight = FontWeight.Bold) } }
            item {
                ToggleChip(
                    checked = settings.enabled,
                    onCheckedChange = { on ->
                        change { it.copy(enabled = on) }
                        if (on && !NewsNotifier.canNotify(context)) onRequestPermission()
                    },
                    label = { Text("Lembrete de leitura") },
                    secondaryLabel = { Text(if (settings.enabled) "Ligado" else "Desligado") },
                    toggleControl = { Icon(ToggleChipDefaults.switchIcon(settings.enabled), null) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (settings.enabled) {
                item {
                    val steps = NotifySettings.INTERVALS
                    val i = steps.indexOf(settings.intervalMinutes).takeIf { it >= 0 }
                        ?: steps.indexOfFirst { it >= settings.intervalMinutes }.coerceAtLeast(0)
                    Stepper(
                        title = "A cada",
                        value = NotifySettings.intervalLabel(settings.intervalMinutes),
                        onMinus = { change { it.copy(intervalMinutes = steps[(i - 1).coerceAtLeast(0)]) } },
                        onPlus = { change { it.copy(intervalMinutes = steps[(i + 1).coerceAtMost(steps.size - 1)]) } },
                    )
                }
                item {
                    Stepper(
                        title = "Silêncio a partir de",
                        value = NotifySettings.hourLabel(settings.quietStart),
                        onMinus = { change { it.copy(quietStart = (it.quietStart + 23) % 24) } },
                        onPlus = { change { it.copy(quietStart = (it.quietStart + 1) % 24) } },
                    )
                }
                item {
                    Stepper(
                        title = "Volta a avisar às",
                        value = NotifySettings.hourLabel(settings.quietEnd),
                        onMinus = { change { it.copy(quietEnd = (it.quietEnd + 23) % 24) } },
                        onPlus = { change { it.copy(quietEnd = (it.quietEnd + 1) % 24) } },
                    )
                }
                item {
                    Text(
                        if (settings.quietStart == settings.quietEnd) {
                            "Sem horário de silêncio: avisa o dia todo."
                        } else {
                            "Não avisa entre ${NotifySettings.hourLabel(settings.quietStart)} e " +
                                "${NotifySettings.hourLabel(settings.quietEnd)}."
                        },
                        fontSize = 11.sp,
                        color = Muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    )
                }
            }
            item {
                Chip(
                    onClick = {
                        if (!NewsNotifier.canNotify(context)) {
                            onRequestPermission()
                            testResult = "Permita as notificações e toque de novo."
                        } else {
                            testResult = if (NewsNotifier.showNext(context)) {
                                "Enviada. Veja nas notificações."
                            } else {
                                "Nada para ler: chegue ao fim da lista ou envie notícias novas."
                            }
                        }
                    },
                    label = { Text("Testar agora") },
                    icon = { Icon(painterResource(R.drawable.ic_bell), null, Modifier.size(ChipDefaults.IconSize)) },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
            testResult?.let { msg ->
                item { Text(msg, fontSize = 11.sp, color = Muted, textAlign = TextAlign.Center) }
            }
        }
    }
}

@Composable
private fun Stepper(title: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(24.dp))
            .padding(vertical = 6.dp),
    ) {
        Text(title, fontSize = 11.sp, color = Muted)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth(),
        ) {
            CompactButton(onClick = onMinus) { Text("−", fontSize = 18.sp) }
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            CompactButton(onClick = onPlus) { Text("+", fontSize = 18.sp) }
        }
    }
}
