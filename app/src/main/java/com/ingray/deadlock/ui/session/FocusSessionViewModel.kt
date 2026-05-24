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
    val sessions: List<FocusSession> = emptyList(),
    val currentSessionIndex: Int = 0,
    val lastTick: Long = 0L, // New: Forces emission on timer updates
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
        observeSessions()
    }

    private fun observeSessions() {
        viewModelScope.launch {
            sessionRepository.observeActiveSessions().collect { sessions ->
                if (sessions.isEmpty()) {
                    _uiState.update { it.copy(isSessionEnded = true) }
                } else {
                    _uiState.update { state -> 
                        val newIndex = if (state.currentSessionIndex >= sessions.size) 0 else state.currentSessionIndex
                        state.copy(sessions = sessions, currentSessionIndex = newIndex, isSessionEnded = false) 
                    }
                }
            }
        }
    }

    fun selectSession(index: Int) {
        _uiState.update { it.copy(currentSessionIndex = index) }
    }

    fun updateRemainingTime() {
        // Update lastTick to ensure StateFlow emits a new value even if sessions list is identical
        _uiState.update { it.copy(lastTick = System.currentTimeMillis()) }
    }

    fun requestEmergencyUnlock() {
        viewModelScope.launch {
            val sessions = _uiState.value.sessions
            val index = _uiState.value.currentSessionIndex
            if (index < sessions.size) {
                val session = sessions[index]
                analyticsRepository.recordEvent(
                    AnalyticsEvent(type = EventType.UNLOCK_ATTEMPT, sessionId = session.id)
                )
                sessionRepository.recordDistractionAttempt(session.id)
            }
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
                val session = state.sessions.getOrNull(state.currentSessionIndex)
                session?.let { sessionRepository.cancelSession(it.id) }
            }
            _uiState.update { it.copy(showMathChallenge = false) }
        } else {
            _uiState.update { it.copy(mathError = true) }
        }
    }
}
