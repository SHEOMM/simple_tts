package domain

sealed class QuotaScope(val isRetryable: Boolean) {
    data object RequestsPerMinute : QuotaScope(isRetryable = true)
    data object InputTokensPerMinute : QuotaScope(isRetryable = true)
    data object RequestsPerDay : QuotaScope(isRetryable = false)
    data class Unknown(val rawId: String) : QuotaScope(isRetryable = true)

    companion object {
        fun fromQuotaId(quotaId: String): QuotaScope = when {
            quotaId.contains("PerDay", ignoreCase = true) -> RequestsPerDay
            quotaId.contains("InputTokens", ignoreCase = true) -> InputTokensPerMinute
            quotaId.contains("PerMinute", ignoreCase = true) -> RequestsPerMinute
            else -> Unknown(quotaId)
        }
    }
}
