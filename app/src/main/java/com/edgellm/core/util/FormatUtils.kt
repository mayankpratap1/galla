package com.edgellm.core.util

import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

object FormatUtils {

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(
            bytes / 1024.0.pow(digitGroups.toDouble())
        ) + " " + units[digitGroups]
    }

    fun formatNumber(number: Int): String {
        return when {
            number >= 1_000_000 -> "${DecimalFormat("#.#").format(number / 1_000_000.0)}M"
            number >= 1_000 -> "${DecimalFormat("#.#").format(number / 1_000.0)}K"
            else -> number.toString()
        }
    }

    fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }

    fun formatSpeed(bytesPerSecond: Long): String {
        return "${formatFileSize(bytesPerSecond)}/s"
    }
}

fun Long.formatSize(): String = FormatUtils.formatFileSize(this)
fun Int.formatCount(): String = FormatUtils.formatNumber(this)