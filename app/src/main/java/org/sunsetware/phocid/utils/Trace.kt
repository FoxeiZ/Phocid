package org.sunsetware.phocid.utils

import android.os.Trace
import android.util.Log

inline fun <T> traceExecution(sectionName: String, param: String? = null, block: () -> T): T {
    val start = System.nanoTime()
    return try {
        Trace.beginSection(sectionName)
        block()
    } finally {
        Trace.endSection()
        val durationMs = (System.nanoTime() - start) / 1_000_000.0
        val label = if (param != null) "[$sectionName | $param]" else "[$sectionName]"
        Log.d("PerfDebug", "$label took $durationMs ms")
    }
}
