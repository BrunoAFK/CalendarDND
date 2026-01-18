package com.brunoafk.calendardnd.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

private sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class Bullet(val text: String) : MarkdownBlock()
    data class Numbered(val index: String, val text: String) : MarkdownBlock()
    object HorizontalRule : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
}

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    enableLinks: Boolean = true,
    onLinkClick: ((String) -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
    val linkHandler = onLinkClick ?: { url -> uriHandler.openUri(url) }
    val blocks = remember(text) { parseMarkdownBlocks(text) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val style = when (block.level) {
                        1 -> MaterialTheme.typography.titleMedium
                        2 -> MaterialTheme.typography.titleSmall
                        else -> MaterialTheme.typography.bodyMedium
                    }
                    MarkdownLine(
                        text = block.text,
                        style = style,
                        color = MaterialTheme.colorScheme.onSurface,
                        enableLinks = enableLinks,
                        onLinkClick = linkHandler
                    )
                }
                is MarkdownBlock.Bullet -> {
                    Row {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        MarkdownLine(
                            text = block.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            enableLinks = enableLinks,
                            onLinkClick = linkHandler
                        )
                    }
                }
                is MarkdownBlock.Numbered -> {
                    Row {
                        Text(
                            text = "${block.index}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        MarkdownLine(
                            text = block.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            enableLinks = enableLinks,
                            onLinkClick = linkHandler
                        )
                    }
                }
                MarkdownBlock.HorizontalRule -> {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                is MarkdownBlock.Paragraph -> {
                    MarkdownLine(
                        text = block.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        enableLinks = enableLinks,
                        onLinkClick = linkHandler
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownLine(
    text: String,
    style: TextStyle,
    color: androidx.compose.ui.graphics.Color,
    enableLinks: Boolean,
    onLinkClick: (String) -> Unit
) {
    val linkStyle = SpanStyle(
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline
    )
    val boldStyle = SpanStyle(fontWeight = FontWeight.SemiBold)
    val italicStyle = SpanStyle(fontStyle = FontStyle.Italic)
    val annotated = remember(text) {
        buildInlineAnnotatedString(text, linkStyle, boldStyle, italicStyle)
    }
    val hasLinks = annotated.getStringAnnotations("URL", 0, annotated.length).isNotEmpty()
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    if (enableLinks && hasLinks) {
        BasicText(
            text = annotated,
            style = style.copy(color = color),
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Clip,
            onTextLayout = { layoutResult = it },
            modifier = Modifier.pointerInput(annotated) {
                detectTapGestures { offset ->
                    val layout = layoutResult ?: return@detectTapGestures
                    val position = layout.getOffsetForPosition(offset)
                    val annotation = annotated
                        .getStringAnnotations("URL", position, position)
                        .firstOrNull()
                    annotation?.let { onLinkClick(it.item) }
                }
            }
        )
    } else {
        Text(
            text = annotated,
            style = style,
            color = color
        )
    }
}

private fun buildInlineAnnotatedString(
    text: String,
    linkStyle: SpanStyle,
    boldStyle: SpanStyle,
    italicStyle: SpanStyle
): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            if (text.startsWith("**", i)) {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    val content = text.substring(i + 2, end)
                    withStyle(boldStyle) { append(content) }
                    i = end + 2
                    continue
                }
            }
            val italicDelimiter = text.getOrNull(i)
            if (italicDelimiter == '*' || italicDelimiter == '_') {
                val end = text.indexOf(italicDelimiter, i + 1)
                if (end != -1) {
                    val content = text.substring(i + 1, end)
                    withStyle(italicStyle) { append(content) }
                    i = end + 1
                    continue
                }
            }
            if (text[i] == '[') {
                val closeBracket = text.indexOf(']', i + 1)
                if (closeBracket != -1 &&
                    closeBracket + 1 < text.length &&
                    text[closeBracket + 1] == '('
                ) {
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeParen != -1) {
                        val label = text.substring(i + 1, closeBracket)
                        val url = text.substring(closeBracket + 2, closeParen)
                        val start = length
                        withStyle(linkStyle) { append(label) }
                        val end = length
                        addStringAnnotation("URL", url, start, end)
                        i = closeParen + 1
                        continue
                    }
                }
            }
            append(text[i])
            i += 1
        }
    }
}

private fun parseMarkdownBlocks(source: String): List<MarkdownBlock> {
    if (source.isBlank()) return emptyList()
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraph = StringBuilder()
    val orderedRegex = Regex("^(\\d+)[.)]\\s+(.*)$")

    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(paragraph.toString().trim()))
            paragraph.clear()
        }
    }

    source.replace("\r\n", "\n").split("\n").forEach { line ->
        val trimmed = line.trim()
        when {
            trimmed.isBlank() -> flushParagraph()
            trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                flushParagraph()
                blocks.add(MarkdownBlock.HorizontalRule)
            }
            trimmed.startsWith("#") -> {
                flushParagraph()
                val level = trimmed.takeWhile { it == '#' }.length.coerceAtMost(3)
                val content = trimmed.dropWhile { it == '#' }.trim()
                if (content.isNotBlank()) {
                    blocks.add(MarkdownBlock.Heading(level, content))
                }
            }
            orderedRegex.matches(trimmed) -> {
                flushParagraph()
                val match = orderedRegex.find(trimmed)
                val index = match?.groupValues?.getOrNull(1).orEmpty()
                val content = match?.groupValues?.getOrNull(2).orEmpty()
                if (content.isNotBlank()) {
                    blocks.add(MarkdownBlock.Numbered(index, content))
                }
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ") -> {
                flushParagraph()
                val content = trimmed.drop(2).trim()
                if (content.isNotBlank()) {
                    blocks.add(MarkdownBlock.Bullet(content))
                }
            }
            else -> {
                if (paragraph.isNotEmpty()) {
                    paragraph.append(' ')
                }
                paragraph.append(trimmed)
            }
        }
    }

    flushParagraph()
    return blocks
}
