package com.hereliesaz.graffitixr.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hereliesaz.graffitixr.data.azphalt.InstalledExtension

@Composable
fun ExtensionsPanel(
    extensions: List<InstalledExtension>,
    onSelect: (String) -> Unit,
    onClose: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Extensions", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(
                "Close",
                color = Color.Gray,
                modifier = Modifier
                    .clickable { onClose() }
                    .padding(8.dp)
            )
        }
        
        if (extensions.isEmpty()) {
            Text(
                "No code extensions installed.",
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            LazyColumn(Modifier.fillMaxWidth()) {
                items(extensions) { extension ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(extension.id) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = extension.manifest.name,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            val description = extension.manifest.description
                            if (!description.isNullOrBlank()) {
                                Text(
                                    text = description,
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
