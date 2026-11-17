package com.xpodcaster.xpodcaster.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.xpodcaster.xpodcaster.R
import com.xpodcaster.xpodcaster.WatchApp

@Composable
fun PlayerScreen(app: WatchApp) {
    val items by app.store.items.collectAsStateWithLifecycle()
    val speech by app.speech.state.collectAsStateWithLifecycle()
    val index = speech.index.coerceAtLeast(0)
    val item = items.getOrNull(index)

    Scaffold(timeText = { TimeText() }) {
        Box(Modifier.fillMaxSize()) {
            CircularProgressIndicator(
                progress = speech.progress,
                modifier = Modifier.fillMaxSize().padding(2.dp),
                indicatorColor = Teal,
                trackColor = Surface2,
                strokeWidth = 4.dp,
            )
            Column(
                Modifier.fillMaxSize().padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (item == null) {
                    Text("Nada para tocar.", color = Muted, textAlign = TextAlign.Center)
                    return@Column
                }
                Text("Notícia ${index + 1} de ${items.size}", fontSize = 11.sp, color = Muted)
                Spacer(Modifier.height(4.dp))
                Text(
                    item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoundButton(R.drawable.ic_prev, "Anterior", 40.dp) { app.speech.skip(-1) }
                    RoundButton(
                        if (speech.speaking) R.drawable.ic_pause else R.drawable.ic_play,
                        if (speech.speaking) "Pausar" else "Tocar",
                        54.dp,
                        primary = true,
                    ) { app.speech.toggle() }
                    RoundButton(R.drawable.ic_next, "Próxima", 40.dp) { app.speech.skip(1) }
                }
                speech.error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, fontSize = 11.sp, color = Danger, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun RoundButton(icon: Int, description: String, size: Dp, primary: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(size),
        colors = if (primary) ButtonDefaults.primaryButtonColors() else ButtonDefaults.buttonColors(backgroundColor = Surface2, contentColor = Color.White),
    ) {
        Icon(painterResource(icon), contentDescription = description, modifier = Modifier.size(size * 0.45f))
    }
}
