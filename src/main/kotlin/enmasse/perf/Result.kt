package enmasse.perf

import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class Result(val numMessages: Long, val duration: Long, val totalLatency: Long, val buckets: List<Bucket>, val minLatency: Long, val maxLatency: Long) {
    fun throughput(): Long {
        return numMessages / (duration / 1000)
    }

    fun averageLatency(): Long {
        return totalLatency / numMessages
    }

    fun percentile(p: Double): Long {
        var entriesToCount = (numMessages * p).toLong()
        for (bucket in buckets) {
            entriesToCount -= bucket.count()
            if (entriesToCount <= 0) {
                return bucket.totalLatency() / bucket.count()
            }
        }
        throw IllegalStateException("Was never able to count required entries with p = ${p}")
    }

}

val emptyResult: Result = Result(0, 0, 0, emptyList(), Long.MAX_VALUE, Long.MIN_VALUE)

interface ResultFormatter {
    fun asString(result: Result): String
}


fun mergeResults(a: Result, b: Result): Result {
    return Result(a.numMessages + b.numMessages,
            Math.max(a.duration, b.duration),
            a.totalLatency + b.totalLatency,
            mergeBuckets(a.buckets, b.buckets),
            Math.min(a.minLatency, b.minLatency),
            Math.max(a.maxLatency, b.maxLatency))
}

fun mergeBuckets(a: List<Bucket>, b: List<Bucket>): List<Bucket> {
    return if (a.size == 0) {
        b
    } else if (b.size == 0) {
        a
    } else {
        a.mapIndexed { i, bucket ->
            val copy = Bucket(bucket.range)
            copy.increment(bucket.totalLatency() + b.get(i).totalLatency(), bucket.count() + b.get(i).count())
            copy
        }
    }
}

class MetricRecorder(bucketStep: Long, numBuckets: Long) {
    @Volatile private var totalLatency = 0L
    @Volatile private var numMessages= 0L
    @Volatile private var minLatency = Long.MAX_VALUE
    @Volatile private var maxLatency = 0L
    private val buckets: List<Bucket>
    init {
        buckets = listOf(0.rangeTo(numBuckets - 1).map { i ->
            val start = i * bucketStep
            val end = start + bucketStep
            Bucket(start.rangeTo(end))
        }, listOf(Bucket((bucketStep * numBuckets).rangeTo(Long.MAX_VALUE)))).flatten()
    }

    fun record(latencyInNanos: Long) {
        val latency = TimeUnit.NANOSECONDS.toMicros(latencyInNanos)
        numMessages++
        totalLatency += latency

        if (latency < minLatency) {
            minLatency = latency
        }

        if (latency > maxLatency) {
            maxLatency = latency
        }

        for (bucket in buckets) {
            if (bucket.range.contains(latency)) {
                bucket.increment(latency)
                break
            }
        }
    }

    fun result(duration: Long): Result {
        return Result(numMessages, duration, totalLatency, buckets, minLatency, maxLatency)
    }
}

class Bucket(val range: LongRange)
{
    private var count = 0L
    private var totalLatency = 0L

    fun increment(latency: Long, inc: Long = 1L) {
        count += inc
        totalLatency += latency
    }

    fun count(): Long {
        return count
    }

    fun totalLatency(): Long {
        return totalLatency
    }

    override fun equals(other: Any?): Boolean {
        if (other is Bucket) {
            if (other.range == this.range) {
                return true
            }
        }
        return false
    }
}