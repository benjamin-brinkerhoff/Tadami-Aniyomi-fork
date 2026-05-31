package eu.kanade.tachiyomi.data.suggestions.sources

/**
 * Media type hint for recommendation sources.
 * NOVEL maps to MANGA on tracker APIs that don't distinguish.
 */
enum class SuggestionMediaType {
    ANIME,
    MANGA,
    NOVEL, // aliases to MANGA for tracker queries
}

fun SuggestionMediaType.toAniListType(): String = when (this) {
    SuggestionMediaType.ANIME -> "ANIME"
    SuggestionMediaType.MANGA, SuggestionMediaType.NOVEL -> "MANGA"
}
