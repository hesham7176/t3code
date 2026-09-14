package com.t3code.explorer.ui.util

import java.text.DateFormat
import java.util.Date

fun formatDate(value: Long): String = if (value <= 0L) "—" else DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(value))
fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d:%02d".format(seconds / 3600, (seconds % 3600) / 60, seconds % 60)
}
