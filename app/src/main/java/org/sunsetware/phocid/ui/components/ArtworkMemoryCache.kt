package org.sunsetware.phocid.ui.components

import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

object ArtworkMemoryCache {
    private const val TAG = "phocid.ArtworkMemoryCache"
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    val cacheSizeKB = maxMemory / 8
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private val inFlight = mutableMapOf<String, Deferred<Bitmap?>>()
    private val cache =
        object : LruCache<String, Bitmap>(cacheSizeKB) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }

    suspend fun getOrPut(requestKey: String, load: suspend () -> Bitmap?): Bitmap? {
        val existing = get(requestKey)
        if (existing != null) {
            Log.i(TAG, "artwork cache hit for: $requestKey")
            return existing
        }
        Log.i(TAG, "artwork cache miss for: $requestKey")

        val deferred =
            synchronized(lock) {
                inFlight[requestKey]?.also {
                    Log.i(TAG, "artwork cache wait for in flight: $requestKey")
                }
                    ?: coroutineScope
                        .async {
                            try {
                                val loaded = load() ?: return@async null
                                putLoaded(requestKey, loaded)
                            } finally {
                                synchronized(lock) { inFlight.remove(requestKey) }
                            }
                        }
                        .also {
                            inFlight[requestKey] = it
                            Log.i(TAG, "artwork cache started load for: $requestKey")
                        }
            }

        return deferred.await()
    }

    fun get(requestKey: String): Bitmap? {
        return cache[requestKey]
    }

    fun clear() {
        synchronized(lock) {
            val inFlightSize = inFlight.size
            val entrySize = cache.size()
            inFlight.clear()
            cache.evictAll()
            Log.i(TAG, "cleared in memory, entries=$entrySize, inFlight=$inFlightSize")
        }
    }

    fun trimToSize(maxSizeKB: Int) {
        cache.trimToSize(maxSizeKB.coerceAtLeast(0))
    }

    private fun putLoaded(requestKey: String, loaded: Bitmap): Bitmap {
        cache.put(requestKey, loaded)
        return loaded
    }
}
