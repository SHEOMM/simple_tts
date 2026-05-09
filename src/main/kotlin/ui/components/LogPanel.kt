package ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LogPanel(logs: List<String>, modifier: Modifier = Modifier) {
    Surface(
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth().heightIn(min = 120.dp).height(180.dp),
    ) {
        Box(modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
            Text(
                text = if (logs.isEmpty()) "(아직 없음)" else logs.joinToString("\n"),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
