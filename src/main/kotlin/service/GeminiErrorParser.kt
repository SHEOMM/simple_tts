package service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import domain.QuotaScope
import domain.QuotaViolation
import domain.TtsError

class GeminiErrorParser(private val mapper: ObjectMapper) {

    fun parseRateLimit(body: String): TtsError.RateLimited {
        val errorBody = runCatching { mapper.readValue<ErrorResponse>(body) }
            .map { it.error }
            .getOrNull()

        val violations = errorBody?.details
            ?.filterIsInstance<ErrorDetail.QuotaFailure>()
            ?.flatMap { it.violations }
            ?.map { it.toDomain() }
            ?: emptyList()

        val retryDelay = errorBody?.details
            ?.filterIsInstance<ErrorDetail.RetryInfo>()
            ?.firstOrNull()
            ?.retryDelay
            ?.let { ProtoDuration.parse(it) }

        return TtsError.RateLimited(
            violations = violations,
            retryDelay = retryDelay,
            rawMessage = errorBody?.message.orEmpty(),
        )
    }

    private fun QuotaViolationDto.toDomain(): QuotaViolation = QuotaViolation(
        scope = QuotaScope.fromQuotaId(quotaId),
        metric = quotaMetric,
        model = quotaDimensions["model"],
        rawQuotaId = quotaId,
    )
}
