package com.ainote.manager.ui.noteedit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ainote.manager.ui.components.MarkdownContent

/** Original vs. AI-organized preview — the user can Apply, Compare (this view), or Cancel. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiOrganizeSheet(
    originalContent: String,
    aiTitle: String,
    aiContent: String,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("AI Organize Preview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Same meaning, better structure. Review before applying.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            Text("Suggested title", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(aiTitle, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            var tabIndex by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(1) }
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Original") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("AI Result") })
            }

            Box(
                Modifier
                    .heightIn(min = 160.dp, max = 340.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 12.dp)
            ) {
                if (tabIndex == 0) {
                    Text(originalContent.ifBlank { "(empty)" }, style = MaterialTheme.typography.bodyMedium)
                } else {
                    MarkdownContent(content = aiContent)
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = onApply, modifier = Modifier.weight(1f)) { Text("Apply") }
            }
        }
    }
}
