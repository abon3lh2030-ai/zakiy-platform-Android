package com.zakiy.platform.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** دفتر الواجبات - معلم/طالب بس (الباك إند يرفض أي حساب فردي عبر
 * require_role). عنصر بالقائمة يحمل حقول المعلم (submittedCount/totalCount)
 * وحقول الطالب (submitted/grade) بنفس الـ data class - كل جهة تستخدم اللي
 * يخصها بس، الباك إند يرجّع مجموعة حقول مختلفة حسب الـ endpoint. */

@Serializable
data class AssignmentSummary(
    val id: String,
    val subject: String,
    val title: String,
    @SerialName("class_id") val classId: String? = null,
    @SerialName("class_name") val className: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("submitted_count") val submittedCount: Int? = null,
    @SerialName("total_count") val totalCount: Int? = null,
    val submitted: Boolean? = null,
    val grade: String? = null,
)

@Serializable
data class AssignmentsResponse(val assignments: List<AssignmentSummary> = emptyList())

@Serializable
data class AssignmentStudentStatus(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("full_name") val fullName: String? = null,
    val submitted: Boolean = false,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    val note: String? = null,
    val grade: String? = null,
)

@Serializable
data class AssignmentSubmission(
    @SerialName("file_name") val fileName: String,
    val note: String? = null,
    @SerialName("submitted_at") val submittedAt: String,
    val grade: String? = null,
)

/** تفصيل واجب وحد - `students` من مسار المعلم بس، `submission` من مسار
 * الطالب بس (الآخر يبقى null دايمًا حسب مين طلبها). */
@Serializable
data class AssignmentDetail(
    val id: String,
    val subject: String,
    val title: String,
    val content: String = "",
    @SerialName("class_id") val classId: String? = null,
    @SerialName("target_student_id") val targetStudentId: String? = null,
    val students: List<AssignmentStudentStatus>? = null,
    val submission: AssignmentSubmission? = null,
)

@Serializable
data class CreateAssignmentRequest(
    @SerialName("class_id") val classId: String,
    @SerialName("target_student_id") val targetStudentId: String? = null,
    val subject: String,
    val title: String,
    val content: String = "",
)

@Serializable
data class GradeRequest(val grade: String)

@Serializable
data class FileUrlResponse(val url: String)
