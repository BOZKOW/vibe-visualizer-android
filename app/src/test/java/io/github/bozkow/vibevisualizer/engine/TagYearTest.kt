package io.github.bozkow.vibevisualizer.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TagYearTest {
    @Test fun `the year tag first, then the date`() {
        assertEquals("2019", tagYear("2019", "20240101T000000.000Z"))
        assertEquals("2024", tagYear(null, "20240315T120000.000Z"))
        assertEquals("1998", tagYear("1998-05-02", null))
    }

    @Test fun `a format's zero date is no year`() {
        // an M4A without a date: the songs showed "· 1904"
        assertNull(tagYear(null, "19040101T000000.000Z"))
        assertNull(tagYear(null, "19700101T000000.000Z"))
        assertNull(tagYear(null, "1904-01-01"))
        assertNull(tagYear(null, null))
        assertNull(tagYear("", "  "))
    }
}
