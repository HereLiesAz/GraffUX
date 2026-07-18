package com.hereliesaz.graphixr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hereliesaz.graffitixr.feature.editor.EditorViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * GraphiXR entry point.
 *
 * This first migration milestone hosts the shared [EditorViewModel] so the whole editor stack
 * (feature:editor + the core modules + native bridge) and its Hilt graph are proven to build and
 * resolve inside GraphiXR. The full editor screen — layer render + canvas + tool rail + panels,
 * extracted from GraffitiXR's MainActivity into :feature:editor — is wired in the next increment.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                GraphixrEditorHost()
            }
        }
    }
}

@Composable
private fun GraphixrEditorHost() {
    val vm: EditorViewModel = hiltViewModel()
    val uiState by vm.uiState.collectAsState()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "GraphiXR editor — ${uiState.layers.size} layer(s)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
