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

    /** Nếu người dùng nhập mã số (không có @) thì ghép thêm đuôi này để thành email đăng nhập. */
    const val EMAIL_DOMAIN = "ut.edu.vn"
}
