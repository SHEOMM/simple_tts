package service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import domain.AudioBuffer
import domain.TtsError
import domain.TtsModel
import domain.Voice
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.time.Duration
import java.util.Base64

private val log = KotlinLogging.logger {}

class GeminiTtsClient(
    private val httpClient: HttpClient = defaultHttpClient(),
    private val mapper: ObjectMapper = jacksonObjectMapper(),
    private val errorParser: GeminiErrorParser = GeminiErrorParser(mapper),
    private val perRequestTimeout: Duration = DEFAULT_REQUEST_TIMEOUT,
) : TtsClient {

    override suspend fun synthesize(
        text: String,
        apiKey: String,
        model: TtsModel,
        voice: Voice,
    ): AudioBuffer = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw TtsError.MissingApiKey()
        val response = httpClient.send(
            buildRequest(text, model, voice, apiKey),
            HttpResponse.BodyHandlers.ofString(Charsets.UTF_8),
        )
        when (response.statusCode()) {
            200 -> parseAudio(response.body())
            429 -> throw errorParser.parseRateLimit(response.body())
            else -> throw TtsError.ApiError(response.statusCode(), response.body())
        }
    }

    private fun buildRequest(
        text: String,
        model: TtsModel,
        voice: Voice,
        apiKey: String,
    ): HttpRequest {
        val payload = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text)))),
            generationConfig = GenerationConfig(
                responseModalities = listOf("AUDIO"),
                speechConfig = SpeechConfig(VoiceConfig(PrebuiltVoiceConfig(voice.id))),
            ),
        )
        return HttpRequest.newBuilder()
            .uri(URI.create("$BASE_URL/${model.id}:generateContent?key=$apiKey"))
            .header("Content-Type", "application/json")
            .timeout(perRequestTimeout)
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload), Charsets.UTF_8))
            .build()
    }

    private fun parseAudio(body: String): AudioBuffer {
        val response = try {
            mapper.readValue<GenerateContentResponse>(body)
        } catch (e: Throwable) {
            throw TtsError.MalformedResponse(buildDiagnostic(body, e), e).also { dumpRawBody(body) }
        }
        val inlineData = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.inlineData
            ?: throw TtsError.MalformedResponse(buildDiagnostic(body, null)).also { dumpRawBody(body) }
        val pcm = Base64.getDecoder().decode(inlineData.data)
        return AudioBuffer(pcm, sampleRateOf(inlineData.mimeType))
    }

    private fun buildDiagnostic(body: String, cause: Throwable?): String {
        val causeMsg = cause?.message?.lineSequence()?.firstOrNull()?.take(200)
        val parts = listOfNotNull(
            "size=${body.length}",
            causeMsg?.let { "cause=$it" },
            "head=${body.take(200)}",
            "tail=${body.takeLast(200)}",
        )
        return parts.joinToString(" | ")
    }

    private fun dumpRawBody(body: String) {
        runCatching {
            val tmp = Files.createTempFile("gemini-tts-failed-", ".json")
            Files.writeString(tmp, body)
            log.warn { "failed body dumped to: $tmp (${body.length} chars)" }
        }
    }

    private fun sampleRateOf(mimeType: String): Int =
        SAMPLE_RATE_REGEX.find(mimeType)?.groupValues?.get(1)?.toIntOrNull() ?: DEFAULT_SAMPLE_RATE

    companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        const val DEFAULT_SAMPLE_RATE = 24000
        val DEFAULT_REQUEST_TIMEOUT: Duration = Duration.ofMinutes(5)
        private val SAMPLE_RATE_REGEX = Regex("rate=(\\d+)")

        fun defaultHttpClient(): HttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build()
    }
}
