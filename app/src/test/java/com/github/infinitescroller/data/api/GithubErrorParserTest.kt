package com.github.infinitescroller.data.api

import com.github.infinitescroller.testing.testMoshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GithubErrorParserTest {

    private val parser = GithubErrorParser(testMoshi)

    private fun parse(code: Int, body: String? = null, headers: Map<String, String> = emptyMap()) =
        parser.parse(code, { name -> headers.entries.firstOrNull { it.key.equals(name, true) }?.value }, body, nowEpochSeconds = 500)

    @Test
    fun `401 is Unauthorized`() {
        assertTrue(parse(401, """{"message":"Bad credentials"}""") is GithubException.Unauthorized)
    }

    @Test
    fun `secondary rate limit uses retry-after relative to now`() {
        val e = parse(403, """{"message":"You have exceeded a secondary rate limit"}""", mapOf("Retry-After" to "30"))
        assertEquals(530L, (e as GithubException.RateLimited).retryAtEpochSeconds)
    }

    @Test
    fun `429 with reset header is RateLimited`() {
        val e = parse(429, "", mapOf("X-RateLimit-Remaining" to "0", "X-RateLimit-Reset" to "900"))
        assertEquals(900L, (e as GithubException.RateLimited).retryAtEpochSeconds)
    }

    @Test
    fun `403 that is not a rate limit stays a plain HTTP error`() {
        val e = parse(403, """{"message":"Resource protected by organization SAML enforcement"}""", mapOf("X-RateLimit-Remaining" to "20"))
        assertEquals(403, (e as GithubException.Http).code)
        assertEquals("Resource protected by organization SAML enforcement", e.detail)
    }

    @Test
    fun `422 prefers the specific error message`() {
        val e = parse(422, """{"message":"Validation Failed","errors":[{"message":"Cannot search for that"}]}""")
        assertEquals("Cannot search for that", (e as GithubException.InvalidQuery).detail)
    }

    @Test
    fun `unparseable body falls back to a generic message`() {
        val e = parse(502, "<html>Bad gateway</html>")
        assertEquals("Unexpected response from GitHub", (e as GithubException.Http).detail)
    }
}
