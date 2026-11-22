package com.xpodcaster.xpodcaster.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Text
import com.xpodcaster.xpodcaster.WatchApp

@Composable
fun ConfirmClearScreen(app: WatchApp, onDone: () -> Unit) {
    val items by app.store.items.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Text("Limpar tudo?", color = Danger, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(
            "Apaga as ${items.size} notícias do relógio. Dá para enviar de novo pelo celular.",
            color = Muted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
        Chip(
            onClick = {
                app.speech.stop()
                app.store.clear()
                app.resume.clear()
                onDone()
            },
            label = { Text("Limpar", color = Danger, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            colors = ChipDefaults.chipColors(backgroundColor = DangerBg),
            modifier = Modifier.fillMaxWidth(),
        )
        Chip(
            onClick = onDone,
            label = { Text("Cancelar", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            colors = ChipDefaults.secondaryChipColors(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
