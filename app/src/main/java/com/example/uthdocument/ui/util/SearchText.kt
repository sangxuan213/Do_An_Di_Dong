package com.example.uthdocument.ui.util

import java.text.Normalizer

/** Bỏ dấu + chữ thường để gõ "kinh te" vẫn tìm ra "Kinh tế". */
fun String.normalizedForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .replace('đ', 'd').replace('Đ', 'D')
        .lowercase()
