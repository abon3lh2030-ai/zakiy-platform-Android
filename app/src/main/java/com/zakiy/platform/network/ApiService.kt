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

    // ---- المساعد الذكي (محادثات محفوظة - متاح لأي حساب، فردي أو مؤسسي) ----
    @GET("api/ai/conversations")
    suspend fun aiConversations(): AiConversationsResponse

    @POST("api/ai/conversations")
    suspend fun createAiConversation(): AiConversationDetail

    @GET("api/ai/conversations/{id}")
    suspend fun aiConversation(@Path("id") id: String): AiConversationDetail

    @DELETE("api/ai/conversations/{id}")
    suspend fun deleteAiConversation(@Path("id") id: String)

    @POST("api/ai/conversations/{id}/messages")
    suspend fun sendAiMessage(@Path("id") id: String, @Body body: SendAiMessageRequest): SendAiMessageResponse

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

    // ---- دفتر الواجبات - معلم ----
    @POST("api/teacher/assignments")
    suspend fun createAssignment(@Body body: CreateAssignmentRequest): AssignmentDetail

    @GET("api/teacher/assignments")
    suspend fun teacherAssignments(): AssignmentsResponse

    @GET("api/teacher/assignments/{id}")
    suspend fun teacherAssignmentDetail(@Path("id") id: String): AssignmentDetail

    @PATCH("api/teacher/assignments/{id}/submissions/{studentId}")
    suspend fun gradeAssignmentSubmission(
        @Path("id") id: String,
        @Path("studentId") studentId: String,
        @Body body: GradeRequest,
    )

    @GET("api/teacher/assignments/{id}/submissions/{studentId}/file")
    suspend fun teacherSubmissionFileUrl(@Path("id") id: String, @Path("studentId") studentId: String): FileUrlResponse

    @DELETE("api/teacher/assignments/{id}")
    suspend fun deleteAssignment(@Path("id") id: String)

    // ---- الاختبارات - معلم ----
    @POST("api/teacher/quizzes")
    suspend fun createQuiz(@Body body: CreateQuizRequest): QuizSummary

    @GET("api/teacher/quizzes")
    suspend fun teacherQuizzes(@Query("class_id") classId: String? = null): TeacherQuizzesResponse

    @GET("api/teacher/quizzes/{id}")
    suspend fun teacherQuizDetail(@Path("id") id: String): TeacherQuizDetail

    @PATCH("api/teacher/quizzes/{id}")
    suspend fun updateQuiz(@Path("id") id: String, @Body body: UpdateQuizRequest): QuizSummary

    @POST("api/teacher/quizzes/{id}/publish")
    suspend fun publishQuiz(@Path("id") id: String): QuizSummary

    @DELETE("api/teacher/quizzes/{id}")
    suspend fun deleteQuiz(@Path("id") id: String)

    @PATCH("api/teacher/quizzes/{id}/attempts/{studentId}")
    suspend fun gradeQuizAttempt(
        @Path("id") id: String,
        @Path("studentId") studentId: String,
        @Body body: GradeAttemptRequest,
    ): QuizAttemptDto

    // ---- Student ----
    @GET("api/student/schedule")
    suspend fun studentSchedule(): ScheduleResponse

    // ---- دفتر الواجبات - طالب ----
    @GET("api/student/assignments")
    suspend fun studentAssignments(): AssignmentsResponse

    @GET("api/student/assignments/{id}")
    suspend fun studentAssignmentDetail(@Path("id") id: String): AssignmentDetail

    @Multipart
    @POST("api/student/assignments/{id}/submit")
    suspend fun submitAssignment(
        @Path("id") id: String,
        @Part file: MultipartBody.Part,
        @Part("note") note: RequestBody? = null,
    ): AssignmentSubmission

    @GET("api/student/assignments/{id}/file")
    suspend fun studentSubmissionFileUrl(@Path("id") id: String): FileUrlResponse

    // ---- الاختبارات - طالب ----
    @GET("api/student/quizzes")
    suspend fun studentQuizzes(): StudentQuizzesResponse

    @GET("api/student/quizzes/{id}")
    suspend fun studentQuizDetail(@Path("id") id: String): StudentQuizDetail

    @POST("api/student/quizzes/{id}/start")
    suspend fun startQuiz(@Path("id") id: String): QuizAttemptDto

    @POST("api/student/quizzes/{id}/submit")
    suspend fun submitQuiz(@Path("id") id: String, @Body body: SubmitQuizRequest): QuizAttemptDto

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

    // ---- مدرستي: التحضير الذكي (معلم - أي حساب مسجّل دخول) ----
    @POST("api/lesson-prep/generate")
    suspend fun generateLessonPrep(@Body body: GenerateLessonPrepRequest): GenerateContentRawResponse

    @POST("api/lesson-prep")
    suspend fun createLessonPrep(@Body body: SaveLessonPrepRequest): SavedRecordResponse

    @GET("api/lesson-prep")
    suspend fun lessonPreps(): LessonPrepListResponse

    @GET("api/lesson-prep/{id}")
    suspend fun lessonPrepDetail(@Path("id") id: String): LessonPrepDetail

    @PATCH("api/lesson-prep/{id}")
    suspend fun updateLessonPrep(@Path("id") id: String, @Body body: UpdateLessonPrepRequest)

    @DELETE("api/lesson-prep/{id}")
    suspend fun deleteLessonPrep(@Path("id") id: String)

    // ---- مدرستي: نشاط إثرائي (معلم - بدون حفظ) ----
    @POST("api/enrichment/generate")
    suspend fun generateEnrichment(@Body body: GenerateEnrichmentRequest): GenerateContentRawResponse

    // ---- مدرستي: تحليل نتائج الطلاب (معلم - بدون حفظ) ----
    @POST("api/results-analysis/generate")
    suspend fun generateResultsAnalysis(@Body body: GenerateResultsAnalysisRequest): GenerateContentRawResponse

    // ---- مدرستي: مساعد الواجب الذكي (طالب) - ما فيه PATCH لهذا المسار ----
    @POST("api/homework-help/generate")
    suspend fun generateHomeworkHelp(@Body body: GenerateHomeworkHelpRequest): GenerateContentRawResponse

    @POST("api/homework-help")
    suspend fun createHomeworkHelp(@Body body: SaveHomeworkHelpRequest): SavedRecordResponse

    @GET("api/homework-help")
    suspend fun homeworkHelpSessions(): HomeworkHelpListResponse

    @GET("api/homework-help/{id}")
    suspend fun homeworkHelpDetail(@Path("id") id: String): HomeworkHelpDetail

    @DELETE("api/homework-help/{id}")
    suspend fun deleteHomeworkHelp(@Path("id") id: String)

    // ---- مدرستي: خطة مذاكرة ذكية (طالب) - ما فيه PATCH لهذا المسار ----
    @POST("api/study-plan/generate")
    suspend fun generateStudyPlan(@Body body: GenerateStudyPlanRequest): GenerateContentRawResponse

    @POST("api/study-plan")
    suspend fun createStudyPlan(@Body body: SaveStudyPlanRequest): SavedRecordResponse

    @GET("api/study-plan")
    suspend fun studyPlans(): StudyPlanListResponse

    @GET("api/study-plan/{id}")
    suspend fun studyPlanDetail(@Path("id") id: String): StudyPlanDetail

    @DELETE("api/study-plan/{id}")
    suspend fun deleteStudyPlan(@Path("id") id: String)
}

@kotlinx.serialization.Serializable
data class ScheduleWithClassesResponse(
    val schedule: List<ClassScheduleEntry> = emptyList(),
    val classes: List<SchoolClassSummary> = emptyList(),
)
