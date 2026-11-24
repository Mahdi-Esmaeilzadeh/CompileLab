package com.mahdi.compilerstudio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val APP_VERSION = "1.10.0"

internal val Bg = Color(0xFF0B1020)
internal val Panel = Color(0xFF141D30)
internal val PanelRaised = Color(0xFF1B2940)
internal val PanelAlt = Color(0xFF111A2C)
internal val Accent = Color(0xFF8BA7FF)
internal val MainText = Color(0xFFE7ECF5)
internal val Muted = Color(0xFF98A7C0)
internal val Green = Color(0xFF75D6A5)
internal val Red = Color(0xFFFF8585)
internal val Amber = Color(0xFFFFC978)

/** Run [content] with left-to-right layout (code, tables and trees must never be mirrored). */
@Composable
internal fun Ltr(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { content() }
}

@Composable
internal fun SectionLabel(label: String) {
    Text(text = label, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
}

/** A small tappable pill. */
@Composable
internal fun CsChip(label: String, color: Color = MainText, background: Color = PanelRaised, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = color, fontSize = 12.sp)
    }
}

/** A table whose columns have fixed widths; scrolls sideways when wider than the screen. */
@Composable
internal fun TableView(table: Table, codeSize: Int) {
    Ltr {
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            Row(Modifier.background(PanelRaised)) {
                table.headers.forEachIndexed { i, header ->
                    Text(
                        text = header,
                        modifier = Modifier.width(table.widths[i].dp).padding(horizontal = 6.dp, vertical = 7.dp),
                        color = Accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            table.rows.forEachIndexed { r, row ->
                Row(Modifier.background(if (r % 2 == 0) Panel else PanelAlt)) {
                    row.forEachIndexed { c, cell ->
                        val prose = c == table.proseColumn
                        Text(
                            text = cell.ifEmpty { "–" },
                            modifier = Modifier.width(table.widths[c].dp).padding(horizontal = 6.dp, vertical = 5.dp),
                            color = if (prose) Muted else if (cell.isEmpty()) Muted else MainText,
                            fontFamily = if (prose) FontFamily.Default else FontFamily.Monospace,
                            fontSize = if (prose) 11.sp else codeSize.sp,
                            lineHeight = (codeSize * 1.4f).sp
                        )
                    }
                }
            }
        }
    }
}

private fun nodeColor(kind: NodeKind): Color = when (kind) {
    NodeKind.ROOT -> Accent
    NodeKind.STATEMENT -> Accent
    NodeKind.GROUP -> Amber
    NodeKind.OPERATOR -> Color(0xFFFFB86C)
    NodeKind.LITERAL -> Green
    NodeKind.VARIABLE -> MainText
}

/** The syntax tree drawn with ├── └── │ branches. */
@Composable
internal fun TreeView(lines: List<TreeLine>, codeSize: Int) {
    Ltr {
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            lines.forEach { line ->
                Row {
                    Text(
                        text = line.prefix,
                        color = Muted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = codeSize.sp,
                        lineHeight = (codeSize * 1.25f).sp,
                        softWrap = false
                    )
                    Text(
                        text = line.label,
                        color = nodeColor(line.kind),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (line.kind == NodeKind.ROOT || line.kind == NodeKind.STATEMENT) FontWeight.Bold else FontWeight.Normal,
                        fontSize = codeSize.sp,
                        lineHeight = (codeSize * 1.25f).sp,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/** Friendly explanation of one error, with the offending line. */
@Composable
internal fun ErrorCard(error: FriendlyError, lang: Lang, codeSize: Int, onShowStage: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0xFF2A1620))
            .border(1.dp, Red, RoundedCornerShape(13.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val where = if (error.stage == 0) lang.pick("WHILE READING YOUR CODE", "هنگام خواندن کد")
            else lang.pick("ERROR IN STAGE ${error.stage}", "خطا در مرحله ${error.stage}")
            Text(text = where, color = Red, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.weight(1f))
            error.line?.let {
                Text(text = lang.pick("Line $it", "خط $it"), color = Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(text = error.title, color = MainText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        error.sourceLine?.let { src ->
            Spacer(Modifier.height(8.dp))
            Ltr {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Bg)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${error.line ?: ""} │ ${src.ifBlank { " " }}",
                        color = Color(0xFFFFB3B3),
                        fontFamily = FontFamily.Monospace,
                        fontSize = codeSize.sp,
                        softWrap = false
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(text = error.explanation, color = MainText, fontSize = 13.sp, lineHeight = 19.sp)
        Spacer(Modifier.height(8.dp))
        Text(text = lang.pick("HOW TO FIX IT", "چطور درستش کنیم"), color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(2.dp))
        Text(text = error.tip, color = Green, fontSize = 13.sp, lineHeight = 19.sp)
        Spacer(Modifier.height(8.dp))
        Ltr {
            Text(text = error.technical.replace("\n", "↵"), color = Muted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        }
        if (error.stage in 1..6) {
            TextButton(onClick = { onShowStage(error.stage - 1) }) {
                Text(text = lang.pick("Show stage ${error.stage}", "نمایش مرحله ${error.stage}"), color = Accent, fontSize = 12.sp)
            }
        }
    }
}
