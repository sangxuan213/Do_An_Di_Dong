package com.example.uthdocument.ui.add

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uthdocument.data.AppConfig
import com.example.uthdocument.domain.model.Category
import com.example.uthdocument.domain.model.Course
import com.example.uthdocument.domain.model.DocumentType
import com.example.uthdocument.ui.theme.UthBlue

private val SUPPORTED_MIME_TYPES = arrayOf(
    "application/pdf",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddDocumentScreen(
    onBack: () -> Unit,
    viewModel: AddDocumentViewModel = viewModel()
) {
    val state = viewModel.uiState.collectAsState().value
    val courses = viewModel.courses.collectAsState().value

    // Lưu xong thì quay về
    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    // Trình chọn tệp của hệ thống (SAF) - không cần xin quyền truy cập bộ nhớ
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.onFilePicked(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thêm tài liệu") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ---- Chọn cách thêm ----
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.mode == SourceMode.DEVICE,
                    onClick = { viewModel.onModeSelected(SourceMode.DEVICE) },
                    label = { Text("Chọn tệp từ máy") }
                )
                FilterChip(
                    selected = state.mode == SourceMode.LINK,
                    onClick = { viewModel.onModeSelected(SourceMode.LINK) },
                    label = { Text("Dán đường dẫn") }
                )
            }

            if (state.mode == SourceMode.DEVICE) {
                // ---- Từ thiết bị ----
                OutlinedButton(
                    onClick = { picker.launch(SUPPORTED_MIME_TYPES) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Chọn tệp (PDF, DOCX, PPTX)")
                }
                if (state.uri != null) {
                    val sizeMb = state.sizeBytes / 1024.0 / 1024.0
                    Text(
                        text = "${state.fileName}\n${state.type.extension} • ${"%.1f".format(sizeMb)} MB",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
                if (!AppConfig.USE_FIREBASE_STORAGE) {
                    Text(
                        text = "Tệp tối đa 10 MB. Tệp lớn hơn hãy chọn \"Dán đường dẫn\".",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            } else {
                // ---- Dán đường dẫn ----
                HowToCard()
                OutlinedTextField(
                    value = state.link,
                    onValueChange = viewModel::onLinkChange,
                    label = { Text("Đường dẫn tệp (https)") },
                    placeholder = { Text("Dán liên kết Google Drive, GitHub, Dropbox...") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.linkSource != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E9E5B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Đã nhận dạng: ${state.linkSource}", fontSize = 13.sp, color = Color(0xFF2E9E5B))
                    }
                }
                Text(text = "Loại tệp", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DocumentType.entries.forEach { type ->
                        FilterChip(
                            selected = state.type == type,
                            onClick = { viewModel.onTypeSelected(type) },
                            label = { Text(type.extension) }
                        )
                    }
                }
            }

            // ---- Thông tin chung ----
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Tiêu đề") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            CourseInputSection(
                courseInput = state.courseInput,
                onCourseInputChange = viewModel::onCourseInputChange,
                courses = courses
            )

            Text(text = "Danh mục chuyên ngành", fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.entries.forEach { category ->
                    FilterChip(
                        selected = state.category == category,
                        onClick = { viewModel.onCategorySelected(category) },
                        label = { Text(category.displayName) }
                    )
                }
            }

            if (state.error != null) {
                Text(text = state.error, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = viewModel::save,
                enabled = state.canSave,
                colors = ButtonDefaults.buttonColors(containerColor = UthBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.mode == SourceMode.DEVICE) "Đang tải lên..." else "Đang kiểm tra liên kết...")
                } else {
                    Text("Lưu tài liệu")
                }
            }
        }
    }
}

/** Hướng dẫn ngắn cách lấy đường dẫn đúng. */
@Composable
private fun HowToCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F1FF))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Cách lấy đường dẫn", fontWeight = FontWeight.SemiBold, color = Color(0xFF002D58))
            Text(
                "• Google Drive: bấm Chia sẻ → đặt \"Bất kỳ ai có đường liên kết\" → Sao chép liên kết.\n" +
                    "• GitHub: mở tệp trong kho (repo công khai) rồi dán địa chỉ trang.\n" +
                    "• Dropbox hoặc liên kết tải trực tiếp .pdf, .docx, .pptx cũng dùng được.\n" +
                    "App sẽ kiểm tra liên kết trước khi lưu.",
                fontSize = 12.sp,
                color = Color(0xFF002D58)
            )
        }
    }
}

@Composable
private fun CourseInputSection(
    courseInput: String,
    onCourseInputChange: (String) -> Unit,
    courses: List<Course>
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = courseInput,
            onValueChange = onCourseInputChange,
            label = { Text("Môn học / Mã môn (Nhập tự do)") },
            placeholder = { Text("Ví dụ: KTVT, TT0305...") },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Gợi ý môn học")
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        DropdownMenu(
            expanded = expanded && courses.isNotEmpty(),
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = false)
        ) {
            courses.forEach { course ->
                DropdownMenuItem(
                    text = { Text("${course.code} • ${course.name}") },
                    onClick = {
                        // chỉ điền mã môn để khớp với mã môn của tài liệu
                        onCourseInputChange(course.code)
                        expanded = false
                    }
                )
            }
        }
    }
}
