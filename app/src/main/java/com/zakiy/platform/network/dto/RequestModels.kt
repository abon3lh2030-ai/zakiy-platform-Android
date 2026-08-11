package com.zakiy.platform.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// طلبات JSON بحقول اختيارية متعددة الأنواع - kotlinx.serialization ما يدعم
// Map<String, Any> مباشرة، فكل واحدة منها DTO صريح بدل map عامة

@Serializable
data class GenerateQuizRequest(
    val text: String,
    @SerialName("num_questions") val numQuestions: Int,
    val lang: String,
)

@Serializable
data class UpdateProfileRequest(
    val bio: String? = null,
    @SerialName("school_name") val schoolName: String? = null,
    @SerialName("is_private") val isPrivate: Boolean? = null,
    @SerialName("show_performance") val showPerformance: Boolean? = null,
    @SerialName("show_library") val showLibrary: Boolean? = null,
    @SerialName("show_archive") val showArchive: Boolean? = null,
    @SerialName("show_friends") val showFriends: Boolean? = null,
)

@Serializable
data class AdminUpdateSchoolRequest(
    @SerialName("max_accounts") val maxAccounts: Int? = null,
    @SerialName("subscription_status") val subscriptionStatus: String? = null,
    @SerialName("subscription_package") val subscriptionPackage: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
)

@Serializable
data class ReassignTeacherRequest(@SerialName("teacher_id") val teacherId: String? = null)

@Serializable
data class UpdateAccountRequest(
    val username: String? = null,
    @SerialName("class_id") val classId: String? = null,
)
