package eu.kanade.presentation.entries.components

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entries.anime.model.AnimeCover
import tachiyomi.domain.entries.manga.model.MangaCover
import tachiyomi.domain.entries.novel.model.NovelCover

class ItemCoverTest {

    @Test
    fun `novel cover model resolves to novel cover`() {
        val cover = NovelCover(
            novelId = 1L,
            sourceId = 2L,
            isNovelFavorite = false,
            url = "https://example.org/cover.jpg",
            lastModified = 0L,
        )

        val model = resolveCoverModel(cover)

        model shouldBe cover
    }

    @Test
    fun `novel cover model with blank url passes through to fetcher`() {
        val cover = NovelCover(
            novelId = 1L,
            sourceId = 2L,
            isNovelFavorite = false,
            url = "   ",
            lastModified = 0L,
        )
        val model = resolveCoverModel(cover)

        // Cover data classes are now passed through even with blank URLs —
        // the fetcher (NovelCoverFetcher) decides whether to handle them.
        model shouldBe cover
    }

    @Test
    fun `anime and manga cover models with blank urls pass through to fetcher`() {
        val animeCover = AnimeCover(
            animeId = 1L,
            sourceId = 2L,
            isAnimeFavorite = false,
            url = " ",
            lastModified = 0L,
        )
        resolveCoverModel(animeCover) shouldBe animeCover

        val mangaCover = MangaCover(
            mangaId = 1L,
            sourceId = 2L,
            isMangaFavorite = false,
            url = "",
            lastModified = 0L,
        )
        resolveCoverModel(mangaCover) shouldBe mangaCover
    }

    @Test
    fun `blank string cover model resolves to null`() {
        resolveCoverModel("   ") shouldBe null
    }

    @Test
    fun `cover model with null url is loadable so fetcher can handle it`() {
        val cover = NovelCover(
            novelId = 1L,
            sourceId = 2L,
            isNovelFavorite = false,
            url = null,
            lastModified = 0L,
        )
        val model = resolveCoverModel(cover)

        // Cover data classes are always loadable — the fetcher decides.
        isLoadableCoverData(model) shouldBe true
    }
}
