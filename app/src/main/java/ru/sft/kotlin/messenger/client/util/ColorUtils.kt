package ru.sft.kotlin.messenger.client.util

import ru.sft.kotlin.messenger.client.R
import java.math.BigInteger
import java.security.MessageDigest
import kotlin.math.absoluteValue

val colors = listOf(
    R.color.palette_amber,
    R.color.palette_tangerine,
    R.color.palette_siena,
    R.color.palette_crimson,
    R.color.palette_fuchsia,
    R.color.palette_lilac,
    R.color.palette_mauve,
    R.color.palette_purple,
    R.color.palette_blue,
    R.color.palette_skyblue,
    R.color.palette_green,
    R.color.palette_lemonyellow
)

fun md5(s: String): BigInteger {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(s.toByteArray()))
}

fun colorByUserId(userId: String) : Int {
    val i = md5(userId).toInt().absoluteValue % colors.size
    return colors[i]
}

