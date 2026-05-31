package eu.kanade.tachiyomi.data.suggestions

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SuggestionTitleResolverTest {

    // ─── resolveCandidates ────────────────────────────────────────────────

    @Test
    fun `resolveCandidates should extract and normalize titles`() {
        val title = "  Solo Leveling  "
        val description = "Some info here.\nOriginal: Only I Level Up\nOther info."
        val metadataAlternativeTitles = listOf("Настоящее поднятие уровня в одиночку", "Solo Leveling")

        val candidates = SuggestionTitleResolver.resolveCandidates(
            title = title,
            description = description,
            metadataAlternativeTitles = metadataAlternativeTitles,
        )

        assertTrue(candidates.isNotEmpty())
        assertEquals("Solo Leveling", candidates[0])
        assertTrue(candidates.contains("Only I Level Up"))
    }

    @Test
    fun `resolveCandidates should deduplicate candidates`() {
        val candidates = SuggestionTitleResolver.resolveCandidates(
            title = "Solo Leveling",
            description = null,
            metadataAlternativeTitles = listOf("Solo Leveling", "Solo Leveling"),
        )
        assertEquals(1, candidates.count { it == "Solo Leveling" })
    }

    @Test
    fun `resolveCandidates should keep Cyrillic title as-is`() {
        val cyrillicTitle = "Атака Титанов"
        val candidates = SuggestionTitleResolver.resolveCandidates(
            title = cyrillicTitle,
            description = null,
            metadataAlternativeTitles = listOf("Shingeki no Kyojin"),
        )
        assertTrue(candidates.contains(cyrillicTitle), "Cyrillic title must be preserved")
        assertTrue(candidates.contains("Shingeki no Kyojin"), "Latin alias must be present")
    }

    @Test
    fun `resolveCandidates should handle null description gracefully`() {
        val candidates = SuggestionTitleResolver.resolveCandidates(
            title = "Some Title",
            description = null,
            metadataAlternativeTitles = emptyList(),
        )
        assertTrue(candidates.isNotEmpty())
        assertTrue(candidates.contains("Some Title"))
    }

    @Test
    fun `resolveCandidates filters blank candidates`() {
        val candidates = SuggestionTitleResolver.resolveCandidates(
            title = "Title",
            description = null,
            metadataAlternativeTitles = listOf("  ", "", "Valid"),
        )
        assertFalse(candidates.any { it.isBlank() }, "Blank candidates must be filtered out")
        assertTrue(candidates.contains("Valid"))
    }

    // ─── scoreMatch ───────────────────────────────────────────────────────

    @Test
    fun `scoreMatch should grade matches properly`() {
        // Exact match (case-insensitive)
        assertEquals(100, SuggestionTitleResolver.scoreMatch("Solo Leveling", "solo leveling"))

        // Prefix match
        assertEquals(75, SuggestionTitleResolver.scoreMatch("Solo Leveling Season 2", "Solo Leveling"))

        // Contains match
        assertEquals(50, SuggestionTitleResolver.scoreMatch("The Solo Leveling Story", "Leveling"))

        // Token overlap – must be > 0 and < 50
        val score = SuggestionTitleResolver.scoreMatch("Solo Leveling Side Story", "Solo Leveling Novel")
        assertTrue(score > 0 && score < 50)
    }

    @Test
    fun `scoreMatch should handle Cyrillic comparison`() {
        // Exact Cyrillic match
        assertEquals(100, SuggestionTitleResolver.scoreMatch("Атака Титанов", "атака титанов"))

        // Latin vs Cyrillic should not score high (different scripts)
        val score = SuggestionTitleResolver.scoreMatch("Shingeki no Kyojin", "атака титанов")
        assertTrue(score < 75)
    }

    // ─── selectBestQueryForProvider ───────────────────────────────────────

    @Test
    fun `selectBestQueryForProvider prefers Latin over Cyrillic`() {
        val candidates = listOf("Атака Титанов", "Shingeki no Kyojin", "Attack on Titan")
        val best = SuggestionTitleResolver.selectBestQueryForProvider(candidates, "Атака Титанов")
        assertNotEquals("Атака Титанов", best, "Cyrillic-only candidate should not be preferred when Latin exists")
    }

    @Test
    fun `selectBestQueryForProvider falls back to first when all Cyrillic`() {
        val candidates = listOf("Атака Титанов", "Наруто")
        val best = SuggestionTitleResolver.selectBestQueryForProvider(candidates, "Атака Титанов")
        assertEquals("Атака Титанов", best)
    }

    @Test
    fun `selectBestQueryForProvider returns rawTitle for empty candidates`() {
        val best = SuggestionTitleResolver.selectBestQueryForProvider(emptyList(), "Fallback Title")
        assertEquals("Fallback Title", best)
    }

    // ─── cleanTitle ───────────────────────────────────────────────────────

    @Test
    fun `cleanTitle should strip volume and chapter markers only when followed by numbers`() {
        // Words followed by numbers should be stripped
        assertEquals("solo leveling", SuggestionTitleResolver.cleanTitle("Solo Leveling Vol 1"))
        assertEquals("solo leveling", SuggestionTitleResolver.cleanTitle("Solo Leveling Season 2"))
        assertEquals("solo leveling", SuggestionTitleResolver.cleanTitle("Solo Leveling Part 3"))

        // Common semantic words NOT followed by numbers should be preserved!
        assertEquals("book of shadows", SuggestionTitleResolver.cleanTitle("Book of Shadows"))
        assertEquals("tome of fire", SuggestionTitleResolver.cleanTitle("Tome of Fire"))
        assertEquals("part of me", SuggestionTitleResolver.cleanTitle("Part of Me"))
    }
}
