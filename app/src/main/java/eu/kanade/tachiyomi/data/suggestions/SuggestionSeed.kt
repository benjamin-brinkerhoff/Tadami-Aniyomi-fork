package eu.kanade.tachiyomi.data.suggestions

import eu.kanade.tachiyomi.data.suggestions.sources.SuggestionMediaType

data class SuggestionSeed(
    val mediaType: SuggestionMediaType,
    val primaryTitle: String,
    val candidateTitles: List<String>,
    val description: String?,
    val author: String? = null,
    val genres: List<String>? = null,
)
