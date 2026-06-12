package eu.kanade.tachiyomi.ui.libraryUpdateError

import android.app.Application
import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.util.addOrRemove
import eu.kanade.tachiyomi.data.library.anime.AnimeLibraryUpdateJob
import eu.kanade.tachiyomi.data.library.manga.MangaLibraryUpdateJob
import eu.kanade.tachiyomi.data.library.novel.NovelLibraryUpdateJob
import eu.kanade.tachiyomi.data.library.updateerror.LibraryUpdateErrorMedia
import eu.kanade.tachiyomi.data.library.updateerror.LibraryUpdateErrorRecord
import eu.kanade.tachiyomi.data.library.updateerror.LibraryUpdateErrorStore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class LibraryUpdateErrorScreenModel : StateScreenModel<LibraryUpdateErrorScreenState>(LibraryUpdateErrorScreenState()) {

    private val selectedErrorIds: HashSet<Long> = HashSet()

    init {
        screenModelScope.launchIO {
            LibraryUpdateErrorStore.errors.collectLatest { errors ->
                mutableState.update { state ->
                    state.copy(
                        isLoading = false,
                        items = errors.map { record ->
                            LibraryUpdateErrorItem(
                                record = record,
                                selected = record.id in selectedErrorIds,
                            )
                        },
                    )
                }
            }
        }
    }

    fun setSelectedTab(media: LibraryUpdateErrorMedia) {
        selectedErrorIds.clear()
        mutableState.update { state ->
            state.copy(
                selectedMedia = media,
                items = state.items.map { it.copy(selected = false) },
            )
        }
    }

    fun toggleSelection(item: LibraryUpdateErrorItem, selected: Boolean) {
        selectedErrorIds.addOrRemove(item.record.id, selected)
        mutableState.update { state ->
            state.copy(
                items = state.items.map {
                    if (it.record.id == item.record.id) it.copy(selected = selected) else it
                },
            )
        }
    }

    fun toggleAllSelection(selected: Boolean) {
        mutableState.update { state ->
            val visibleIds = state.visibleItems.map { it.record.id }.toSet()
            if (selected) {
                selectedErrorIds.addAll(visibleIds)
            } else {
                selectedErrorIds.removeAll(visibleIds)
            }
            state.copy(
                items = state.items.map {
                    if (it.record.id in visibleIds) it.copy(selected = selected) else it
                },
            )
        }
    }

    fun invertSelection() {
        mutableState.update { state ->
            val visibleIds = state.visibleItems.map { it.record.id }.toSet()
            state.copy(
                items = state.items.map {
                    if (it.record.id in visibleIds) {
                        selectedErrorIds.addOrRemove(it.record.id, !it.selected)
                        it.copy(selected = !it.selected)
                    } else {
                        it
                    }
                },
            )
        }
    }


    fun retryVisibleErrors() {
        val visibleItems = state.value.visibleItems
        if (visibleItems.isEmpty()) return

        val context = Injekt.get<Application>()
        val entryIds = visibleItems
            .map { it.record.entryId }
            .distinct()
            .toLongArray()

        when (state.value.selectedMedia) {
            LibraryUpdateErrorMedia.Manga -> MangaLibraryUpdateJob.startNow(context, entryIds)
            LibraryUpdateErrorMedia.Anime -> AnimeLibraryUpdateJob.startNow(context, entryIds)
            LibraryUpdateErrorMedia.Novel -> NovelLibraryUpdateJob.startNow(context, entryIds)
        }
    }

    fun deleteSelected() {
        val selected = selectedErrorIds.toList()
        LibraryUpdateErrorStore.delete(selected)
        selectedErrorIds.removeAll(selected.toSet())
    }

    fun delete(errorId: Long) {
        LibraryUpdateErrorStore.delete(errorId)
        selectedErrorIds.remove(errorId)
    }

    fun clearVisible() {
        val selectedMedia = state.value.selectedMedia
        val ids = state.value.items
            .filter { it.record.media == selectedMedia }
            .map { it.record.id }
        LibraryUpdateErrorStore.delete(ids)
        selectedErrorIds.removeAll(ids.toSet())
    }
}

@Immutable
data class LibraryUpdateErrorScreenState(
    val isLoading: Boolean = true,
    val selectedMedia: LibraryUpdateErrorMedia = LibraryUpdateErrorMedia.Novel,
    val items: List<LibraryUpdateErrorItem> = emptyList(),
) {
    val visibleItems = items.filter { it.record.media == selectedMedia }
    val selected = visibleItems.filter { it.selected }
    val selectionMode = selected.isNotEmpty()

    fun count(media: LibraryUpdateErrorMedia): Int = items.count { it.record.media == media }

    fun groupedVisibleItems(): List<LibraryUpdateErrorUiModel> {
        return visibleItems
            .sortedWith(compareBy<LibraryUpdateErrorItem> { it.record.message }.thenBy { it.record.title })
            .groupBy { it.record.message }
            .flatMap { (message, errors) ->
                listOf(LibraryUpdateErrorUiModel.Header(message, errors.size)) +
                    errors.map { LibraryUpdateErrorUiModel.Item(it) }
            }
    }
}

@Immutable
data class LibraryUpdateErrorItem(
    val record: LibraryUpdateErrorRecord,
    val selected: Boolean,
)

sealed class LibraryUpdateErrorUiModel {
    data class Header(val errorMessage: String, val count: Int) : LibraryUpdateErrorUiModel()
    data class Item(val item: LibraryUpdateErrorItem) : LibraryUpdateErrorUiModel()
}
