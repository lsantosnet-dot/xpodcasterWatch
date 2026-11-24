package com.xpodcaster.xpodcaster.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.xpodcaster.xpodcaster.WatchApp
import com.xpodcaster.xpodcaster.data.ResumeMode
import com.xpodcaster.xpodcaster.notify.NewsNotifier

/** Pedido vindo da notificação: abrir a notícia [index] para ler (no parágrafo [position]) ou ouvir. */
data class OpenRequest(val listen: Boolean, val index: Int, val position: Int, val serial: Long)

class MainActivity : ComponentActivity() {
    private val app get() = application as WatchApp
    private val openRequest = mutableStateOf<OpenRequest?>(null)
    private val askPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            WearTheme {
                WearNavigation(
                    app = app,
                    openRequest = openRequest.value,
                    onOpenHandled = { openRequest.value = null },
                    onRequestPermission = ::requestNotificationPermission,
                )
            }
        }
        if (app.notifySettings.settings.value.enabled && !NewsNotifier.canNotify(this)) {
            requestNotificationPermission()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        // Reanuncia ao celular que o app está aqui (ajuda o celular a achar o relógio).
        app.store.publishStatus()
    }

    private fun requestNotificationPermission() {
        askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun handleIntent(intent: Intent?) {
        val mode = intent?.getStringExtra(NewsNotifier.EXTRA_OPEN) ?: return
        val index = intent.getIntExtra(NewsNotifier.EXTRA_INDEX, -1)
        if (index < 0) return
        openRequest.value = OpenRequest(
            listen = mode == NewsNotifier.OPEN_LISTEN,
            index = index,
            position = intent.getIntExtra(NewsNotifier.EXTRA_POSITION, 0),
            serial = System.nanoTime(),
        )
        NewsNotifier.cancel(this)
    }
}

@Composable
private fun WearNavigation(
    app: WatchApp,
    openRequest: OpenRequest?,
    onOpenHandled: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    val nav = rememberSwipeDismissableNavController()

    LaunchedEffect(openRequest) {
        val req = openRequest ?: return@LaunchedEffect
        if (req.index < app.store.items.value.size) {
            if (req.listen) {
                app.speech.playFrom(req.index)
                nav.navigate("player") { popUpTo("list") }
            } else {
                nav.navigate("detail/${req.index}/${req.position}") { popUpTo("list") }
            }
        }
        onOpenHandled()
    }

    SwipeDismissableNavHost(navController = nav, startDestination = "list") {
        composable("list") {
            ListScreen(
                app = app,
                onOpen = { i -> nav.navigate("detail/$i/0") },
                onResume = { target ->
                    if (target.mode == ResumeMode.LISTEN) {
                        app.speech.playFrom(target.index, target.position)
                        nav.navigate("player")
                    } else {
                        nav.navigate("detail/${target.index}/${target.position}")
                    }
                },
                onPlayAll = {
                    app.speech.playFrom(0)
                    nav.navigate("player")
                },
                onPlayer = { nav.navigate("player") },
                onNotifySettings = { nav.navigate("notify") },
                onClear = { nav.navigate("confirm") },
            )
        }
        composable("detail/{i}/{p}") { entry ->
            val i = entry.arguments?.getString("i")?.toIntOrNull() ?: return@composable
            val p = entry.arguments?.getString("p")?.toIntOrNull() ?: 0
            DetailScreen(
                app = app,
                index = i,
                startParagraph = p,
                onListen = {
                    app.speech.playFrom(i)
                    nav.navigate("player") { popUpTo("list") }
                },
            )
        }
        composable("player") {
            PlayerScreen(app = app)
        }
        composable("notify") {
            NotifySettingsScreen(app = app, onRequestPermission = onRequestPermission)
        }
        composable("confirm") {
            ConfirmClearScreen(
                app = app,
                onDone = { nav.popBackStack("list", inclusive = false) },
            )
        }
    }
}
