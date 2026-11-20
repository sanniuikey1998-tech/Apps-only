package com.example

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.service.DeckLinkQuickTileService
import com.example.ui.DeckLinkApp
import com.example.ui.DeckLinkViewModel

class MainActivity : ComponentActivity() {

    private val deckLinkViewModel: DeckLinkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        handleQuickActionIntent(intent)
        setContent {
            DeckLinkApp(viewModel = deckLinkViewModel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleQuickActionIntent(intent)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && deckLinkViewModel.handleHardwareVolumeKey(isVolumeUp = true)) {
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && deckLinkViewModel.handleHardwareVolumeKey(isVolumeUp = false)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun handleQuickActionIntent(intent: Intent?) {
        val action = intent?.getStringExtra(DeckLinkQuickTileService.EXTRA_QUICK_ACTION) ?: return
        intent.removeExtra(DeckLinkQuickTileService.EXTRA_QUICK_ACTION)
        deckLinkViewModel.handleExternalQuickAction(action)
    }
}
