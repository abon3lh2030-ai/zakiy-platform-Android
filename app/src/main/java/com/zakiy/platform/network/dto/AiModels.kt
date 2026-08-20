package com.zakiy.platform.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** المساعد الذكي - محادثات محفوظة، متاحة لأي حساب (فردي أو مؤسسي). كل
 * محادثة تقدر تكون سولفة عامة أو مربوطة بكتاب يلخّصه ذكيّ. سلسلة المحادثة
 * الفعلية مع Gemini محفوظة عند الباك إند (interaction id متسلسل)، إحنا بس
 * نعرض النصوص المحفوظة محليًا. */

@Serializable
data class AiConversationSummary(
    val id: String,
    val title: String = "",
    @SerialName("book_title") val bookTitle: String? = null,
    @SerialName("updated_at") val updatedAt: String = "",
)

@Serializable
data class AiConversationsResponse(val conversations: List<AiConversationSummary> = emptyList())

@Serializable
data class AiMessage(
    val role: String,
    val content: String,
    @SerialName("created_at") val createdAt: String? = null,
)

/** نسخة كاملة لمحادثة وحدة - نفس الشكل يُستخدم لرد الإنشاء (POST بدون
 * messages) ولرد فتح محادثة موجودة (GET، فيها messages) */
@Serializable
data class AiConversationDetail(
    val id: String,
    val title: String = "",
    @SerialName("book_title") val bookTitle: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val messages: List<AiMessage> = emptyList(),
)

/** رسالة عادية (content بس) أو طلب تلخيص كتاب (bookTitle + bookText معًا) */
@Serializable
data class SendAiMessageRequest(
    val content: String? = null,
    @SerialName("book_title") val bookTitle: String? = null,
    @SerialName("book_text") val bookText: String? = null,
    val lang: String? = null,
)

@Serializable
data class SendAiMessageResponse(val reply: String, val title: String? = null)
