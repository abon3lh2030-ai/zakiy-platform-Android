package com.zakiy.platform.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class School(
    val id: String,
    val name: String,
    @SerialName("max_accounts") val maxAccounts: Int,
    @SerialName("subscription_status") val subscriptionStatus: String? = null,
    @SerialName("subscription_package") val subscriptionPackage: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("admin_email") val adminEmail: String? = null,
    @SerialName("accounts_used") val accountsUsed: Int? = null,
)

@Serializable
data class SchoolsResponse(val schools: List<School> = emptyList())

@Serializable
data class CreateSchoolRequest(
    val name: String,
    @SerialName("admin_email") val adminEmail: String,
    @SerialName("max_accounts") val maxAccounts: Int,
)

@Serializable
data class CreateSchoolResponse(val school: School, @SerialName("school_admin") val schoolAdmin: GeneratedCredentials)

@Serializable
data class SchoolInfo(
    @SerialName("accounts_used") val accountsUsed: Int,
    @SerialName("max_accounts") val maxAccounts: Int,
)

@Serializable
data class SchoolClassSummary(val id: String, val name: String)

@Serializable
data class TeacherSummary(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("full_name") val fullName: String? = null,
    val classes: List<SchoolClassSummary> = emptyList(),
    @SerialName("student_count") val studentCount: Int = 0,
    @SerialName("last_login") val lastLogin: String? = null,
)

@Serializable
data class TeachersResponse(val teachers: List<TeacherSummary> = emptyList())

@Serializable
data class AddStaffRequest(val name: String, val email: String)

@Serializable
data class AdminStaffSummary(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("last_login") val lastLogin: String? = null,
)

@Serializable
data class AdministrationResponse(val administration: List<AdminStaffSummary> = emptyList())

@Serializable
data class SchoolStudent(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("class_id") val classId: String? = null,
)

@Serializable
data class SchoolStudentsResponse(val students: List<SchoolStudent> = emptyList())

@Serializable
data class GeneratedStudent(val name: String, val username: String, val password: String)

@Serializable
data class BulkAddRequest(@SerialName("class_id") val classId: String, val names: List<String>)

@Serializable
data class BulkAddResponse(val students: List<GeneratedStudent> = emptyList())

@Serializable
data class SchoolClass(
    val id: String,
    val name: String,
    @SerialName("teacher_id") val teacherId: String? = null,
    @SerialName("school_id") val schoolId: String? = null,
)

@Serializable
data class SchoolClassesResponse(val classes: List<SchoolClass> = emptyList())

@Serializable
data class CreateClassRequest(val name: String, @SerialName("teacher_id") val teacherId: String? = null)

@Serializable
data class ScheduleRequest(
    @SerialName("day_of_week") val dayOfWeek: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val subject: String,
)

@Serializable
data class ClassScheduleEntry(
    val id: String,
    @SerialName("class_id") val classId: String? = null,
    @SerialName("day_of_week") val dayOfWeek: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val subject: String? = null,
)

@Serializable
data class ScheduleResponse(val schedule: List<ClassScheduleEntry> = emptyList())

@Serializable
data class TeacherRosterResponse(
    val classes: List<SchoolClassSummary> = emptyList(),
    val students: List<SchoolStudent> = emptyList(),
)

@Serializable
data class TeacherPerformanceRow(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("class_id") val classId: String? = null,
    @SerialName("attempts_count") val attemptsCount: Int = 0,
    @SerialName("avg_score") val avgScore: Int = 0,
    @SerialName("total_study_minutes") val totalStudyMinutes: Int = 0,
    @SerialName("current_streak") val currentStreak: Int = 0,
)

@Serializable
data class TeacherPerformanceResponse(val performance: List<TeacherPerformanceRow> = emptyList())

@Serializable
data class SessionAttendanceRow(
    @SerialName("class_id") val classId: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("joined_at") val joinedAt: String? = null,
)

@Serializable
data class AttendanceResponse(val attendance: List<SessionAttendanceRow> = emptyList())

@Serializable
data class ManualAttendanceRecord(
    @SerialName("student_id") val studentId: String,
    val status: String,
)

@Serializable
data class ManualAttendanceResponse(val attendance: List<ManualAttendanceRecord> = emptyList())

@Serializable
data class SaveManualAttendanceRequest(
    @SerialName("class_id") val classId: String,
    val date: String,
    val records: List<ManualAttendanceRecord>,
)

@Serializable
data class SchoolAttendanceReport(
    val attendance: List<SessionAttendanceRow> = emptyList(),
    @SerialName("manual_attendance") val manualAttendance: List<ManualAttendanceRecord> = emptyList(),
    val classes: List<SchoolClassSummary> = emptyList(),
)

@Serializable
data class InstitutionalProfile(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("full_name") val fullName: String? = null,
    val bio: String? = null,
    @SerialName("school_name") val schoolName: String? = null,
    val role: String? = null,
    val performance: ProfilePerformanceSummary? = null,
    val archive: List<SessionArchiveItem>? = null,
)
