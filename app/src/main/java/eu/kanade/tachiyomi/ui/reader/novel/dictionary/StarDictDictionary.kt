package eu.kanade.tachiyomi.ui.reader.novel.dictionary

import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import java.io.File
import java.io.RandomAccessFile

data class StarDictInfo(
    val bookname: String,
    val wordcount: Int,
    val synwordcount: Int = 0,
    val idxoffsetbits: Int = 32,
    val sametypesequence: String? = null,
    val version: String = "2.4.2",
    val author: String? = null,
    val description: String? = null,
)

data class StarDictArticle(
    val headword: String,
    val definitionsHtml: String,
)

/**
 * Minimal StarDict dictionary reader.
 *
 * Supports the standard StarDict on-disk layout:
 *  - `<name>.ifo`  — metadata (versions 2.4.2 / 3.0.0)
 *  - `<name>.idx`  — sorted word index (32-bit or 64-bit article offsets)
 *  - `<name>.syn`  — optional synonym index
 *  - `<name>.dict` — article data (`.dict.dz`/`.idx.gz` archives are decompressed at import time)
 *
 * The `.idx`/`.syn` index is loaded into memory once per process; article bodies are read
 * lazily from the `.dict` file on lookup. Nothing is bundled with the app — dictionaries are
 * always imported by the user.
 */
class StarDictDictionary private constructor(
    val id: String,
    val info: StarDictInfo,
    private val dictFile: File,
    private val words: Array<String>,
    private val offsets: LongArray,
    private val sizes: IntArray,
    private val index: HashMap<String, MutableList<Int>>,
) {

    val wordCount: Int get() = words.size

    /**
     * Looks up [rawTerm] with forgiving fallbacks (trimmed punctuation, case-insensitive
     * index). Returns at most [maxArticles] rendered articles.
     */
    fun lookup(rawTerm: String, maxArticles: Int = MAX_ARTICLES): List<StarDictArticle> {
        val term = rawTerm.trim()
        if (term.isEmpty()) return emptyList()

        val candidates = LinkedHashSet<String>()
        candidates += term
        candidates += term.trim { !it.isLetterOrDigit() }

        val articles = ArrayList<StarDictArticle>(maxArticles)
        val seen = HashSet<Int>()
        for (candidate in candidates) {
            if (candidate.isEmpty()) continue
            val hits = index[candidate.lowercase()] ?: continue
            for (entryIndex in hits) {
                if (!seen.add(entryIndex)) continue
                val html = runCatching { readArticle(entryIndex) }.getOrNull() ?: continue
                if (html.isBlank()) continue
                articles += StarDictArticle(headword = words[entryIndex], definitionsHtml = html)
                if (articles.size >= maxArticles) return articles
            }
        }
        return articles
    }

    private fun readArticle(entryIndex: Int): String {
        val size = sizes[entryIndex]
        if (size <= 0 || size > MAX_ARTICLE_BYTES) return ""
        val buffer = ByteArray(size)
        RandomAccessFile(dictFile, "r").use { raf ->
            raf.seek(offsets[entryIndex])
            raf.readFully(buffer)
        }
        return renderFields(buffer)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Article rendering (StarDict field types → sanitized HTML)
    // ─────────────────────────────────────────────────────────────────────────

    private fun renderFields(data: ByteArray): String {
        val sb = StringBuilder()
        val sequence = info.sametypesequence
        var pos = 0
        if (!sequence.isNullOrEmpty()) {
            for ((i, type) in sequence.withIndex()) {
                if (pos >= data.size) break
                val isLast = i == sequence.length - 1
                if (type.isUpperCase()) {
                    // Uppercase types: 32-bit size prefix, omitted for the last field.
                    if (isLast) {
                        appendField(sb, type, data.copyOfRange(pos, data.size))
                        pos = data.size
                    } else {
                        if (pos + 4 > data.size) break
                        val size = readUInt32(data, pos).toInt()
                        val end = (pos + 4 + size).coerceAtMost(data.size)
                        appendField(sb, type, data.copyOfRange(pos + 4, end))
                        pos = end
                    }
                } else {
                    // Lowercase types: NUL-terminated string, terminator omitted for the last field.
                    val end = if (isLast) data.size else indexOfNul(data, pos)
                    appendField(sb, type, data.copyOfRange(pos, end))
                    pos = if (isLast || end >= data.size) data.size else end + 1
                }
            }
        } else {
            // Without sametypesequence every field is prefixed with its type character.
            while (pos < data.size) {
                val type = data[pos].toInt().toChar()
                pos++
                if (type.isUpperCase()) {
                    if (pos + 4 > data.size) break
                    val size = readUInt32(data, pos).toInt()
                    pos += 4
                    val end = (pos + size).coerceAtMost(data.size)
                    appendField(sb, type, data.copyOfRange(pos, end))
                    pos = end
                } else {
                    val end = indexOfNul(data, pos)
                    appendField(sb, type, data.copyOfRange(pos, end))
                    pos = if (end >= data.size) data.size else end + 1
                }
            }
        }
        return sb.toString().trim()
    }

    private fun appendField(sb: StringBuilder, type: Char, bytes: ByteArray) {
        if (bytes.isEmpty()) return
        when (type) {
            // Plain text variants (meaning, wiki/plain text, KingSoft, WordNet, tree)
            'm', 'l', 'y', 'k', 'w', 'n' -> {
                val text = decodeUtf8(bytes).trim()
                if (text.isNotEmpty()) {
                    sb.append("<p>").append(escapeHtml(text).replace("\n", "<br>")).append("</p>")
                }
            }
            // Pronunciation / phonetics
            't' -> {
                val text = decodeUtf8(bytes).trim()
                if (text.isNotEmpty()) {
                    sb.append("<p><i>[").append(escapeHtml(text)).append("]</i></p>")
                }
            }
            // Pango markup — close enough to HTML for a sanitizer pass.
            'g' -> sb.append(sanitizeHtml(decodeUtf8(bytes)))
            // XDXF markup — convert well-known tags to HTML first, then sanitize.
            'x' -> sb.append(sanitizeHtml(xdxfToHtml(decodeUtf8(bytes))))
            // HTML
            'h' -> sb.append(sanitizeHtml(decodeUtf8(bytes)))
            // 'r' (resource file list) and uppercase media types (W/P/X) are not supported offline.
            else -> Unit
        }
    }

    private fun decodeUtf8(bytes: ByteArray): String = String(bytes, Charsets.UTF_8)

    private fun xdxfToHtml(source: String): String {
        return source
            .replace(Regex("<k>(.*?)</k>", RegexOption.DOT_MATCHES_ALL), "<b>$1</b><br>")
            .replace(Regex("<(/?)ex>"), "<$1i>")
            .replace(Regex("<(/?)abr>"), "<$1i>")
            .replace(Regex("<(/?)tr>"), "<$1i>")
            .replace(Regex("<kref>(.*?)</kref>", RegexOption.DOT_MATCHES_ALL), "<u>$1</u>")
            .replace(Regex("<c(?:\\s+c=\"[^\"]*\")?>"), "<span>")
            .replace("</c>", "</span>")
            .replace(Regex("<rref>.*?</rref>", RegexOption.DOT_MATCHES_ALL), "")
    }

    companion object {
        private const val MAX_ARTICLE_BYTES = 1 shl 20 // 1 MiB per article
        private const val MAX_ARTICLES = 3

        private val SAFE_HTML: Safelist = Safelist.relaxed()
            .removeTags("img")
            .addTags("span", "font", "hr")

        private fun sanitizeHtml(html: String): String = Jsoup.clean(html, SAFE_HTML)

        private fun escapeHtml(text: String): String = text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

        private fun indexOfNul(data: ByteArray, from: Int): Int {
            var i = from
            while (i < data.size && data[i] != 0.toByte()) i++
            return i
        }

        private fun readUInt32(data: ByteArray, pos: Int): Long {
            return ((data[pos].toLong() and 0xFF) shl 24) or
                ((data[pos + 1].toLong() and 0xFF) shl 16) or
                ((data[pos + 2].toLong() and 0xFF) shl 8) or
                (data[pos + 3].toLong() and 0xFF)
        }

        private fun readUInt64(data: ByteArray, pos: Int): Long {
            var value = 0L
            for (i in 0 until 8) {
                value = (value shl 8) or (data[pos + i].toLong() and 0xFF)
            }
            return value
        }

        /** Parses a StarDict `.ifo` file; returns null when the file is not a valid ifo. */
        fun parseIfo(file: File): StarDictInfo? {
            val lines = runCatching { file.readLines(Charsets.UTF_8) }.getOrNull() ?: return null
            if (lines.isEmpty() || !lines.first().contains("StarDict's dict ifo file")) return null
            val values = lines.drop(1).mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator <= 0) {
                    null
                } else {
                    line.substring(0, separator).trim() to line.substring(separator + 1).trim()
                }
            }.toMap()
            val bookname = values["bookname"]?.takeIf { it.isNotEmpty() } ?: return null
            val wordcount = values["wordcount"]?.toIntOrNull() ?: return null
            return StarDictInfo(
                bookname = bookname,
                wordcount = wordcount,
                synwordcount = values["synwordcount"]?.toIntOrNull() ?: 0,
                idxoffsetbits = values["idxoffsetbits"]?.toIntOrNull() ?: 32,
                sametypesequence = values["sametypesequence"]?.takeIf { it.isNotEmpty() },
                version = values["version"] ?: "2.4.2",
                author = values["author"],
                description = values["description"],
            )
        }

        /**
         * Loads a dictionary from [dir], which must contain `<base>.ifo`, `<base>.idx`
         * and `<base>.dict` (already decompressed).
         */
        fun load(id: String, dir: File): StarDictDictionary {
            val ifoFile = dir.listFiles()?.firstOrNull { it.isFile && it.extension.equals("ifo", true) }
                ?: error("Missing .ifo file in ${dir.name}")
            val info = parseIfo(ifoFile) ?: error("Invalid .ifo file: ${ifoFile.name}")
            val base = ifoFile.name.removeSuffix(".ifo")
            val idxFile = File(dir, "$base.idx")
            val dictFile = File(dir, "$base.dict")
            check(idxFile.isFile) { "Missing .idx file for $base" }
            check(dictFile.isFile) { "Missing .dict file for $base" }
            check(info.idxoffsetbits == 32 || info.idxoffsetbits == 64) {
                "Unsupported idxoffsetbits=${info.idxoffsetbits}"
            }

            val idxBytes = idxFile.readBytes()
            val offsetBytes = info.idxoffsetbits / 8
            val expected = info.wordcount.coerceAtLeast(16)
            val wordList = ArrayList<String>(expected)
            val offsetList = ArrayList<Long>(expected)
            val sizeList = ArrayList<Int>(expected)

            var pos = 0
            while (pos < idxBytes.size) {
                val start = pos
                while (pos < idxBytes.size && idxBytes[pos] != 0.toByte()) pos++
                if (pos >= idxBytes.size) break
                val word = String(idxBytes, start, pos - start, Charsets.UTF_8)
                pos++ // NUL terminator
                if (pos + offsetBytes + 4 > idxBytes.size) break
                val offset = if (offsetBytes == 8) readUInt64(idxBytes, pos) else readUInt32(idxBytes, pos)
                pos += offsetBytes
                val size = readUInt32(idxBytes, pos).toInt()
                pos += 4
                if (word.isEmpty()) continue
                wordList += word
                offsetList += offset
                sizeList += size
            }
            check(wordList.isNotEmpty()) { "Empty .idx index for $base" }

            val words = wordList.toTypedArray()
            val offsets = offsetList.toLongArray()
            val sizes = sizeList.toIntArray()
            val index = HashMap<String, MutableList<Int>>(words.size * 2)
            for (i in words.indices) {
                index.getOrPut(words[i].lowercase()) { mutableListOf() } += i
            }

            // Optional synonym index (.syn) maps alternative spellings to idx entries.
            val synFile = File(dir, "$base.syn")
            if (synFile.isFile) {
                runCatching {
                    val synBytes = synFile.readBytes()
                    var p = 0
                    while (p < synBytes.size) {
                        val start = p
                        while (p < synBytes.size && synBytes[p] != 0.toByte()) p++
                        if (p >= synBytes.size) break
                        val word = String(synBytes, start, p - start, Charsets.UTF_8)
                        p++ // NUL terminator
                        if (p + 4 > synBytes.size) break
                        val target = readUInt32(synBytes, p).toInt()
                        p += 4
                        if (word.isNotEmpty() && target in words.indices) {
                            index.getOrPut(word.lowercase()) { mutableListOf() } += target
                        }
                    }
                }
            }

            return StarDictDictionary(
                id = id,
                info = info,
                dictFile = dictFile,
                words = words,
                offsets = offsets,
                sizes = sizes,
                index = index,
            )
        }
    }
}
