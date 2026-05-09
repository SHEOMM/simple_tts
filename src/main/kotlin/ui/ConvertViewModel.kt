package ui

import domain.QuotaScope
import domain.TtsError
import domain.TtsModel
import domain.TtsRequest
import domain.Voice
import infra.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import service.ChunkEvent
import service.ProgressCallback
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

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
        val isRunning: Boolean = false,
        val progress: Float = 0f,
        val statusText: String = "",
        val logs: List<String> = emptyList(),
        val lastOutput: Path? = null,
    ) {
        val canConvert: Boolean
            get() = !isRunning && apiKey.isNotBlank() && inputPath.isNotBlank() && outputPath.isNotBlank()
    }

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var job: Job? = null

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
            val nextOutput = if (it.outputPath.isBlank()) deriveOutputPath(path) else it.outputPath
            it.copy(inputPath = path, outputPath = nextOutput)
        }
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

        _state.update {
            it.copy(
                isRunning = true,
                progress = 0f,
                statusText = "준비 중...",
                logs = emptyList(),
                lastOutput = null,
            )
        }

        val callback = ProgressCallback { event -> handleEvent(event) }
        module.progressCallback = callback

        job = scope.launch {
            try {
                val text = Files.readString(input.toPath())
                val request = TtsRequest(
                    text = text,
                    apiKey = current.apiKey,
                    model = current.model,
                    voice = current.voice,
                    styleInstruction = current.style,
                )
                val outputPath = pipeline.synthesizeToFile(
                    request = request,
                    outputPath = File(current.outputPath).toPath(),
                    onProgress = callback,
                )
                val sizeKb = Files.size(outputPath) / 1024
                appendLog("완료: $outputPath (${sizeKb} KB)")
                _state.update { it.copy(progress = 1f, statusText = "완료", lastOutput = outputPath) }
            } catch (e: TtsError.RateLimited) {
                handleRateLimited(e)
            } catch (e: TtsError) {
                appendLog("실패: ${e.message}")
                _state.update { it.copy(statusText = "실패") }
            } catch (e: Throwable) {
                appendLog("예상치 못한 오류: ${e.message}")
                _state.update { it.copy(statusText = "실패") }
            } finally {
                module.progressCallback = ProgressCallback.Noop
                _state.update { it.copy(isRunning = false) }
            }
        }
    }

    private fun handleEvent(event: ChunkEvent) {
        when (event) {
            is ChunkEvent.Started -> {
                val fraction = event.current.toFloat() / event.total.toFloat()
                val message = "[${event.current}/${event.total}] 합성 중 (${event.chars}자)"
                _state.update { it.copy(progress = fraction, statusText = message) }
                appendLog(message)
            }
            is ChunkEvent.Waiting -> {
                val statusText = "${event.reason.displayName} · ${event.remainingSeconds}s 남음"
                _state.update { it.copy(statusText = statusText) }
                if (event.isFirstTick) {
                    val advice = event.reason.advice.takeIf { it.isNotBlank() }?.let { " — $it" }.orEmpty()
                    appendLog("${event.reason.displayName} · 약 ${event.totalSeconds}초 대기$advice")
                }
            }
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
                ?: "잠시 후 자동 재시도되지 않았습니다"
            is QuotaScope.Unknown -> "원본 ID: ${violation.scope.rawId}"
            null -> e.rawMessage.ifBlank { "응답에 상세 정보 없음" }
        }
        appendLog("실패: $head$modelHint — $tail")
        _state.update { it.copy(statusText = "실패: $head") }
    }

    private fun appendLog(line: String) {
        _state.update { it.copy(logs = it.logs + line) }
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
}
