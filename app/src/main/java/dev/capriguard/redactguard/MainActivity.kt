package dev.capriguard.redactguard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.capriguard.redactguard.core.GuardTheme
import dev.capriguard.redactguard.redact.RedactViewModel
import dev.capriguard.redactguard.ui.HomeScreen
import dev.capriguard.redactguard.ui.SettingsScreen

private enum class Route { Home, Settings }

/**
 * Share targets: the moment a picture or a message is about to be sent is the
 * moment this app is useful, and that moment happens in another app.
 *
 * A content URI from a share is read once and copied, because the provider behind
 * it is not obliged to hand the same stream out twice. ACTION_VIEW is not
 * registered at all — being offered a file is not a reason to keep it, and the
 * copy this app makes is deleted as soon as it has been decoded.
 */
class MainActivity : ComponentActivity() {

    private val vm: RedactViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        takeShared(intent)
        setContent { GuardTheme { RedactGuardRoot() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        takeShared(intent)
    }

    /** The ViewModel is this activity's own store instance, so the route below shares it. */
    @Suppress("DEPRECATION")
    private fun takeShared(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        (intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri)?.let { vm.takeImage(it) }
        val shared = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            ?: intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT)?.toString()
        if (!shared.isNullOrBlank()) vm.receiveSharedText(shared)
    }
}

@Composable
fun RedactGuardRoot(vm: RedactViewModel = viewModel()) {
    var route by remember { mutableStateOf(Route.Home) }

    BackHandler(enabled = route == Route.Settings) { route = Route.Home }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = route,
            transitionSpec = {
                if (targetState == Route.Settings) {
                    (slideInHorizontally(tween(230)) { it / 5 } + fadeIn(tween(140))) togetherWith
                        (slideOutHorizontally(tween(230)) { -it / 6 } + fadeOut(tween(120)))
                } else {
                    (slideInHorizontally(tween(230)) { -it / 6 } + fadeIn(tween(140))) togetherWith
                        (slideOutHorizontally(tween(230)) { it / 5 } + fadeOut(tween(120)))
                }
            },
            label = "route",
        ) { current ->
            when (current) {
                Route.Home -> HomeScreen(vm = vm, onOpenSettings = { route = Route.Settings })
                Route.Settings -> SettingsScreen(vm = vm, onBack = { route = Route.Home })
            }
        }
    }
}
