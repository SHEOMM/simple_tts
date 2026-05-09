package infra

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import core.ParagraphSentenceChunker
import core.RateLimiter
import core.TextChunker
import domain.TtsModel
import service.Bucket4jRateLimiter
import service.ChunkEvent
import service.GeminiTtsClient
import service.ProgressCallback
import service.RateLimitedTtsClient
import service.RetryingTtsClient
import service.TtsClient
import service.TtsPipeline
import service.WaitReason
import java.net.http.HttpClient
import java.time.Duration

class AppModule(
    prefsNode: String = DEFAULT_PREFS_NODE,
    chunkSize: Int = ParagraphSentenceChunker.DEFAULT_MAX_CHARS,
) {
    private val store: PreferencesStore = JavaPrefsStore(prefsNode)
    val settings: AppSettings = AppSettings(store)

    var progressCallback: ProgressCallback = ProgressCallback.Noop

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    private val mapper = jacksonObjectMapper()

    private val limiters: Map<TtsModel, RateLimiter> = TtsModel.all.associateWith {
        Bucket4jRateLimiter.forSpec(it.rateLimitSpec)
    }

    private val baseClient: TtsClient = GeminiTtsClient(httpClient, mapper)

    private val rateLimitedClient: TtsClient = RateLimitedTtsClient(
        delegate = baseClient,
        limiterFor = { limiters.getValue(it) },
        onWaitTick = { model, total, remaining ->
            progressCallback.report(
                ChunkEvent.Waiting(
                    current = 0,
                    total = 0,
                    totalSeconds = total,
                    remainingSeconds = remaining,
                    reason = WaitReason.RpmThrottle(model.displayName),
                )
            )
        },
    )

    private val retryingClient: TtsClient = RetryingTtsClient(
        delegate = rateLimitedClient,
        onRetryTick = { attempt, total, remaining, cause ->
            progressCallback.report(
                ChunkEvent.Waiting(
                    current = 0,
                    total = 0,
                    totalSeconds = total,
                    remainingSeconds = remaining,
                    reason = WaitReason.Retry(attempt, cause.mostSevere),
                )
            )
        },
    )

    private val chunker: TextChunker = ParagraphSentenceChunker(maxChars = chunkSize)

    val pipeline: TtsPipeline = TtsPipeline(retryingClient, chunker)

    companion object {
        const val DEFAULT_PREFS_NODE = "dev.tts.gemini"
    }
}
