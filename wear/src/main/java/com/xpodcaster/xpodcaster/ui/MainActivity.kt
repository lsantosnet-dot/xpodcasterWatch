package com.xpodcaster.xpodcaster.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.xpodcaster.xpodcaster.WatchApp
import com.xpodcaster.xpodcaster.data.ResumeMode

class MainActivity : ComponentActivity() {
    private val app get() = application as WatchApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WearTheme { WearNavigation(app) } }
    }

    override fun onStart() {
        super.onStart()
        // Reanuncia ao celular que o app está aqui (ajuda o celular a achar o relógio).
        app.store.publishStatus()
    }
}

@Composable
private fun WearNavigation(app: WatchApp) {
    val nav = rememberSwipeDismissableNavController()
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
        composable("confirm") {
            ConfirmClearScreen(
                app = app,
                onDone = { nav.popBackStack("list", inclusive = false) },
            )
        }
    }
}
