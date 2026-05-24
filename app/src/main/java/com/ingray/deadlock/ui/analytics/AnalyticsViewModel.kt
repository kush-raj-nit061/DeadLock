package com.ingray.deadlock.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ingray.deadlock.domain.model.AnalyticsSummary
import com.ingray.deadlock.domain.model.UsageStat
import com.ingray.deadlock.domain.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class AnalyticsUiState(
    val summary: AnalyticsSummary? = null,
    val selectedDateIndex: Int = 0, // 0 is today, 1 is yesterday, etc.
    val dailyUsageStats: List<UsageStat> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun selectDate(index: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedDateIndex = index, isLoading = true) }
            val usage = analyticsRepository.getAppUsageStatsForDay(index)
            _uiState.update { it.copy(dailyUsageStats = usage, isLoading = false) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val summary = analyticsRepository.getSummary()
            val usage = analyticsRepository.getAppUsageStats(1)
            _uiState.update { it.copy(
                summary = summary, 
                dailyUsageStats = usage,
                isLoading = false 
            ) }
        }
    }
}
