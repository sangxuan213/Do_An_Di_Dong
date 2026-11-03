package com.example.uthdocument.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uthdocument.ui.home.HomeScreen
import com.example.uthdocument.ui.library.LibraryScreen
import com.example.uthdocument.ui.profile.ProfileScreen
import com.example.uthdocument.ui.schedule.ScheduleScreen

// Định nghĩa các tab
sealed class BottomNavItem(val title: String, val icon: ImageVector) {
    object Home : BottomNavItem("Trang chủ", Icons.Default.Home)
    object Library : BottomNavItem("Thư viện", Icons.Default.Folder)
    object Empty : BottomNavItem("", Icons.Default.Home) // Khoảng trống cho FAB
    object Schedule : BottomNavItem("Lịch học", Icons.Default.CalendarMonth)
    object Profile : BottomNavItem("Cá nhân", Icons.Default.Person)
}

@Composable
fun MainScreen() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Library,
        BottomNavItem.Empty,
        BottomNavItem.Schedule,
        BottomNavItem.Profile
    )

    val uthBlue = Color(0xFF3B71F7) // Màu xanh theo design của bạn (ước lượng)
    val unselectedColor = Color(0xFFA0AAB9)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                items.forEachIndexed { index, item ->
                    if (item == BottomNavItem.Empty) {
                        // Bỏ qua item giữa để nhường chỗ cho FAB
                        NavigationBarItem(
                            selected = false,
                            onClick = { },
                            icon = { },
                            enabled = false
                        )
                    } else {
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (selectedItem == index) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = selectedItem == index,
                            onClick = { selectedItem = index },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = uthBlue,
                                selectedTextColor = uthBlue,
                                unselectedIconColor = unselectedColor,
                                unselectedTextColor = unselectedColor,
                                indicatorColor = Color.White // Ẩn viền background mặc định của Material 3
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Mở màn hình Thêm tài liệu */ },
                shape = CircleShape,
                containerColor = uthBlue,
                contentColor = Color.White,
                modifier = Modifier.padding(top = 40.dp) // Kéo FAB xuống một chút để đè lên thanh điều hướng
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        // Nội dung của từng màn hình sẽ hiển thị ở đây dựa vào selectedItem
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (items[selectedItem]) {
                BottomNavItem.Home -> HomeScreen()
                BottomNavItem.Library -> LibraryScreen()
                BottomNavItem.Schedule -> ScheduleScreen()
                BottomNavItem.Profile -> ProfileScreen()
                BottomNavItem.Empty -> {} // Không làm gì cả, đây là khoảng trống cho FAB
            }
        }
    }
}