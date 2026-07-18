package eu.kanade.domain.entries.metadata

import eu.kanade.tachiyomi.data.track.AnimeTracker
import eu.kanade.tachiyomi.data.track.MangaTracker
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.data.track.model.MangaTrackSearch
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.track.anime.interactor.GetAnimeTracks
import tachiyomi.domain.track.manga.interactor.GetMangaTracks
import tachiyomi.domain.track.novel.interactor.GetNovelTracks

/**
 * Pulls display metadata (title/author/description/status/…) from a linked tracker
 * into a [TrackerMetadataDraft] for the Edit Info form.
 *
 * Works for any logged-in tracker that exposes search results with remote metadata.
 * Does not auto-save — callers only fill the form.
 */
class FetchEntryMetadataFromTracker(
    private val getMangaTracks: GetMangaTracks,
    private val getAnimeTracks: GetAnimeTracks,
    private val getNovelTracks: GetNovelTracks,
    private val trackerManager: TrackerManager,
) {

    /**
     * @param trackerId null → auto-pick if exactly one linked+logged-in tracker,
     *                  or [TrackerMetadataFetchOutcome.ChooseTracker] when several.
     * @param fallbackTitle used as search query when the stored track title is blank.
     */
    suspend fun fetchManga(
        mangaId: Long,
        trackerId: Long? = null,
        fallbackTitle: String = "",
    ): TrackerMetadataFetchOutcome = withIOContext {
        val tracks = getMangaTracks.await(mangaId)
        fetchFromMangaTrackers(
            linked = tracks.map { LinkedRef(it.trackerId, it.remoteId, it.title) },
            trackerId = trackerId,
            fallbackTitle = fallbackTitle,
        )
    }

    suspend fun fetchAnime(
        animeId: Long,
        trackerId: Long? = null,
        fallbackTitle: String = "",
    ): TrackerMetadataFetchOutcome = withIOContext {
        val tracks = getAnimeTracks.await(animeId)
        fetchFromAnimeTrackers(
            linked = tracks.map { LinkedRef(it.trackerId, it.remoteId, it.title) },
            trackerId = trackerId,
            fallbackTitle = fallbackTitle,
        )
    }

    /**
     * Novel trackers implement [MangaTracker] and store tracks via [GetNovelTracks].
     */
    suspend fun fetchNovel(
        novelId: Long,
        trackerId: Long? = null,
        fallbackTitle: String = "",
    ): TrackerMetadataFetchOutcome = withIOContext {
        val tracks = getNovelTracks.await(novelId)
        fetchFromMangaTrackers(
            linked = tracks.map { LinkedRef(it.trackerId, it.remoteId, it.title) },
            trackerId = trackerId,
            fallbackTitle = fallbackTitle,
        )
    }

    private suspend fun fetchFromMangaTrackers(
        linked: List<LinkedRef>,
        trackerId: Long?,
        fallbackTitle: String,
    ): TrackerMetadataFetchOutcome {
        if (linked.isEmpty()) {
            return TrackerMetadataFetchOutcome.Error(TrackerMetadataFetchError.NoLinkedTracks)
        }

        val candidates = resolveMangaCandidates(linked)
        if (candidates.isEmpty()) {
            return TrackerMetadataFetchOutcome.Error(TrackerMetadataFetchError.TrackerNotLoggedIn)
        }

        if (trackerId == null && candidates.size > 1) {
            return TrackerMetadataFetchOutcome.ChooseTracker(
                candidates.map { (ref, tracker) ->
                    TrackerMetadataSource(
                        trackerId = tracker.id,
                        trackerName = tracker.name,
                        remoteTitle = ref.remoteTitle.ifBlank { fallbackTitle },
                    )
                },
            )
        }

        val pair = selectCandidate(candidates, trackerId)
            ?: return TrackerMetadataFetchOutcome.Error(TrackerMetadataFetchError.NoLinkedTracks)
        val (ref, tracker) = pair
        val mangaTracker = tracker as? MangaTracker
            ?: return TrackerMetadataFetchOutcome.Error(TrackerMetadataFetchError.Unexpected("Tracker does not support manga search"))

        return searchMangaAndMap(
            service = mangaTracker,
            trackerId = tracker.id,
            trackerName = tracker.name,
            ref = ref,
            fallbackTitle = fallbackTitle,
        )
    }

    private suspend fun fetchFromAnimeTrackers(
        linked: List<LinkedRef>,
        trackerId: Long?,
        fallbackTitle: String,
    ): TrackerMetadataFetchOutcome {
        if (linked.isEmpty()) {
            return TrackerMetadataFetchOutcome.Error(TrackerMetadataFetchError.NoLinkedTracks)
        }

        val candidates = resolveAnimeCandidates(linked)
        if (candidates.isEmpty()) {
            return TrackerMetadataFetchOutcome.Error(TrackerMetadataFetchError.TrackerNotLoggedIn)
        }

        if (trackerId == null && candidates.size > 1) {
            return TrackerMetadataFetchOutcome.ChooseTracker(
                candidates.map { (ref, tracker) ->
                    TrackerMetadataSource(
                        trackerId = tracker.id,
                        trackerName = tracker.name,
                        remoteTitle = ref.remoteTitle.ifBlank { fallbackTitle },
                    )
                },
            )
        }

        val pair = selectCandidate(candidates, trackerId)
            ?: return TrackerMetadataFetchOutcome.Error(TrackerMetadataFetchError.NoLinkedTracks)
        val (ref, tracker) = pair
        val animeTracker = tracker as? AnimeTracker
            ?: return TrackerMetadataFetchOutcome.Error(TrackerMetadataFetchError.Unexpected("Tracker does not support anime search"))

        return searchAnimeAndMap(
            service = animeTracker,
            trackerId = tracker.id,
            trackerName = tracker.name,
            ref = ref,
            fallbackTitle = fallbackTitle,
        )
    }

    private fun resolveMangaCandidates(linked: List<LinkedRef>): List<Pair<LinkedRef, Tracker>> {
        return linked.mapNotNull { ref ->
            val tracker = trackerManager.get(ref.trackerId) ?: return@mapNotNull null
            if (!tracker.isLoggedIn) return@mapNotNull null
            if (tracker !is MangaTracker) return@mapNotNull null
            ref to tracker
        }
    }

    private fun resolveAnimeCandidates(linked: List<LinkedRef>): List<Pair<LinkedRef, Tracker>> {
        return linked.mapNotNull { ref ->
            val tracker = trackerManager.get(ref.trackerId) ?: return@mapNotNull null
            if (!tracker.isLoggedIn) return@mapNotNull null
            if (tracker !is AnimeTracker) return@mapNotNull null
            ref to tracker
        }
    }

    private fun selectCandidate(
        candidates: List<Pair<LinkedRef, Tracker>>,
        trackerId: Long?,
    ): Pair<LinkedRef, Tracker>? {
        return if (trackerId != null) {
            candidates.firstOrNull { it.second.id == trackerId }
        } else {
            candidates.firstOrNull()
        }
    }

    private suspend fun searchMangaAndMap(
        service: MangaTracker,
        trackerId: Long,
        trackerName: String,
        ref: LinkedRef,
        fallbackTitle: String,
    ): TrackerMetadataFetchOutcome {
        return try {
            val query = listOf(ref.remoteTitle, fallbackTitle)
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
            if (query.isBlank()) {
                return TrackerMetadataFetchOutcome.Error(TrackerMetadataFetchError.NoSearchResults)
            }

            val results = service.searchManga(query)
            if (results.isEmpty()) {
                // Retry with fallback title if primary query was remote title and failed
                val retry = fallbackTitle.takeIf {
                    it.isNotBlank() && !it.equals(query, ignoreCase = true)
                }
                val retryResults = if (retry != null) service.searchManga(retry) else emptyList()
                if (retryResults.isEmpty()) {
                    return TrackerMetadataFetchOutcome.Error(TrackerMetadataFetchError.NoSearchResults)
                }
                return mapMangaMatch(retryResults, ref, trackerName)
            }
            mapMangaMatch(results, ref, trackerName)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) {
                "Failed to fetch manga metadata from tracker id=$trackerId"
            }
            TrackerMetadataFetchOutcome.Error(TrackerMetadataFetchError.Unexpected(e.message))
        }
    }

    private suspend fun searchAnimeAndMap(
        service: AnimeTracker,
        trackerId: Long,
        trackerName: String,
        ref: LinkedRef,
        fallbackTitle: String,
    ): TrackerMetadataFetchOutcome {
        return try {
            val query = listOf(ref.remoteTitle, fallbackTitle)
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
            if (query.isBlank()) {
                return TrackerMetadataFetchOutcome.Error(TrackerMetadataFetchError.NoSearchResults)
            }

            val results = service.searchAnime(query)
            if (results.isEmpty()) {
                val retry = fallbackTitle.takeIf {
                    it.isNotBlank() && !it.equals(query, ignoreCase = true)
                }
                val retryResults = if (retry != null) service.searchAnime(retry) else emptyList()
                if (retryResults.isEmpty()) {
                    return TrackerMetadataFetchOutcome.Error(TrackerMetadataFetchError.NoSearchResults)
                }
                return mapAnimeMatch(retryResults, ref, trackerName)
            }
            mapAnimeMatch(results, ref, trackerName)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) {
                "Failed to fetch anime metadata from tracker id=$trackerId"
            }
            TrackerMetadataFetchOutcome.Error(TrackerMetadataFetchError.Unexpected(e.message))
        }
    }

    private fun mapMangaMatch(
        results: List<MangaTrackSearch>,
        ref: LinkedRef,
        trackerName: String,
    ): TrackerMetadataFetchOutcome {
        val match = pickMangaMatch(results, ref)
            ?: return TrackerMetadataFetchOutcome.Error(TrackerMetadataFetchError.NoRemoteMatch)
        val draft = match.toMetadataDraft(trackerName)
        if (!draft.hasAnyField) {
            return TrackerMetadataFetchOutcome.Error(TrackerMetadataFetchError.EmptyMetadata)
        }
        return TrackerMetadataFetchOutcome.Success(draft)
    }

    private fun mapAnimeMatch(
        results: List<AnimeTrackSearch>,
        ref: LinkedRef,
        trackerName: String,
    ): TrackerMetadataFetchOutcome {
        val match = pickAnimeMatch(results, ref)
            ?: return TrackerMetadataFetchOutcome.Error(TrackerMetadataFetchError.NoRemoteMatch)
        val draft = match.toMetadataDraft(trackerName)
        if (!draft.hasAnyField) {
            return TrackerMetadataFetchOutcome.Error(TrackerMetadataFetchError.EmptyMetadata)
        }
        return TrackerMetadataFetchOutcome.Success(draft)
    }

    private fun pickMangaMatch(results: List<MangaTrackSearch>, ref: LinkedRef): MangaTrackSearch? {
        results.firstOrNull { it.remote_id == ref.remoteId && ref.remoteId != 0L }?.let { return it }
        val title = ref.remoteTitle
        if (title.isNotBlank()) {
            results.firstOrNull { it.title.equals(title, ignoreCase = true) }?.let { return it }
            results.firstOrNull { candidate ->
                candidate.alternative_titles.any { it.equals(title, ignoreCase = true) }
            }?.let { return it }
        }
        // Single result → safe enough for linked title
        return results.singleOrNull()
    }

    private fun pickAnimeMatch(results: List<AnimeTrackSearch>, ref: LinkedRef): AnimeTrackSearch? {
        results.firstOrNull { it.remote_id == ref.remoteId && ref.remoteId != 0L }?.let { return it }
        val title = ref.remoteTitle
        if (title.isNotBlank()) {
            results.firstOrNull { it.title.equals(title, ignoreCase = true) }?.let { return it }
            results.firstOrNull { candidate ->
                candidate.alternative_titles.any { it.equals(title, ignoreCase = true) }
            }?.let { return it }
        }
        return results.singleOrNull()
    }

    private data class LinkedRef(
        val trackerId: Long,
        val remoteId: Long,
        val remoteTitle: String,
    )
}
