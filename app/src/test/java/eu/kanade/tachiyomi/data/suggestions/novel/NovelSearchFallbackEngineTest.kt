package eu.kanade.tachiyomi.data.suggestions.novel

import eu.kanade.tachiyomi.data.suggestions.SuggestionSeed
import eu.kanade.tachiyomi.data.suggestions.sources.SuggestionMediaType
import eu.kanade.tachiyomi.novelsource.model.SNovel
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.entries.novel.model.Novel

class NovelSearchFallbackEngineTest {

    @Test
    fun `fetchSearchFallback returns SEARCH_EMPTY when no results match similarity threshold`() = runTest {
        val novel = Novel.create().copy(id = 123L, title = "Solo Leveling", url = "/solo-search-empty")
        val source = FakeNovelCatalogueSource()
        source.searchNovelsToReturn = listOf(
            SNovel.create().apply {
                title = "Completely Unrelated Book"
                url = "/unrelated"
            },
        )

        val seed = SuggestionSeed(
            mediaType = SuggestionMediaType.NOVEL,
            primaryTitle = "Solo Leveling",
            candidateTitles = listOf("Solo Leveling"),
            description = "",
        )

        val engine = NovelSearchFallbackEngine()
        val outcome = engine.fetchSearchFallback(novel, source, seed)

        assertTrue(outcome is NovelFallbackOutcome.Empty)
        assertEquals(NovelFallbackReason.SEARCH_EMPTY, (outcome as NovelFallbackOutcome.Empty).reason)
    }

    @Test
    fun `fetchSearchFallback returns Success and ranks matching titles above threshold`() = runTest {
        val novel = Novel.create().copy(id = 123L, title = "Solo Leveling", url = "/solo-search-success")
        val source = FakeNovelCatalogueSource()
        source.searchNovelsToReturn = listOf(
            SNovel.create().apply {
                title = "Solo Leveling Side Stories"
                url = "/solo-1"
            },
            SNovel.create().apply {
                title = "Solo Leveling Official"
                url = "/solo-official"
            },
            SNovel.create().apply {
                title = "Completely Unrelated Book"
                url = "/unrelated"
            },
        )

        val seed = SuggestionSeed(
            mediaType = SuggestionMediaType.NOVEL,
            primaryTitle = "Solo Leveling",
            candidateTitles = listOf("Solo Leveling"),
            description = "",
        )

        val engine = NovelSearchFallbackEngine()
        val outcome = engine.fetchSearchFallback(novel, source, seed)

        assertTrue(outcome is NovelFallbackOutcome.Success)
        val success = outcome as NovelFallbackOutcome.Success
        assertEquals(2, success.items.size)
        assertTrue(success.items.any { it.title == "Solo Leveling Side Stories" })
        assertTrue(success.items.any { it.title == "Solo Leveling Official" })
    }

    @Test
    fun `fetchSearchFallback handles Cyrillic titles correctly`() = runTest {
        val novel = Novel.create().copy(id = 123L, title = "Атака Титанов", url = "/titan-search")
        val source = FakeNovelCatalogueSource()
        source.searchNovelsToReturn = listOf(
            SNovel.create().apply {
                title = "Атака Титанов Побочная история"
                url = "/titan-ru"
            },
        )

        val seed = SuggestionSeed(
            mediaType = SuggestionMediaType.NOVEL,
            primaryTitle = "Атака Титанов",
            candidateTitles = listOf("Атака Титанов", "Attack on Titan"),
            description = "",
        )

        val engine = NovelSearchFallbackEngine()
        val outcome = engine.fetchSearchFallback(novel, source, seed)

        assertTrue(outcome is NovelFallbackOutcome.Success)
        val success = outcome as NovelFallbackOutcome.Success
        assertEquals(1, success.items.size)
        assertEquals("Атака Титанов Побочная история", success.items.first().title)
    }

    @Test
    fun `fetchSearchFallback backfills from popular novels when search results are empty`() = runTest {
        val novel = Novel.create().copy(id = 123L, title = "Solo Leveling", url = "/solo-backfill")
        val source = FakeNovelCatalogueSource()
        source.popularNovelsToReturn = List(5) { i ->
            SNovel.create().apply {
                title = "Catalog Novel $i"
                url = "/catalog-$i"
                thumbnail_url = "http://thumb/catalog-$i"
            }
        }
        val seed = SuggestionSeed(
            mediaType = SuggestionMediaType.NOVEL,
            primaryTitle = "Solo Leveling",
            candidateTitles = listOf("Solo Leveling"),
            description = "",
        )

        val engine = NovelSearchFallbackEngine()
        val outcome = engine.fetchSearchFallback(novel, source, seed, maxResults = 4)

        assertTrue(outcome is NovelFallbackOutcome.Success)
        val success = outcome as NovelFallbackOutcome.Success
        assertEquals(4, success.items.size)
        assertEquals("Catalog Novel 0", success.items.first().title)
        assertTrue(source.getPopularNovelsCalled)
    }

    @Test
    fun `fetchSearchFallback does not reuse smaller cached result for larger request`() = runTest {
        val novel = Novel.create().copy(id = 123L, title = "Solo Leveling", url = "/solo-search-cache-limit")
        val source = FakeNovelCatalogueSource()
        source.popularNovelsToReturn = List(5) { i ->
            SNovel.create().apply {
                title = "Catalog Cache Novel $i"
                url = "/catalog-cache-$i"
            }
        }
        val seed = SuggestionSeed(
            mediaType = SuggestionMediaType.NOVEL,
            primaryTitle = "Solo Leveling",
            candidateTitles = listOf("Solo Leveling"),
            description = "",
        )

        val engine = NovelSearchFallbackEngine()
        val carouselOutcome = engine.fetchSearchFallback(novel, source, seed, maxResults = 2)
        val fullOutcome = engine.fetchSearchFallback(novel, source, seed, maxResults = 4)

        assertEquals(2, (carouselOutcome as NovelFallbackOutcome.Success).items.size)
        assertEquals(4, (fullOutcome as NovelFallbackOutcome.Success).items.size)
    }
}
