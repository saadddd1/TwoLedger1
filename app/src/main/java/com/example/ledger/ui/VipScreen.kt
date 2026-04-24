package com.example.ledger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ledger.data.AuthSession
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipScreen(onNavigateBack: () -> Unit) {
    val isVip by AuthSession.isVip.collectAsState()
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var selectedPlan by remember { mutableStateOf(1) } // 1 for monthly, 12 for yearly

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("会员特权", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = IosScreenBg,
                    titleContentColor = IosTextPrimary
                )
            )
        },
        containerColor = IosScreenBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // VIP Status Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        if (isVip) "尊贵的 VIP 会员" else "未开通 VIP",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700) // Gold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (isVip) "您的数据正在云端安全同步" else "开通获取云同步与无限账单容量特权",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Text(
                "选择您的订阅方案",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = IosTextPrimary,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 16.dp)
            )

            // Subscription Plans
            Row(modifier = Modifier.fillMaxWidth()) {
                PlanCard(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    title = "连续包月",
                    price = "¥ 6.00",
                    period = "/ 月",
                    isSelected = selectedPlan == 1,
                    onClick = { selectedPlan = 1 }
                )
                PlanCard(
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                    title = "连续包年",
                    price = "¥ 60.00",
                    period = "/ 年",
                    isSelected = selectedPlan == 12,
                    onClick = { selectedPlan = 12 }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        // 模拟调用阿里支付接口
                        kotlinx.coroutines.delay(1500)
                        AuthSession.updateVipStatus(true)
                        isLoading = false
                    }
                },
                enabled = !isVip,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = IosBg, modifier = Modifier.size(24.dp))
                } else {
                    Text(if (isVip) "已开通" else "立即开通 / 续费", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun PlanCard(
    modifier: Modifier = Modifier,
    title: String,
    price: String,
    period: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFFD4AF37) else Color.Transparent
    val bgColor = if (isSelected) Color(0xFFFFF9E6) else IosCardBg

    Card(
        modifier = modifier
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = IosTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(price, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37))
                Text(period, fontSize = 12.sp, color = IosTextSecondary, modifier = Modifier.padding(bottom = 4.dp))
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFFD4AF37),
                    modifier = Modifier.padding(top = 8.dp).size(20.dp)
                )
            }
        }
    }
}
