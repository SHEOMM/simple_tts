package ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChunkPreview(chunks: List<String>, modifier: Modifier = Modifier) {
    if (chunks.isEmpty()) return
    var expanded by remember(chunks) { mutableStateOf(false) }
    val totalChars = remember(chunks) { chunks.sumOf { it.length } }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "${chunks.size}개 청크로 분할 예정 · 총 ${totalChars}자",
                style = MaterialTheme.typography.bodySmall,
            )
            Box(modifier = Modifier.weight(1f))
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "접기" else "펼쳐보기")
            }
        }

        if (expanded) {
            Surface(
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth().height(140.dp),
            ) {
                Column(
                    modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    chunks.forEachIndexed { index, chunk ->
                        val firstLine = chunk.lineSequence().firstOrNull()?.take(MAX_LINE_PREVIEW).orEmpty()
                        Text(
                            "[${index + 1}] ${chunk.length}자 · $firstLine${if (chunk.length > firstLine.length) "…" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

private const val MAX_LINE_PREVIEW = 80
