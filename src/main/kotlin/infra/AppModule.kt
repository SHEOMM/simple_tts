package infra

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import core.ParagraphSentenceChunker
import core.TextChunker
import service.GeminiTtsClient
import service.TtsClient
import service.TtsPipeline
import java.net.http.HttpClient
import java.time.Duration

class AppModule(
    prefsNode: String = DEFAULT_PREFS_NODE,
    chunkSize: Int = ParagraphSentenceChunker.DEFAULT_MAX_CHARS,
) {
    private val store: PreferencesStore = JavaPrefsStore(prefsNode)
    val settings: AppSettings = AppSettings(store)

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    private val mapper = jacksonObjectMapper()

    private val ttsClient: TtsClient = GeminiTtsClient(
        httpClient = httpClient,
        mapper = mapper,
    )

    private val chunker: TextChunker = ParagraphSentenceChunker(maxChars = chunkSize)

    val pipeline: TtsPipeline = TtsPipeline(ttsClient, chunker)

    companion object {
        const val DEFAULT_PREFS_NODE = "dev.tts.gemini"
    }
}
