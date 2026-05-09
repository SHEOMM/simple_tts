package service

import java.time.Duration

internal object ProtoDuration {
    private val PATTERN = Regex("""^(\d+(?:\.\d+)?)s$""")

    fun parse(input: String): Duration? {
        val match = PATTERN.matchEntire(input.trim()) ?: return null
        val seconds = match.groupValues[1].toDoubleOrNull() ?: return null
        return Duration.ofMillis((seconds * 1000).toLong())
    }
}
