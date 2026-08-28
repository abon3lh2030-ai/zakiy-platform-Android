package com.zakiy.platform.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** مختبر العلوم - مستكشف الأحياء: مساعد ذكيّ مستمر طول الجلسة + تلخيص
 * نهاية الجلسة. نفس شكل الطلبات/الردود اللي الموقع يستخدمها بالضبط
 * (website/src/js/30-science-lab.js: slSendChatMessage / summary handler). */

@Serializable
data class ScienceLabChatRequest(
    val message: String,
    val lang: String,
    val context: String = "",
    @SerialName("interaction_id") val interactionId: String? = null,
)

@Serializable
data class ScienceLabChatResponse(
    val reply: String,
    @SerialName("interaction_id") val interactionId: String? = null,
)

@Serializable
data class ScienceLabSummaryRequest(
    val log: List<String>,
    val lang: String,
)

@Serializable
data class ScienceLabSummaryResponse(val summary: String)
