package service

import core.TextChunker
import core.WavEncoder
import domain.TtsError
import domain.TtsRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path

fun interface ProgressCallback {
    fun report(current: Int, total: Int, chunkChars: Int)

    companion object {
        val Noop = ProgressCallback { _, _, _ -> }
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
            onProgress.report(index + 1, chunks.size, chunk.length)
            val effective = applyStyle(chunk, request.styleInstruction)
            val audio = client.synthesize(effective, request.model, request.voice)
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
