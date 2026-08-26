package com.zakiy.platform.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/** مدرستي: اختصار لموقع مدرستي الرسمي (بدون أي تكامل بيانات) + أدوات ذكيّ
 * الذكية المستقلة للمعلم (تحضير درس/نشاط إثرائي/تحليل نتائج) والطالب (مساعد
 * واجب/خطة مذاكرة) - متاحة لأي حساب مسجّل دخول، فردي أو مؤسسي، بدون أي
 * تقييد دور (نفس سلوك زر السايد بار "مدرستي" بالموقع بالضبط). كل أداة توليد
 * ترجع content_raw كنص خام محتمل ملفوف بـ ```json fences، نفس الشكل اللي
 * الباك إند يرجعه لتوليد اختبار المذاكرة الفردية. */

@PublishedApi
internal val madrasatiJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

/** يشيل ```json fences (إن وجدت) من نص خام ويحوّله لكائن Kotlin - نفس تنظيف
 * النص المستخدم بتوليد اختبار المذاكرة الفردية (StudySummaryScreen). */
inline fun <reified T> parseAiContent(raw: String): T {
    val cleaned = raw.trim()
        .removePrefix("```json").removePrefix("```")
        .removeSuffix("```").trim()
    return madrasatiJson.decodeFromString(cleaned)
}

/** يحاول يطلّع حقل `error` من جسم رد الباك إند (Flask بيرجعه بكل الأخطاء)
 * عشان نعرض رسالة الخطأ العربية الفعلية بدل رسالة HTTP العامة. */
fun Throwable.toApiErrorMessage(fallback: String): String {
    if (this is HttpException) {
        val body = runCatching { response()?.errorBody()?.string() }.getOrNull()
        if (!body.isNullOrBlank()) {
            val extracted = runCatching {
                madrasatiJson.decodeFromString<Map<String, String>>(body)["error"]
            }.getOrNull()
            if (!extracted.isNullOrBlank()) return extracted
        }
    }
    return message?.takeIf { it.isNotBlank() } ?: fallback
}

/** رد كل نقاط التوليد - نص خام يُحلَّل محليًا بـ parseAiContent. */
@Serializable
data class GenerateContentRawResponse(@SerialName("content_raw") val contentRaw: String)

/** رد الحفظ (POST) - يكفينا الـ id، باقي الحقول ما نحتاجها فورًا. */
@Serializable
data class SavedRecordResponse(val id: String)

// ================= التحضير الذكي (معلم) ================= //

@Serializable
data class LessonPrepContent(
    val objectives: List<String> = emptyList(),
    val intro: String = "",
    val steps: List<String> = emptyList(),
    val activities: List<String> = emptyList(),
    val assessment: String = "",
    val homework: String = "",
    val enrichment: String = "",
)

@Serializable
data class GenerateLessonPrepRequest(
    val subject: String,
    @SerialName("grade_level") val gradeLevel: String,
    val unit: String? = null,
    @SerialName("lesson_title") val lessonTitle: String,
    val lang: String,
)

@Serializable
data class SaveLessonPrepRequest(
    val subject: String,
    @SerialName("grade_level") val gradeLevel: String,
    val unit: String? = null,
    @SerialName("lesson_title") val lessonTitle: String,
    val content: LessonPrepContent,
)

@Serializable
data class UpdateLessonPrepRequest(
    val subject: String? = null,
    @SerialName("grade_level") val gradeLevel: String? = null,
    val unit: String? = null,
    @SerialName("lesson_title") val lessonTitle: String? = null,
    val content: LessonPrepContent? = null,
)

@Serializable
data class LessonPrepSummary(
    val id: String,
    val subject: String,
    @SerialName("grade_level") val gradeLevel: String,
    val unit: String? = null,
    @SerialName("lesson_title") val lessonTitle: String,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class LessonPrepDetail(
    val id: String,
    val subject: String,
    @SerialName("grade_level") val gradeLevel: String,
    val unit: String? = null,
    @SerialName("lesson_title") val lessonTitle: String,
    val content: LessonPrepContent,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class LessonPrepListResponse(val preparations: List<LessonPrepSummary> = emptyList())

// ================= نشاط إثرائي (معلم - بدون حفظ) ================= //

@Serializable
data class GenerateEnrichmentRequest(
    val subject: String,
    @SerialName("grade_level") val gradeLevel: String,
    val topic: String,
    val lang: String,
)

@Serializable
data class EnrichmentContent(
    val title: String = "",
    val description: String = "",
    val instructions: List<String> = emptyList(),
    @SerialName("materials_needed") val materialsNeeded: String? = null,
)

// ================= تحليل نتائج الطلاب (معلم - بدون حفظ) ================= //

@Serializable
data class GenerateResultsAnalysisRequest(
    @SerialName("raw_results") val rawResults: String,
    val lang: String,
)

@Serializable
data class ResultsAnalysisContent(
    @SerialName("overall_summary") val overallSummary: String = "",
    val strengths: List<String> = emptyList(),
    val weaknesses: List<String> = emptyList(),
    @SerialName("at_risk_students") val atRiskStudents: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
)

// ================= مساعد الواجب الذكي (طالب) ================= //

@Serializable
data class PracticeQuestion(
    val question: String = "",
    val answer: String = "",
)

@Serializable
data class HomeworkHelpContent(
    val explanation: String = "",
    @SerialName("worked_example") val workedExample: String = "",
    @SerialName("practice_questions") val practiceQuestions: List<PracticeQuestion> = emptyList(),
    val tips: String = "",
)

@Serializable
data class GenerateHomeworkHelpRequest(
    val subject: String,
    @SerialName("grade_level") val gradeLevel: String,
    val topic: String,
    val lang: String,
)

@Serializable
data class SaveHomeworkHelpRequest(
    val subject: String,
    @SerialName("grade_level") val gradeLevel: String,
    val topic: String,
    val content: HomeworkHelpContent,
)

@Serializable
data class HomeworkHelpSummary(
    val id: String,
    val subject: String,
    @SerialName("grade_level") val gradeLevel: String,
    val topic: String,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class HomeworkHelpDetail(
    val id: String,
    val subject: String,
    @SerialName("grade_level") val gradeLevel: String,
    val topic: String,
    val content: HomeworkHelpContent,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class HomeworkHelpListResponse(val sessions: List<HomeworkHelpSummary> = emptyList())

// ================= خطة مذاكرة ذكية (طالب) ================= //

@Serializable
data class StudyDay(
    @SerialName("date_label") val dateLabel: String = "",
    val tasks: List<String> = emptyList(),
)

@Serializable
data class StudyPlanContent(
    val days: List<StudyDay> = emptyList(),
    @SerialName("general_tips") val generalTips: String = "",
)

@Serializable
data class GenerateStudyPlanRequest(
    val subjects: String,
    @SerialName("exam_date") val examDate: String? = null,
    @SerialName("hours_per_day") val hoursPerDay: Double? = null,
    val lang: String,
)

@Serializable
data class SaveStudyPlanRequest(
    val subjects: String,
    @SerialName("exam_date") val examDate: String? = null,
    @SerialName("hours_per_day") val hoursPerDay: Double? = null,
    val content: StudyPlanContent,
)

@Serializable
data class StudyPlanSummary(
    val id: String,
    val subjects: String,
    @SerialName("exam_date") val examDate: String? = null,
    @SerialName("hours_per_day") val hoursPerDay: Double? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class StudyPlanDetail(
    val id: String,
    val subjects: String,
    @SerialName("exam_date") val examDate: String? = null,
    @SerialName("hours_per_day") val hoursPerDay: Double? = null,
    val content: StudyPlanContent,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class StudyPlanListResponse(val plans: List<StudyPlanSummary> = emptyList())
