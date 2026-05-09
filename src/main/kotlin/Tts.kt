import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.Base64

const val MODEL_FLASH = "gemini-2.5-flash-preview-tts"
const val MODEL_PRO = "gemini-2.5-pro-preview-tts"
const val DEFAULT_VOICE = "Kore"
const val MAX_CHARS_PER_CHUNK = 3000
const val DEFAULT_SAMPLE_RATE = 24000

data class TtsRequest(
    val text: String,
    val apiKey: String,
    val model: String,
    val voice: String,
    val styleInstruction: String,
)

suspend fun synthesizeToWav(
    req: TtsRequest,
    outputPath: Path,
    onProgress: (current: Int, total: Int, chunkChars: Int) -> Unit,
): Path = withContext(Dispatchers.IO) {
    require(req.text.isNotBlank()) { "입력 텍스트가 비어 있습니다." }
    require(req.apiKey.isNotBlank()) { "API 키가 설정되지 않았습니다." }

    val chunks = chunkText(req.text, MAX_CHARS_PER_CHUNK)

    val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    val pcmAccumulator = ByteArrayOutputStream()
    var sampleRate = DEFAULT_SAMPLE_RATE

    chunks.forEachIndexed { i, chunk ->
        val effectiveText = if (req.styleInstruction.isBlank()) chunk
        else "${req.styleInstruction.trim()}\n\n$chunk"
        onProgress(i + 1, chunks.size, chunk.length)
        val (pcm, rate) = synthesize(client, effectiveText, req.apiKey, req.model, req.voice)
        sampleRate = rate
        pcmAccumulator.write(pcm)
    }

    val wav = wrapAsWav(pcmAccumulator.toByteArray(), sampleRate)
    Files.write(outputPath, wav)
    outputPath
}

internal fun chunkText(text: String, maxChars: Int): List<String> {
    val trimmed = text.trim()
    if (trimmed.length <= maxChars) return listOf(trimmed)

    val paragraphs = trimmed.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }
    val chunks = mutableListOf<String>()
    val current = StringBuilder()

    fun flush() {
        if (current.isNotEmpty()) {
            chunks += current.toString().trim()
            current.clear()
        }
    }

    for (p in paragraphs) {
        if (p.length > maxChars) {
            flush()
            chunks += splitBySentence(p, maxChars)
            continue
        }
        if (current.length + p.length + 2 > maxChars) flush()
        if (current.isNotEmpty()) current.append("\n\n")
        current.append(p)
    }
    flush()
    return chunks
}

private fun splitBySentence(text: String, maxChars: Int): List<String> {
    val sentences = Regex("(?<=[.!?。…]|다\\.)\\s+").split(text)
    val chunks = mutableListOf<String>()
    val current = StringBuilder()
    for (s in sentences) {
        if (s.length > maxChars) {
            if (current.isNotEmpty()) { chunks += current.toString(); current.clear() }
            s.chunked(maxChars).forEach { chunks += it }
            continue
        }
        if (current.length + s.length + 1 > maxChars) {
            chunks += current.toString()
            current.clear()
        }
        if (current.isNotEmpty()) current.append(' ')
        current.append(s)
    }
    if (current.isNotEmpty()) chunks += current.toString()
    return chunks
}

private fun synthesize(
    client: HttpClient,
    text: String,
    apiKey: String,
    model: String,
    voice: String,
): Pair<ByteArray, Int> {
    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
    val body = """{"contents":[{"parts":[{"text":${jsonEscape(text)}}]}],"generationConfig":{"responseModalities":["AUDIO"],"speechConfig":{"voiceConfig":{"prebuiltVoiceConfig":{"voiceName":"$voice"}}}}}"""

    val request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .timeout(Duration.ofMinutes(5))
        .POST(HttpRequest.BodyPublishers.ofString(body, Charsets.UTF_8))
        .build()

    val response = client.send(request, HttpResponse.BodyHandlers.ofString(Charsets.UTF_8))
    if (response.statusCode() != 200) {
        error("Gemini API 오류 ${response.statusCode()}: ${response.body()}")
    }
    val responseText = response.body()

    val dataMatch = Regex("\"data\"\\s*:\\s*\"([^\"]+)\"").find(responseText)
        ?: error("응답에서 오디오 데이터를 찾지 못했습니다. 본문 일부: ${responseText.take(500)}")
    val mimeMatch = Regex("\"mimeType\"\\s*:\\s*\"([^\"]+)\"").find(responseText)
    val sampleRate = mimeMatch?.groupValues?.get(1)
        ?.let { Regex("rate=(\\d+)").find(it)?.groupValues?.get(1)?.toInt() }
        ?: DEFAULT_SAMPLE_RATE

    val pcm = Base64.getDecoder().decode(dataMatch.groupValues[1])
    return pcm to sampleRate
}

private fun jsonEscape(s: String): String {
    val sb = StringBuilder(s.length + 2)
    sb.append('"')
    for (c in s) {
        when (c) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            '\b' -> sb.append("\\b")
            '\u000C' -> sb.append("\\f")
            else -> if (c.code < 0x20) sb.append("\\u%04x".format(c.code)) else sb.append(c)
        }
    }
    sb.append('"')
    return sb.toString()
}

private fun wrapAsWav(
    pcm: ByteArray,
    sampleRate: Int,
    channels: Int = 1,
    bitsPerSample: Int = 16,
): ByteArray {
    val byteRate = sampleRate * channels * bitsPerSample / 8
    val blockAlign = channels * bitsPerSample / 8
    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
    header.put("RIFF".toByteArray(Charsets.US_ASCII))
    header.putInt(36 + pcm.size)
    header.put("WAVE".toByteArray(Charsets.US_ASCII))
    header.put("fmt ".toByteArray(Charsets.US_ASCII))
    header.putInt(16)
    header.putShort(1.toShort())
    header.putShort(channels.toShort())
    header.putInt(sampleRate)
    header.putInt(byteRate)
    header.putShort(blockAlign.toShort())
    header.putShort(bitsPerSample.toShort())
    header.put("data".toByteArray(Charsets.US_ASCII))
    header.putInt(pcm.size)
    return header.array() + pcm
}
