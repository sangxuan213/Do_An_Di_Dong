package com.example.uthdocument.data.repository

import android.util.Log
import com.example.uthdocument.domain.model.Category
import com.example.uthdocument.domain.model.Course
import com.example.uthdocument.domain.repository.CourseRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

/**
 * Danh sách môn học từ Firestore (collection "courses"):
 *   courses/{CODE}   code (mặc định = id tài liệu), name, category (ECONOMICS | LOGISTICS | LAW | MAJOR | OTHER)
 * Collection còn trống hoặc không đọc được thì dùng danh sách mặc định để app vẫn chạy.
 * Quy ước: Course.id = mã môn viết hoa, khớp với courseCode của tài liệu.
 */
class FirebaseCourseRepositoryImpl(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : CourseRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val allCourses: Flow<List<Course>> = callbackFlow {
        val registration = db.collection(COLLECTION).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener
            trySend(snapshot.documents.mapNotNull { it.toCourse() }.sortedBy { it.code })
        }
        awaitClose { registration.remove() }
    }
        .map { list -> withMajorCourses(list.ifEmpty { DEFAULT_COURSES }) }
        .catch { e ->
            Log.e(TAG, "Không đọc được collection '$COLLECTION', dùng danh sách mặc định", e)
            emit(withMajorCourses(DEFAULT_COURSES))
        }
        .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    override fun getCourses(): Flow<List<Course>> = allCourses

    override fun getCourseById(id: String): Flow<Course?> =
        allCourses.map { list -> list.find { it.id.equals(id, ignoreCase = true) } }

    /** Luôn có đủ các môn chuyên ngành; môn cùng mã trên Firestore sẽ được ưu tiên. */
    private fun withMajorCourses(list: List<Course>): List<Course> {
        val codes = list.map { it.code.uppercase() }.toSet()
        return (list + MAJOR_COURSES.filter { it.code !in codes }).sortedBy { it.code }
    }

    private fun DocumentSnapshot.toCourse(): Course? {
        val name = getString("name")?.trim().orEmpty()
        if (name.isEmpty()) return null
        val code = (getString("code")?.trim()?.ifBlank { null } ?: id).uppercase()
        val category = Category.entries.firstOrNull { it.name.equals(getString("category")?.trim(), ignoreCase = true) }
            ?: Category.OTHER
        return Course(id = code, code = code, name = name, category = category)
    }

    companion object {
        private const val TAG = "FirebaseCourses"
        private const val COLLECTION = "courses"

        /** Các môn chuyên ngành Công nghệ thông tin (kể cả học phần tự chọn). */
        val MAJOR_COURSES = listOf(
            Course("XSTKTN", "XSTKTN", "Xác suất thống kê và xử lý số liệu thực nghiệm", Category.MAJOR),
            Course("PPNC", "PPNC", "Phương pháp nghiên cứu", Category.MAJOR),
            Course("QTH", "QTH", "Quản trị học", Category.MAJOR),
            Course("NMCNTT", "NMCNTT", "Nhập môn ngành công nghệ thông tin", Category.MAJOR),
            Course("KTLT", "KTLT", "Kỹ thuật lập trình", Category.MAJOR),
            Course("CSDL", "CSDL", "Cơ sở dữ liệu", Category.MAJOR),
            Course("LTHDT", "LTHDT", "Lập trình hướng đối tượng", Category.MAJOR),
            Course("CTRR", "CTRR", "Cấu trúc rời rạc", Category.MAJOR),
            Course("MMT", "MMT", "Mạng máy tính", Category.MAJOR),
            Course("KTMT", "KTMT", "Kiến trúc máy tính", Category.MAJOR),
            Course("PLDC", "PLDC", "Pháp luật đại cương", Category.MAJOR),
            Course("KTCTML", "KTCTML", "Kinh tế chính trị Mác - Lênin", Category.MAJOR),
            Course("TKCSDL", "TKCSDL", "Thiết kế cơ sở dữ liệu", Category.MAJOR),
            Course("CNXHKH", "CNXHKH", "Chủ nghĩa xã hội khoa học", Category.MAJOR),
            Course("LTM", "LTM", "Lập trình mạng", Category.MAJOR),
            Course("ATTT", "ATTT", "An toàn thông tin", Category.MAJOR),
            Course("TTHCM", "TTHCM", "Tư tưởng Hồ Chí Minh", Category.MAJOR),
            Course("CDHTGTTM", "CDHTGTTM", "Chuyên đề Hệ thống giao thông thông minh", Category.MAJOR),
            Course("DATTCNPM", "DATTCNPM", "Đồ án thực tế công nghệ phần mềm", Category.MAJOR),
            Course("LSDCSVN", "LSDCSVN", "Lịch sử Đảng cộng sản Việt Nam", Category.MAJOR),
            Course("TTNT", "TTNT", "Trí tuệ nhân tạo", Category.MAJOR),
            Course("LTTBDD", "LTTBDD", "Lập trình thiết bị di động", Category.MAJOR),
            Course("XDPMHDT", "XDPMHDT", "XD phần mềm hướng đối tượng", Category.MAJOR),
            Course("QTM", "QTM", "Quản trị mạng", Category.MAJOR),
            Course("QTDACNTT", "QTDACNTT", "Quản trị dự án CNTT", Category.MAJOR),
            Course("ANM", "ANM", "An ninh mạng", Category.MAJOR),
            Course("XLATGMT", "XLATGMT", "Xử lý ảnh và thị giác máy tính", Category.MAJOR),
            Course("KCPM", "KCPM", "Kiểm chứng phần mềm", Category.MAJOR),
            Course("KTDL", "KTDL", "Khai thác dữ liệu", Category.MAJOR),
            Course("DTDM", "DTDM", "Điện toán đám mây", Category.MAJOR),
            Course("CNPMN", "CNPMN", "Công nghệ phần mềm nhúng", Category.MAJOR),
            Course("CDPTHTTM", "CDPTHTTM", "Chuyên đề - Phát triển hệ thống thông minh", Category.MAJOR),
            Course("CDHTMBM", "CDHTMBM", "Chuyên đề - Hệ thống mạng và bảo mật", Category.MAJOR),
            Course("CDGTTU", "CDGTTU", "Chuyên đề - Các giải thuật tối ưu", Category.MAJOR),
            Course("TTTN", "TTTN", "Thực tập tốt nghiệp", Category.MAJOR),
            Course("HKDN", "HKDN", "Học kỳ doanh nghiệp", Category.MAJOR),
            Course("KLTN", "KLTN", "Khóa luận tốt nghiệp", Category.MAJOR)
        )

        val DEFAULT_COURSES = listOf(
            Course("KTVT", "KTVT", "Kinh tế vận tải", Category.ECONOMICS),
            Course("LOG", "LOG", "Logistics cơ bản", Category.LOGISTICS),
            Course("PLHH", "PLHH", "Pháp luật hàng hải", Category.LAW),
            Course("XSTK", "XSTK", "Xác suất thống kê", Category.ECONOMICS),
            Course("QTCU", "QTCU", "Quản trị chuỗi cung ứng", Category.LOGISTICS),
            Course("LTMQT", "LTMQT", "Luật thương mại quốc tế", Category.LAW)
        )
    }
}
