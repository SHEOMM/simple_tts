package ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LogPanel(logs: List<String>, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }
    val joined = remember(logs) { logs.joinToString("\n") }

    val scrollState = rememberScrollState()
    LaunchedEffect(logs.size) {
        if (logs.isEmpty()) return@LaunchedEffect
        val shouldFollow = scrollState.value >= scrollState.maxValue - AUTO_FOLLOW_THRESHOLD || logs.size <= 1
        if (shouldFollow) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("로그", style = MaterialTheme.typography.titleSmall)
            Box(modifier = Modifier.weight(1f))
            TextButton(
                enabled = logs.isNotEmpty(),
                onClick = {
                    clipboard.setText(AnnotatedString(joined))
                    copied = true
                    scope.launch {
                        delay(COPY_FEEDBACK_MS)
                        copied = false
                    }
                },
            ) {
                Text(if (copied) "복사됨" else "전체 복사")
            }
        }

        Surface(
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).height(180.dp),
        ) {
            Box(modifier = Modifier.padding(8.dp).verticalScroll(scrollState)) {
                SelectionContainer {
                    Text(
                        text = if (logs.isEmpty()) "(아직 없음)" else joined,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private const val COPY_FEEDBACK_MS = 1500L
private const val AUTO_FOLLOW_THRESHOLD = 50
