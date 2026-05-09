package service

import core.RateLimiter
import domain.RateLimitSpec
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration

class Bucket4jRateLimiter(private val bucket: Bucket) : RateLimiter {

    override suspend fun acquire() {
        withContext(Dispatchers.IO) {
            bucket.asBlocking().consume(1)
        }
    }

    override fun nanosToWait(): Long =
        bucket.estimateAbilityToConsume(1).nanosToWaitForRefill

    companion object {
        fun forSpec(spec: RateLimitSpec): Bucket4jRateLimiter {
            val bandwidth = Bandwidth.builder()
                .capacity(spec.burst.toLong())
                .refillGreedy(spec.rpm.toLong(), Duration.ofMinutes(1))
                .build()
            return Bucket4jRateLimiter(Bucket.builder().addLimit(bandwidth).build())
        }
    }
}
