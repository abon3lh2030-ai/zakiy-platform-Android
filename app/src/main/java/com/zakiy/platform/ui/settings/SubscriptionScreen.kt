package com.zakiy.platform.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zakiy.platform.R
import com.zakiy.platform.billing.BillingManager
import com.zakiy.platform.billing.PlanCatalog
import com.zakiy.platform.billing.PlanTier
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.PlanLimits
import com.zakiy.platform.network.dto.SubscriptionMeResponse

private val planOrder = listOf(PlanTier.FREE, PlanTier.PLUS, PlanTier.PRO, PlanTier.ULTIMATE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val billingManager = remember { BillingManager(context) }
    var plans by remember { mutableStateOf<Map<String, PlanLimits>>(emptyMap()) }
    var me by remember { mutableStateOf<SubscriptionMeResponse?>(null) }
    var periodIndex by remember { mutableIntStateOf(0) } // 0=شهري 1=سنوي
    val isArabic = java.util.Locale.getDefault().language == "ar"

    LaunchedEffect(Unit) {
        billingManager.startConnection()
        plans = runCatching { NetworkModule.backendApi.subscriptionPlans().plans }.getOrDefault(emptyMap())
        me = runCatching { NetworkModule.backendApi.subscriptionMe() }.getOrNull()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.subscription)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "${stringResource(R.string.current_plan)}: ${me?.tier ?: "free"}",
                style = MaterialTheme.typography.titleMedium,
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(6.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = periodIndex == 0,
                    onClick = { periodIndex = 0 },
                    shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text(stringResource(R.string.monthly_label)) }
                SegmentedButton(
                    selected = periodIndex == 1,
                    onClick = { periodIndex = 1 },
                    shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text(stringResource(R.string.yearly_label)) }
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(10.dp))

            planOrder.forEach { tier ->
                val limits = plans[tier.key] ?: return@forEach
                val isCurrent = me?.tier == tier.key
                val price = if (periodIndex == 0) limits.priceMonthly else limits.priceAnnual
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isArabic) limits.nameAr else limits.nameEn, style = MaterialTheme.typography.titleLarge)
                            Text(if (price > 0) "$price ﷼" else "—", style = MaterialTheme.typography.titleLarge)
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(4.dp))
                        Text(
                            planFeatureText(limits, isArabic),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(6.dp))
                        if (isCurrent) {
                            Text(stringResource(R.string.current_plan_badge), color = MaterialTheme.colorScheme.primary)
                        } else if (tier != PlanTier.FREE) {
                            Button(
                                onClick = {
                                    val ids = PlanCatalog.productIds[tier] ?: return@Button
                                    val productId = if (periodIndex == 0) ids.monthly else ids.yearly
                                    (context as? android.app.Activity)?.let { billingManager.launchPurchase(it, productId) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(stringResource(R.string.btn_subscribe)) }
                        }
                    }
                }
            }
        }
    }
}

private fun planFeatureText(limits: PlanLimits, isArabic: Boolean): String {
    val unlimited = if (isArabic) "بلا حدود" else "unlimited"
    val full = if (isArabic) "كامل" else "full"
    return if (isArabic) {
        "📚 ${limits.libraryLimit ?: unlimited} كتاب · 🎯 ${limits.soloDaily ?: unlimited} جلسة فردية/يوم · " +
            "👥 ${limits.groupDaily ?: unlimited} جلسة جماعية/يوم · 🖍️ ${limits.lessonDaily ?: unlimited} درس/يوم · " +
            "📂 أرشيف ${limits.archiveLimit ?: full} · 📊 أداء ${limits.performanceLimit ?: full}"
    } else {
        "📚 ${limits.libraryLimit ?: unlimited} books · 🎯 ${limits.soloDaily ?: unlimited} solo/day · " +
            "👥 ${limits.groupDaily ?: unlimited} group/day · 🖍️ ${limits.lessonDaily ?: unlimited} lessons/day · " +
            "📂 archive ${limits.archiveLimit ?: full} · 📊 performance ${limits.performanceLimit ?: full}"
    }
}
