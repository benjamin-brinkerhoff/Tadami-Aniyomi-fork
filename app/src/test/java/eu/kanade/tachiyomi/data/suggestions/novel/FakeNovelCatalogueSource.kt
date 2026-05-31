package eu.kanade.tachiyomi.data.suggestions.novel

import eu.kanade.tachiyomi.novelsource.NovelCatalogueSource
import eu.kanade.tachiyomi.novelsource.model.NovelFilterList
import eu.kanade.tachiyomi.novelsource.model.NovelsPage
import eu.kanade.tachiyomi.novelsource.model.SNovel
import rx.Observable

open class FakeNovelCatalogueSource(
    override val id: Long = 1L,
    override val name: String = "Fake Source",
    override val lang: String = "en",
    override val supportsLatest: Boolean = false,
    override val supportsRelatedNovels: Boolean = false,
) : NovelCatalogueSource {

    var relatedNovelsToReturn: List<SNovel> = emptyList()
    var searchNovelsToReturn: List<SNovel> = emptyList()
    var popularNovelsToReturn: List<SNovel> = emptyList()
    var getRelatedNovelsCalled = false
    var getSearchNovelsCalledWithQuery: String? = null
    var getPopularNovelsCalled = false

    override suspend fun getRelatedNovels(novel: SNovel): List<SNovel> {
        getRelatedNovelsCalled = true
        return relatedNovelsToReturn
    }

    override suspend fun getSearchNovels(page: Int, query: String, filters: NovelFilterList): NovelsPage {
        getSearchNovelsCalledWithQuery = query
        return NovelsPage(searchNovelsToReturn, false)
    }

    override suspend fun getPopularNovels(page: Int): NovelsPage {
        getPopularNovelsCalled = true
        return NovelsPage(popularNovelsToReturn, false)
    }

    override fun getFilterList(): NovelFilterList = NovelFilterList()

    override fun fetchPopularNovels(page: Int): Observable<NovelsPage> = throw UnsupportedOperationException()
    override fun fetchSearchNovels(
        page: Int,
        query: String,
        filters: NovelFilterList,
    ): Observable<NovelsPage> = throw UnsupportedOperationException()
    override fun fetchLatestUpdates(page: Int): Observable<NovelsPage> = throw UnsupportedOperationException()
}
