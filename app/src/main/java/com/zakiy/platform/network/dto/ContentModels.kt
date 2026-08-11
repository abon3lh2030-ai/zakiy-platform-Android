package com.zakiy.platform.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UploadResponse(val message: String? = null, val filename: String)

@Serializable
data class ExtractResponse(val text: String)

@Serializable
data class SummarizeResponse(val summary: String)

@Serializable
data class GenerateQuizResponse(@SerialName("quiz_raw") val quizRaw: String)

/** سؤال اختبار واحد (نتيجة تفكيك quiz_raw - JSON نصي يرجّعه الذكاء الاصطناعي) */
@Serializable
data class QuizQuestion(
    val question: String,
    val choices: List<String>,
    @SerialName("correct_index") val correctIndex: Int,
    val explanation: String? = null,
)

@Serializable
data class QuizAttemptRequest(
    val mode: String,
    val score: Int,
    val total: Int,
    @SerialName("time_taken") val timeTaken: Int,
    @SerialName("wrong_topics") val wrongTopics: List<String> = emptyList(),
)

@Serializable
data class LibraryBook(
    val id: String,
    val title: String,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class LibraryBooksResponse(val books: List<LibraryBook>)

@Serializable
data class LibraryBookDetail(
    val id: String,
    val title: String,
    @SerialName("extracted_text") val extractedText: String,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class QuizAttempt(
    val id: String? = null,
    val mode: String? = null,
    val score: Int = 0,
    val total: Int = 0,
    @SerialName("time_taken") val timeTaken: Int = 0,
    @SerialName("wrong_topics") val wrongTopics: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class WeakTopic(val topic: String, val count: Int)

@Serializable
data class PerformanceResponse(
    val attempts: List<QuizAttempt> = emptyList(),
    @SerialName("weak_topics") val weakTopics: List<WeakTopic> = emptyList(),
    @SerialName("total_study_minutes") val totalStudyMinutes: Int = 0,
    @SerialName("current_streak") val currentStreak: Int = 0,
    @SerialName("longest_streak") val longestStreak: Int = 0,
)

@Serializable
data class SessionArchiveItem(
    val id: String? = null,
    @SerialName("room_code") val roomCode: String? = null,
    @SerialName("room_type") val roomType: String? = null,
    @SerialName("host_name") val hostName: String? = null,
    @SerialName("host_user_id") val hostUserId: String? = null,
    @SerialName("participants") val participants: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class SessionsResponse(val sessions: List<SessionArchiveItem> = emptyList())

@Serializable
data class CreateRoomRequest(
    @SerialName("room_type") val roomType: String,
    @SerialName("class_id") val classId: String? = null,
)

@Serializable
data class CreateRoomResponse(
    @SerialName("room_code") val roomCode: String,
    @SerialName("room_type") val roomType: String,
)
