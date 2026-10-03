package br.com.filafacil.logistics.util

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object EventCounter {

    private val counters = ConcurrentHashMap<String, AtomicLong>()

    fun increment(event: String) {
        counters.computeIfAbsent(event) { AtomicLong() }.incrementAndGet()
    }

    fun get(event: String): Long = counters[event]?.get() ?: 0
}
