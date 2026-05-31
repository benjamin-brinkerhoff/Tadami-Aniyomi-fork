package eu.kanade.tachiyomi.data.suggestions

import java.util.concurrent.ConcurrentHashMap

object SuggestionCache {
    private val cache = ConcurrentHashMap<String, Pair<Long, List<SuggestionItem>>>()
    private const val TTL_MS = 15 * 60 * 1000L // 15 minutes

    fun get(key: String): List<SuggestionItem>? {
        val entry = cache[key] ?: return null
        val (timestamp, list) = entry
        return if (System.currentTimeMillis() - timestamp < TTL_MS) {
            list
        } else {
            cache.remove(key)
            null
        }
    }

    fun put(key: String, list: List<SuggestionItem>) {
        cache[key] = Pair(System.currentTimeMillis(), list)
    }

    fun invalidateAll() {
        cache.clear()
    }

    /**
     * Build a cache key that includes a fingerprint of the candidate title list.
     * This ensures that a metadata-enriched seed (with more aliases) produces a
     * different cache key than the initial weak seed, enabling a real second fetch.
     */
    fun makeKey(
        sourceName: String,
        primaryTitle: String,
        mediaType: String,
        candidateTitles: List<String> = emptyList(),
    ): String {
        val baseKey = "$sourceName:${primaryTitle.lowercase().trim()}:$mediaType"
        if (candidateTitles.isEmpty()) return baseKey
        val fingerprint = candidateTitles
            .map { it.lowercase().trim() }
            .sorted()
            .joinToString("|")
        return "$baseKey:$fingerprint"
    }

    /**
     * Legacy key without candidate fingerprint – kept for backward compatibility.
     * Prefer [makeKey] with candidateTitles for new code.
     */
    fun makeKey(sourceName: String, title: String, mediaType: String): String =
        makeKey(sourceName, title, mediaType, emptyList())
}
