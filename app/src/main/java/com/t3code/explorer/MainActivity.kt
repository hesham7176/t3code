package com.t3code.explorer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.t3code.explorer.ui.ExplorerApp
import com.t3code.explorer.ui.ExplorerViewModel
import com.t3code.explorer.ui.theme.ExplorerTheme
import com.t3code.explorer.media.MediaLifecycleObserver

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<ExplorerViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(MediaLifecycleObserver((application as ExplorerApplication).mediaEngine))
        enableEdgeToEdge()
        setContent {
            val state = viewModel.uiState.collectAsStateWithLifecycle().value
            ExplorerTheme(preferences = state.preferences) {
                ExplorerApp(viewModel)
            }
        }
    }
}
