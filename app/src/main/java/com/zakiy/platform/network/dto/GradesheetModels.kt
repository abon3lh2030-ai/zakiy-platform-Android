package com.zakiy.platform.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** كشف الدرجات - معلم بس (نفس /api/teacher/gradesheet بالموقع بالضبط).
 * المشاركة/المهام الأدائية يحطهم المعلم يدويًا، والواجبات/الاختبارات تُحسب
 * تلقائيًا من درجاتها الموجودة أصلًا (قد تكون null لو ما فيه تصحيح لسا)،
 * والمجموع يُحسب لحظيًا بالباك إند - المعلم ما يقدر يعدّله مباشرة أبدًا. */
@Serializable
data class GradesheetStudentRow(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("full_name") val fullName: String? = null,
    val participation: Double = 0.0,
    @SerialName("performance_tasks") val performanceTasks: Double = 0.0,
    @SerialName("assignments_avg") val assignmentsAvg: Double? = null,
    @SerialName("assignments_count") val assignmentsCount: Int = 0,
    @SerialName("quizzes_avg") val quizzesAvg: Double? = null,
    @SerialName("quizzes_count") val quizzesCount: Int = 0,
    val total: Double = 0.0,
)

@Serializable
data class GradesheetResponse(val students: List<GradesheetStudentRow> = emptyList())

/** جسم PATCH صف كشف الدرجات - الحقلان اختياريان (يُرسل اللي تغيّر بس). */
@Serializable
data class UpdateGradesheetRowRequest(
    @SerialName("class_id") val classId: String,
    val participation: Double? = null,
    @SerialName("performance_tasks") val performanceTasks: Double? = null,
)

/** رد PATCH - صف class_participation_grades الخام (بدون المجموع/متوسطات
 * الواجبات والاختبارات) - لازم نعيد تحميل الكشف كامل بعد الحفظ لتحديثها. */
@Serializable
data class GradesheetRowUpdateResponse(
    @SerialName("class_id") val classId: String,
    @SerialName("student_id") val studentId: String,
    val participation: Double? = null,
    @SerialName("performance_tasks") val performanceTasks: Double? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val id: String? = null,
)

/** رد تصدير كشف الدرجات - رابط موقّع من Supabase صالح لمدة ساعة، لملف
 * PDF/CSV جاهز يُنزَّل مباشرة (نعرضه كـ QR كمان عشان مسح الجوال الثاني). */
@Serializable
data class GradesheetExportResponse(val url: String)
