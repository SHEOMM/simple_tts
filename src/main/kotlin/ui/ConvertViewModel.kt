package ui

import domain.QuotaScope
import domain.TtsError
import domain.TtsModel
import domain.TtsRequest
import domain.Voice
import infra.AppModule
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import service.ChunkEvent
import service.ProgressCallback
import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

private val log = KotlinLogging.logger {}

class ConvertViewModel(
    private val module: AppModule,
    private val scope: CoroutineScope,
) {
    private val pipeline = module.pipeline
    private val settings = module.settings

    data class UiState(
        val apiKey: String,
        val inputPath: String,
        val outputPath: String,
        val model: TtsModel,
        val voice: Voice,
        val style: String,
        val chunkPreview: List<String> = emptyList(),
        val isRunning: Boolean = false,
        val progress: Float = 0f,
        val statusText: String = "",
        val etaSeconds: Long? = null,
        val logs: List<String> = emptyList(),
        val lastOutput: Path? = null,
    ) {
        val canConvert: Boolean
            get() = !isRunning && apiKey.isNotBlank() && inputPath.isNotBlank() && outputPath.isNotBlank()
    }

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var job: Job? = null
    private var previewJob: Job? = null

    private var firstChunkStartNanos = 0L
    private var lastChunkStartNanos = 0L
    private var avgChunkNanos = 0L
    private var sampledChunks = 0

    fun onApiKeyChange(value: String) = _state.update { it.copy(apiKey = value) }

    fun onApiKeySave() {
        settings.apiKey = _state.value.apiKey
        appendLog("API 키 저장됨.")
    }

    fun onApiKeyClear() {
        settings.clearApiKey()
        _state.update { it.copy(apiKey = "") }
        appendLog("API 키 삭제됨.")
    }

    fun onInputPathChange(path: String) {
        _state.update {
            val nextOutput = if (it.outputPath.isBlank() || it.outputPath == deriveOutputPath(it.inputPath)) {
                deriveOutputPath(path)
            } else {
                it.outputPath
            }
            it.copy(inputPath = path, outputPath = nextOutput)
        }
        refreshChunkPreview(path)
    }

    fun onOutputPathChange(path: String) {
        if (path.isNotBlank()) settings.lastOutputDir = File(path).parent.orEmpty()
        _state.update { it.copy(outputPath = path) }
    }

    fun onModelSelect(model: TtsModel) {
        settings.lastModel = model
        _state.update { it.copy(model = model) }
    }

    fun onVoiceSelect(voice: Voice) {
        settings.lastVoice = voice
        _state.update { it.copy(voice = voice) }
    }

    fun onStyleChange(value: String) {
        settings.lastStyle = value
        _state.update { it.copy(style = value) }
    }

    fun onConvert() {
        val current = _state.value
        if (!current.canConvert) return

        val input = File(current.inputPath)
        if (!input.exists()) {
            appendLog("입력 파일을 찾을 수 없습니다: ${current.inputPath}")
            return
        }

        val target = ensureUniquePath(File(current.outputPath).toPath())
        if (target.toString() != current.outputPath) {
            appendLog("기존 파일이 있어 ${target.fileName}로 저장합니다.")
        }

        resetTiming()
        _state.update {
            it.copy(
                outputPath = target.toString(),
                isRunning = true,
                progress = 0f,
                statusText = "준비 중...",
                etaSeconds = null,
                logs = emptyList(),
                lastOutput = null,
            )
        }

        val callback = ProgressCallback { event -> handleEvent(event) }
        module.progressCallback = callback

        job = scope.launch {
            try {
                val text = withContext(Dispatchers.IO) { Files.readString(input.toPath()) }
                val request = TtsRequest(
                    text = text,
                    apiKey = current.apiKey,
                    model = current.model,
                    voice = current.voice,
                    styleInstruction = current.style,
                )
                val outputPath = pipeline.synthesizeToFile(
                    request = request,
                    outputPath = target,
                    onProgress = callback,
                )
                val sizeKb = Files.size(outputPath) / 1024
                appendLog("완료: $outputPath (${sizeKb} KB)")
                _state.update {
                    it.copy(progress = 1f, statusText = "완료", lastOutput = outputPath, etaSeconds = null)
                }
            } catch (e: TtsError.RateLimited) {
                handleRateLimited(e)
            } catch (e: TtsError) {
                appendLog("실패: ${e.message}")
                _state.update { it.copy(statusText = "실패") }
            } catch (e: CancellationException) {
                appendLog("변환 취소됨.")
                _state.update { it.copy(statusText = "취소됨") }
                throw e
            } catch (e: Throwable) {
                log.error(e) { "unexpected error during conversion" }
                appendLog("예상치 못한 오류: ${e.message}")
                _state.update { it.copy(statusText = "실패") }
            } finally {
                module.progressCallback = ProgressCallback.Noop
                _state.update { it.copy(isRunning = false) }
            }
        }
    }

    fun onCancel() {
        job?.cancel()
    }

    fun onPlayLastOutput() {
        val file = _state.value.lastOutput?.toFile() ?: return
        if (!file.exists()) {
            appendLog("재생 실패: 파일이 존재하지 않습니다.")
            return
        }
        val opened = runCatching {
            if (!Desktop.isDesktopSupported()) return@runCatching false
            val desktop = Desktop.getDesktop()
            if (!desktop.isSupported(Desktop.Action.OPEN)) return@runCatching false
            desktop.open(file)
            true
        }.getOrElse { e ->
            log.warn(e) { "Desktop.open failed for ${file.absolutePath}" }
            false
        }
        if (!opened) {
            appendLog("재생 실패: 이 환경에서 파일 열기를 지원하지 않습니다. 직접 열어 보세요: ${file.absolutePath}")
        }
    }

    private fun handleEvent(event: ChunkEvent) {
        when (event) {
            is ChunkEvent.Started -> recordChunkStart(event)
            is ChunkEvent.Waiting -> recordWaiting(event)
        }
    }

    private fun recordChunkStart(event: ChunkEvent.Started) {
        val now = System.nanoTime()
        if (firstChunkStartNanos == 0L) firstChunkStartNanos = now
        if (lastChunkStartNanos > 0L) {
            val elapsed = now - lastChunkStartNanos
            avgChunkNanos = ((avgChunkNanos * sampledChunks) + elapsed) / (sampledChunks + 1)
            sampledChunks++
        }
        lastChunkStartNanos = now

        val remainingChunks = event.total - event.current + 1
        val etaSeconds = if (sampledChunks > 0) (avgChunkNanos * remainingChunks) / NANOS_PER_SECOND else null
        val etaSuffix = etaSeconds?.let { " · ETA ${formatEta(it)}" }.orEmpty()
        val message = "[${event.current}/${event.total}] 합성 중 (${event.chars}자)$etaSuffix"

        _state.update {
            it.copy(
                progress = (event.current - 1).toFloat() / event.total.toFloat(),
                statusText = message,
                etaSeconds = etaSeconds,
            )
        }
        appendLog(message)
    }

    private fun recordWaiting(event: ChunkEvent.Waiting) {
        val statusText = "${event.reason.displayName} · ${event.remainingSeconds}s 남음"
        _state.update { it.copy(statusText = statusText) }
        if (event.isFirstTick) {
            val advice = event.reason.advice.takeIf { it.isNotBlank() }?.let { " — $it" }.orEmpty()
            appendLog("${event.reason.displayName} · 약 ${event.totalSeconds}초 대기$advice")
        }
    }

    private fun handleRateLimited(e: TtsError.RateLimited) {
        val violation = e.mostSevere
        val head = violation?.let { "${it.displayName} 초과" } ?: "호출 한도 초과"
        val modelHint = violation?.model?.let { " (모델: $it)" }.orEmpty()
        val tail = when (violation?.scope) {
            QuotaScope.RequestsPerDay -> "내일 다시 시도하거나 결제를 활성화하세요"
            QuotaScope.InputTokensPerMinute -> "더 짧은 청크로 재시도하거나 잠시 후 다시 시도하세요"
            QuotaScope.RequestsPerMinute -> e.retryDelay?.let { "${it.toSeconds()}초 후 재시도 가능" }
                ?: "잠시 후 다시 시도하세요"
            is QuotaScope.Unknown -> "원본 ID: ${violation.scope.rawId}"
            null -> e.rawMessage.ifBlank { "응답에 상세 정보 없음" }
        }
        appendLog("실패: $head$modelHint — $tail")
        _state.update { it.copy(statusText = "실패: $head") }
    }

    private fun refreshChunkPreview(path: String) {
        previewJob?.cancel()
        if (path.isBlank()) {
            _state.update { it.copy(chunkPreview = emptyList()) }
            return
        }
        val file = File(path)
        if (!file.exists() || !file.isFile) {
            _state.update { it.copy(chunkPreview = emptyList()) }
            return
        }
        previewJob = scope.launch {
            try {
                val text = withContext(Dispatchers.IO) { Files.readString(file.toPath()) }
                val chunks = pipeline.previewChunks(text)
                _state.update { it.copy(chunkPreview = chunks) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                log.warn(e) { "chunk preview failed for $path" }
                _state.update { it.copy(chunkPreview = emptyList()) }
            }
        }
    }

    private fun appendLog(line: String) {
        _state.update { it.copy(logs = it.logs + line) }
    }

    private fun resetTiming() {
        firstChunkStartNanos = 0L
        lastChunkStartNanos = 0L
        avgChunkNanos = 0L
        sampledChunks = 0
    }

    private fun initialState(): UiState = UiState(
        apiKey = settings.apiKey,
        inputPath = "",
        outputPath = "",
        model = settings.lastModel,
        voice = settings.lastVoice,
        style = settings.lastStyle,
    )

    private fun deriveOutputPath(input: String): String {
        if (input.isBlank()) return ""
        val file = File(input)
        return file.resolveSibling(file.nameWithoutExtension + ".wav").absolutePath
    }

    private fun ensureUniquePath(original: Path): Path {
        if (!Files.exists(original)) return original
        val parent = original.parent ?: return original
        val name = original.fileName.toString()
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "")
        var counter = 1
        while (true) {
            val candidate = parent.resolve(buildString {
                append(base).append('-').append(counter)
                if (ext.isNotEmpty()) append('.').append(ext)
            })
            if (!Files.exists(candidate)) return candidate
            counter++
        }
    }

    private fun formatEta(seconds: Long): String =
        if (seconds < 60) "${seconds}초"
        else "${seconds / 60}분 ${seconds % 60}초"

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000L
    }
}
