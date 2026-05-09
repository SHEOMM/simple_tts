package ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

enum class FilePickerMode { Load, Save }

@Composable
fun FilePickerField(
    label: String,
    value: String,
    extension: String,
    mode: FilePickerMode,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialDirectory: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = {
            val initial = initialDirectory.takeUnless { it.isNullOrBlank() }
                ?: value.takeIf { it.isNotBlank() }?.let { File(it).parent }
            pickFile(mode, extension, initial)?.let { onValueChange(it.absolutePath) }
        }) { Text("찾기...") }
    }
}

private fun pickFile(mode: FilePickerMode, extension: String, initialDirectory: String?): File? {
    val title = if (mode == FilePickerMode.Load) "파일 선택" else "저장 위치"
    val flag = if (mode == FilePickerMode.Load) FileDialog.LOAD else FileDialog.SAVE
    val dialog = FileDialog(null as Frame?, title, flag).apply {
        if (!initialDirectory.isNullOrBlank()) directory = initialDirectory
        setFilenameFilter { _, name -> name.endsWith(".$extension", ignoreCase = true) }
        if (mode == FilePickerMode.Load) file = "*.$extension"
        isVisible = true
    }
    val name = dialog.file ?: return null
    val dir = dialog.directory ?: return null
    val raw = File(dir, name)
    return if (mode == FilePickerMode.Save && !raw.extension.equals(extension, ignoreCase = true)) {
        File(raw.absolutePath + ".$extension")
    } else {
        raw
    }
}
