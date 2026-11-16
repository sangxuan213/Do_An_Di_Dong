package com.example.uthdocument.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uthdocument.data.repository.FakeScheduleRepositoryImpl
import com.example.uthdocument.domain.model.ClassSession
import com.example.uthdocument.domain.model.Weekday
import com.example.uthdocument.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ScheduleUiState(
    val selectedDay: Weekday,
    val sessions: List<ClassSession>,
    val daysWithClasses: Set<Weekday>
)

class ScheduleViewModel(
    repository: ScheduleRepository = FakeScheduleRepositoryImpl()
) : ViewModel() {

    private val selectedDay = MutableStateFlow(Weekday.today())

    val uiState: StateFlow<ScheduleUiState> = combine(repository.getSessions(), selectedDay) { all, day ->
        ScheduleUiState(
            selectedDay = day,
            sessions = all.filter { it.day == day }.sortedBy { it.startMinute },
            daysWithClasses = all.map { it.day }.toSet()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScheduleUiState(selectedDay.value, emptyList(), emptySet())
    )

    fun onDaySelected(day: Weekday) {
        selectedDay.value = day
    }
}
