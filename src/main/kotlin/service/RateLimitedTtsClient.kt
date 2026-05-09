package service

import core.RateLimiter
import domain.AudioBuffer
import domain.TtsModel
import domain.Voice

class RateLimitedTtsClient(
    private val delegate: TtsClient,
    private val limiterFor: (TtsModel) -> RateLimiter,
    private val onWaitTick: suspend (model: TtsModel, totalSeconds: Long, remainingSeconds: Long) -> Unit =
        { _, _, _ -> },
) : TtsClient {

    override suspend fun synthesize(
        text: String,
        apiKey: String,
        model: TtsModel,
        voice: Voice,
    ): AudioBuffer {
        val limiter = limiterFor(model)
        val waitMs = limiter.nanosToWait() / NANOS_PER_MILLI
        if (waitMs > NOTIFY_THRESHOLD_MS) {
            Countdown.emitAndWait(totalMillis = waitMs) { total, remaining ->
                onWaitTick(model, total, remaining)
            }
        }
        limiter.acquire()
        return delegate.synthesize(text, apiKey, model, voice)
    }

    private companion object {
        const val NOTIFY_THRESHOLD_MS = 500L
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
