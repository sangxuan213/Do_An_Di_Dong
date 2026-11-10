package com.example.uthdocument.ui.add

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uthdocument.domain.model.Category
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
            OutlinedButton(
                onClick = { picker.launch(SUPPORTED_MIME_TYPES) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AttachFile, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Chọn tệp (PDF, DOCX, PPTX)")
            }

            if (state.type != null) {
                val sizeMb = state.sizeBytes / 1024.0 / 1024.0
                Text(
                    text = "${state.fileName}\n${state.type.extension} • ${"%.1f".format(sizeMb)} MB",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Tiêu đề") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(text = "Danh mục", fontWeight = FontWeight.SemiBold)
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
                enabled = state.uri != null && state.title.isNotBlank() && !state.isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = UthBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Lưu tài liệu")
                }
            }
        }
    }
}
