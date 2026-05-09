package service

import domain.AudioBuffer
import domain.TtsError
import domain.TtsModel
import domain.Voice
import java.time.Duration
import kotlin.random.Random

class RetryingTtsClient(
    private val delegate: TtsClient,
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val baseBackoff: Duration = DEFAULT_BASE_BACKOFF,
    private val onRetryTick: suspend (attempt: Int, totalSeconds: Long, remainingSeconds: Long, cause: TtsError.RateLimited) -> Unit =
        { _, _, _, _ -> },
) : TtsClient {

    override suspend fun synthesize(
        text: String,
        apiKey: String,
        model: TtsModel,
        voice: Voice,
    ): AudioBuffer {
        var attempt = 0
        while (true) {
            try {
                return delegate.synthesize(text, apiKey, model, voice)
            } catch (e: TtsError.RateLimited) {
                if (!e.isRetryable) throw e
                attempt++
                if (attempt >= maxAttempts) throw e
                val waitMs = (e.retryDelay ?: backoffFor(attempt)).toMillis()
                Countdown.emitAndWait(totalMillis = waitMs) { total, remaining ->
                    onRetryTick(attempt, total, remaining, e)
                }
            }
        }
    }

    private fun backoffFor(attempt: Int): Duration {
        val expSeconds = baseBackoff.seconds shl (attempt - 1)
        val jitterMs = Random.nextLong(JITTER_MAX_MS)
        return Duration.ofMillis(expSeconds * 1000 + jitterMs)
    }

    private companion object {
        const val DEFAULT_MAX_ATTEMPTS = 3
        val DEFAULT_BASE_BACKOFF: Duration = Duration.ofSeconds(5)
        const val JITTER_MAX_MS = 1000L
    }
}
