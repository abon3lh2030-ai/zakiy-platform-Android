package com.zakiy.platform.ui.study

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zakiy.platform.network.dto.QuizQuestion

/** حالة جلسة مذاكرة فردية وحدة (نص مستخرج -> ملخص -> اختبار) - تُمرَّر بين
 * الشاشات الثلاث بدل ما نعيد الرفع كل مرة. تُصفّر تلقائيًا عند بداية جلسة
 * جديدة (رفع ملف جديد من الرئيسية). */
class StudyFlowState {
    var extractedText by mutableStateOf("")
    var summary by mutableStateOf<String?>(null)
    var quizQuestions by mutableStateOf<List<QuizQuestion>>(emptyList())
    var quizAnswers by mutableStateOf<Map<Int, Int>>(emptyMap())
    var quizSubmitted by mutableStateOf(false)

    fun reset(text: String) {
        extractedText = text
        summary = null
        quizQuestions = emptyList()
        quizAnswers = emptyMap()
        quizSubmitted = false
    }
}
