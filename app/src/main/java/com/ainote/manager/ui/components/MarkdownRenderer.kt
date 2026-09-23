package com.ainote.manager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ainote.manager.util.MarkdownParser
import com.ainote.manager.util.MdBlock

/**
 * Renders the app's Markdown-like note content as real, formatted UI — headings, bold/
 * italic text, bullet/numbered lists and checkable checklists — never as raw '#'/'*'
 * symbols on screen.
 */
@Composable
fun MarkdownContent(
    content: String,
    modifier: Modifier = Modifier,
    onChecklistToggle: ((lineIndex: Int, checked: Boolean) -> Unit)? = null,
) {
    val blocks = remember(content) { MarkdownParser.parse(content) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is MdBlock.Heading -> {
                    val style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineMedium
                        2 -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.titleMedium
                    }
                    Text(inlineAnnotated(block.text), style = style, modifier = Modifier.padding(top = 6.dp))
                }
                is MdBlock.Paragraph -> Text(inlineAnnotated(block.text), style = MaterialTheme.typography.bodyLarge)
                is MdBlock.BulletItem -> Row {
                    Text("•  ", style = MaterialTheme.typography.bodyLarge)
                    Text(inlineAnnotated(block.text), style = MaterialTheme.typography.bodyLarge)
                }
                is MdBlock.NumberedItem -> Row {
                    Text("${block.number}.  ", style = MaterialTheme.typography.bodyLarge)
                    Text(inlineAnnotated(block.text), style = MaterialTheme.typography.bodyLarge)
                }
                is MdBlock.ChecklistItem -> {
                    var checked by remember(content, index) { mutableStateOf(block.checked) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { newValue ->
                                checked = newValue
                                onChecklistToggle?.invoke(index, newValue)
                            }
                        )
                        Text(
                            inlineAnnotated(block.text),
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun inlineAnnotated(text: String) = buildAnnotatedString {
    MarkdownParser.parseInline(text).forEach { run ->
        when {
            run.bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(run.text) }
            run.italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(run.text) }
            else -> append(run.text)
        }
    }
}
