package com.ainotes.app.ui.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Lightweight Markdown renderer: headings, bold/italic/code, bullet and numbered
 * lists, interactive checklists, quotes and code fences render visually -
 * never raw symbols.
 */
@Composable
fun MarkdownView(
    content: String,
    onToggleLine: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val lines = content.lines()
    Column(modifier = modifier.fillMaxWidth()) {
        var i = 0
        while (i < lines.size) {
            val raw = lines[i]
            val lineIndex = i
            val line = raw.trimEnd()

            when {
                line.trimStart().startsWith("```") -> {
                    val buf = StringBuilder()
                    i++
                    while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                        buf.append(lines[i]).append("\n")
                        i++
                    }
                    i++
                    CodeBlock(buf.toString().trimEnd())
                    Spacer(Modifier.height(10.dp))
                    continue
                }

                headingLevel(line) > 0 -> {
                    Heading(line, headingLevel(line))
                    Spacer(Modifier.height(8.dp))
                }

                isCheckbox(line) -> {
                    CheckboxRow(
                        checked = isChecked(line),
                        text = checkboxText(line),
                        onClick = { onToggleLine(lineIndex) }
                    )
                }

                bulletMarker(line) != null -> {
                    BulletRow(bulletMarker(line)!!, inlineText(line.removePrefix(bulletMarker(line)!!).trim()))
                    Spacer(Modifier.height(4.dp))
                }

                orderedMarker(line) != null -> {
                    NumberRow(orderedMarker(line)!!, inlineText(line.removePrefix(orderedMarker(line)!!).trim()))
                    Spacer(Modifier.height(4.dp))
                }

                line.trimStart().startsWith(">") -> {
                    QuoteRow(inlineText(line.trimStart().removePrefix(">").trim()))
                    Spacer(Modifier.height(8.dp))
                }

                line.isBlank() -> Spacer(Modifier.height(10.dp))

                else -> {
                    Text(
                        inlineText(line),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
            i++
        }
    }
}

private fun headingLevel(line: String): Int {
    val t = line.trimStart()
    return when {
        t.startsWith("### ") -> 3
        t.startsWith("## ") -> 2
        t.startsWith("# ") -> 1
        else -> 0
    }
}

private val CHECKBOX = Regex("^\\s*(?:[-*]\\s*\\[[ xX]\\]|\\[ [xX]\\]|☐|☑)\\s*(.*)$")

private fun isCheckbox(line: String) = CHECKBOX.matches(line)

private fun isChecked(line: String): Boolean {
    val t = line.trimStart()
    return t.contains("[x]") || t.contains("[X]") || t.startsWith("☑")
}

private fun checkboxText(line: String): String {
    val t = line.trimStart()
    val stripped = when {
        t.startsWith("☐") || t.startsWith("☑") -> t.substring(1).trim()
        else -> CHECKBOX.find(t)?.groupValues?.get(1) ?: t
    }
    return stripped
}

private fun bulletMarker(line: String): String? {
    val t = line.trimStart()
    return when {
        t.startsWith("- ") || t.startsWith("* ") || t.startsWith("+ ") -> t.take(2)
        t == "-" || t == "*" -> t
        else -> null
    }
}

private fun orderedMarker(line: String): String? {
    val m = Regex("^\\s*(\\d+\\.)\\s").find(line) ?: return null
    return m.groupValues[1]
}

@Composable
private fun Heading(line: String, level: Int) {
    val style = when (level) {
        1 -> MaterialTheme.typography.headlineSmall
        2 -> MaterialTheme.typography.titleLarge
        else -> MaterialTheme.typography.titleMedium
    }
    val text = line.trimStart().substring(level + 1).trim()
    Column {
        Text(inlineText(text), style = style, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        if (level == 1) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .width(56.dp)
                    .height(3.dp)
            ) {}
        }
    }
}

@Composable
private fun CheckboxRow(checked: Boolean, text: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = { onClick() })
        Text(
            inlineText(text),
            style = MaterialTheme.typography.bodyLarge,
            color = if (checked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (checked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
        )
    }
}

@Composable
private fun BulletRow(marker: String, text: AnnotatedString) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(start = 4.dp)) {
        Text(
            "\u2022",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 10.dp, top = 1.dp)
        )
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun NumberRow(marker: String, text: AnnotatedString) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(start = 4.dp)) {
        Text(
            marker,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(end = 8.dp, top = 1.dp)
        )
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun QuoteRow(text: AnnotatedString) {
    Surface(
        shape = RoundedCornerShape(0.dp, 12.dp, 12.dp, 0.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
            ) {}
            Spacer(Modifier.width(10.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic)
            )
        }
    }
}

@Composable
private fun CodeBlock(code: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            code,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(14.dp)
        )
    }
}

/** Inline **bold**, *italic* and `code` parsing into a styled AnnotatedString. */
fun inlineText(raw: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    val s = raw
    while (i < s.length) {
        when {
            s.startsWith("**", i) -> {
                val end = s.indexOf("**", i + 2)
                if (end > 0) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(s.substring(i + 2, end))
                    }
                    i = end + 2
                } else { append("**"); i += 2 }
            }
            s.startsWith("*", i) && !s.startsWith("**", i) -> {
                val end = s.indexOf('*', i + 1)
                if (end > 0) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(s.substring(i + 1, end))
                    }
                    i = end + 1
                } else { append('*'); i += 1 }
            }
            s.startsWith("`", i) -> {
                val end = s.indexOf('`', i + 1)
                if (end > 0) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = androidx.compose.ui.graphics.Color(0x227C4DFF)
                        )
                    ) { append(s.substring(i + 1, end)) }
                    i = end + 1
                } else { append('`'); i += 1 }
            }
            s.startsWith("_", i) -> {
                val end = s.indexOf('_', i + 1)
                if (end > 0) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(s.substring(i + 1, end))
                    }
                    i = end + 1
                } else { append('_'); i += 1 }
            }
            else -> { append(s[i]); i++ }
        }
    }
}
