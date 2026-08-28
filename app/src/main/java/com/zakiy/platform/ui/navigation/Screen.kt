package com.zakiy.platform.ui.navigation

/** كل مسارات التنقّل بالتطبيق - قيم route نصية بسيطة (Navigation Compose)،
 * والمسارات اللي تاخذ معطى (كود غرفة، رقم بطاقة...) تُبنى بدالة مساعدة. */
object Screen {
    // بوابات الدخول
    const val Splash = "splash"
    const val Login = "login"
    const val SignUp = "signup"
    const val ForcePasswordChange = "force_password_change"

    // التبويبات الرئيسية (حساب فردي)
    const val Home = "home"
    const val Library = "library"
    const val Performance = "performance"
    const val Messages = "messages"
    const val Settings = "settings"

    // المذاكرة الفردية
    const val StudyText = "study_text"
    const val StudySummary = "study_summary"
    const val StudyQuiz = "study_quiz"

    // المكتبة
    fun libraryDetail(bookId: String) = "library_detail/$bookId"
    const val LibraryDetailPattern = "library_detail/{bookId}"

    // الأرشيف/الأصدقاء/البروفايل
    const val Archive = "archive"
    const val Friends = "friends"
    const val MyProfile = "profile/me"
    fun profile(userId: String) = "profile/$userId"
    const val ProfilePattern = "profile/{userId}"
    const val EditProfile = "edit_profile"

    // الاشتراك
    const val Subscription = "subscription"

    // دفتر الملاحظات (حساب فردي بس)
    const val Notes = "notes"
    fun noteEditor(noteId: String) = "notes/$noteId"
    const val NoteEditorPattern = "notes/{noteId}"

    // المساعد الذكي (محادثات محفوظة - متاح لأي حساب)
    const val AiConversations = "ai_conversations"
    fun aiConversation(conversationId: String) = "ai_conversations/$conversationId"
    const val AiConversationPattern = "ai_conversations/{conversationId}"
    fun aiBookPicker(conversationId: String) = "ai_conversations/$conversationId/book_picker"
    const val AiBookPickerPattern = "ai_conversations/{conversationId}/book_picker"

    // الغرف
    const val RoomLobbyGroup = "room_lobby/quiz"
    const val RoomLobbyClassroom = "room_lobby/classroom"
    fun room(code: String, type: String, isCreator: Boolean) = "room/$code/$type/$isCreator"
    const val RoomPattern = "room/{code}/{type}/{isCreator}"

    // الرسائل
    fun thread(userId: String, username: String) = "thread/$userId/$username"
    const val ThreadPattern = "thread/{userId}/{username}"
    const val Notifications = "notifications"

    // الأدوار المؤسسية
    const val AdminDashboard = "admin_dashboard"
    const val SchoolDashboard = "school_dashboard"
    const val SchoolTeachers = "school_teachers"
    const val SchoolAdministration = "school_administration"
    const val SchoolStudents = "school_students"
    const val SchoolClasses = "school_classes"
    const val SchoolBulkAdd = "school_bulk_add"
    const val SchoolAttendance = "school_attendance"
    const val SchoolLibrary = "school_library"
    const val TeacherDashboard = "teacher_dashboard"
    const val TeacherRoster = "teacher_roster"
    const val TeacherPerformance = "teacher_performance"
    const val TeacherSchedule = "teacher_schedule"
    const val TeacherAttendance = "teacher_attendance"
    const val TeacherLibrary = "teacher_library"
    const val Gradesheet = "gradesheet"
    const val StudentSchedule = "student_schedule"

    // دفتر الواجبات (معلم/طالب بس)
    const val Assignments = "assignments"
    fun assignmentDetail(id: String) = "assignments/$id"
    const val AssignmentDetailPattern = "assignments/{assignmentId}"

    // الاختبارات (معلم/طالب بس)
    const val Quizzes = "quizzes"
    const val QuizCreate = "quizzes/create"
    fun quizEdit(id: String) = "quizzes/$id/edit"
    const val QuizEditPattern = "quizzes/{quizId}/edit"
    fun quizDetail(id: String) = "quizzes/$id"
    const val QuizDetailPattern = "quizzes/{quizId}"
    fun quizTake(id: String) = "quizzes/$id/take"
    const val QuizTakePattern = "quizzes/{quizId}/take"

    // مدرستي وأدوات ذكيّ (معلم/طالب) - متاح لأي حساب مسجّل دخول بدون تقييد دور
    const val MadrasatiHub = "madrasati"
    const val LessonPrepCreate = "madrasati/lesson_prep/create"
    fun lessonPrepView(id: String) = "madrasati/lesson_prep/$id"
    const val LessonPrepViewPattern = "madrasati/lesson_prep/{prepId}"
    const val Enrichment = "madrasati/enrichment"
    const val ResultsAnalysis = "madrasati/results_analysis"
    const val HomeworkHelpCreate = "madrasati/homework_help/create"
    fun homeworkHelpView(id: String) = "madrasati/homework_help/$id"
    const val HomeworkHelpViewPattern = "madrasati/homework_help/{sessionId}"
    const val StudyPlanCreate = "madrasati/study_plan/create"
    fun studyPlanView(id: String) = "madrasati/study_plan/$id"
    const val StudyPlanViewPattern = "madrasati/study_plan/{planId}"

    // مختبر العلوم (مستكشف الأحياء Native + الكيمياء والفيزياء مدمج
    // بالمتصفح) ومعمل الروبوتات (مدمج بالمتصفح بالكامل، مسار مستقل تمامًا
    // زي السايد بار بالموقع - roboticsLabBtn/scienceLabBtn مو تبويب تحت
    // مدرستي) - متاحين لأي حساب مسجّل دخول بدون تقييد دور
    const val ScienceLab = "science_lab"
    const val RoboticsLab = "robotics_lab"
}
