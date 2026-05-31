package eu.kanade.tachiyomi.data.suggestions

import eu.kanade.tachiyomi.ui.reader.novel.translation.GoogleTranslationService
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object MultilingualQueryHelper {

    private val genreMap = mapOf(
        "action" to listOf("экшен", "боевик"),
        "adventure" to listOf("приключения"),
        "comedy" to listOf("комедия"),
        "drama" to listOf("драма"),
        "fantasy" to listOf("фэнтези"),
        "mystery" to listOf("детектив", "мистика"),
        "psychological" to listOf("психология", "психологическое"),
        "romance" to listOf("романтика"),
        "sci-fi" to listOf("фантастика", "научная фантастика"),
        "science fiction" to listOf("фантастика", "научная фантастика"),
        "slice of life" to listOf("повседневность"),
        "supernatural" to listOf("сверхъестественное"),
        "thriller" to listOf("триллер"),
        "mecha" to listOf("меха"),
        "shounen" to listOf("сёнен", "для мальчиков"),
        "shoujo" to listOf("сёдзё", "для девочек"),
        "seinen" to listOf("сэйнэн", "для мужчин"),
        "josei" to listOf("дзёсэй", "для женщин"),
        "historical" to listOf("исторический", "история"),
        "horror" to listOf("ужасы"),
        "isekai" to listOf("исекай", "попаданцы"),
        "magic" to listOf("магия"),
        "martial arts" to listOf("боевые искусства"),
        "school" to listOf("школа"),
        "sports" to listOf("спорт"),
        "vampire" to listOf("вампиры"),
        "military" to listOf("военное"),
        "harem" to listOf("гарем"),
        "demons" to listOf("демоны"),
        "game" to listOf("игры", "игровое"),
        "music" to listOf("музыка"),
        "parody" to listOf("пародия"),
        "police" to listOf("полиция"),
        "space" to listOf("космос"),
    )

    fun containsCyrillic(text: String): Boolean {
        return text.any { it in '\u0400'..'\u04FF' }
    }

    fun containsLatin(text: String): Boolean {
        return text.any { it in 'a'..'z' || it in 'A'..'Z' }
    }

    private val translationService by lazy {
        Injekt.get<eu.kanade.tachiyomi.ui.reader.novel.translation.GoogleTranslationService>()
    }

    suspend fun translate(text: String): String? {
        if (text.isBlank()) return null
        val targetLang = when {
            containsCyrillic(text) -> "en"
            containsLatin(text) -> "ru"
            else -> return null
        }
        return try {
            translationService.translateSingle(
                text = text,
                sourceLanguage = "auto",
                targetLanguage = targetLang,
            )
        } catch (e: Exception) {
            logcat { "[MultilingualQueryHelper] Translation failed for '$text': ${e.message}" }
            null
        }
    }

    /**
     * Translates a genre bidirectionally between Latin (English) and Cyrillic (Russian).
     * Returns a list of alternative translations.
     */
    fun getGenreTranslations(genre: String): List<String> {
        val cleaned = genre.trim().lowercase()
        val results = mutableListOf<String>()

        // 1. Check if the input genre is in the keys (English)
        val mappedCyrillic = genreMap[cleaned]
        if (mappedCyrillic != null) {
            results.addAll(mappedCyrillic)
        }

        // 2. Check if the input genre is in any of the values (Russian)
        for ((english, RussianList) in genreMap) {
            if (RussianList.any { it == cleaned }) {
                results.add(english)
                // Add other Cyrillic synonyms as well!
                results.addAll(RussianList.filter { it != cleaned })
            }
        }

        return results.distinct()
    }

    /**
     * Helper to get both original and translated variations of an author/artist name or genre.
     */
    suspend fun getMultilingualVariants(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val cleaned = text.trim()
        val variants = mutableListOf(cleaned)

        translate(cleaned)?.let { translated ->
            if (translated.isNotBlank() && !translated.equals(cleaned, ignoreCase = true)) {
                variants.add(translated)
            }
        }

        return variants.distinct()
    }
}
