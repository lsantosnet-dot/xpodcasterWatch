package com.xpodcaster.xpodcaster.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CardDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.xpodcaster.xpodcaster.R
import com.xpodcaster.xpodcaster.WatchApp
import com.xpodcaster.xpodcaster.data.Resume
import com.xpodcaster.xpodcaster.data.ResumeMode
import com.xpodcaster.xpodcaster.data.ResumeTarget

@Composable
fun ListScreen(
    app: WatchApp,
    onOpen: (Int) -> Unit,
    onResume: (ResumeTarget) -> Unit,
    onPlayAll: () -> Unit,
    onPlayer: () -> Unit,
    onClear: () -> Unit,
) {
    val items by app.store.items.collectAsStateWithLifecycle()
    val speech by app.speech.state.collectAsStateWithLifecycle()
    val point by app.resume.point.collectAsStateWithLifecycle()
    val target = remember(point, items) { Resume.resolve(point, items, System.currentTimeMillis()) }
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
            item {
                ListHeader {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text("Perplexity", color = Teal, fontWeight = FontWeight.Bold)
                        if (items.isNotEmpty()) Text("${items.size} notícias", fontSize = 11.sp, color = Muted)
                    }
                }
            }
            if (items.isEmpty()) {
                item {
                    Text(
                        "Nenhuma notícia no relógio.\nNo celular, abra a aba Perplexity e toque em \"Enviar para o relógio\".",
                        textAlign = TextAlign.Center,
                        color = Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp),
                    )
                }
                return@ScalingLazyColumn
            }
            if (target != null && !speech.speaking) {
                item {
                    val listening = target.mode == ResumeMode.LISTEN
                    Chip(
                        onClick = { onResume(target) },
                        label = { Text(if (listening) "Continuar ouvindo" else "Continuar lendo") },
                        secondaryLabel = {
                            Text(
                                "${target.index + 1} de ${items.size} · ${items[target.index].title}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        icon = {
                            Icon(
                                painterResource(if (listening) R.drawable.ic_headphones else R.drawable.ic_read),
                                contentDescription = null,
                                modifier = Modifier.size(ChipDefaults.IconSize),
                            )
                        },
                        colors = ChipDefaults.primaryChipColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                val playing = speech.speaking
                Chip(
                    onClick = if (playing) onPlayer else onPlayAll,
                    label = { Text(if (playing) "Tocando…" else "Ouvir tudo") },
                    secondaryLabel = if (playing) {
                        { Text("Notícia ${speech.index + 1} de ${items.size}", maxLines = 1) }
                    } else {
                        null
                    },
                    icon = {
                        Icon(
                            painterResource(if (playing) R.drawable.ic_headphones else R.drawable.ic_play),
                            contentDescription = null,
                            modifier = Modifier.size(ChipDefaults.IconSize),
                        )
                    },
                    colors = if (target != null && !playing) ChipDefaults.secondaryChipColors() else ChipDefaults.primaryChipColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            itemsIndexed(items, key = { _, it -> it.id }) { i, item ->
                val current = speech.speaking && speech.index == i
                val resumeHere = !speech.speaking && target?.index == i
                Card(
                    onClick = { onOpen(i) },
                    backgroundPainter = CardDefaults.cardBackgroundPainter(CardBg, CardBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (current) Modifier.border(2.dp, Teal, RoundedCornerShape(24.dp)) else Modifier),
                ) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            if (resumeHere) "${i + 1} de ${items.size} · continuar" else "${i + 1} de ${items.size}",
                            fontSize = 10.sp,
                            color = if (current || resumeHere) Teal else Muted,
                            modifier = Modifier.weight(1f),
                        )
                        item.dateLabel?.let {
                            Text(it, fontSize = 10.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            item {
                Chip(
                    onClick = onClear,
                    label = { Text("Limpar tudo", color = Danger) },
                    icon = {
                        Icon(
                            painterResource(R.drawable.ic_trash),
                            contentDescription = null,
                            tint = Danger,
                            modifier = Modifier.size(ChipDefaults.IconSize),
                        )
                    },
                    colors = ChipDefaults.chipColors(backgroundColor = DangerBg),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
        }
    }
}
