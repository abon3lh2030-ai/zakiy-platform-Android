package com.zakiy.platform.network

/** نفس المشروع والباك إند اللي الموقع وتطبيق iOS يستخدمونه بالضبط - مفتاح
 * anon عام وآمن يُشحن بأي تطبيق عميل (محمي بـ RLS، مو بالسرية) - مفتاح
 * service_role الحقيقي يبقى بالباك إند بس ولا يوصل هذا التطبيق إطلاقًا. */
object ApiConfig {
    const val API_BASE = "https://zakiy-platform.onrender.com"
    const val SOCKET_URL = API_BASE

    // نطاق الموقع نفسه (CNAME بجذر مستودع zakiy-platform) - يُستخدم بمختبر
    // العلوم (تبويب الكيمياء) ومعمل الروبوتات، المعروضين جوّا WebView مدمج
    // بدل إعادة بنائهم Native (شاشات ثلاثية الأبعاد/محاكي دوائر معقّد جدًا)
    const val WEB_BASE = "https://zakiy.tech"

    const val SUPABASE_URL = "https://qwlbufcailgpxxatgyez.supabase.co"
    const val SUPABASE_ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InF3bGJ1ZmNhaWxncHh4YXRneWV6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODU1MTY1NTgsImV4cCI6MjEwMTA5MjU1OH0.ApKmMBSdZNIbSNFF0prm_cUUc2flIuVdtaGE97gonyQ"
}
