package com.riad.bizaccount.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

object DateUtils {

    private val banglaMonths = listOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    fun today(): LocalDate = LocalDate.now()

    fun timeOfDaySeconds(time: LocalTime = LocalTime.now()): Int =
        time.hour * 3600 + time.minute * 60 + time.second

    fun startOfMonth(date: LocalDate = today()): LocalDate = date.withDayOfMonth(1)

    fun endOfMonth(date: LocalDate = today()): LocalDate = YearMonth.from(date).atEndOfMonth()

    fun startOfYear(date: LocalDate = today()): LocalDate = date.withDayOfYear(1)

    fun endOfYear(date: LocalDate = today()): LocalDate = date.withMonth(12).withDayOfMonth(31)

    fun startOfWeek(date: LocalDate = today()): LocalDate = date.minusDays((date.dayOfWeek.value % 7).toLong())

    fun endOfWeek(date: LocalDate = today()): LocalDate = startOfWeek(date).plusDays(6)

    /** e.g. "১৫ জুলাই, ২০২৬" */
    fun formatBangla(date: LocalDate): String {
        val day = CurrencyFormatter.toBanglaDigits(date.dayOfMonth.toString())
        val month = banglaMonths[date.monthValue - 1]
        val year = CurrencyFormatter.toBanglaDigits(date.year.toString())
        return "$day $month, $year"
    }

    fun formatTimeBangla(seconds: Int): String {
        val h24 = seconds / 3600
        val m = (seconds % 3600) / 60
        val period = if (h24 < 12) "AM" else "PM"
        val h12 = if (h24 % 12 == 0) 12 else h24 % 12
        val hStr = CurrencyFormatter.toBanglaDigits(h12.toString())
        val mStr = CurrencyFormatter.toBanglaDigits(m.toString().padStart(2, '0'))
        return "$hStr:$mStr $period"
    }
}
