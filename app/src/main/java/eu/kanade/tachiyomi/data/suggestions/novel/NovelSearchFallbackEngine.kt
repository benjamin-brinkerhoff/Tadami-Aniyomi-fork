package eu.kanade.tachiyomi.data.suggestions.novel

import eu.kanade.tachiyomi.data.suggestions.SuggestionCache
import eu.kanade.tachiyomi.data.suggestions.SuggestionItem
import eu.kanade.tachiyomi.data.suggestions.SuggestionSeed
import eu.kanade.tachiyomi.data.suggestions.SuggestionTitleResolver
import eu.kanade.tachiyomi.data.suggestions.sources.SuggestionMediaType
import eu.kanade.tachiyomi.novelsource.NovelCatalogueSource
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entries.novel.model.Novel

class NovelSearchFallbackEngine {

    suspend fun fetchSearchFallback(
        novel: Novel,
        source: NovelCatalogueSource,
        seed: SuggestionSeed,
        maxResults: Int = 40,
        onProgress: ((List<SuggestionItem>) -> Unit)? = null,
    ): NovelFallbackOutcome {
        if (maxResults <= 0) {
            return NovelFallbackOutcome.Empty(NovelFallbackReason.SEARCH_EMPTY)
        }

        val cacheKey = SuggestionCache.makeKey(
            "search:${source.id}:limit:$maxResults",
            novel.url,
            "NOVEL",
            seed.candidateTitles,
        )
        val cached = SuggestionCache.get(cacheKey)
        if (cached != null) {
            logcat { "[NovelSearchFallbackEngine] Cache HIT for key $cacheKey, count=${cached.size}" }
            return if (cached.isEmpty()) {
                NovelFallbackOutcome.Empty(NovelFallbackReason.SEARCH_EMPTY)
            } else {
                NovelFallbackOutcome.Success(cached)
            }
        }

        logcat { "[NovelSearchFallbackEngine] Cache MISS. Running tiered search fallback for '${novel.title}'" }

        val rawAuthorParts = buildList {
            val author = novel.displayAuthor
            if (!author.isNullOrBlank()) {
                val garbage = setOf(
                    "null", "undefined", "unknown", "none", "no author", "n/a",
                    "нет", "неизвестен", "неизвестный", "неизвестно",
                )
                addAll(
                    author.split(Regex("[,;/&]"))
                        .map { it.trim() }
                        .filter { it.length >= 2 && it.lowercase() !in garbage },
                )
            }
        }.distinct()

        val authorParts = rawAuthorParts

        val rawGenreParts = buildList {
            val genres = novel.displayGenre
            if (!genres.isNullOrEmpty()) {
                addAll(genres.take(3).map { it.trim() }.filter { it.length >= 2 })
            }
        }.distinct()

        val genreParts = buildList {
            rawGenreParts.forEach { genre ->
                add(genre)
                addAll(eu.kanade.tachiyomi.data.suggestions.MultilingualQueryHelper.getGenreTranslations(genre))
            }
        }.distinct()

        val mainTitle = seed.primaryTitle
        val titlesToProcess = listOf(mainTitle)
        val isCyrillicEntry = eu.kanade.tachiyomi.data.suggestions.MultilingualQueryHelper.containsCyrillic(mainTitle)

        // Tier 1: Exact titles
        val tier1Queries = buildList {
            addAll(titlesToProcess)
            val desc = novel.displayDescription
            eu.kanade.domain.metadata.interactor.parseOriginalTitle(desc)?.let { add(it) }
            addAll(seed.candidateTitles)
        }.map { it.trim() }
            .filter { it.length >= 2 }
            .filter {
                !isCyrillicEntry ||
                    eu.kanade.tachiyomi.data.suggestions.MultilingualQueryHelper.containsCyrillic(it)
            }
            .distinct()

        // Tier 2: Relaxed title queries (e.g. remove volume/season suffixes, split by punctuation, or truncate long titles)
        val tier2Queries = buildList {
            titlesToProcess.forEach { title ->
                // 1. Split by common separators: :, -, (, [, comma, semicolon
                val separators = listOf(":", "-", "(", "[", ",", ";")
                separators.forEach { sep ->
                    val part = title.substringBefore(sep).trim()
                    if (part.isNotEmpty() && part != title && part.length >= 3) {
                        add(part)
                    }
                }

                // 2. Cleaned title (removes volumes, chapters, seasons)
                val cleaned = eu.kanade.tachiyomi.data.suggestions.SuggestionTitleResolver.cleanTitle(title)
                if (cleaned.isNotEmpty() && cleaned != title && cleaned.length >= 3) {
                    add(cleaned)
                }

                // 3. For long titles, truncate to first 4, 3, or 5 words to relax primitive search engines
                val words = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
                if (words.size > 4) {
                    val first4 = words.take(4).joinToString(" ")
                    add(first4)

                    val first3 = words.take(3).joinToString(" ")
                    add(first3)

                    val first5 = words.take(5).joinToString(" ")
                    add(first5)
                }
            }
        }.map { it.trim() }
            .filter { it.length >= 2 }
            .filter {
                !isCyrillicEntry ||
                    eu.kanade.tachiyomi.data.suggestions.MultilingualQueryHelper.containsCyrillic(it)
            }
            .distinct()

        // Tier 3: Author queries
        val tier3Queries = authorParts.map { it.trim() }.filter { it.length >= 2 }.distinct()

        // Tier 4: Genre queries
        val tier4Queries = genreParts.map { it.trim() }.filter { it.length >= 2 }.distinct()

        val queryTiers = listOf(
            Pair("Tier 1 (Exact Title)", tier1Queries),
            Pair("Tier 2 (Relaxed Title)", tier2Queries),
            Pair("Tier 3 (Author)", tier3Queries),
            Pair("Tier 4 (Genre)", tier4Queries),
        )

        val candidatesToScore = seed.candidateTitles.distinct()

        val uniqueResults = LinkedHashMap<String, SuggestionItem>() // key: providerUrl
        val filterList = source.getFilterList()
        var authorAdded = 0
        var genreAdded = 0
        val maxAuthor = 8
        val maxGenre = 8

        logcat {
            "[NovelSearchFallbackEngine] Starting suggestions search for '${novel.title}' (url: ${novel.url}). Candidates: ${seed.candidateTitles}, author: '${novel.displayAuthor}', genres: ${novel.displayGenre}"
        }

        for ((tierName, tierQueries) in queryTiers) {
            if (synchronized(uniqueResults) { uniqueResults.size >= maxResults }) {
                logcat {
                    "[NovelSearchFallbackEngine] Reached target results limit ($maxResults) before processing all tiers. Stopping early."
                }
                break
            }
            if (tierQueries.isEmpty()) continue
            logcat { "[NovelSearchFallbackEngine] Processing $tierName with queries: $tierQueries" }

            val staggerMs = when {
                tierName.startsWith("Tier 4") -> 3000L
                tierName.startsWith("Tier 3") -> 1500L
                else -> 500L
            }
            coroutineScope {
                tierQueries.forEachIndexed { index, query ->
                    launch {
                        delay(staggerMs * index)
                        if (synchronized(uniqueResults) { uniqueResults.size >= maxResults }) return@launch
                        try {
                            logcat { "[NovelSearchFallbackEngine] Searching for query: '$query'" }
                            val page = source.getSearchNovels(1, query, filterList)
                            if (page.novels.isEmpty()) {
                                logcat {
                                    "[NovelSearchFallbackEngine] Query '$query' returned 0 results from source '${source.name}'"
                                }
                                return@launch
                            } else {
                                logcat {
                                    "[NovelSearchFallbackEngine] Query '$query' returned ${page.novels.size} raw results from source '${source.name}'"
                                }
                            }

                            val isAuthorQuery = authorParts.any { it.equals(query, ignoreCase = true) }
                            val isGenreQuery = genreParts.any { it.equals(query, ignoreCase = true) }
                            val isTitleQuery = !isAuthorQuery && !isGenreQuery

                            val scoredItems = page.novels.mapNotNull { sNovel ->
                                if (sNovel.url == novel.url) {
                                    logcat { "[NovelSearchFallbackEngine] Excluding self reference: '${sNovel.title}'" }
                                    return@mapNotNull null
                                }

                                if (SuggestionTitleResolver.isFranchiseDuplicate(sNovel.title, novel.title)) {
                                    logcat {
                                        "[NovelSearchFallbackEngine] Excluding franchise duplicate: '${sNovel.title}' against '${novel.title}'"
                                    }
                                    return@mapNotNull null
                                }

                                val bestScore = candidatesToScore.maxOfOrNull { candidate ->
                                    SuggestionTitleResolver.scoreMatch(candidate, sNovel.title)
                                } ?: 0

                                val finalScore = when {
                                    bestScore >= 30 -> bestScore
                                    isTitleQuery -> 0
                                    isAuthorQuery -> {
                                        val overlapBonus = minOf(bestScore / 10, 10)
                                        40 + overlapBonus
                                    }
                                    isGenreQuery -> 30
                                    else -> 0
                                }

                                logcat {
                                    "[NovelSearchFallbackEngine] '${sNovel.title}' score=$finalScore " +
                                        "(bestScore=$bestScore, isTitleQuery=$isTitleQuery, isAuthorQuery=$isAuthorQuery, isGenreQuery=$isGenreQuery)"
                                }

                                if (finalScore >= 30) {
                                    val item = SuggestionItem(
                                        title = sNovel.title,
                                        searchQuery = sNovel.title,
                                        thumbnailUrl = sNovel.thumbnail_url,
                                        providerName = source.name,
                                        providerUrl = sNovel.url,
                                        providerId = "${source.id}:${sNovel.url}",
                                        mediaType = SuggestionMediaType.NOVEL,
                                    )
                                    Pair(item, finalScore)
                                } else {
                                    logcat {
                                        "[NovelSearchFallbackEngine] Rejecting '${sNovel.title}': score $finalScore below threshold (30)"
                                    }
                                    null
                                }
                            }

                            var addedAny = false
                            val currentProgress = synchronized(uniqueResults) {
                                if (isGenreQuery && genreAdded >= maxGenre) return@launch
                                if (isAuthorQuery && authorAdded >= maxAuthor) return@launch
                                scoredItems.sortedByDescending { it.second }.forEach { (item, _) ->
                                    if (!uniqueResults.containsKey(item.providerUrl) &&
                                        uniqueResults.size < maxResults
                                    ) {
                                        if ((isGenreQuery && genreAdded >= maxGenre) ||
                                            (isAuthorQuery && authorAdded >= maxAuthor)
                                        ) {
                                            return@forEach
                                        }
                                        uniqueResults[item.providerUrl] = item
                                        addedAny = true
                                        if (isGenreQuery) genreAdded++
                                        if (isAuthorQuery) authorAdded++
                                    }
                                }
                                if (addedAny) {
                                    uniqueResults.values.toList()
                                } else {
                                    null
                                }
                            }
                            if (currentProgress != null) {
                                onProgress?.invoke(currentProgress)
                            }
                        } catch (e: Exception) {
                            logcat { "[NovelSearchFallbackEngine] Search failed for query '$query': ${e.message}" }
                        }
                    }
                }
            }
        }

        if (synchronized(uniqueResults) { uniqueResults.size < maxResults }) {
            backfillFromPopularNovels(
                source = source,
                novel = novel,
                maxResults = maxResults,
                uniqueResults = uniqueResults,
                onProgress = onProgress,
            )
        }

        val items = uniqueResults.values.toList()
        if (items.isEmpty()) {
            logcat {
                "[NovelSearchFallbackEngine] Total 0 similar items found for novel '${novel.title}'. Check source connectivity or query matching strictness."
            }
        } else {
            logcat {
                "[NovelSearchFallbackEngine] Fallback finished, found ${items.size} matching items: ${items.map {
                    it.title
                }}"
            }
        }
        SuggestionCache.put(cacheKey, items)

        return if (items.isEmpty()) {
            NovelFallbackOutcome.Empty(NovelFallbackReason.SEARCH_EMPTY)
        } else {
            NovelFallbackOutcome.Success(items)
        }
    }

    private suspend fun backfillFromPopularNovels(
        source: NovelCatalogueSource,
        novel: Novel,
        maxResults: Int,
        uniqueResults: LinkedHashMap<String, SuggestionItem>,
        onProgress: ((List<SuggestionItem>) -> Unit)?,
    ) {
        var page = 1
        var hasNextPage = true
        while (hasNextPage && synchronized(uniqueResults) { uniqueResults.size < maxResults }) {
            val novelsPage = try {
                source.getPopularNovels(page)
            } catch (e: Exception) {
                logcat { "[NovelSearchFallbackEngine] Popular backfill failed on page $page: ${e.message}" }
                return
            }
            if (novelsPage.novels.isEmpty()) return

            var addedAny = false
            val currentProgress = synchronized(uniqueResults) {
                novelsPage.novels.forEach { sNovel ->
                    if (uniqueResults.size >= maxResults) return@forEach
                    if (sNovel.url == novel.url) return@forEach
                    if (SuggestionTitleResolver.isFranchiseDuplicate(sNovel.title, novel.title)) return@forEach
                    if (uniqueResults.containsKey(sNovel.url)) return@forEach

                    uniqueResults[sNovel.url] = SuggestionItem(
                        title = sNovel.title,
                        searchQuery = sNovel.title,
                        thumbnailUrl = sNovel.thumbnail_url,
                        providerName = source.name,
                        providerUrl = sNovel.url,
                        providerId = "${source.id}:${sNovel.url}",
                        mediaType = SuggestionMediaType.NOVEL,
                    )
                    addedAny = true
                }
                if (addedAny) uniqueResults.values.toList() else null
            }
            if (currentProgress != null) {
                onProgress?.invoke(currentProgress)
            }

            hasNextPage = novelsPage.hasNextPage
            page++
        }
    }
}
