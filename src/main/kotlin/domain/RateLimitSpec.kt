package domain

data class RateLimitSpec(val rpm: Int, val burst: Int = rpm) {
    init {
        require(rpm > 0) { "rpm must be > 0, got $rpm" }
        require(burst > 0) { "burst must be > 0, got $burst" }
    }
}
