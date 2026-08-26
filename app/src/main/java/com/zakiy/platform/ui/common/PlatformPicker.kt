package com.zakiy.platform.ui.common

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R

/** منصة حل الواجب/الاختبار - "zakiy" (الوضع الحالي بدون أي تغيير) أو
 * "madrasati" (يُحل بالكامل على مدرستي، ذكيّ يحفظ بس رابط اختياري). مشتركة
 * بين الواجبات والاختبارات - نفس الحقلين platform/external_link بالضبط
 * بالباك إند لكل من المسارين. */
const val PLATFORM_ZAKIY = "zakiy"
const val PLATFORM_MADRASATI = "madrasati"

/** لا يوجد رابط عام موحّد لكل واجب/اختبار على مدرستي - هذا اختصار لصفحة
 * الدخول الرسمية بس (نفس الرابط المستخدم بمركز "مدرستي" بالتطبيق). */
const val MADRASATI_SIGNIN_URL = "https://schools.madrasati.sa/Auth/SignIn"

fun openMadrasatiLink(context: Context, url: String?) {
    CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url?.takeIf { it.isNotBlank() } ?: MADRASATI_SIGNIN_URL))
}

/** اختيار المنصة (ذكيّ / مدرستي) - صف من خيارين، نفس نمط QuestionTypeOption. */
@Composable
fun PlatformPicker(platform: String, onPlatformChange: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        PlatformOption(PLATFORM_ZAKIY, stringResource(R.string.platform_zakiy), platform, onPlatformChange)
        PlatformOption(PLATFORM_MADRASATI, stringResource(R.string.platform_madrasati), platform, onPlatformChange)
    }
}

@Composable
private fun PlatformOption(value: String, label: String, current: String, onChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .padding(end = 16.dp)
            .selectable(selected = current == value, onClick = { onChange(value) }),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = current == value, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
