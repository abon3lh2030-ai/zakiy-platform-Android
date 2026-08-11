package com.zakiy.platform.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserSearchResult(
    @SerialName("user_id") val userId: String,
    val username: String,
)

@Serializable
data class UserSearchResponse(val users: List<UserSearchResult> = emptyList())

@Serializable
data class FriendSummary(
    @SerialName("user_id") val userId: String,
    val username: String,
)

@Serializable
data class FriendsResponse(val friends: List<FriendSummary> = emptyList())

@Serializable
data class FriendRequestItem(
    val id: String,
    @SerialName("from_user_id") val fromUserId: String,
    val username: String,
)

@Serializable
data class FriendRequestsResponse(val requests: List<FriendRequestItem> = emptyList())

@Serializable
data class ProfilePerformanceSummary(
    @SerialName("attempts_count") val attemptsCount: Int = 0,
    @SerialName("avg_score") val avgScore: Int = 0,
    @SerialName("current_streak") val currentStreak: Int = 0,
)

@Serializable
data class ProfileResponse(
    @SerialName("user_id") val userId: String,
    val username: String,
    val bio: String? = null,
    @SerialName("school_name") val schoolName: String? = null,
    val role: String? = null,
    @SerialName("is_private") val isPrivate: Boolean = false,
    @SerialName("is_owner") val isOwner: Boolean = false,
    val performance: ProfilePerformanceSummary? = null,
    val archive: List<SessionArchiveItem>? = null,
)

@Serializable
data class ConversationSummary(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("last_message") val lastMessage: String? = null,
    @SerialName("last_at") val lastAt: String? = null,
    @SerialName("unread_count") val unreadCount: Int = 0,
)

@Serializable
data class ConversationsResponse(val conversations: List<ConversationSummary> = emptyList())

@Serializable
data class DirectMessage(
    val id: String? = null,
    @SerialName("sender_id") val senderId: String,
    @SerialName("recipient_id") val recipientId: String,
    val body: String,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class MessageThreadResponse(val messages: List<DirectMessage> = emptyList())

@Serializable
data class SendMessageRequest(
    @SerialName("recipient_id") val recipientId: String,
    val body: String,
)

@Serializable
data class NotificationItem(
    val id: String? = null,
    val type: String,
    val title: String,
    val body: String? = null,
    @SerialName("related_class_id") val relatedClassId: String? = null,
    @SerialName("related_room_code") val relatedRoomCode: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("read_at") val readAt: String? = null,
)

@Serializable
data class NotificationsResponse(
    val notifications: List<NotificationItem> = emptyList(),
    @SerialName("unread_count") val unreadCount: Int = 0,
)

@Serializable
data class BroadcastRequest(val body: String, @SerialName("class_id") val classId: String? = null)

@Serializable
data class BroadcastResponse(@SerialName("sent_to") val sentTo: Int)
