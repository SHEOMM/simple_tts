package ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import domain.TtsModel
import domain.Voice
import domain.Voices
import ui.components.ApiKeyField
import ui.components.DropdownSelector
import ui.components.FilePickerField
import ui.components.FilePickerMode
import ui.components.LogPanel
import ui.components.StyleField
import java.awt.Desktop

@Composable
fun ConvertScreen(viewModel: ConvertViewModel) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Gemini TTS", style = MaterialTheme.typography.headlineSmall)

        ApiKeyField(
            value = state.apiKey,
            onValueChange = viewModel::onApiKeyChange,
            onSave = viewModel::onApiKeySave,
            onClear = viewModel::onApiKeyClear,
        )

        FilePickerField(
            label = "입력 .txt 파일",
            value = state.inputPath,
            extension = "txt",
            mode = FilePickerMode.Load,
            onValueChange = viewModel::onInputPathChange,
        )

        FilePickerField(
            label = "출력 .wav 파일",
            value = state.outputPath,
            extension = "wav",
            mode = FilePickerMode.Save,
            onValueChange = viewModel::onOutputPathChange,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DropdownSelector(
                label = "모델",
                options = TtsModel.all,
                selected = state.model,
                displayName = TtsModel::displayName,
                onSelect = viewModel::onModelSelect,
                modifier = Modifier.weight(1f),
            )
            DropdownSelector(
                label = "음성",
                options = Voices.all,
                selected = state.voice,
                displayName = Voice::label,
                onSelect = viewModel::onVoiceSelect,
                modifier = Modifier.weight(1f),
            )
        }

        StyleField(value = state.style, onValueChange = viewModel::onStyleChange)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(enabled = state.canConvert, onClick = viewModel::onConvert) {
                Text(if (state.isRunning) "변환 중..." else "변환 시작")
            }
            state.lastOutput?.let { path ->
                TextButton(onClick = { Desktop.getDesktop().open(path.toFile()) }) {
                    Text("재생")
                }
            }
            if (state.statusText.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Text(state.statusText, style = MaterialTheme.typography.bodySmall)
            }
        }

        if (state.isRunning || state.progress > 0f) {
            LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
        }

        LogPanel(logs = state.logs)
    }
}
