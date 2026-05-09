package core

interface RateLimiter {
    suspend fun acquire()
    fun nanosToWait(): Long
}
