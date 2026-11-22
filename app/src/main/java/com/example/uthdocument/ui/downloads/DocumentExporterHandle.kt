package com.example.uthdocument.ui.downloads

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uthdocument.domain.model.Document

class DocumentExporterHandle(
    /** id tài liệu đang được tải về / lưu (null nếu rảnh), để hiện vòng xoay ở đúng nút. */
    val workingId: String?,
    val export: (Document) -> Unit
)

/**
 * Dùng ở bất kỳ màn hình nào có nút "Tải về": lo luôn việc hiện thông báo kết quả
 * và (Android 9 trở xuống) mở hộp thoại chọn nơi lưu.
 */
@Composable
fun rememberDocumentExporter(viewModel: DocumentExportViewModel = viewModel()): DocumentExporterHandle {
    val context = LocalContext.current
    val state = viewModel.state.collectAsState().value

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) viewModel.saveToUri(uri) else viewModel.cancel()
    }

    LaunchedEffect(state) {
        when (state) {
            is ExportState.NeedsLocation -> saveLauncher.launch(state.suggestedName)
            is ExportState.Done -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.consume()
            }
            is ExportState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.consume()
            }
            else -> Unit
        }
    }

    return DocumentExporterHandle(
        workingId = (state as? ExportState.Working)?.documentId,
        export = viewModel::export
    )
}
