package com.example.uthdocument.data.repository

import android.content.Context
import com.example.uthdocument.domain.repository.DownloadsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Theo dõi thư mục filesDir/documents/ (nơi DocumentFileRepository lưu tệp tải về, dạng <id>.<đuôi>).
 * Là singleton để mọi màn hình cùng thấy một danh sách.
 */
class LocalDownloadsRepository private constructor(context: Context) : DownloadsRepository {

    private val dir = File(context.filesDir, "documents")

    private val _downloaded = MutableStateFlow(scan())
    override val downloaded: StateFlow<Map<String, Long>> = _downloaded.asStateFlow()

    override fun refresh() {
        _downloaded.value = scan()
    }

    override suspend fun remove(documentId: String) {
        withContext(Dispatchers.IO) {
            dir.listFiles()
                ?.filter { it.name.substringBeforeLast('.') == documentId } // gồm cả tệp .part dở dang
                ?.forEach { it.delete() }
        }
        refresh()
    }

    private fun scan(): Map<String, Long> =
        dir.listFiles().orEmpty()
            .filter { it.isFile && !it.name.endsWith(".part") && it.length() > 0 }
            .associate { it.name.substringBeforeLast('.') to it.length() }

    companion object {
        @Volatile
        private var instance: LocalDownloadsRepository? = null

        fun getInstance(context: Context): LocalDownloadsRepository =
            instance ?: synchronized(this) {
                instance ?: LocalDownloadsRepository(context.applicationContext).also { instance = it }
            }
    }
}
