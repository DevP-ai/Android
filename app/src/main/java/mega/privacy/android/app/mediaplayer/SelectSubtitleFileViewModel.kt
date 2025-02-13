package mega.privacy.android.app.mediaplayer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.mediaplayer.VideoPlayerFragment.Companion.INTENT_KEY_SUBTITLE_FILE_ID
import mega.privacy.android.app.mediaplayer.mapper.SubtitleFileInfoItemMapper
import mega.privacy.android.app.mediaplayer.model.SubtitleFileInfoItem
import mega.privacy.android.app.mediaplayer.model.SubtitleLoadState
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import mega.privacy.android.domain.entity.statistics.MediaPlayerStatisticsEvents
import mega.privacy.android.domain.usecase.mediaplayer.GetSRTSubtitleFileListUseCase
import mega.privacy.android.domain.usecase.mediaplayer.SendStatisticsMediaPlayerUseCase
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import mega.privacy.mobile.analytics.event.SearchModeEnablePressedEvent
import javax.inject.Inject

/**
 * The view model for select subtitle file
 */
@HiltViewModel
class SelectSubtitleFileViewModel @Inject constructor(
    private val getSRTSubtitleFileListUseCase: GetSRTSubtitleFileListUseCase,
    private val subtitleFileInfoItemMapper: SubtitleFileInfoItemMapper,
    private val sendStatisticsMediaPlayerUseCase: SendStatisticsMediaPlayerUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow<SubtitleLoadState>(SubtitleLoadState.Loading)

    /**
     * The state for updating the subtitle file info item list
     */
    val state = _state.asStateFlow()

    private val query = MutableStateFlow<String?>(null)

    private val selected = MutableStateFlow<SubtitleFileInfo?>(null)

    private val subtitleFileListState = MutableStateFlow(emptyList<SubtitleFileInfo>())

    /**
     * The state for updating search widget
     */
    var searchState by mutableStateOf(SearchWidgetState.COLLAPSED)
        private set

    private val currentSubtitleFileInfoId: Long =
        savedStateHandle[INTENT_KEY_SUBTITLE_FILE_ID] ?: INVALID_VALUE.toLong()

    init {
        viewModelScope.launch {
            combine(
                query,
                selected,
                subtitleFileListState,
                ::mapToSubtitleFileInfoItem
            ).collect { list ->
                _state.update {
                    if (list.isEmpty()) {
                        SubtitleLoadState.Empty
                    } else {
                        SubtitleLoadState.Success(list)
                    }
                }
            }
        }
    }

    private fun mapToSubtitleFileInfoItem(
        search: String?,
        selected: SubtitleFileInfo?,
        subtitleFileInfoList: List<SubtitleFileInfo>,
    ): List<SubtitleFileInfoItem> =
        if (subtitleFileInfoList.isEmpty()) {
            emptyList()
        } else {
            subtitleFileInfoList.filter { info ->
                (search == null || info.name.contains(search, ignoreCase = true)) &&
                        info.id != currentSubtitleFileInfoId
            }.map { info ->
                subtitleFileInfoItemMapper(
                    isSelected = info.id == selected?.id,
                    subtitleFileInfo = info
                )
            }
        }

    /**
     * Get subtitle file info list
     *
     * @return subtitle file info list
     */
    suspend fun getSubtitleFileInfoList() =
        subtitleFileListState.update { getSRTSubtitleFileListUseCase() }

    /**
     * Update when the item is clicked
     *
     * @param subtitleFileInfo the selected [SubtitleFileInfo]
     */
    fun itemClickedUpdate(subtitleFileInfo: SubtitleFileInfo) {
        selected.update {
            if (subtitleFileInfo.id == selected.value?.id) {
                null
            } else {
                subtitleFileInfo
            }
        }
    }

    /**
     * Filter the items that matches the query
     * @param queryString search query
     */
    fun searchQuery(queryString: String) {
        query.update { queryString }
    }

    /**
     * Get query state flow
     */
    fun getQueryStateFlow() = query

    /**
     * Get current selected subtitle file info flow
     */
    fun getSelectedSubtitleFileInfoFlow() = selected


    /**
     * Update for search widget state
     */
    fun searchWidgetStateUpdate() {
        searchState = if (searchState == SearchWidgetState.COLLAPSED) {
            sendSearchModeEnabledEvent()
            Analytics.tracker.trackEvent(SearchModeEnablePressedEvent)
            SearchWidgetState.EXPANDED
        } else {
            SearchWidgetState.COLLAPSED
        }
    }

    /**
     * Close search
     */
    fun closeSearch() {
        query.update { null }
        searchState = SearchWidgetState.COLLAPSED
    }

    /**
     * Send SearchModeEnabledEvent
     */
    private fun sendSearchModeEnabledEvent() {
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.SearchModeEnabledEvent())
    }

    /**
     * Send AddSubtitleClickedEvent
     */
    fun sendAddSubtitleClickedEvent() {
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.AddSubtitleClickedEvent())
    }

    /**
     * Send SelectSubtitleCancelledEvent
     */
    fun sendSelectSubtitleCancelledEvent() {
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.SelectSubtitleCancelledEvent())
    }

    /**
     * Send MediaPlayerStatisticsEvent
     *
     * @param event MediaPlayerStatisticsEvents
     */
    private fun sendMediaPlayerStatisticsEvent(event: MediaPlayerStatisticsEvents) {
        viewModelScope.launch {
            sendStatisticsMediaPlayerUseCase(event)
        }
    }
}