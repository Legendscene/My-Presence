package com.kyrx.mypresence.feature.presence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyrx.mypresence.domain.model.PresenceData
import com.kyrx.mypresence.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapLatest
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class PresenceEditorViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val activityType = MutableStateFlow(0)
    val name = MutableStateFlow("My Presence")
    val details = MutableStateFlow("")
    val state = MutableStateFlow("")
    val largeImage = MutableStateFlow("")
    val smallImage = MutableStateFlow("")
    val largeText = MutableStateFlow("")
    val smallText = MutableStateFlow("")
    val startTimestamp = MutableStateFlow<Long?>(null)
    val endTimestamp = MutableStateFlow<Long?>(null)
    val partySize = MutableStateFlow("")
    val partyMax = MutableStateFlow("")
    val button1Label = MutableStateFlow("")
    val button1Url = MutableStateFlow("")
    val button2Label = MutableStateFlow("")
    val button2Url = MutableStateFlow("")
    val platform = MutableStateFlow("android")
    val streamUrl = MutableStateFlow("")
    val joinSecret = MutableStateFlow("")
    val spectateSecret = MutableStateFlow("")
    val matchSecret = MutableStateFlow("")
    val status = MutableStateFlow("online")

    private var saveJob: kotlinx.coroutines.Job? = null

    private fun triggerAutoSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500) // debounce 500ms
            saveAllFields()
        }
    }

    private fun saveAllFields() {
        viewModelScope.launch {
            preferencesRepository.savePresenceConfig(
                PresenceData(
                    name = name.value,
                    details = details.value,
                    state = state.value,
                    type = activityType.value,
                    largeImage = largeImage.value,
                    largeText = largeText.value,
                    smallImage = smallImage.value,
                    smallText = smallText.value,
                    startTimestamp = startTimestamp.value,
                    endTimestamp = endTimestamp.value,
                    status = status.value,
                    button1Label = button1Label.value,
                    button1Url = button1Url.value,
                    button2Label = button2Label.value,
                    button2Url = button2Url.value,
                    partySize = partySize.value.toIntOrNull(),
                    partyMax = partyMax.value.toIntOrNull(),
                    platform = platform.value,
                    streamUrl = streamUrl.value
                )
            )
        }
    }

    val livePreview: StateFlow<String> = combine(
        combine(activityType, name, details, state, largeImage) { a, b, c, d, e ->
            listOf<Any?>(a, b, c, d, e)
        },
        combine(smallImage, largeText, smallText, startTimestamp, endTimestamp) { a, b, c, d, e ->
            listOf<Any?>(a, b, c, d, e)
        },
        combine(partySize, partyMax, button1Label, button1Url, button2Label) { a, b, c, d, e ->
            listOf<Any?>(a, b, c, d, e)
        },
        combine(button2Url, platform, streamUrl, joinSecret, spectateSecret) { a, b, c, d, e ->
            listOf<Any?>(a, b, c, d, e)
        },
        combine(matchSecret, status) { a, b -> listOf<Any?>(a, b) }
    ) { g1, g2, g3, g4, g5 ->
        val v = g1 + g2 + g3 + g4 + g5
        JSONObject().apply {
            put("activityType", v[0])
            put("name", v[1])
            put("details", v[2])
            put("state", v[3])
            put("largeImage", v[4])
            put("smallImage", v[5])
            put("largeText", v[6])
            put("smallText", v[7])
            put("startTimestamp", v[8])
            put("endTimestamp", v[9])
            put("partySize", v[10])
            put("partyMax", v[11])
            put("button1Label", v[12])
            put("button1Url", v[13])
            put("button2Label", v[14])
            put("button2Url", v[15])
            put("platform", v[16])
            put("streamUrl", v[17])
            put("joinSecret", v[18])
            put("spectateSecret", v[19])
            put("matchSecret", v[20])
            put("status", v[21])
        }.toString(2)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "{}")

    init {
        viewModelScope.launch {
            val presence = preferencesRepository.customPresence.first()
            name.value = presence.name
            details.value = presence.details
            state.value = presence.state
            activityType.value = presence.type
            largeImage.value = presence.largeImage
            largeText.value = presence.largeText
            smallImage.value = presence.smallImage
            smallText.value = presence.smallText
            startTimestamp.value = presence.startTimestamp
            endTimestamp.value = presence.endTimestamp
            status.value = presence.status
            button1Label.value = presence.button1Label
            button1Url.value = presence.button1Url
            button2Label.value = presence.button2Label
            button2Url.value = presence.button2Url
            partySize.value = presence.partySize?.toString().orEmpty()
            partyMax.value = presence.partyMax?.toString().orEmpty()
            platform.value = presence.platform
            streamUrl.value = presence.streamUrl
        }
    }

    // Auto-save on any field change
    fun onFieldChanged() = triggerAutoSave()
}
