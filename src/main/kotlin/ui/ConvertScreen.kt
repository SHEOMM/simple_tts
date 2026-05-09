package ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import domain.TtsModel
import domain.Voice
import domain.Voices
import io.github.oshai.kotlinlogging.KotlinLogging
import ui.components.ApiKeyField
import ui.components.ChunkPreview
import ui.components.DropdownSelector
import ui.components.FilePickerField
import ui.components.FilePickerMode
import ui.components.LogPanel
import ui.components.StyleField
import java.awt.datatransfer.DataFlavor
import java.io.File

private val log = KotlinLogging.logger {}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun ConvertScreen(viewModel: ConvertViewModel) {
    val state by viewModel.state.collectAsState()
    val running = state.isRunning
    var dragHover by remember { mutableStateOf(false) }

    val dropTarget = remember {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) { dragHover = true }
            override fun onExited(event: DragAndDropEvent) { dragHover = false }
            override fun onEnded(event: DragAndDropEvent) { dragHover = false }
            override fun onDrop(event: DragAndDropEvent): Boolean {
                dragHover = false
                return runCatching {
                    val transferable = event.awtTransferable
                    if (!transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return@runCatching false
                    @Suppress("UNCHECKED_CAST")
                    val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                    val txt = files.firstOrNull { it.extension.equals("txt", ignoreCase = true) }
                    if (txt != null) {
                        viewModel.onInputPathChange(txt.absolutePath)
                        true
                    } else false
                }.getOrElse {
                    log.warn(it) { "drop handling failed" }
                    false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .let {
                if (dragHover) it.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(8.dp))
                else it
            }
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    !running && event.awtTransferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                },
                target = dropTarget,
            )
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Gemini TTS", style = MaterialTheme.typography.headlineSmall)

        if (dragHover) {
            Text(
                "여기에 .txt 파일을 놓으세요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        ApiKeyField(
            value = state.apiKey,
            onValueChange = viewModel::onApiKeyChange,
            onSave = viewModel::onApiKeySave,
            onClear = viewModel::onApiKeyClear,
            enabled = !running,
        )

        FilePickerField(
            label = "입력 .txt 파일 (창 어디든 끌어다 놓기 가능)",
            value = state.inputPath,
            extension = "txt",
            mode = FilePickerMode.Load,
            onValueChange = viewModel::onInputPathChange,
            enabled = !running,
        )

        if (state.chunkPreview.isNotEmpty()) {
            ChunkPreview(chunks = state.chunkPreview)
        }

        FilePickerField(
            label = "출력 .wav 파일",
            value = state.outputPath,
            extension = "wav",
            mode = FilePickerMode.Save,
            onValueChange = viewModel::onOutputPathChange,
            enabled = !running,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DropdownSelector(
                label = "모델",
                options = TtsModel.all,
                selected = state.model,
                displayName = TtsModel::displayName,
                onSelect = viewModel::onModelSelect,
                modifier = Modifier.weight(1f),
                enabled = !running,
            )
            DropdownSelector(
                label = "음성",
                options = Voices.all,
                selected = state.voice,
                displayName = Voice::label,
                onSelect = viewModel::onVoiceSelect,
                modifier = Modifier.weight(1f),
                enabled = !running,
            )
        }

        StyleField(
            value = state.style,
            onValueChange = viewModel::onStyleChange,
            enabled = !running,
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(enabled = state.canConvert, onClick = viewModel::onConvert) {
                Text(if (running) "변환 중..." else "변환 시작")
            }
            if (running) {
                OutlinedButton(onClick = viewModel::onCancel) { Text("취소") }
            }
            if (state.lastOutput != null) {
                TextButton(onClick = viewModel::onPlayLastOutput) {
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
