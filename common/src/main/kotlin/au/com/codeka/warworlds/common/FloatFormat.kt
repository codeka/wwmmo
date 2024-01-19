package au.com.codeka.warworlds.common

fun Double.format(digits: Int) = "%.${digits}f".format(this)

fun Float.format(digits: Int) = "%.${digits}f".format(this)
