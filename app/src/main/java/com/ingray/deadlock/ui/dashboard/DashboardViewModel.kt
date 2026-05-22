package com.ingray.deadlock.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ingray.deadlock.domain.model.AnalyticsSummary
import com.ingray.deadlock.domain.model.FocusSession
import com.ingray.deadlock.domain.repository.AnalyticsRepository
import com.ingray.deadlock.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val activeSession: FocusSession? = null,
    val summary: AnalyticsSummary? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                sessionRepository.observeActiveSession(),
                analyticsRepository.observeSummary()
            ) { session, summary ->
                DashboardUiState(
                    activeSession = session,
                    summary = summary,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun refreshSummary() {
        viewModelScope.launch {
            val summary = analyticsRepository.getSummary()
            _uiState.update { it.copy(summary = summary) }
        }
    }
}
