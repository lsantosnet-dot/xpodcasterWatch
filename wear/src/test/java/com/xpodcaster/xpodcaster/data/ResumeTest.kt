package com.xpodcaster.xpodcaster.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResumeTest {
    private val items = listOf("a", "b", "c").map { NewsItem(id = it, title = it) }
    private val now = 10 * 60_000L

    @Test
    fun nothingSaved() = assertNull(Resume.resolve(null, items, now))

    @Test
    fun findsItemById() {
        val p = ResumePoint("b", ResumeMode.READ, position = 3, savedAt = now)
        assertEquals(ResumeTarget(1, ResumeMode.READ, 3), Resume.resolve(p, items, now))
    }

    @Test
    fun goneFromNewList() {
        val p = ResumePoint("z", ResumeMode.LISTEN, savedAt = now)
        assertNull(Resume.resolve(p, items, now))
    }

    @Test
    fun finishedGoesToNext() {
        val p = ResumePoint("a", ResumeMode.LISTEN, position = 5, finished = true, savedAt = now)
        assertEquals(ResumeTarget(1, ResumeMode.LISTEN, 0), Resume.resolve(p, items, now))
    }

    @Test
    fun finishedLastHasNothing() {
        val p = ResumePoint("c", ResumeMode.READ, finished = true, savedAt = now)
        assertNull(Resume.resolve(p, items, now))
    }

    @Test
    fun listeningRewindsAfterLongPause() {
        val recent = ResumePoint("a", ResumeMode.LISTEN, position = 4, savedAt = now - 30_000)
        assertEquals(4, Resume.resolve(recent, items, now)?.position)
        val old = recent.copy(savedAt = now - Resume.LONG_PAUSE_MS - 1)
        assertEquals(3, Resume.resolve(old, items, now)?.position)
        assertEquals(0, Resume.resolve(old.copy(position = 0), items, now)?.position)
    }

    @Test
    fun readingNeverRewinds() {
        val p = ResumePoint("a", ResumeMode.READ, position = 2, savedAt = 0)
        assertEquals(2, Resume.resolve(p, items, now)?.position)
    }
}
