package com.example.uthdocument.data

object AppConfig {
    /**
     * true  = đăng nhập giả (mật khẩu nào cũng vào được) để test giao diện khi Firebase chưa sẵn sàng.
     * false = đăng nhập thật bằng Firebase Authentication.
     */
    const val USE_FAKE_AUTH = false

    /**
     * true  = tài liệu giả (4 tài liệu mẫu trong bộ nhớ).
     * false = tài liệu thật từ Firestore (collection "documents") + Storage.
     */
    const val USE_FAKE_DOCUMENTS = false

    /**
     * false = KHÔNG dùng Firebase Storage (chưa có gói Blaze): tệp tải lên từ máy (tối đa 10 MB) được cắt thành
     *         các mảnh nhỏ lưu trong Firestore, ai có app cũng xem được.
     * true  = tải tệp lên Firebase Storage (cần gói Blaze), không giới hạn 10 MB.
     */
    const val USE_FIREBASE_STORAGE = false

    /** Nếu người dùng nhập mã số (không có @) thì ghép thêm đuôi này để thành email đăng nhập. */
    const val EMAIL_DOMAIN = "ut.edu.vn"
}
