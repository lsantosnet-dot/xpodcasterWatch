package com.xpodcaster.xpodcaster.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.xpodcaster.xpodcaster.WatchApp

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
                onOpen = { i -> nav.navigate("detail/$i") },
                onPlayAll = {
                    app.speech.playFrom(0)
                    nav.navigate("player")
                },
                onPlayer = { nav.navigate("player") },
                onClear = { nav.navigate("confirm") },
            )
        }
        composable("detail/{i}") { entry ->
            val i = entry.arguments?.getString("i")?.toIntOrNull() ?: return@composable
            DetailScreen(
                app = app,
                index = i,
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
