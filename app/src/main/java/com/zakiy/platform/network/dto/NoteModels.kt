package com.zakiy.platform.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** دفتر الملاحظات - حساب فردي بس (الباك إند يرفض أي حساب مؤسسي عبر
 * require_personal_account). كل ملاحظة نصية أو تو-دو ليست، وممكن تنتمي
 * لمجلد أو تبقى بدون مجلد. */

@Serializable
data class NoteFolder(
    val id: String,
    val name: String,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class NoteFoldersResponse(val folders: List<NoteFolder> = emptyList())

@Serializable
data class ChecklistItem(
    val text: String,
    val done: Boolean = false,
)

/** نسخة مختصرة لقائمة الملاحظات - الباك إند ما يرجّع content/checklist_items
 * بمسار القائمة توفيرًا لحجم الرد */
@Serializable
data class NoteSummary(
    val id: String,
    @SerialName("folder_id") val folderId: String? = null,
    val title: String,
    @SerialName("note_type") val noteType: String,
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class NotesResponse(val notes: List<NoteSummary> = emptyList())

/** النسخة الكاملة - تُستخدم لفتح/تحديث ملاحظة وحدة (فيها content/checklist_items) */
@Serializable
data class NoteDetail(
    val id: String,
    @SerialName("folder_id") val folderId: String? = null,
    val title: String = "",
    val content: String = "",
    @SerialName("note_type") val noteType: String = "text",
    @SerialName("checklist_items") val checklistItems: List<ChecklistItem> = emptyList(),
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class CreateNoteRequest(
    val title: String = "",
    val content: String = "",
    @SerialName("note_type") val noteType: String = "text",
    @SerialName("folder_id") val folderId: String? = null,
)

/** تحديث جزئي - كل الحقول null افتراضيًا وتُحذف تلقائيًا وقت التسلسل
 * (Json { explicitNulls = false } بـ NetworkModule)، فقط الحقول المُمرَّرة
 * فعليًا توصل الباك إند. استثناء وحيد: folderId - نص فاضي "" صراحة يعني
 * "بدون مجلد" (امسح المجلد)، بينما null يعني "لا تغيّره إطلاقًا" - null
 * يُحذف بالتسلسل فما يوصل الباك إند أصلًا، فما نقدر نميّز "امسحه" عنه إلا
 * بقيمة غير null فعلية زي النص الفاضي (الباك إند يفسّرها خصيصًا كـ "امسح
 * المجلد" - راجع update_note بالباك إند). */
@Serializable
data class UpdateNoteRequest(
    val title: String? = null,
    val content: String? = null,
    @SerialName("folder_id") val folderId: String? = null,
    @SerialName("note_type") val noteType: String? = null,
    @SerialName("checklist_items") val checklistItems: List<ChecklistItem>? = null,
    @SerialName("is_pinned") val isPinned: Boolean? = null,
)
