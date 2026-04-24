package com.example.ledger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.ledger.data.AuthSession
import com.example.ledger.network.ApiClient
import com.example.ledger.network.LoginRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVip: () -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("登录 / 注册", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Outlined.Close, contentDescription = "关闭")
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
            Text(
                "保护你的财务数据",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = IosTextPrimary,
                modifier = Modifier.padding(top = 40.dp, bottom = 8.dp)
            )
            
            Text(
                "开启云同步，数据永不丢失",
                fontSize = 14.sp,
                color = IosTextSecondary,
                modifier = Modifier.padding(bottom = 40.dp)
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("手机号") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IosBlue,
                    unfocusedContainerColor = IosCardBg,
                    focusedContainerColor = IosCardBg
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("验证码") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IosBlue,
                    unfocusedContainerColor = IosCardBg,
                    focusedContainerColor = IosCardBg
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (phone.isEmpty() || code.isEmpty()) {
                        showMessage = "请输入手机号和验证码"
                        return@Button
                    }
                    scope.launch {
                        isLoading = true
                        try {
                            // 调用模拟的接口，这里直接进行成功模拟，方便你后续对接到真实阿里云后端
                            // val response = ApiClient.apiService.loginByPhone(LoginRequest(phone, code))
                            // AuthSession.login(response.token, response.isVip)
                            
                            // 模拟无服务器响应
                            kotlinx.coroutines.delay(1000)
                            AuthSession.login("mock_token_12345", false) 
                            
                            showMessage = "登录成功"
                            onNavigateBack() // 返回主界面
                        } catch (e: Exception) {
                            showMessage = "登录失败: 网络错误或后端未启动"
                            e.printStackTrace()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IosBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = IosBg, modifier = Modifier.size(24.dp))
                } else {
                    Text("获取验证码 / 登录", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 微信登录 Mock
            Text(
                "微信一键登录",
                color = IosBlue,
                fontSize = 14.sp,
                modifier = Modifier.clickable {
                    showMessage = "正在调起微信..."
                    // 模拟微信登录
                    scope.launch {
                        kotlinx.coroutines.delay(1000)
                        AuthSession.login("mock_wechat_token", false)
                        onNavigateBack()
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            if (showMessage != null) {
                Snackbar(modifier = Modifier.padding(16.dp)) {
                    Text(showMessage ?: "")
                }
                LaunchedEffect(showMessage) {
                    kotlinx.coroutines.delay(2000)
                    showMessage = null
                }
            }
        }
    }
}
