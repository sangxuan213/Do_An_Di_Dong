package com.example.uthdocument.domain.model

/** Một môn học. Tài liệu thuộc về môn học qua Document.courseId. */
data class Course(
    val id: String,
    val code: String,      // Mã môn, vd "KTVT"
    val name: String,      // Tên môn
    val category: Category // Khối ngành
)
