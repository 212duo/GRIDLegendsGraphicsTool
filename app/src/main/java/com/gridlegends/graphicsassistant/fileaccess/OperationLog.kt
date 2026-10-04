package com.gridlegends.graphicsassistant.fileaccess

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * In-app troubleshooting log shown on the home screen. File access now runs on background
 * threads, so every access is synchronized and readers get a snapshot.
 */
internal object OperationLog {
    private const val MAX_ENTRIES = 200
    private val entries = ArrayDeque<String>()

    @Synchronized
    fun add(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        entries.addLast("[$time] $message")
        while (entries.size > MAX_ENTRIES) entries.removeFirst()
        Log.d("GLGraphics", message)
    }

    @Synchronized
    fun snapshot(): List<String> = entries.toList()
}
