package tachiyomi.data.source.manga

import androidx.paging.PagingState
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.items.chapter.model.NoChaptersException
import tachiyomi.domain.source.manga.repository.SourcePagingSourceType

class SourceSearchPagingSource(
    source: CatalogueSource,
    val query: String,
    val filters: FilterList,
) :
    SourcePagingSource(
        source,
    ) {
    override suspend fun requestNextPage(currentPage: Int): MangasPage {
        return source.getSearchManga(currentPage, query, filters)
    }
}

class SourcePopularPagingSource(source: CatalogueSource) : SourcePagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): MangasPage {
        return source.getPopularManga(currentPage)
    }
}

class SourceLatestPagingSource(source: CatalogueSource) : SourcePagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): MangasPage {
        return source.getLatestUpdates(currentPage)
    }
}

abstract class SourcePagingSource(
    protected val source: CatalogueSource,
) : SourcePagingSourceType() {

    abstract suspend fun requestNextPage(currentPage: Int): MangasPage

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, SManga> {
        val page = params.key ?: 1

        return try {
            withIOContext {
                val mangasPage = requestNextPage(page.toInt())
                when {
                    mangasPage.mangas.isNotEmpty() -> {
                        LoadResult.Page(
                            data = mangasPage.mangas,
                            prevKey = null,
                            nextKey = if (mangasPage.hasNextPage) page + 1 else null,
                        )
                    }
                    page == 1L -> throw NoChaptersException()
                    else -> {
                        // Some sources incorrectly report that another page exists,
                        // then return an empty trailing page. Treat that as the end
                        // of pagination instead of surfacing a false "no results" error.
                        LoadResult.Page(
                            data = emptyList(),
                            prevKey = null,
                            nextKey = null,
                        )
                    }
                }
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Long, SManga>): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey ?: anchorPage?.nextKey
        }
    }
}
