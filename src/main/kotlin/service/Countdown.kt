package service

import kotlinx.coroutines.delay
import kotlin.math.max

internal object Countdown {

    suspend fun emitAndWait(
        totalMillis: Long,
        tickMillis: Long = DEFAULT_TICK_MS,
        emit: suspend (totalSeconds: Long, remainingSeconds: Long) -> Unit,
    ) {
        if (totalMillis <= 0) return
        val totalSeconds = (totalMillis + 999) / 1000
        var remainingMs = totalMillis

        while (remainingMs > 0) {
            val remainingSeconds = (remainingMs + 999) / 1000
            emit(totalSeconds, remainingSeconds)
            val sleep = minOf(remainingMs, tickMillis)
            delay(sleep)
            remainingMs = max(remainingMs - sleep, 0)
        }
    }

    private const val DEFAULT_TICK_MS = 1_000L
}
