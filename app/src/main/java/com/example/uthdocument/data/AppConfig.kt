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

    /**
     * Địa chỉ Cloudflare Worker đẩy thông báo tin nhắn (xem cloudflare-worker/README.md), ví dụ
     * "https://uth-chat-push.ten-ban.workers.dev". Để trống = không đẩy; app vẫn báo tin nhắn
     * bằng cách cũ (tức thì khi app còn chạy nền, trễ tối đa ~15 phút khi app bị tắt hẳn).
     */
    const val PUSH_WORKER_URL = "https://uth-chat-push.tuilasang2006.workers.dev"

    /** Nếu người dùng nhập mã số (không có @) thì ghép thêm đuôi này để thành email đăng nhập. */
    const val EMAIL_DOMAIN = "ut.edu.vn"
}
