package eu.kanade.tachiyomi.data.suggestions

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SuggestionCacheTest {

    // Clear singleton cache state before each test via fresh calls
    private val dummyItem = SuggestionItem(
        title = "Test Anime",
        searchQuery = "test anime",
        thumbnailUrl = null,
        providerName = "AniList",
        providerUrl = "https://anilist.co/anime/1",
        providerId = "1",
        mediaType = eu.kanade.tachiyomi.data.suggestions.sources.SuggestionMediaType.ANIME,
    )

    @Test
    fun `makeKey with no candidates equals legacy key`() {
        val withEmpty = SuggestionCache.makeKey("AniList", "Naruto", "ANIME", emptyList())
        val legacy = SuggestionCache.makeKey("AniList", "Naruto", "ANIME")
        assertEquals(withEmpty, legacy)
    }

    @Test
    fun `makeKey differs when candidates differ`() {
        val keyA = SuggestionCache.makeKey("AniList", "Naruto", "ANIME", listOf("Naruto"))
        val keyB = SuggestionCache.makeKey("AniList", "Naruto", "ANIME", listOf("Naruto", "ナルト"))
        assertNotEquals(keyA, keyB, "Keys with different candidate sets should differ")
    }

    @Test
    fun `makeKey is stable (same candidates, same key)`() {
        val key1 = SuggestionCache.makeKey("AniList", "Naruto", "ANIME", listOf("Naruto", "ナルト"))
        val key2 = SuggestionCache.makeKey("AniList", "Naruto", "ANIME", listOf("ナルト", "Naruto"))
        assertEquals(key1, key2, "Key should be order-independent (sorted fingerprint)")
    }

    @Test
    fun `makeKey normalizes primary title casing`() {
        val upper = SuggestionCache.makeKey("AniList", "NARUTO", "ANIME")
        val lower = SuggestionCache.makeKey("AniList", "naruto", "ANIME")
        assertEquals(upper, lower)
    }

    @Test
    fun `get returns null on cache miss`() {
        val result = SuggestionCache.get("nonexistent-key-xyz-12345")
        assertNull(result)
    }

    @Test
    fun `put and get round-trip works`() {
        val key = "test-roundtrip-${System.nanoTime()}"
        SuggestionCache.put(key, listOf(dummyItem))
        val fetched = SuggestionCache.get(key)
        assertEquals(1, fetched?.size)
        assertEquals("Test Anime", fetched?.first()?.title)
    }
}
