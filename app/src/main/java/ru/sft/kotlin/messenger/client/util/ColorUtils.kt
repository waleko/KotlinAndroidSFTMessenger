package ru.sft.kotlin.messenger.client.util

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import androidx.core.content.ContextCompat
import ru.sft.kotlin.messenger.client.R
import java.math.BigInteger
import java.security.MessageDigest
import kotlin.math.absoluteValue


val colors = listOf(
    R.color.palette_siena,
    R.color.palette_crimson,
    R.color.palette_fuchsia,
    R.color.palette_lilac,
    R.color.palette_mauve,
    R.color.palette_purple,
    R.color.palette_blue,
    R.color.palette_skyblue,
    R.color.palette_green
)

fun md5(s: String): BigInteger {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(s.toByteArray()))
}

fun colorBySeed(seed: String) : Int {
    val i = md5(seed).toInt().absoluteValue % colors.size
    return colors[i]
}

fun darkenColor(color: Int, gray: Int = 30): Int {
    val red = (Color.red(color) + gray) / 2
    val green = (Color.green(color) + gray) / 2
    val blue = (Color.blue(color) + gray) / 2

    return Color.rgb(red, green, blue)
}

fun String.getColoredString(context: Context, colorId: Int, isActive: Boolean): Spannable {
    var color = ContextCompat.getColor(context, colorId)
    if(!isActive)
        color = darkenColor(color)

    val spannable: Spannable = SpannableString(this)
    spannable.setSpan(
        ForegroundColorSpan(color),
        0,
        spannable.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    return spannable
}

fun String.getAutoColoredString(context: Context, seed: String, isActive: Boolean = true) : Spannable {
    val colorId = colorBySeed(seed)
    return getColoredString(context, colorId, isActive)
}

fun String.getAutoColoredString(context: Context, seed: Int, isActive: Boolean = true) : Spannable {
    return getAutoColoredString(context, seed.toString(), isActive)
}
