package com.riad.bizaccount

import com.riad.bizaccount.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {

    @Test
    fun `startOfMonth and endOfMonth bound the given date's month`() {
        val date = LocalDate.of(2026, 7, 15)
        assertEquals(LocalDate.of(2026, 7, 1), DateUtils.startOfMonth(date))
        assertEquals(LocalDate.of(2026, 7, 31), DateUtils.endOfMonth(date))
    }

    @Test
    fun `startOfYear and endOfYear bound the given date's year`() {
        val date = LocalDate.of(2026, 3, 10)
        assertEquals(LocalDate.of(2026, 1, 1), DateUtils.startOfYear(date))
        assertEquals(LocalDate.of(2026, 12, 31), DateUtils.endOfYear(date))
    }

    @Test
    fun `startOfWeek and endOfWeek span exactly seven days`() {
        val date = LocalDate.of(2026, 7, 15) // Wednesday
        val start = DateUtils.startOfWeek(date)
        val end = DateUtils.endOfWeek(date)
        assertEquals(6, java.time.temporal.ChronoUnit.DAYS.between(start, end))
    }

    @Test
    fun `formatBangla converts digits to Bangla numerals`() {
        val date = LocalDate.of(2026, 1, 5)
        assertEquals("৫ জানুয়ারি, ২০২৬", DateUtils.formatBangla(date))
    }
}
