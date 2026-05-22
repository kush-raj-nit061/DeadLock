package com.ingray.deadlock.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ingray.deadlock.domain.model.AnalyticsEvent
import com.ingray.deadlock.domain.model.EventType
import com.ingray.deadlock.domain.model.FocusSession
import com.ingray.deadlock.domain.repository.AnalyticsRepository
import com.ingray.deadlock.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FocusSessionUiState(
    val session: FocusSession? = null,
    val remainingMillis: Long = 0L,
    val showEmergencyConfirm: Boolean = false,
    val showMathChallenge: Boolean = false,
    val mathA: Int = 0,
    val mathB: Int = 0,
    val mathAnswer: String = "",
    val mathError: Boolean = false,
    val isSessionEnded: Boolean = false
)

@HiltViewModel
class FocusSessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusSessionUiState())
    val uiState: StateFlow<FocusSessionUiState> = _uiState.asStateFlow()

    init {
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionRepository.observeActiveSession().collect { session ->
                if (session == null) {
                    _uiState.update { it.copy(isSessionEnded = true) }
                } else {
                    _uiState.update { it.copy(session = session, remainingMillis = session.remainingMillis) }
                }
            }
        }
    }

    fun updateRemainingTime() {
        val session = _uiState.value.session ?: return
        _uiState.update { it.copy(remainingMillis = session.remainingMillis) }
    }

    fun requestEmergencyUnlock() {
        viewModelScope.launch {
            val session = _uiState.value.session ?: return@launch
            analyticsRepository.recordEvent(
                AnalyticsEvent(type = EventType.UNLOCK_ATTEMPT, sessionId = session.id)
            )
            sessionRepository.recordDistractionAttempt(session.id)
        }
        val a = (10..99).random()
        val b = (10..99).random()
        _uiState.update {
            it.copy(
                showEmergencyConfirm = true,
                showMathChallenge = false,
                mathA = a,
                mathB = b,
                mathAnswer = "",
                mathError = false
            )
        }
    }

    fun confirmEmergencyIntent() {
        _uiState.update { it.copy(showEmergencyConfirm = false, showMathChallenge = true) }
    }

    fun dismissEmergency() {
        _uiState.update {
            it.copy(
                showEmergencyConfirm = false,
                showMathChallenge = false,
                mathAnswer = "",
                mathError = false
            )
        }
    }

    fun setMathAnswer(answer: String) {
        _uiState.update { it.copy(mathAnswer = answer, mathError = false) }
    }

    fun submitMathAnswer() {
        val state = _uiState.value
        val correct = state.mathA + state.mathB
        if (state.mathAnswer.trim().toIntOrNull() == correct) {
            viewModelScope.launch {
                state.session?.let { sessionRepository.cancelSession(it.id) }
            }
            _uiState.update { it.copy(showMathChallenge = false) }
        } else {
            _uiState.update { it.copy(mathError = true) }
        }
    }
}
