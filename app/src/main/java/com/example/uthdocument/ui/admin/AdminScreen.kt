package com.example.uthdocument.ui.admin

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uthdocument.domain.model.Category
import com.example.uthdocument.domain.model.Document
import com.example.uthdocument.domain.model.UserRole
import com.example.uthdocument.ui.util.formatTimeAgo
import com.example.uthdocument.ui.util.normalizedForSearch

private val UthNavy = Color(0xFF002D58)
private val UthTeal = Color(0xFF007B7A)
private val UthBackground = Color(0xFFF8FAFC)
private val DangerRed = Color(0xFFD32F2F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    onNavigateToAddDocument: () -> Unit,
    onDocumentClick: (String) -> Unit = {},
    viewModel: AdminViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    // Thông báo kết quả (thành công hoặc lỗi)
    LaunchedEffect(state.message, state.error) {
        val text = state.message ?: state.error
        if (text != null) {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản trị hệ thống UTH", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::loadUsers) {
                        Icon(Icons.Default.Refresh, contentDescription = "Làm mới", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = UthNavy)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(UthBackground)
        ) {
            TabRow(selectedTabIndex = selectedTab, containerColor = Color.White) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Tổng quan") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Tài khoản (${state.users.size})") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Tài liệu (${documents?.size ?: 0})") })
            }

            when (selectedTab) {
                0 -> OverviewTab(state, documents.orEmpty(), onNavigateToAddDocument, onDocumentClick)
                1 -> UsersTab(state, viewModel)
                2 -> DocumentsTab(state, documents, viewModel, onNavigateToAddDocument, onDocumentClick)
            }
        }
    }
}

// ============================ TỔNG QUAN ============================

@Composable
private fun OverviewTab(
    state: AdminUiState,
    documents: List<Document>,
    onAddDocument: () -> Unit,
    onDocumentClick: (String) -> Unit
) {
    val users = state.users
    val totalViews = documents.sumOf { it.viewCount }
    val roleCounts = UserRole.entries.map { role -> role to users.count { it.role == role } }
    val lockedCount = users.count { it.disabled }
    val byCategory = Category.entries.map { c -> c to documents.count { it.category == c } }.filter { it.second > 0 }
    val topViewed = documents.sortedByDescending { it.viewCount }.take(5).filter { it.viewCount > 0 }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Người dùng", users.size.toString(), UthTeal, Modifier.weight(1f))
                StatCard("Tài liệu", documents.size.toString(), UthNavy, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Lượt xem", totalViews.toString(), Color(0xFF1976D2), Modifier.weight(1f))
                StatCard("Tài khoản bị khóa", lockedCount.toString(), if (lockedCount > 0) DangerRed else Color.Gray, Modifier.weight(1f))
            }
        }

        item { SectionTitle("Người dùng theo vai trò") }
        item {
            SectionCard {
                val max = roleCounts.maxOfOrNull { it.second } ?: 0
                roleCounts.forEach { (role, count) -> BarRow(role.displayName, count, max, UthTeal) }
            }
        }

        item { SectionTitle("Tài liệu theo danh mục") }
        item {
            SectionCard {
                if (byCategory.isEmpty()) {
                    Text("Chưa có tài liệu.", color = Color.Gray, fontSize = 13.sp)
                } else {
                    val max = byCategory.maxOf { it.second }
                    byCategory.forEach { (category, count) -> BarRow(category.displayName, count, max, UthNavy) }
                }
            }
        }

        item { SectionTitle("Xem nhiều nhất") }
        item {
            SectionCard {
                if (topViewed.isEmpty()) {
                    Text("Chưa có lượt xem nào.", color = Color.Gray, fontSize = 13.sp)
                } else {
                    topViewed.forEachIndexed { index, doc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${index + 1}.", fontWeight = FontWeight.Bold, color = UthTeal, modifier = Modifier.width(24.dp))
                            TextButton(onClick = { onDocumentClick(doc.id) }, modifier = Modifier.weight(1f)) {
                                Text(doc.title, maxLines = 1, modifier = Modifier.weight(1f))
                            }
                            Text("${doc.viewCount} lượt xem", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onAddDocument,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = UthTeal),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Đăng tài liệu mới", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = color)
            Text(title, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.Bold, color = UthNavy, fontSize = 15.sp)
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

/** Một dòng biểu đồ cột ngang: nhãn, thanh tỉ lệ, con số. */
@Composable
private fun BarRow(label: String, count: Int, max: Int, color: Color) {
    val fraction = if (max <= 0) 0f else count.toFloat() / max
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, modifier = Modifier.width(110.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE8ECF2))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(count.toString(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(32.dp))
    }
}

// ============================ TÀI KHOẢN ============================

@Composable
private fun UsersTab(state: AdminUiState, viewModel: AdminViewModel) {
    val filteredUsers = remember(state.users, state.userQuery, state.roleFilter) {
        val key = state.userQuery.trim().normalizedForSearch()
        state.users.filter { user ->
            (state.roleFilter == null || user.role == state.roleFilter) &&
                (key.isEmpty() ||
                    user.fullName.normalizedForSearch().contains(key) ||
                    user.studentId.normalizedForSearch().contains(key) ||
                    user.email.normalizedForSearch().contains(key))
        }
    }

    var pendingRole by remember { mutableStateOf<Pair<AdminUserItem, UserRole>?>(null) }
    var pendingLock by remember { mutableStateOf<AdminUserItem?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.userQuery,
            onValueChange = viewModel::onUserQueryChange,
            placeholder = { Text("Tìm theo tên, email, mã số sinh viên...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = state.roleFilter == null,
                    onClick = { viewModel.onRoleFilterChange(null) },
                    label = { Text("Tất cả") }
                )
            }
            items(UserRole.entries) { role ->
                FilterChip(
                    selected = state.roleFilter == role,
                    onClick = { viewModel.onRoleFilterChange(role) },
                    label = { Text(role.displayName) }
                )
            }
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = UthTeal)
            }
        } else if (filteredUsers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Không tìm thấy tài khoản nào.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredUsers, key = { it.uid }) { user ->
                    UserCard(
                        user = user,
                        isSelf = user.uid == viewModel.currentUserId,
                        onRoleChange = { newRole -> if (newRole != user.role) pendingRole = user to newRole },
                        onToggleLock = { pendingLock = user }
                    )
                }
            }
        }
    }

    pendingRole?.let { (user, newRole) ->
        AlertDialog(
            onDismissRequest = { pendingRole = null },
            title = { Text("Đổi vai trò") },
            text = {
                Text(
                    "Đổi vai trò của ${user.fullName} từ ${user.role.displayName} sang ${newRole.displayName}?" +
                        if (newRole == UserRole.ADMIN) "\n\nQuản trị viên có toàn quyền quản lý tài khoản và tài liệu." else ""
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateUserRole(user.uid, newRole)
                    pendingRole = null
                }) { Text("Đổi") }
            },
            dismissButton = { TextButton(onClick = { pendingRole = null }) { Text("Hủy") } }
        )
    }

    pendingLock?.let { user ->
        val locking = !user.disabled
        AlertDialog(
            onDismissRequest = { pendingLock = null },
            title = { Text(if (locking) "Khóa tài khoản" else "Mở khóa tài khoản") },
            text = {
                Text(
                    if (locking) {
                        "Khóa tài khoản của ${user.fullName}? Người này sẽ bị đăng xuất ở lần mở app tiếp theo và không đăng nhập lại được cho tới khi bạn mở khóa."
                    } else {
                        "Mở khóa để ${user.fullName} dùng lại ứng dụng bình thường?"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setUserLocked(user.uid, locking)
                    pendingLock = null
                }) { Text(if (locking) "Khóa" else "Mở khóa", color = if (locking) DangerRed else UthTeal) }
            },
            dismissButton = { TextButton(onClick = { pendingLock = null }) { Text("Hủy") } }
        )
    }
}

@Composable
private fun UserCard(
    user: AdminUserItem,
    isSelf: Boolean,
    onRoleChange: (UserRole) -> Unit,
    onToggleLock: () -> Unit
) {
    var showRoleMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (user.disabled) Color(0xFFFDF0F0) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = UthTeal.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = UthTeal)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.fullName + if (isSelf) " (bạn)" else "",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = UthNavy
                    )
                    Text(text = user.email, fontSize = 12.sp, color = Color.Gray)
                }

                // Nhãn vai trò; bấm để đổi (trừ chính mình)
                Box {
                    Surface(
                        onClick = { showRoleMenu = true },
                        enabled = !isSelf,
                        shape = RoundedCornerShape(20.dp),
                        color = when (user.role) {
                            UserRole.ADMIN -> Color(0xFFFFEBEE)
                            UserRole.LECTURER -> Color(0xFFFFF3E0)
                            UserRole.STUDENT -> Color(0xFFE3F2FD)
                        }
                    ) {
                        Text(
                            text = user.role.displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (user.role) {
                                UserRole.ADMIN -> Color(0xFFD32F2F)
                                UserRole.LECTURER -> Color(0xFFE65100)
                                UserRole.STUDENT -> Color(0xFF1976D2)
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    DropdownMenu(expanded = showRoleMenu, onDismissRequest = { showRoleMenu = false }) {
                        UserRole.entries.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.displayName) },
                                onClick = {
                                    showRoleMenu = false
                                    onRoleChange(role)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (user.studentId.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = user.studentId, fontSize = 12.sp, color = Color.Gray)
                    }
                }
                if (user.faculty.isNotBlank()) {
                    Text(text = "Khoa: ${user.faculty}", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (user.disabled) {
                    Text("Đã bị khóa", color = DangerRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                if (!isSelf) {
                    TextButton(onClick = onToggleLock) {
                        Icon(
                            if (user.disabled) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (user.disabled) UthTeal else DangerRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (user.disabled) "Mở khóa" else "Khóa tài khoản",
                            color = if (user.disabled) UthTeal else DangerRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ============================ TÀI LIỆU ============================

@Composable
private fun DocumentsTab(
    state: AdminUiState,
    documents: List<Document>?,
    viewModel: AdminViewModel,
    onAddDocument: () -> Unit,
    onDocumentClick: (String) -> Unit
) {
    val usersById = remember(state.users) { state.users.associateBy { it.uid } }
    var pendingDelete by remember { mutableStateOf<Document?>(null) }

    val filtered = remember(documents, state.docQuery) {
        val key = state.docQuery.trim().normalizedForSearch()
        documents.orEmpty().filter { key.isEmpty() || it.title.normalizedForSearch().contains(key) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.docQuery,
                onValueChange = viewModel::onDocQueryChange,
                placeholder = { Text("Tìm tài liệu...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onAddDocument,
                colors = ButtonDefaults.buttonColors(containerColor = UthTeal)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Đăng tài liệu mới")
            }
        }

        Spacer(Modifier.height(8.dp))

        when {
            documents == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = UthTeal)
            }

            filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Không có tài liệu nào.", color = Color.Gray)
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { doc ->
                    val uploader = usersById[doc.uploaderId]?.fullName ?: "không rõ"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(doc.title, fontWeight = FontWeight.Bold, color = UthNavy, maxLines = 2)
                            Text(
                                "${doc.type.extension} • ${doc.category.displayName}" +
                                    if (doc.sizeInMb > 0) " • ${"%.1f".format(doc.sizeInMb)} MB" else "",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                "Người đăng: $uploader • ${doc.viewCount} lượt xem" +
                                    if (doc.uploadedAt > 0) " • ${formatTimeAgo(doc.uploadedAt)}" else "",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { onDocumentClick(doc.id) }) { Text("Mở") }
                                TextButton(onClick = { pendingDelete = doc }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Xóa", color = DangerRed, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Xóa tài liệu") },
            text = {
                Text(
                    "Xóa vĩnh viễn \"${doc.title}\" của ${usersById[doc.uploaderId]?.fullName ?: "người đăng không rõ"}? " +
                        "Mọi người sẽ không còn thấy tài liệu này và không thể hoàn tác."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDocument(doc.id)
                    pendingDelete = null
                }) { Text("Xóa", color = DangerRed) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Hủy") } }
        )
    }
}
