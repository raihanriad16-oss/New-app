package com.riad.bizaccount

import com.riad.bizaccount.util.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun `toMinor converts major taka to poisha correctly`() {
        assertEquals(150000L, CurrencyFormatter.toMinor(1500.0))
        assertEquals(150050L, CurrencyFormatter.toMinor(1500.50))
    }

    @Test
    fun `toMajor converts poisha back to taka`() {
        assertEquals(1500.0, CurrencyFormatter.toMajor(150000L), 0.001)
    }

    @Test
    fun `format groups digits using Indian style grouping`() {
        // 1234567.50 taka -> ৳ ১২,৩৪,৫৬৭.৫০
        val formatted = CurrencyFormatter.format(123456750L, useBanglaDigits = false)
        assertEquals("৳ 12,34,567.50", formatted)
    }

    @Test
    fun `format handles negative amounts with a leading minus`() {
        val formatted = CurrencyFormatter.format(-50000L, useBanglaDigits = false)
        assertEquals("-৳ 500.00", formatted)
    }

    @Test
    fun `toBanglaDigits converts ascii digits`() {
        assertEquals("১২৩", CurrencyFormatter.toBanglaDigits("123"))
    }
}
