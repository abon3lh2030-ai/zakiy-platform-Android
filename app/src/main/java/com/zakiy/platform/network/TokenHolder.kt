package com.zakiy.platform.network

/** آخر access token معروف - نسخة متزامنة (غير suspend) يقدر OkHttp Interceptor
 * يقرأها مباشرة بأي طلب، بدل ما يحتاج ينتظر DataStore (suspend). AuthManager
 * هو المسؤول الوحيد عن تحديثها كل ما تتغيّر الجلسة (دخول/تجديد/خروج). */
object TokenHolder {
    @Volatile
    var accessToken: String? = null
}
