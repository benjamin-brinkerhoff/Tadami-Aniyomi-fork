package eu.kanade.tachiyomi.data.suggestions

import eu.kanade.tachiyomi.data.suggestions.sources.SuggestionMediaType
import java.io.Serializable

data class SuggestionItem(
    val title: String,
    val searchQuery: String,
    val thumbnailUrl: String?,
    val providerName: String,
    val providerUrl: String,
    val providerId: String?,
    val mediaType: SuggestionMediaType,
) : Serializable
