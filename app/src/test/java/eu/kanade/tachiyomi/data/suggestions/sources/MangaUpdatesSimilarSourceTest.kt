package eu.kanade.tachiyomi.data.suggestions.sources

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MangaUpdatesSimilarSourceTest {

    @Test
    fun `name should be MangaUpdates`() {
        val source = MangaUpdatesSimilarSource(SuggestionMediaType.MANGA)
        assertEquals("MangaUpdates", source.name)
    }
}
