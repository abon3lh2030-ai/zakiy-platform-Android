package com.zakiy.platform.network

/** الأدوار المؤسسية الخمسة - role=null (مو من ضمن هذا enum) يعني حساب فردي
 * عادي، يكمل بنفس تجربة التطبيق الحالية بدون أي تغيير. */
enum class AccountRole(val raw: String) {
    Admin("admin"),
    SchoolAdmin("school_admin"),
    SchoolAdministration("school_administration"),
    Teacher("teacher"),
    Student("student");

    companion object {
        fun from(raw: String?): AccountRole? = entries.firstOrNull { it.raw == raw }
    }
}
