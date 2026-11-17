package com.xpodcaster.xpodcaster.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.xpodcaster.xpodcaster.R
import com.xpodcaster.xpodcaster.WatchApp

@Composable
fun DetailScreen(app: WatchApp, index: Int, onListen: () -> Unit) {
    val items by app.store.items.collectAsStateWithLifecycle()
    val item = items.getOrNull(index)
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 0)
    // Texto longo: sem o efeito de encolher nas bordas, que atrapalha a leitura.
    val noScaling = ScalingLazyColumnDefaults.scalingParams(edgeScale = 1f, edgeAlpha = 1f)

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(
            state = listState,
            scalingParams = noScaling,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (item == null) {
                item { Text("Notícia não encontrada.", color = Muted) }
                return@ScalingLazyColumn
            }
            item {
                Text(
                    listOfNotNull("Perplexity", item.dateLabel).joinToString(" · "),
                    fontSize = 10.sp,
                    color = Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
            }
            item {
                Text(
                    item.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                )
            }
            // Um parágrafo por item da lista: rola melhor que um Text gigante.
            val paragraphs = item.text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
            paragraphs.forEach { p ->
                item {
                    Text(
                        p,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = Body,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                    )
                }
            }
            item {
                Chip(
                    onClick = onListen,
                    label = { Text("Ouvir a partir daqui") },
                    icon = { Icon(painterResource(R.drawable.ic_play), null, Modifier.size(ChipDefaults.IconSize)) },
                    colors = ChipDefaults.primaryChipColors(),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            item {
                Text("${index + 1} de ${items.size}", fontSize = 11.sp, color = Muted)
            }
        }
    }
}
