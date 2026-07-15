package eu.kanade.tachiyomi.ui.reader.novel.dictionary

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.ui.reader.novel.NovelDictionaryResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import java.io.File

@Serializable
data class NovelDictionaryHistoryEntry(
    val term: String,
    val language: String? = null,
    val targetLanguage: String? = null,
    val preview: String? = null,
    val firstLookupAt: Long,
    val lastLookupAt: Long,
    val lookupCount: Int = 1,
)

@Serializable
data class NovelDictionaryHistoryFile(
    val version: Int = 1,
    val entries: List<NovelDictionaryHistoryEntry> = emptyList(),
)

/**
 * Local, persistent history of dictionary word lookups in the novel reader.
 *
 * Stored as JSON in `filesDir/novel_dictionary_history.json`. The history can be exported to
 * and imported from a user-selected document, so readers can back it up or share it.
 */
object NovelDictionaryHistory {

    private const val FILE_NAME = "novel_dictionary_history.json"
    private const val MAX_ENTRIES = 2000
    private const val PREVIEW_MAX_LENGTH = 160

    private val lock = Any()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun historyFile(context: Context): File = File(context.filesDir, FILE_NAME)

    /** Builds a short plain-text preview from a dictionary result for history display. */
    fun previewOf(result: NovelDictionaryResult): String? {
        val html = result.entries.firstOrNull()?.definitionsHtml ?: return null
        val text = runCatching { Jsoup.parse(html).text() }.getOrNull()?.trim() ?: return null
        if (text.isEmpty()) return null
        return if (text.length <= PREVIEW_MAX_LENGTH) text else text.take(PREVIEW_MAX_LENGTH - 1) + "\u2026"
    }

    fun record(
        context: Context,
        term: String,
        language: String?,
        targetLanguage: String?,
        preview: String?,
    ) {
        val cleanTerm = term.trim()
        if (cleanTerm.isEmpty()) return
        val now = System.currentTimeMillis()
        synchronized(lock) {
            val current = read(context)
            val key = entryKey(cleanTerm, language)
            val existing = current.entries.firstOrNull { entryKey(it.term, it.language) == key }
            val updatedEntry = if (existing != null) {
                existing.copy(
                    lastLookupAt = now,
                    lookupCount = existing.lookupCount + 1,
                    targetLanguage = targetLanguage ?: existing.targetLanguage,
                    preview = preview ?: existing.preview,
                )
            } else {
                NovelDictionaryHistoryEntry(
                    term = cleanTerm,
                    language = language,
                    targetLanguage = targetLanguage,
                    preview = preview,
                    firstLookupAt = now,
                    lastLookupAt = now,
                    lookupCount = 1,
                )
            }
            val remaining = current.entries.filter { entryKey(it.term, it.language) != key }
            val merged = (listOf(updatedEntry) + remaining)
                .sortedByDescending { it.lastLookupAt }
                .take(MAX_ENTRIES)
            write(context, current.copy(entries = merged))
        }
    }

    /** Returns history entries, most recently looked-up first. */
    fun entries(context: Context): List<NovelDictionaryHistoryEntry> {
        return synchronized(lock) {
            read(context).entries.sortedByDescending { it.lastLookupAt }
        }
    }

    fun clear(context: Context) {
        synchronized(lock) {
            historyFile(context).delete()
        }
    }

    /** Exports the history as JSON to [uri]. Returns the number of exported entries. */
    fun exportTo(context: Context, uri: Uri): Result<Int> = runCatching {
        val payload = synchronized(lock) { read(context) }
        val output = context.contentResolver.openOutputStream(uri, "wt")
            ?: error("Cannot open output document")
        output.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(json.encodeToString(NovelDictionaryHistoryFile.serializer(), payload))
        }
        payload.entries.size
    }

    /**
     * Imports history JSON from [uri] and merges it with the local history
     * (lookup counts are summed, timestamps are widened). Returns the number
     * of imported entries.
     */
    fun importFrom(context: Context, uri: Uri): Result<Int> = runCatching {
        val input = context.contentResolver.openInputStream(uri) ?: error("Cannot open document")
        val text = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val imported = json.decodeFromString(NovelDictionaryHistoryFile.serializer(), text)
        synchronized(lock) {
            val current = read(context)
            val byKey = LinkedHashMap<String, NovelDictionaryHistoryEntry>()
            current.entries.forEach { entry -> byKey[entryKey(entry.term, entry.language)] = entry }
            imported.entries.forEach { entry ->
                if (entry.term.isBlank()) return@forEach
                val key = entryKey(entry.term, entry.language)
                val existing = byKey[key]
                byKey[key] = if (existing == null) {
                    entry
                } else {
                    existing.copy(
                        firstLookupAt = minOf(existing.firstLookupAt, entry.firstLookupAt),
                        lastLookupAt = maxOf(existing.lastLookupAt, entry.lastLookupAt),
                        lookupCount = existing.lookupCount + entry.lookupCount,
                        preview = existing.preview ?: entry.preview,
                        targetLanguage = existing.targetLanguage ?: entry.targetLanguage,
                    )
                }
            }
            val merged = byKey.values
                .sortedByDescending { it.lastLookupAt }
                .take(MAX_ENTRIES)
            write(context, NovelDictionaryHistoryFile(entries = merged))
        }
        imported.entries.size
    }

    private fun entryKey(term: String, language: String?): String {
        return term.trim().lowercase() + "\u0000" + language?.trim()?.lowercase().orEmpty()
    }

    private fun read(context: Context): NovelDictionaryHistoryFile {
        val file = historyFile(context)
        if (!file.isFile) return NovelDictionaryHistoryFile()
        return runCatching {
            json.decodeFromString(NovelDictionaryHistoryFile.serializer(), file.readText(Charsets.UTF_8))
        }.getOrElse { NovelDictionaryHistoryFile() }
    }

    private fun write(context: Context, payload: NovelDictionaryHistoryFile) {
        val file = historyFile(context)
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json.encodeToString(NovelDictionaryHistoryFile.serializer(), payload), Charsets.UTF_8)
        if (!tmp.renameTo(file)) {
            file.writeText(json.encodeToString(NovelDictionaryHistoryFile.serializer(), payload), Charsets.UTF_8)
            tmp.delete()
        }
    }
}
