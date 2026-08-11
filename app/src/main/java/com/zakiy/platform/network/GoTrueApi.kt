package com.zakiy.platform.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

@Serializable
data class GoTrueUser(
    val id: String,
    val email: String? = null,
    @SerialName("user_metadata") val userMetadata: Map<String, String?> = emptyMap(),
)

@Serializable
data class GoTrueSession(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long = 3600,
    val user: GoTrueUser,
)

@Serializable
data class PasswordGrantRequest(val email: String, val password: String)

@Serializable
data class RefreshGrantRequest(@SerialName("refresh_token") val refreshToken: String)

@Serializable
data class SignUpRequest(val email: String, val password: String, val data: Map<String, String> = emptyMap())

@Serializable
data class UpdateUserRequest(val password: String? = null, val data: Map<String, String>? = null)

/** نداءات مباشرة لـ Supabase Auth (GoTrue) - نفس النمط المستخدم بالضبط
 * بالموقع (@supabase/supabase-js) وiOS (supabase-swift)، بمفتاح anon
 * العام - محمي بـ RLS مو بالسرية، آمن يُشحن بتطبيق عميل. */
interface GoTrueApi {
    @Headers("Content-Type: application/json")
    @POST("auth/v1/token?grant_type=password")
    suspend fun signInWithPassword(@Body body: PasswordGrantRequest): GoTrueSession

    @Headers("Content-Type: application/json")
    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refreshSession(@Body body: RefreshGrantRequest): GoTrueSession

    @Headers("Content-Type: application/json")
    @POST("auth/v1/signup")
    suspend fun signUp(@Body body: SignUpRequest): GoTrueSession

    @Headers("Content-Type: application/json")
    @PUT("auth/v1/user")
    suspend fun updateUser(@retrofit2.http.Header("Authorization") bearer: String, @Body body: UpdateUserRequest): GoTrueUser

    @POST("auth/v1/logout?scope=local")
    suspend fun signOut(@retrofit2.http.Header("Authorization") bearer: String, @Query("scope") scope: String = "local")
}
