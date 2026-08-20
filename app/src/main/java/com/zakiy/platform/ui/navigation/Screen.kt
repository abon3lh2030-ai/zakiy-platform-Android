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
    const val StudentSchedule = "student_schedule"

    // دفتر الواجبات (معلم/طالب بس)
    const val Assignments = "assignments"
    fun assignmentDetail(id: String) = "assignments/$id"
    const val AssignmentDetailPattern = "assignments/{assignmentId}"
}
