package eu.kanade.tachiyomi.data.suggestions.novel

import eu.kanade.domain.entries.novel.model.toSNovel
import eu.kanade.tachiyomi.data.suggestions.SuggestionCache
import eu.kanade.tachiyomi.data.suggestions.SuggestionItem
import eu.kanade.tachiyomi.data.suggestions.SuggestionSeed
import eu.kanade.tachiyomi.data.suggestions.sources.SuggestionMediaType
import eu.kanade.tachiyomi.novelsource.NovelCatalogueSource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entries.novel.model.Novel

class NovelRelatedSuggestionCoordinator {

    suspend fun fetchRelatedSuggestions(
        novel: Novel,
        source: NovelCatalogueSource,
        seed: SuggestionSeed,
        maxResults: Int = 20,
    ): NovelFallbackOutcome {
        val cacheKey = SuggestionCache.makeKey(
            "related:${source.id}:limit:$maxResults",
            novel.url,
            "NOVEL",
            seed.candidateTitles,
        )

        if (!source.supportsRelatedNovels) {
            logcat { "[NovelRelatedSuggestionCoordinator] Source ${source.name} does not support related novels" }
            SuggestionCache.put(cacheKey, emptyList())
            return NovelFallbackOutcome.Empty(NovelFallbackReason.NO_RELATED_SUPPORT)
        }

        val cached = SuggestionCache.get(cacheKey)
        if (cached != null) {
            logcat { "[NovelRelatedSuggestionCoordinator] Cache HIT for key $cacheKey, count=${cached.size}" }
            return if (cached.isEmpty()) {
                NovelFallbackOutcome.Empty(NovelFallbackReason.RELATED_EMPTY)
            } else {
                NovelFallbackOutcome.Success(cached)
            }
        }

        logcat {
            "[NovelRelatedSuggestionCoordinator] Cache MISS. Fetching related novels from source for '${novel.title}'"
        }
        return try {
            val relatedNovels = source.getRelatedNovels(novel.toSNovel())
            if (relatedNovels.isEmpty()) {
                logcat { "[NovelRelatedSuggestionCoordinator] Source returned empty related list for '${novel.title}'" }
                SuggestionCache.put(cacheKey, emptyList())
                NovelFallbackOutcome.Empty(NovelFallbackReason.RELATED_EMPTY)
            } else {
                val items = relatedNovels
                    .distinctBy { it.url }
                    .map { sNovel ->
                        SuggestionItem(
                            title = sNovel.title,
                            searchQuery = sNovel.title,
                            thumbnailUrl = sNovel.thumbnail_url,
                            providerName = source.name,
                            providerUrl = sNovel.url,
                            providerId = "${source.id}:${sNovel.url}",
                            mediaType = SuggestionMediaType.NOVEL,
                        )
                    }
                    .take(maxResults)

                logcat { "[NovelRelatedSuggestionCoordinator] Successfully loaded ${items.size} related novels" }
                SuggestionCache.put(cacheKey, items)
                NovelFallbackOutcome.Success(items)
            }
        } catch (e: Exception) {
            logcat { "[NovelRelatedSuggestionCoordinator] Failed to fetch related novels: ${e.message}" }
            NovelFallbackOutcome.Empty(NovelFallbackReason.RELATED_EMPTY)
        }
    }
}
