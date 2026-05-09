package domain

import java.time.Duration

sealed class TtsError(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    class MissingApiKey : TtsError("API 키가 설정되지 않았습니다. https://aistudio.google.com/apikey 에서 발급하세요.")

    class EmptyText : TtsError("입력 텍스트가 비어 있습니다.")

    class InputFileNotFound(val path: String) : TtsError("입력 파일이 없습니다: $path")

    class ApiError(val status: Int, val body: String) :
        TtsError("Gemini API 오류 $status: ${body.take(500)}")

    class RateLimited(
        val violations: List<QuotaViolation>,
        val retryDelay: Duration?,
        val rawMessage: String,
    ) : TtsError(buildMessage(violations, retryDelay, rawMessage)) {

        val mostSevere: QuotaViolation? = violations.minByOrNull { severityRank(it.scope) }

        val isRetryable: Boolean = violations.isEmpty() || violations.all { it.scope.isRetryable }

        private companion object {
            fun severityRank(scope: QuotaScope): Int = when (scope) {
                QuotaScope.RequestsPerDay -> 0
                QuotaScope.InputTokensPerMinute -> 1
                QuotaScope.RequestsPerMinute -> 2
                is QuotaScope.Unknown -> 3
            }

            fun buildMessage(
                violations: List<QuotaViolation>,
                retryDelay: Duration?,
                rawMessage: String,
            ): String {
                val severe = violations.minByOrNull { severityRank(it.scope) }
                val head = severe?.displayName?.let { "$it 초과" } ?: "호출 한도 초과"
                val tail = retryDelay?.let { " · ${it.toSeconds()}초 후 가능" }.orEmpty()
                return if (rawMessage.isBlank()) "$head$tail" else "$head$tail — $rawMessage"
            }
        }
    }

    class MalformedResponse(val snippet: String, cause: Throwable? = null) :
        TtsError("응답 파싱 실패. 본문 일부: $snippet", cause)
}
