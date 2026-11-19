package com.github.infinitescroller.ui.util

import com.github.infinitescroller.data.api.GithubException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Instant

class FormatTest {

    @Test
    fun `compact counts`() {
        assertEquals("0", Format.compactCount(0))
        assertEquals("999", Format.compactCount(999))
        assertEquals("1k", Format.compactCount(1_000))
        assertEquals("1.2k", Format.compactCount(1_249))
        assertEquals("12.3k", Format.compactCount(12_345))
        assertEquals("99.9k", Format.compactCount(99_999))
        assertEquals("123k", Format.compactCount(123_456))
        assertEquals("999k", Format.compactCount(999_999))
        assertEquals("1.2M", Format.compactCount(1_234_567))
    }

    @Test
    fun `relative times`() {
        val now = Instant.parse("2026-11-19T12:00:00Z")
        assertEquals("just now", Format.relativeTime("2026-11-19T11:59:30Z", now))
        assertEquals("5m ago", Format.relativeTime("2026-11-19T11:55:00Z", now))
        assertEquals("3h ago", Format.relativeTime("2026-11-19T09:00:00Z", now))
        assertEquals("2d ago", Format.relativeTime("2026-11-17T12:00:00Z", now))
        assertEquals("2w ago", Format.relativeTime("2026-11-05T12:00:00Z", now))
        assertEquals("4mo ago", Format.relativeTime("2026-07-19T12:00:00Z", now))
        assertEquals("3y ago", Format.relativeTime("2023-11-01T12:00:00Z", now))
        assertEquals("just now", Format.relativeTime("2026-11-19T12:05:00Z", now)) // clock skew
        assertNull(Format.relativeTime("not a date", now))
        assertNull(Format.relativeTime(null, now))
    }

    @Test
    fun `rate limit message counts down and suggests a token`() {
        val now = Instant.ofEpochSecond(1_000)
        val msg = GithubException.RateLimited(1_042).toErrorMessage(hasToken = false, now = now)
        assertTrue(msg.body, msg.body.contains("42s"))
        assertTrue(msg.body.contains("token"))

        val withToken = GithubException.RateLimited(1_150).toErrorMessage(hasToken = true, now = now)
        assertTrue(withToken.body, withToken.body.contains("3 min"))
        assertTrue(!withToken.body.contains("token"))
    }

    @Test
    fun `offline message for IO errors`() {
        assertEquals("You're offline", IOException().toErrorMessage(hasToken = false).title)
    }
}
