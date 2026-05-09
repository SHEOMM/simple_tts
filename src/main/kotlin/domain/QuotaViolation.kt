package domain

data class QuotaViolation(
    val scope: QuotaScope,
    val metric: String,
    val model: String?,
    val rawQuotaId: String,
) {
    val displayName: String
        get() = when (scope) {
            QuotaScope.RequestsPerMinute -> "분당 호출 한도(RPM)"
            QuotaScope.InputTokensPerMinute -> "분당 토큰 한도(TPM)"
            QuotaScope.RequestsPerDay -> "일일 호출 한도(RPD)"
            is QuotaScope.Unknown -> "한도(${scope.rawId})"
        }

    val advice: String
        get() = when (scope) {
            QuotaScope.RequestsPerDay -> "내일 다시 시도하거나 결제를 활성화하세요"
            QuotaScope.InputTokensPerMinute -> "더 짧은 청크로 재시도하거나 잠시 후 다시 시도하세요"
            QuotaScope.RequestsPerMinute -> "잠시 후 자동 재시도됩니다"
            is QuotaScope.Unknown -> "잠시 후 다시 시도하세요"
        }
}
