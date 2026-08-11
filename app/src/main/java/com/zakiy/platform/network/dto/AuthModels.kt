package com.zakiy.platform.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** رد GET /api/me - يحدد توجيه المستخدم بعد الدخول (حساب فردي/دور مؤسسي) */
@Serializable
data class MeResponse(
    val role: String? = null,
    @SerialName("school_id") val schoolId: String? = null,
    @SerialName("class_id") val classId: String? = null,
    @SerialName("must_change_password") val mustChangePassword: Boolean = false,
    val username: String? = null,
)

@Serializable
data class ResolveIdentifierRequest(val identifier: String)

@Serializable
data class ResolveIdentifierResponse(val email: String)

/** بيانات دخول تُنشأ مرة وحدة (حساب مدرسة/معلم/طالب) - كلمة السر تظهر مرة وحدة بس */
@Serializable
data class GeneratedCredentials(val email: String, val password: String)

/** نتيجة إعادة تعيين كلمة سر أي حساب بالمدرسة - identifier عام (بريد أو اسم مستخدم) */
@Serializable
data class AccountResetCredentials(val identifier: String, val password: String)
