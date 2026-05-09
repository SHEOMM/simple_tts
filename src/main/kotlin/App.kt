import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

private val VOICES = listOf(
    "Kore" to "차분, 중성",
    "Charon" to "깊은 남성",
    "Puck" to "밝고 활기",
    "Aoede" to "부드러운 여성",
    "Leda" to "또렷함",
    "Fenrir" to "강렬한 남성",
    "Algieba" to "매끄러움",
    "Achernar" to "부드러운 여성",
)

private val MODELS = listOf(
    MODEL_FLASH to "Flash (무료 티어 가능)",
    MODEL_PRO to "Pro (결제 활성 필요, 더 자연스러움)",
)

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        runCli(args)
        return
    }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Gemini TTS",
            state = rememberWindowState(width = 760.dp, height = 720.dp),
        ) {
            MaterialTheme { Surface(modifier = Modifier.fillMaxSize()) { App() } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App() {
    var apiKey by remember { mutableStateOf(Settings.loadApiKey()) }
    var apiKeyMasked by remember { mutableStateOf(true) }

    var inputPath by remember { mutableStateOf(Settings.lastInputPath()) }
    var outputPath by remember { mutableStateOf("") }

    var model by remember { mutableStateOf(Settings.lastModel()) }
    var modelExpanded by remember { mutableStateOf(false) }

    var voice by remember { mutableStateOf(Settings.lastVoice()) }
    var voiceExpanded by remember { mutableStateOf(false) }

    var styleInstruction by remember { mutableStateOf(Settings.lastStyle()) }

    var isRunning by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }
    var progressFraction by remember { mutableStateOf(0f) }
    var log by remember { mutableStateOf("") }
    var lastOutput by remember { mutableStateOf<File?>(null) }

    val scope = rememberCoroutineScope()

    fun appendLog(line: String) { log = if (log.isEmpty()) line else "$log\n$line" }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Gemini TTS", style = MaterialTheme.typography.headlineSmall)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                singleLine = true,
                visualTransformation = if (apiKeyMasked) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { apiKeyMasked = !apiKeyMasked }) {
                Text(if (apiKeyMasked) "보기" else "숨김")
            }
            TextButton(onClick = { Settings.saveApiKey(apiKey); appendLog("API 키 저장됨.") }) { Text("저장") }
            TextButton(onClick = { Settings.clearApiKey(); apiKey = ""; appendLog("API 키 삭제됨.") }) { Text("지우기") }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = inputPath,
                onValueChange = { inputPath = it },
                label = { Text("입력 .txt 파일") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = {
                pickFile(load = true, ext = "txt", initialDir = File(inputPath).parent)?.let { f ->
                    inputPath = f.absolutePath
                    Settings.setLastInputPath(f.absolutePath)
                    if (outputPath.isBlank()) {
                        outputPath = f.resolveSibling(f.nameWithoutExtension + ".wav").absolutePath
                    }
                }
            }) { Text("찾기...") }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = outputPath,
                onValueChange = { outputPath = it },
                label = { Text("출력 .wav 파일") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = {
                val initial = outputPath.ifBlank { Settings.lastOutputDir() }
                pickFile(load = false, ext = "wav", initialDir = File(initial).parent)?.let { f ->
                    val withExt = if (f.extension.equals("wav", true)) f else File(f.absolutePath + ".wav")
                    outputPath = withExt.absolutePath
                    Settings.setLastOutputDir(withExt.parent)
                }
            }) { Text("찾기...") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                ExposedDropdownMenuBox(expanded = modelExpanded, onExpandedChange = { modelExpanded = it }) {
                    OutlinedTextField(
                        value = MODELS.firstOrNull { it.first == model }?.second ?: model,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("모델") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                        MODELS.forEach { (id, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                model = id; modelExpanded = false; Settings.setLastModel(id)
                            })
                        }
                    }
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                ExposedDropdownMenuBox(expanded = voiceExpanded, onExpandedChange = { voiceExpanded = it }) {
                    OutlinedTextField(
                        value = "$voice — ${VOICES.firstOrNull { it.first == voice }?.second ?: ""}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("음성") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = voiceExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = voiceExpanded, onDismissRequest = { voiceExpanded = false }) {
                        VOICES.forEach { (id, desc) ->
                            DropdownMenuItem(text = { Text("$id — $desc") }, onClick = {
                                voice = id; voiceExpanded = false; Settings.setLastVoice(id)
                            })
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = styleInstruction,
            onValueChange = { styleInstruction = it; Settings.setLastStyle(it) },
            label = { Text("스타일 지시 (선택) — 예: 차분하고 부드러운 톤으로 천천히 낭독해주세요") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
            keyboardOptions = KeyboardOptions.Default,
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                enabled = !isRunning && apiKey.isNotBlank() && inputPath.isNotBlank() && outputPath.isNotBlank(),
                onClick = {
                    val inFile = File(inputPath)
                    val outFile = File(outputPath)
                    if (!inFile.exists()) { appendLog("입력 파일을 찾을 수 없습니다: $inputPath"); return@Button }
                    log = ""
                    progressText = "준비 중..."
                    progressFraction = 0f
                    isRunning = true
                    lastOutput = null
                    scope.launch {
                        try {
                            val text = withContext(Dispatchers.IO) { Files.readString(inFile.toPath()) }
                            val req = TtsRequest(
                                text = text,
                                apiKey = apiKey,
                                model = model,
                                voice = voice,
                                styleInstruction = styleInstruction,
                            )
                            val outPath: Path = synthesizeToWav(req, outFile.toPath()) { current, total, chars ->
                                progressFraction = current.toFloat() / total.toFloat()
                                progressText = "[$current/$total] 합성 중 ($chars 자)"
                                appendLog(progressText)
                            }
                            val sizeKb = Files.size(outPath) / 1024
                            appendLog("완료: ${outPath} (${sizeKb} KB)")
                            progressText = "완료"
                            progressFraction = 1f
                            lastOutput = outPath.toFile()
                        } catch (e: Throwable) {
                            appendLog("실패: ${e.message}")
                            progressText = "실패"
                        } finally {
                            isRunning = false
                        }
                    }
                },
            ) { Text(if (isRunning) "변환 중..." else "변환 시작") }

            if (lastOutput != null) {
                TextButton(onClick = {
                    runCatching { Desktop.getDesktop().open(lastOutput!!) }
                        .onFailure { appendLog("재생 실패: ${it.message}") }
                }) { Text("재생") }
            }

            if (progressText.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Text(progressText, style = MaterialTheme.typography.bodySmall)
            }
        }

        if (isRunning || progressFraction > 0f) {
            LinearProgressIndicator(progress = { progressFraction }, modifier = Modifier.fillMaxWidth())
        }

        Text("로그", style = MaterialTheme.typography.titleSmall)
        Surface(
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).height(180.dp),
        ) {
            Box(modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
                Text(log.ifEmpty { "(아직 없음)" }, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (apiKey.isBlank()) appendLog("API 키를 입력하세요. https://aistudio.google.com/apikey 에서 발급.")
    }
}

private fun pickFile(load: Boolean, ext: String, initialDir: String?): File? {
    val dialog = FileDialog(null as Frame?, if (load) "파일 선택" else "저장 위치", if (load) FileDialog.LOAD else FileDialog.SAVE)
    if (!initialDir.isNullOrBlank()) dialog.directory = initialDir
    dialog.setFilenameFilter { _, name -> name.endsWith(".$ext", ignoreCase = true) }
    if (load) dialog.file = "*.$ext"
    dialog.isVisible = true
    val file = dialog.file ?: return null
    val dir = dialog.directory ?: return null
    return File(dir, file)
}

private fun runCli(args: Array<String>) {
    val apiKey = System.getenv("GEMINI_API_KEY")
        ?: Settings.loadApiKey().ifBlank {
            System.err.println("GEMINI_API_KEY 환경변수가 설정되지 않았고 저장된 키도 없습니다.")
            exitProcess(1)
        }
    val inputPath = Path.of(args[0])
    if (!Files.exists(inputPath)) {
        System.err.println("입력 파일을 찾을 수 없습니다: $inputPath")
        exitProcess(1)
    }
    val outputPath = if (args.size > 1) Path.of(args[1]) else {
        val base = inputPath.fileName.toString().substringBeforeLast('.', inputPath.fileName.toString())
        inputPath.resolveSibling("$base.wav")
    }
    val text = Files.readString(inputPath)
    val req = TtsRequest(
        text = text,
        apiKey = apiKey,
        model = Settings.lastModel(),
        voice = Settings.lastVoice(),
        styleInstruction = Settings.lastStyle(),
    )
    kotlinx.coroutines.runBlocking {
        synthesizeToWav(req, outputPath) { c, t, n ->
            println("[$c/$t] 합성 중... ($n 자)")
        }
    }
    println("완료: $outputPath")
}
