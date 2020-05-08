package ru.sft.kotlin.messenger.client.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * Formats the date to desired pattern (e.g. pattern = "HH:mm" => "12:34")
 */
fun Date.format(pattern: String): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(this)
}

/**
 * Formats the date relative to today's date
 */
fun formatTimeString(date: Date) : String {
    // Get calendar of message created date
    val prevTime = Calendar.getInstance()
    prevTime.time = date

    // Get current calendar
    val curTime = Calendar.getInstance()

    // True if calendars have equal fields, else false
    val haveSame = {field: Int -> curTime.get(field) == prevTime.get(field)}

    // If it's today only HH:mm (e.g. 12:34)
    if (haveSame(Calendar.YEAR) && haveSame(Calendar.DAY_OF_YEAR)) {
        return date.format("HH:mm")
    }

    // If it's the same year only dd MMM HH:MM (e.g. 3 April 12:34)
    if (haveSame(Calendar.YEAR)) {
        return date.format("dd MMM HH:MM")
    }

    // Else we return the full date
    return date.format("dd MMM yyyy HH:MM")
}
