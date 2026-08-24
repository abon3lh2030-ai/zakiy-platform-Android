package com.zakiy.platform.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** الاختبارات - معلم/طالب بس (نفس تقييد دفتر الواجبات، الباك إند يرفض أي
 * حساب فردي عبر require_role). سؤال يحمل 3 أنواع: mcq (اختيارات) /
 * true_false (صح وخطأ) / essay (مقالي). عنصر القائمة يحمل حقول المعلم
 * (submittedCount/totalCount) وحقول الطالب (submitted/grade) بنفس الـ data
 * class - كل جهة تستخدم اللي يخصها بس، نفس نمط AssignmentSummary. */

@Serializable
data class QuizQuestionInput(
    @SerialName("question_type") val questionType: String,
    @SerialName("question_text") val questionText: String,
    val choices: List<String>? = null,
    @SerialName("correct_answer") val correctAnswer: String? = null,
)

@Serializable
data class CreateQuizRequest(
    @SerialName("class_id") val classId: String,
    val subject: String,
    val title: String,
    @SerialName("time_limit_minutes") val timeLimitMinutes: Int,
    val questions: List<QuizQuestionInput>,
)

@Serializable
data class UpdateQuizRequest(
    val subject: String? = null,
    val title: String? = null,
    @SerialName("time_limit_minutes") val timeLimitMinutes: Int? = null,
    val questions: List<QuizQuestionInput>? = null,
)

@Serializable
data class QuizSummary(
    val id: String,
    @SerialName("teacher_id") val teacherId: String? = null,
    @SerialName("class_id") val classId: String? = null,
    val subject: String,
    val title: String,
    @SerialName("time_limit_minutes") val timeLimitMinutes: Int,
    @SerialName("is_published") val isPublished: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("class_name") val className: String? = null,
    @SerialName("submitted_count") val submittedCount: Int? = null,
    @SerialName("total_count") val totalCount: Int? = null,
    val submitted: Boolean? = null,
    @SerialName("is_graded") val isGraded: Boolean? = null,
    val score: Int? = null,
    @SerialName("total_questions") val totalQuestions: Int? = null,
    val grade: String? = null,
)

@Serializable
data class TeacherQuizzesResponse(val quizzes: List<QuizSummary> = emptyList())

@Serializable
data class StudentQuizzesResponse(val quizzes: List<QuizSummary> = emptyList())

@Serializable
data class QuizQuestionDetail(
    val id: String,
    @SerialName("order_index") val orderIndex: Int,
    @SerialName("question_type") val questionType: String,
    @SerialName("question_text") val questionText: String,
    val choices: List<String>? = null,
    @SerialName("correct_answer") val correctAnswer: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class QuizStudentStatus(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("full_name") val fullName: String? = null,
    val submitted: Boolean = false,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("auto_submitted") val autoSubmitted: Boolean = false,
    val answers: Map<String, String>? = null,
    @SerialName("is_graded") val isGraded: Boolean = false,
    val score: Int? = null,
    @SerialName("total_questions") val totalQuestions: Int? = null,
    val grade: String? = null,
)

/** تفصيل اختبار من جهة المعلم - يحمل الأسئلة كاملة (بما فيها الإجابة
 * الصحيحة) وحالة كل طالب بالفصل. */
@Serializable
data class TeacherQuizDetail(
    val id: String,
    @SerialName("teacher_id") val teacherId: String? = null,
    @SerialName("class_id") val classId: String? = null,
    val subject: String,
    val title: String,
    @SerialName("time_limit_minutes") val timeLimitMinutes: Int,
    @SerialName("is_published") val isPublished: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val questions: List<QuizQuestionDetail> = emptyList(),
    val students: List<QuizStudentStatus> = emptyList(),
)

/** سؤال بصيغة الطالب - بدون correct_answer، الباك إند ما يعرضه إطلاقًا لهذا
 * المسار. */
@Serializable
data class StudentQuizQuestion(
    val id: String,
    @SerialName("order_index") val orderIndex: Int,
    @SerialName("question_type") val questionType: String,
    @SerialName("question_text") val questionText: String,
    val choices: List<String>? = null,
)

@Serializable
data class QuizAttemptDto(
    val id: String,
    @SerialName("quiz_id") val quizId: String? = null,
    @SerialName("student_id") val studentId: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("auto_submitted") val autoSubmitted: Boolean = false,
    val answers: Map<String, String>? = null,
    @SerialName("is_graded") val isGraded: Boolean = false,
    val score: Int? = null,
    @SerialName("total_questions") val totalQuestions: Int? = null,
    val grade: String? = null,
    @SerialName("graded_at") val gradedAt: String? = null,
)

/** تفصيل اختبار من جهة الطالب - `attempt` تبقى null لو ما بدأ الاختبار
 * لحد الآن (يبدأه عبر /start). */
@Serializable
data class StudentQuizDetail(
    val id: String,
    @SerialName("class_id") val classId: String? = null,
    val subject: String,
    val title: String,
    @SerialName("time_limit_minutes") val timeLimitMinutes: Int,
    @SerialName("is_published") val isPublished: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    val questions: List<StudentQuizQuestion> = emptyList(),
    val attempt: QuizAttemptDto? = null,
)

@Serializable
data class SubmitQuizRequest(
    val answers: Map<String, String>,
    @SerialName("auto_submitted") val autoSubmitted: Boolean,
)

@Serializable
data class GradeAttemptRequest(val grade: String)
