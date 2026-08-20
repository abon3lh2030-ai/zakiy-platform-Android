package com.zakiy.platform.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/** يحوّل Uri (من منتقي ملفات النظام) إلى جزء Multipart جاهز للرفع - ينسخ
 * الملف لملف مؤقت بالكاش أول (content:// URIs ما تُقرأ مباشرة كـ File). */
fun uriToMultipart(context: Context, uri: Uri, fieldName: String): MultipartBody.Part {
    val fileName = queryDisplayName(context, uri) ?: "upload_${System.currentTimeMillis()}.pdf"
    val tempFile = File(context.cacheDir, fileName)
    context.contentResolver.openInputStream(uri)?.use { input ->
        tempFile.outputStream().use { output -> input.copyTo(output) }
    }
    val requestBody = tempFile.asRequestBody("application/pdf".toMediaTypeOrNull())
    return MultipartBody.Part.createFormData(fieldName, fileName, requestBody)
}

/** نفس مبدأ uriToMultipart بس بدون افتراض PDF - تستخدمها ميزات ملفات عامة
 * (زي تسليم واجب) اللي ممكن يجي منها أي نوع ملف، نوع المحتوى يُكتشف من
 * الـ Uri نفسه بدل ما يكون ثابت. */
fun uriToMultipartAny(context: Context, uri: Uri, fieldName: String): MultipartBody.Part {
    val fileName = queryDisplayName(context, uri) ?: "upload_${System.currentTimeMillis()}"
    val tempFile = File(context.cacheDir, fileName)
    context.contentResolver.openInputStream(uri)?.use { input ->
        tempFile.outputStream().use { output -> input.copyTo(output) }
    }
    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
    val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
    return MultipartBody.Part.createFormData(fieldName, fileName, requestBody)
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull()
}
