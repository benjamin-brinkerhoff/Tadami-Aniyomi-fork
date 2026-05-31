package eu.kanade.tachiyomi.data.suggestions

import eu.kanade.tachiyomi.data.suggestions.sources.AniListRecommendationSource
import eu.kanade.tachiyomi.data.suggestions.sources.MangaUpdatesSimilarSource
import eu.kanade.tachiyomi.data.suggestions.sources.MyAnimeListRecommendationSource
import eu.kanade.tachiyomi.data.suggestions.sources.RecommendationPagingSource
import eu.kanade.tachiyomi.data.suggestions.sources.SuggestionMediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import tachiyomi.core.common.util.system.logcat

class SuggestionCoordinator {

    fun createSources(mediaType: SuggestionMediaType): List<RecommendationPagingSource> =
        buildList {
            add(AniListRecommendationSource(mediaType))
            if (mediaType == SuggestionMediaType.ANIME) {
                add(MyAnimeListRecommendationSource(mediaType))
            }
            if (mediaType == SuggestionMediaType.MANGA) {
                add(MangaUpdatesSimilarSource(mediaType))
            }
        }

    /**
     * Fetch and aggregate suggestions from all applicable sources in parallel.
     *
     * Structured logcat events emitted:
     * - Seed candidates chosen
     * - Provider start/end (delegated to each source)
     * - Coordinator: aggregated result count and failed source count
     * - Cache hit vs miss is logged per-source
     *
     * Deduplication: by providerId (if available) or providerUrl.
     */
    suspend fun fetchSuggestions(
        seed: SuggestionSeed,
        limit: Int = 40,
    ): SuggestionFetchResult = coroutineScope {
        val sources = createSources(seed.mediaType)
        if (sources.isEmpty()) {
            logcat { "[Coordinator] No sources for mediaType=${seed.mediaType}" }
            return@coroutineScope SuggestionFetchResult(emptyList(), 0, 0)
        }

        val translatedTitle = eu.kanade.tachiyomi.data.suggestions.MultilingualQueryHelper.translate(seed.primaryTitle)
        val enrichedCandidates = buildList {
            addAll(seed.candidateTitles)
            if (translatedTitle != null) {
                add(translatedTitle)
            }
        }.distinct()
        val enrichedSeed = seed.copy(candidateTitles = enrichedCandidates)

        logcat {
            "[Coordinator] Fetching '${enrichedSeed.primaryTitle}' (${enrichedSeed.mediaType}) via ${sources.map {
                it.name
            }}" +
                " | candidates=${enrichedSeed.candidateTitles}"
        }

        val jobs = sources.map { source ->
            async(Dispatchers.IO) {
                try {
                    val result = source.fetchSuggestions(enrichedSeed)
                    Pair(result, false)
                } catch (e: Exception) {
                    logcat { "[Coordinator] ${source.name} FAILED: ${e.message}" }
                    Pair(emptyList<SuggestionItem>(), true)
                }
            }
        }

        val results = jobs.map { it.await() }
        val attemptedSources = sources.size
        val failedSources = results.count { it.second }
        val items = results.flatMap { it.first }
            .distinctBy { it.providerId ?: it.providerUrl }
            .take(limit) // Cap at requested limit

        val matchedBase = sources.any { it.matchedBase } || results.any { it.first.isNotEmpty() }

        logcat {
            "[Coordinator] Done '${enrichedSeed.primaryTitle}': ${items.size} items, " +
                "attempted=$attemptedSources, failed=$failedSources, matchedBase=$matchedBase"
        }

        SuggestionFetchResult(
            items = items,
            attemptedSources = attemptedSources,
            failedSources = failedSources,
            matchedBase = matchedBase,
        )
    }
}
