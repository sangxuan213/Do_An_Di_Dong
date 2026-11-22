package com.example.uthdocument.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uthdocument.domain.model.ClassSession
import com.example.uthdocument.domain.model.Weekday
import com.example.uthdocument.ui.theme.UthBlue

@Composable
fun ScheduleScreen(viewModel: ScheduleViewModel = viewModel()) {
    val state = viewModel.uiState.collectAsState().value

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Lịch học",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
        )
        Text(
            text = state.selectedDay.fullLabel,
            color = Color.Gray,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )

        // Thanh chọn thứ trong tuần
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Weekday.entries.forEach { day ->
                DayChip(
                    day = day,
                    selected = day == state.selectedDay,
                    hasClasses = day in state.daysWithClasses,
                    onClick = { viewModel.onDaySelected(day) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Không có lịch học vào ${state.selectedDay.fullLabel}.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.sessions, key = { it.id }) { session -> SessionCard(session) }
            }
        }
    }
}

@Composable
private fun DayChip(
    day: Weekday,
    selected: Boolean,
    hasClasses: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) UthBlue else Color(0xFFF0F3FA))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = day.shortLabel,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else Color(0xFF4A5568)
        )
        Spacer(modifier = Modifier.height(4.dp))
        // chấm báo hiệu ngày đó có tiết học
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(
                    when {
                        !hasClasses -> Color.Transparent
                        selected -> Color.White
                        else -> UthBlue
                    }
                )
        )
    }
}

@Composable
private fun SessionCard(session: ClassSession) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = session.startText, fontWeight = FontWeight.Bold, color = UthBlue)
                Text(text = session.endText, fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = session.subject, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Phòng ${session.room} • ${session.lecturer}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
