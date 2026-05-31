package eu.kanade.tachiyomi.data.suggestions.anime

import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.data.suggestions.SuggestionSeed
import eu.kanade.tachiyomi.data.suggestions.sources.SuggestionMediaType
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.entries.anime.model.Anime

class AnimeSearchFallbackEngineTest {

    @Test
    fun `fetchSearchFallback returns Success and ranks matching titles above threshold`() = runTest {
        val anime = Anime.create().copy(id = 123L, title = "Solo Leveling", url = "/solo-search-success")
        val source = FakeAnimeCatalogueSource()
        source.searchAnimesToReturn = listOf(
            SAnime.create().apply {
                title = "Solo Leveling Side Stories"
                url = "/solo-1"
            },
            SAnime.create().apply {
                title = "Solo Leveling Official"
                url = "/solo-official"
            },
            SAnime.create().apply {
                title = "Completely Unrelated Book"
                url = "/unrelated"
            },
        )

        val seed = SuggestionSeed(
            mediaType = SuggestionMediaType.ANIME,
            primaryTitle = "Solo Leveling",
            candidateTitles = listOf("Solo Leveling"),
            description = "",
        )

        val engine = AnimeSearchFallbackEngine()
        val outcome = engine.fetchSearchFallback(anime, source, seed)

        assertTrue(outcome is AnimeFallbackOutcome.Success)
        val success = outcome as AnimeFallbackOutcome.Success
        assertEquals(2, success.items.size)
        assertTrue(success.items.any { it.title == "Solo Leveling Side Stories" })
        assertTrue(success.items.any { it.title == "Solo Leveling Official" })
    }

    @Test
    fun `fetchSearchFallback filters out franchise duplicates`() = runTest {
        val anime = Anime.create().copy(id = 123L, title = "Solo Leveling Vol 1", url = "/solo-1")
        val source = FakeAnimeCatalogueSource()
        source.searchAnimesToReturn = listOf(
            SAnime.create().apply {
                title = "Solo Leveling Vol 2"
                url = "/solo-2"
            },
            SAnime.create().apply {
                title = "Solo Leveling Season 2"
                url = "/solo-season-2"
            },
        )

        val seed = SuggestionSeed(
            mediaType = SuggestionMediaType.ANIME,
            primaryTitle = "Solo Leveling Vol 1",
            candidateTitles = listOf("Solo Leveling Vol 1"),
            description = "",
        )

        val engine = AnimeSearchFallbackEngine()
        val outcome = engine.fetchSearchFallback(anime, source, seed)

        // Volume 2 and Season 2 should be filtered out by franchise duplicates filter
        assertTrue(outcome is AnimeFallbackOutcome.Empty)
    }

    @Test
    fun `fetchSearchFallback handles Author search and filters correctly`() = runTest {
        val anime = Anime.create().copy(
            id = 123L,
            title = "Solo Leveling",
            author = "Chugong",
            url = "/solo-search-author",
        )
        val source = FakeAnimeCatalogueSource()
        source.searchAnimesToReturn = listOf(
            SAnime.create().apply {
                title = "Overgeared"
                url = "/overgeared"
            },
        )

        val seed = SuggestionSeed(
            mediaType = SuggestionMediaType.ANIME,
            primaryTitle = "Solo Leveling",
            candidateTitles = listOf("Solo Leveling"),
            description = "",
        )

        val engine = AnimeSearchFallbackEngine()
        val outcome = engine.fetchSearchFallback(anime, source, seed)

        // Searching by author "Chugong" returned "Overgeared", which should be accepted with author baseline score
        assertTrue(outcome is AnimeFallbackOutcome.Success)
        val success = outcome as AnimeFallbackOutcome.Success
        assertEquals(1, success.items.size)
        assertEquals("Overgeared", success.items.first().title)
    }

    @Test
    fun `fetchSearchFallback handles Cyrillic author and genre search correctly`() = runTest {
        val anime = Anime.create().copy(
            id = 123L,
            title = "Магическая битва",
            author = "Гэгэ Акутами",
            genre = listOf("Сёнен", "Фэнтези"),
            url = "/jujutsu-ru",
        )
        val source = FakeAnimeCatalogueSource()
        source.searchAnimesToReturn = listOf(
            SAnime.create().apply {
                title = "Волейбол"
                url = "/haikyu-ru"
            },
        )

        val seed = SuggestionSeed(
            mediaType = SuggestionMediaType.ANIME,
            primaryTitle = "Магическая битва",
            candidateTitles = listOf("Магическая битва", "Jujutsu Kaisen"),
            description = "",
        )

        val engine = AnimeSearchFallbackEngine()
        val outcome = engine.fetchSearchFallback(anime, source, seed)

        assertTrue(outcome is AnimeFallbackOutcome.Success)
        val success = outcome as AnimeFallbackOutcome.Success
        assertEquals(1, success.items.size)
        assertEquals("Волейбол", success.items.first().title)
    }
}
