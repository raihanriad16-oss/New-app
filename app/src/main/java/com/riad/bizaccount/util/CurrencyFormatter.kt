package com.riad.bizaccount.util

import java.util.Locale

/**
 * Amounts are stored as Long "minor units" (poisha, 1/100 taka) throughout the app
 * to avoid floating-point rounding errors in financial totals.
 */
object CurrencyFormatter {

    fun toMajor(minor: Long): Double = minor / 100.0

    fun toMinor(major: Double): Long = Math.round(major * 100.0)

    /** Formats with Bangladeshi digit grouping (##,##,###) and a currency symbol, e.g. ৳ ১২,৩৪,৫৬৭.৫০ */
    fun format(minor: Long, symbol: String = "৳", useBanglaDigits: Boolean = true): String {
        val negative = minor < 0
        val abs = kotlin.math.abs(minor)
        val rupees = abs / 100
        val paisa = abs % 100

        val grouped = groupIndian(rupees.toString())
        var result = "$grouped.${paisa.toString().padStart(2, '0')}"
        if (useBanglaDigits) result = toBanglaDigits(result)
        val sign = if (negative) "-" else ""
        return "$sign$symbol $result"
    }

    /** Indian/Bangladeshi digit grouping: last 3 digits, then groups of 2. */
    private fun groupIndian(number: String): String {
        if (number.length <= 3) return number
        val last3 = number.substring(number.length - 3)
        var remaining = number.substring(0, number.length - 3)
        val groups = StringBuilder()
        while (remaining.length > 2) {
            groups.insert(0, "," + remaining.substring(remaining.length - 2))
            remaining = remaining.substring(0, remaining.length - 2)
        }
        groups.insert(0, remaining)
        return "$groups,$last3"
    }

    fun toBanglaDigits(input: String): String {
        val map = mapOf(
            '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
            '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
        )
        return input.map { map[it] ?: it }.joinToString("")
    }
}
