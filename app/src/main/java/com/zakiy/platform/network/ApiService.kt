package com.zakiy.platform.network

import com.zakiy.platform.network.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/** كل نقاط نهاية باك إند ذكيّ (Flask) - نفس الـ endpoints اللي الموقع
 * وتطبيق iOS يستخدمونها بالضبط. توكن Bearer يُضاف تلقائيًا لأي طلب عبر
 * NetworkModule (اقرأ TokenHolder.accessToken). */
interface ApiService {

    // ---- المحتوى (رفع/استخراج/تلخيص/اختبار) ----
    @Multipart
    @POST("api/upload")
    suspend fun upload(@Part file: MultipartBody.Part, @Part("context") context: RequestBody? = null): UploadResponse

    @POST("api/extract")
    suspend fun extract(@Body body: Map<String, String>): ExtractResponse

    @POST("api/summarize")
    suspend fun summarize(@Body body: Map<String, String>): SummarizeResponse

    @POST("api/generate-quiz")
    suspend fun generateQuiz(@Body body: GenerateQuizRequest): GenerateQuizResponse

    @POST("api/quiz-attempt")
    suspend fun recordQuizAttempt(@Body body: QuizAttemptRequest)

    @POST("api/ping-active")
    suspend fun pingActive()

    // ---- المكتبة ----
    @GET("api/library")
    suspend fun libraryBooks(): LibraryBooksResponse

    @GET("api/library/{id}")
    suspend fun libraryBook(@Path("id") id: String): LibraryBookDetail

    @POST("api/library")
    suspend fun createLibraryBook(@Body body: Map<String, String>): Map<String, String>

    @PATCH("api/library/{id}")
    suspend fun renameLibraryBook(@Path("id") id: String, @Body body: Map<String, String>)

    @DELETE("api/library/{id}")
    suspend fun deleteLibraryBook(@Path("id") id: String)

    // ---- دفتر الملاحظات (حساب فردي بس - الباك إند يرفض أي حساب مؤسسي) ----
    @GET("api/notes/folders")
    suspend fun noteFolders(): NoteFoldersResponse

    @POST("api/notes/folders")
    suspend fun createNoteFolder(@Body body: Map<String, String>): NoteFolder

    @PATCH("api/notes/folders/{id}")
    suspend fun renameNoteFolder(@Path("id") id: String, @Body body: Map<String, String>): NoteFolder

    @DELETE("api/notes/folders/{id}")
    suspend fun deleteNoteFolder(@Path("id") id: String)

    @GET("api/notes")
    suspend fun notes(
        @Query("folder_id") folderId: String? = null,
        @Query("q") query: String? = null,
    ): NotesResponse

    @GET("api/notes/{id}")
    suspend fun note(@Path("id") id: String): NoteDetail

    @POST("api/notes")
    suspend fun createNote(@Body body: CreateNoteRequest): NoteDetail

    @PATCH("api/notes/{id}")
    suspend fun updateNote(@Path("id") id: String, @Body body: UpdateNoteRequest): NoteDetail

    @DELETE("api/notes/{id}")
    suspend fun deleteNote(@Path("id") id: String)

    // ---- الأداء والأرشيف ----
    @GET("api/performance")
    suspend fun performance(): PerformanceResponse

    @GET("api/sessions")
    suspend fun sessions(): SessionsResponse

    // ---- البروفايل ----
    @POST("api/profile/sync")
    suspend fun syncProfile(@Body body: Map<String, String>)

    @PATCH("api/profile")
    suspend fun updateProfile(@Body body: UpdateProfileRequest)

    @GET("api/profile/{userId}")
    suspend fun profile(@Path("userId") userId: String): ProfileResponse

    // ---- الأصدقاء ----
    @GET("api/friends/search")
    suspend fun searchUsers(@Query("q") query: String): UserSearchResponse

    @POST("api/friends/request")
    suspend fun sendFriendRequest(@Body body: Map<String, String>)

    @GET("api/friends/requests")
    suspend fun friendRequests(): FriendRequestsResponse

    @POST("api/friends/accept")
    suspend fun acceptFriendRequest(@Body body: Map<String, String>)

    @POST("api/friends/reject")
    suspend fun rejectFriendRequest(@Body body: Map<String, String>)

    @GET("api/friends")
    suspend fun friends(): FriendsResponse

    @DELETE("api/friends/{friendUserId}")
    suspend fun removeFriend(@Path("friendUserId") friendUserId: String)

    // ---- الغرف ----
    @POST("api/room/create")
    suspend fun createRoom(@Body body: CreateRoomRequest): CreateRoomResponse

    // ---- الحساب / الأدوار ----
    @GET("api/me")
    suspend fun me(): MeResponse

    @POST("api/me/complete-password-change")
    suspend fun completePasswordChange()

    @POST("api/resolve-login-identifier")
    suspend fun resolveLoginIdentifier(@Body body: ResolveIdentifierRequest): ResolveIdentifierResponse

    // ---- Admin (صاحب المنصة) ----
    @POST("api/admin/schools")
    suspend fun adminCreateSchool(@Body body: CreateSchoolRequest): CreateSchoolResponse

    @GET("api/admin/schools")
    suspend fun adminSchools(): SchoolsResponse

    @PATCH("api/admin/schools/{id}")
    suspend fun adminUpdateSchool(@Path("id") id: String, @Body body: AdminUpdateSchoolRequest)

    @DELETE("api/admin/schools/{id}")
    suspend fun adminDeleteSchool(@Path("id") id: String)

    @POST("api/admin/schools/{id}/reset-admin-password")
    suspend fun adminResetSchoolAdminPassword(@Path("id") id: String): GeneratedCredentials

    // ---- School Admin / School Administration ----
    @GET("api/school/info")
    suspend fun schoolInfo(): SchoolInfo

    @POST("api/school/teachers")
    suspend fun schoolAddTeacher(@Body body: AddStaffRequest): GeneratedCredentials

    @GET("api/school/teachers")
    suspend fun schoolTeachers(): TeachersResponse

    @POST("api/school/administration")
    suspend fun schoolAddAdministration(@Body body: AddStaffRequest): GeneratedCredentials

    @GET("api/school/administration")
    suspend fun schoolAdministration(): AdministrationResponse

    @PATCH("api/school/accounts/{userId}")
    suspend fun schoolUpdateAccount(@Path("userId") userId: String, @Body body: UpdateAccountRequest)

    @DELETE("api/school/accounts/{userId}")
    suspend fun schoolDeleteAccount(@Path("userId") userId: String)

    @POST("api/school/accounts/{userId}/reset-password")
    suspend fun schoolResetAccountPassword(@Path("userId") userId: String): AccountResetCredentials

    @GET("api/school/profile/{userId}")
    suspend fun schoolProfile(@Path("userId") userId: String): InstitutionalProfile

    @POST("api/school/classes")
    suspend fun schoolCreateClass(@Body body: CreateClassRequest): SchoolClass

    @GET("api/school/classes")
    suspend fun schoolClasses(): SchoolClassesResponse

    @PATCH("api/school/classes/{id}")
    suspend fun schoolReassignClassTeacher(@Path("id") id: String, @Body body: ReassignTeacherRequest)

    @DELETE("api/school/classes/{id}")
    suspend fun schoolDeleteClass(@Path("id") id: String)

    @POST("api/school/classes/{id}/schedule")
    suspend fun schoolAddSchedule(@Path("id") id: String, @Body body: ScheduleRequest)

    @GET("api/school/classes/{id}/schedule")
    suspend fun schoolClassSchedule(@Path("id") id: String): ScheduleResponse

    @DELETE("api/school/schedule/{id}")
    suspend fun schoolDeleteSchedule(@Path("id") id: String)

    @POST("api/school/students/bulk")
    suspend fun schoolBulkAddStudents(@Body body: BulkAddRequest): BulkAddResponse

    @GET("api/school/students")
    suspend fun schoolStudents(@Query("class_id") classId: String? = null): SchoolStudentsResponse

    @GET("api/school/attendance")
    suspend fun schoolAttendance(@Query("class_id") classId: String? = null): SchoolAttendanceReport

    @POST("api/school/broadcast")
    suspend fun schoolBroadcast(@Body body: BroadcastRequest): BroadcastResponse

    // ---- Teacher ----
    @GET("api/teacher/roster")
    suspend fun teacherRoster(): TeacherRosterResponse

    @GET("api/teacher/students/{userId}")
    suspend fun teacherStudentProfile(@Path("userId") userId: String): InstitutionalProfile

    @GET("api/teacher/performance")
    suspend fun teacherPerformance(@Query("class_id") classId: String? = null): TeacherPerformanceResponse

    @GET("api/teacher/schedule")
    suspend fun teacherSchedule(): ScheduleWithClassesResponse

    @GET("api/teacher/attendance")
    suspend fun teacherAttendance(@Query("class_id") classId: String? = null): AttendanceResponse

    @GET("api/teacher/attendance/manual")
    suspend fun teacherManualAttendance(@Query("class_id") classId: String, @Query("date") date: String): ManualAttendanceResponse

    @POST("api/teacher/attendance/manual")
    suspend fun teacherSaveManualAttendance(@Body body: SaveManualAttendanceRequest)

    @POST("api/teacher/broadcast")
    suspend fun teacherBroadcast(@Body body: BroadcastRequest): BroadcastResponse

    // ---- Student ----
    @GET("api/student/schedule")
    suspend fun studentSchedule(): ScheduleResponse

    // ---- الرسائل والتنبيهات ----
    @GET("api/messages/conversations")
    suspend fun conversations(): ConversationsResponse

    @GET("api/messages/thread/{otherUserId}")
    suspend fun messageThread(@Path("otherUserId") otherUserId: String): MessageThreadResponse

    @POST("api/messages/send")
    suspend fun sendMessage(@Body body: SendMessageRequest)

    @GET("api/notifications")
    suspend fun notifications(): NotificationsResponse

    @POST("api/notifications/mark-read")
    suspend fun markNotificationsRead()

    // ---- الاشتراكات ----
    @GET("api/subscription/plans")
    suspend fun subscriptionPlans(): SubscriptionPlansResponse

    @GET("api/subscription/me")
    suspend fun subscriptionMe(): SubscriptionMeResponse

    @POST("api/subscription/checkout")
    suspend fun subscriptionCheckout(@Body body: CheckoutRequest): CheckoutResponse

    @POST("api/subscription/google/verify")
    suspend fun subscriptionGoogleVerify(@Body body: GoogleVerifyRequest): SubscriptionSyncResponse

    @POST("api/subscription/cancel")
    suspend fun subscriptionCancel()
}

@kotlinx.serialization.Serializable
data class ScheduleWithClassesResponse(
    val schedule: List<ClassScheduleEntry> = emptyList(),
    val classes: List<SchoolClassSummary> = emptyList(),
)
