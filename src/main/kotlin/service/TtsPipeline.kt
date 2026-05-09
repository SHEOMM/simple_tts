package service

import core.TextChunker
import core.WavEncoder
import domain.QuotaViolation
import domain.TtsError
import domain.TtsRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path

sealed interface ChunkEvent {
    data class Started(val current: Int, val total: Int, val chars: Int) : ChunkEvent

    data class Waiting(
        val current: Int,
        val total: Int,
        val totalSeconds: Long,
        val remainingSeconds: Long,
        val reason: WaitReason,
    ) : ChunkEvent {
        val isFirstTick: Boolean get() = remainingSeconds == totalSeconds
    }
}

sealed class WaitReason(val displayName: String, val advice: String = "") {
    data class RpmThrottle(val modelDisplay: String) : WaitReason(
        displayName = "RPM 한도 페이싱 ($modelDisplay)",
        advice = "분당 호출 한도 도달 — 다음 토큰 충전까지 자동 대기",
    )
    data class Retry(val attempt: Int, val violation: QuotaViolation?) : WaitReason(
        displayName = if (violation != null) "${violation.displayName} 재시도 #$attempt" else "재시도 #$attempt",
        advice = violation?.advice.orEmpty(),
    )
}

fun interface ProgressCallback {
    fun report(event: ChunkEvent)

    companion object {
        val Noop = ProgressCallback {}
    }
}

class TtsPipeline(
    private val client: TtsClient,
    private val chunker: TextChunker,
    private val encoder: WavEncoder = WavEncoder,
) {

    suspend fun synthesizeToFile(
        request: TtsRequest,
        outputPath: Path,
        onProgress: ProgressCallback = ProgressCallback.Noop,
    ): Path {
        val chunks = chunker.chunk(request.text).also {
            if (it.isEmpty()) throw TtsError.EmptyText()
        }

        val pcmAccumulator = ByteArrayOutputStream()
        var sampleRate = GeminiTtsClient.DEFAULT_SAMPLE_RATE

        chunks.forEachIndexed { index, chunk ->
            onProgress.report(ChunkEvent.Started(index + 1, chunks.size, chunk.length))
            val effective = applyStyle(chunk, request.styleInstruction)
            val audio = client.synthesize(effective, request.apiKey, request.model, request.voice)
            pcmAccumulator.write(audio.pcm)
            sampleRate = audio.sampleRate
        }

        val wav = encoder.encode(pcmAccumulator.toByteArray(), sampleRate)
        return withContext(Dispatchers.IO) {
            Files.write(outputPath, wav)
            outputPath
        }
    }

    private fun applyStyle(chunk: String, style: String): String =
        if (style.isBlank()) chunk else "${style.trim()}\n\n$chunk"
}
