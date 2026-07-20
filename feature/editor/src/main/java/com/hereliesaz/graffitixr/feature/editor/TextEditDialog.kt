// FILE: feature/editor/src/main/java/com/hereliesaz/graffitixr/feature/editor/TextEditDialog.kt
package com.hereliesaz.graffitixr.feature.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.model.AzButtonShape
import kotlin.math.roundToInt

/**
 * Edits a text layer's content and size. Both apply live — every keystroke / slider move calls back
 * so the layer re-rasterizes and the canvas updates. Seeded from the layer's current params; the
 * view model owns re-rasterization and persistence.
 */
@Composable
fun TextEditDialog(
    initialText: String,
    initialSizeDp: Float,
    onTextChange: (String) -> Unit,
    onSizeChange: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialText) }
    var size by remember { mutableFloatStateOf(initialSizeDp.coerceIn(8f, 300f)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Text") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        onTextChange(it)
                    },
                    label = { Text("Content") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Size ${size.roundToInt()} dp")
                Slider(
                    value = size,
                    onValueChange = {
                        size = it
                        onSizeChange(it)
                    },
                    valueRange = 8f..300f,
                )
            }
        },
        confirmButton = {
            AzButton(text = "Done", onClick = onDismiss, shape = AzButtonShape.RECTANGLE)
        },
    )
}
