package com.xpodcaster.xpodcaster.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotifySettingsTest {
    @Test
    fun quietAcrossMidnight() {
        val s = NotifySettings(quietStart = 22, quietEnd = 8)
        assertTrue(s.isQuiet(22))
        assertTrue(s.isQuiet(23))
        assertTrue(s.isQuiet(0))
        assertTrue(s.isQuiet(7))
        assertFalse(s.isQuiet(8))
        assertFalse(s.isQuiet(21))
    }

    @Test
    fun quietSameDay() {
        val s = NotifySettings(quietStart = 13, quietEnd = 15)
        assertTrue(s.isQuiet(13))
        assertTrue(s.isQuiet(14))
        assertFalse(s.isQuiet(15))
        assertFalse(s.isQuiet(12))
    }

    @Test
    fun sameHourMeansNoQuiet() {
        val s = NotifySettings(quietStart = 8, quietEnd = 8)
        (0..23).forEach { assertFalse(s.isQuiet(it)) }
    }

    @Test
    fun labels() {
        assertEquals("15 min", NotifySettings.intervalLabel(15))
        assertEquals("1 h", NotifySettings.intervalLabel(60))
        assertEquals("1 h 30 min", NotifySettings.intervalLabel(90))
    }
}

class NextToReadTest {
    private val items = listOf("a", "b", "c").map { NewsItem(id = it, title = it) }

    @Test
    fun nothingSavedStartsAtFirst() =
        assertEquals(ResumeTarget(0, ResumeMode.READ, 0), Resume.nextToRead(null, items))

    @Test
    fun noItems() = assertNull(Resume.nextToRead(null, emptyList()))

    @Test
    fun finishedGoesToNext() {
        val p = ResumePoint("a", ResumeMode.READ, position = 4, finished = true)
        assertEquals(ResumeTarget(1, ResumeMode.READ, 0), Resume.nextToRead(p, items))
    }

    @Test
    fun midReadingKeepsParagraph() {
        val p = ResumePoint("b", ResumeMode.READ, position = 2)
        assertEquals(ResumeTarget(1, ResumeMode.READ, 2), Resume.nextToRead(p, items))
    }

    @Test
    fun midListeningOpensFromTop() {
        val p = ResumePoint("b", ResumeMode.LISTEN, position = 7)
        assertEquals(ResumeTarget(1, ResumeMode.READ, 0), Resume.nextToRead(p, items))
    }

    @Test
    fun lastFinishedMeansNothingLeft() =
        assertNull(Resume.nextToRead(ResumePoint("c", ResumeMode.READ, finished = true), items))

    @Test
    fun newListStartsAtFirst() =
        assertEquals(ResumeTarget(0, ResumeMode.READ, 0), Resume.nextToRead(ResumePoint("zz", ResumeMode.READ), items))
}
