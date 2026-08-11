package com.zakiy.platform.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** صف قائمة موحّد لكل لوحات الأدوار المؤسسية - أيقونة ملوّنة + عنوان، نفس
 * نمط DashboardMenuRow بتطبيق iOS بالضبط عشان التنقّل يحس متسق. */
@Composable
fun DashboardMenuRow(icon: ImageVector, tint: Color, title: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = {
            Box(
                modifier = Modifier.size(34.dp).background(tint, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}

/** صندوق بيانات دخول تُعرض مرة وحدة (كلمة سر عشوائية جديدة) - نفس التنسيق
 * بكل شاشات إنشاء/إعادة تعيين حسابات المدرسة. */
@Composable
fun CredentialBox(title: String, identifier: String, password: String) {
    androidx.compose.material3.Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(identifier, style = MaterialTheme.typography.bodyMedium)
            Text(password, style = MaterialTheme.typography.bodyMedium, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
    }
}
