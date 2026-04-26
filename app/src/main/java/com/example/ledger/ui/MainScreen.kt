@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
package com.example.ledger.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ledger.data.AppDatabase
import com.example.ledger.data.AutoBill
import com.example.ledger.data.Item
import com.example.ledger.viewmodel.ItemViewModel
import com.example.ledger.viewmodel.ItemViewModelFactory
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.max

// 全局缓存日期格式化器，避免每次重组创建
private val dateFormatYMD = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
private val dateFormatMDHM = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
private val dailyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
private val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
private val monthLabelFormat = SimpleDateFormat("yyyy年 MM月", Locale.getDefault())
private val dayLabelFormat = SimpleDateFormat("MM月dd日", Locale.getDefault())

val IosBlue = Color(0xFF007AFF)
val IosRed = Color(0xFFFF3B30)
val IosScreenBg = Color(0xFFF2F2F7)
val IosBg = Color(0xFFFFFFFF)
val IosTextPrimary = Color(0xFF000000)
val IosTextSecondary = Color(0xFF8E8E93)
val IosDivider = Color(0xFFE5E5EA)
val IosCardBg = Color(0xFFFFFFFF)

fun isNotificationListenerEnabled(context: Context): Boolean {
    val packageName = context.packageName
    val enabledListeners = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    ) ?: return false
    return enabledListeners.contains(packageName)
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabledServices.contains(context.packageName)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    viewModelFactory: ItemViewModelFactory,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToVip: () -> Unit = {}
) {
    val viewModel: ItemViewModel = viewModel(factory = viewModelFactory)
    val items by viewModel.items.collectAsState()
    val pendingBills by viewModel.pendingBills.collectAsState()
    val allBills by viewModel.allBills.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToSell by remember { mutableStateOf<Item?>(null) }
    var itemToEdit by remember { mutableStateOf<Item?>(null) }
    var billToConvert by remember { mutableStateOf<AutoBill?>(null) }
    var billToEdit by remember { mutableStateOf<AutoBill?>(null) }
    
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // 请求通知权限，用于在 Android 13 及以上设备上显示通知
    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            android.widget.Toast.makeText(context, "通知权限没开，我帮你跳到设置页了", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                "android.permission.POST_NOTIFICATIONS"
            )
            if (permissionCheck != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch("android.permission.POST_NOTIFICATIONS")
            }
        }
    }

    Scaffold(
        containerColor = IosScreenBg,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            text = "账本", 
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold, 
                            fontSize = 17.sp,
                            color = IosTextPrimary
                        ) 
                    },
                    navigationIcon = {
                        val isLoggedIn by com.example.ledger.data.AuthSession.isLoggedIn.collectAsState()
                        IconButton(onClick = { 
                            if (isLoggedIn) onNavigateToVip() else onNavigateToLogin() 
                        }) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Outlined.Person,
                                contentDescription = "账户",
                                tint = if (isLoggedIn) Color(0xFFD4AF37) else IosBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = IosBg
                    ),
                    actions = {
                        if (pagerState.currentPage == 0) {
                            IconButton(onClick = { showAddDialog = true }) {
                                Icon(
                                    Icons.Outlined.Add, 
                                    contentDescription = "添加", 
                                    tint = IosBlue,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                )
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = IosBg,
                    contentColor = IosBlue,
                    divider = { Divider(color = IosDivider, thickness = 0.5.dp) }
                ) {
                    val tabs = listOf("我的家当", "自动记账", "算总账")
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                            text = { 
                                Text(
                                    title, 
                                    fontFamily = FontFamily.SansSerif,
                                    color = if (pagerState.currentPage == index) IosBlue else IosTextSecondary,
                                    fontWeight = if (pagerState.currentPage == index) FontWeight.SemiBold else FontWeight.Normal
                                ) 
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(padding).fillMaxSize()
        ) { page ->
            when (page) {
                0 -> ItemListContent(
                        items = items,
                        onDelete = { viewModel.deleteItem(it) },
                        onEdit = { itemToEdit = it },
                        onSell = { itemToSell = it }
                     )
                1 -> AutoRecordContent(
                        pendingBills = pendingBills,
                        onDismiss = { viewModel.dismissAutoBill(it) },
                        onConvert = { billToConvert = it },
                        onEdit = { billToEdit = it },
                        context = context
                     )
                2 -> OverviewContent(items = items, allBills = allBills)
            }
        }

        if (showAddDialog) {
            AddItemDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, price, dateMillis, residual ->
                    viewModel.addItem(name, price, dateMillis, residual)
                    showAddDialog = false
                }
            )
        }

        if (itemToEdit != null) {
            EditItemDialog(
                item = itemToEdit!!,
                onDismiss = { itemToEdit = null },
                onEdit = { name, price, dateMillis ->
                    viewModel.updateItemDetails(itemToEdit!!, name, price, dateMillis)
                    itemToEdit = null
                }
            )
        }

        if (itemToSell != null) {
            SellItemDialog(
                item = itemToSell!!,
                onDismiss = { itemToSell = null },
                onSell = { soldPrice, soldDateMillis ->
                    viewModel.sellItem(itemToSell!!, soldPrice, soldDateMillis)
                    itemToSell = null
                }
            )
        }
        
        if (billToConvert != null) {
            ConvertBillDialog(
                bill = billToConvert!!,
                onDismiss = { billToConvert = null },
                onConvert = { name, residual ->
                    viewModel.convertBillToItem(billToConvert!!, name, residual)
                    billToConvert = null
                }
            )
        }

        if (billToEdit != null) {
            EditBillDialog(
                bill = billToEdit!!,
                onDismiss = { billToEdit = null },
                onEdit = { merchantName, amount, timestamp ->
                    viewModel.updateBillDetails(billToEdit!!, merchantName, amount, timestamp)
                    billToEdit = null
                }
            )
        }
    }
}

@Composable
fun AutoRecordContent(
    pendingBills: List<AutoBill>,
    onDismiss: (AutoBill) -> Unit,
    onConvert: (AutoBill) -> Unit,
    onEdit: (AutoBill) -> Unit,
    context: Context,
    modifier: Modifier = Modifier
) {
    var isNotificationEnabled by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var isAccessibilityEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }

    // Resume effect to refresh the state when coming back from settings
    DisposableEffect(context) {
        val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                isNotificationEnabled = isNotificationListenerEnabled(context)
                isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor("enabled_notification_listeners"),
            false,
            observer
        )
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            false,
            observer
        )
        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (!isNotificationEnabled) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = if (pendingBills.isEmpty()) 16.dp else 0.dp)
                    .height(176.dp)
                    .shadow(
                        elevation = 8.dp, 
                        shape = RoundedCornerShape(16.dp), 
                        spotColor = Color(0x0C000000), 
                        ambientColor = Color.Transparent
                    ),
                colors = CardDefaults.cardColors(containerColor = IosCardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxSize(), 
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "打开通知读取，我就能在你付完钱的瞬间抓到账单",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 15.sp,
                        color = IosTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IosBlue.copy(alpha = 0.1f), contentColor = IosBlue),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text("去打开", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (!isAccessibilityEnabled) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = if (pendingBills.isEmpty()) 16.dp else 0.dp)
                    .height(176.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = Color(0x0C000000),
                        ambientColor = Color.Transparent
                    ),
                colors = CardDefaults.cardColors(containerColor = IosCardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "微信/支付宝付完钱的瞬间，我就能抓到账单。\n说的就是无障碍服务，打开就行。",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 15.sp,
                        color = IosTextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IosBlue.copy(alpha = 0.1f), contentColor = IosBlue),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text("去开启", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (pendingBills.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无新账单，看来今天还没花钱", color = IosTextSecondary, fontFamily = FontFamily.SansSerif)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(pendingBills, key = { it.id }) { bill ->
                    Box(modifier = Modifier.animateItemPlacement(tween(250))) {
                        AutoBillCard(bill, onDismiss = { onDismiss(bill) }, onConvert = { onConvert(bill) }, onEdit = { onEdit(bill) })
                    }
                }
            }
        }
    }
}

@Composable
fun AutoBillCard(bill: AutoBill, onDismiss: () -> Unit, onConvert: () -> Unit, onEdit: () -> Unit) {
    val dateStr = dateFormatMDHM.format(Date(bill.timestampMillis))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .shadow(
                elevation = 8.dp, 
                shape = RoundedCornerShape(16.dp), 
                spotColor = Color(0x0C000000), 
                ambientColor = Color.Transparent
            ),
        colors = CardDefaults.cardColors(containerColor = IosCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = bill.merchantName,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = IosTextPrimary
                )
                Box(contentAlignment = Alignment.TopEnd) {
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { expanded = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "更多", tint = IosTextSecondary)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(IosCardBg)
                    ) {
                        if (!bill.paymentMethod.isNullOrBlank()) {
                            DropdownMenuItem(
                                text = { Text("怎么付的：${bill.paymentMethod}", fontFamily = FontFamily.SansSerif, color = IosTextSecondary, fontSize = 13.sp) },
                                onClick = { expanded = false }
                            )
                        }
                        if (!bill.fullPayeeName.isNullOrBlank()) {
                            DropdownMenuItem(
                                text = { Text("钱给了谁：${bill.fullPayeeName}", fontFamily = FontFamily.SansSerif, color = IosTextSecondary, fontSize = 13.sp) },
                                onClick = { expanded = false }
                            )
                            Divider(color = IosDivider, thickness = 0.5.dp)
                        } else if (!bill.paymentMethod.isNullOrBlank()) {
                            Divider(color = IosDivider, thickness = 0.5.dp)
                        }

                        DropdownMenuItem(
                            text = { Text("改一下", fontFamily = FontFamily.SansSerif, color = IosBlue) },
                            onClick = { expanded = false; onEdit() },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = "编辑", tint = IosBlue) }
                        )
                        DropdownMenuItem(
                            text = { Text("不是我的账", fontFamily = FontFamily.SansSerif, color = IosRed) },
                            onClick = { expanded = false; onDismiss() },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = "忽略", tint = IosRed) }
                        )
                    }
                }
            }
            
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "渠道: ${bill.appSource}",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        color = IosTextSecondary
                    )
                    Text(
                        text = dateStr,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        color = IosTextSecondary
                    )
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "¥${String.format("%.2f", bill.amount)}",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = IosTextPrimary
                )
                Button(
                    onClick = onConvert,
                    colors = ButtonDefaults.buttonColors(containerColor = IosBlue.copy(alpha = 0.1f), contentColor = IosBlue),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = "导入", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("计入家当", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun ConvertBillDialog(bill: AutoBill, onDismiss: () -> Unit, onConvert: (String, Double) -> Unit) {
    // 自动补全的智能化预推理 (Smart Auto-Complete)
    val guessedName = bill.merchantName.replace("账单", "").replace("支付", "").trim()
    var name by remember(bill) { mutableStateOf(guessedName) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IosBg,
        shape = RoundedCornerShape(14.dp),
        title = {
            Text(
                "这笔钱买了什么？",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                color = IosTextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                Text("在 ${bill.appSource} 花了 ¥${bill.amount}", color = IosTextSecondary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("给它起个名", fontFamily = FontFamily.SansSerif) },
                    singleLine = true,
                    isError = isError && name.isBlank(),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConvert(name, 0.0)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("记下了", color = IosBlue, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("算了", color = IosBlue, fontFamily = FontFamily.SansSerif) }
        }
    )
}

@Composable
fun EditBillDialog(bill: AutoBill, onDismiss: () -> Unit, onEdit: (String, Double, Long) -> Unit) {
    var merchantName by remember(bill) { mutableStateOf(bill.merchantName) }
    var amountStr by remember(bill) { mutableStateOf(bill.amount.toString()) }
    var dateStr by remember(bill) { mutableStateOf(dateFormatMDHM.format(Date(bill.timestampMillis))) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IosBg,
        shape = RoundedCornerShape(14.dp),
        title = {
            Text(
                "改一下账单",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                color = IosTextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                Text("渠道: ${bill.appSource}", color = IosTextSecondary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
                OutlinedTextField(
                    value = merchantName,
                    onValueChange = { merchantName = it },
                    label = { Text("在哪儿花的", fontFamily = FontFamily.SansSerif) },
                    singleLine = true,
                    isError = isError && merchantName.isBlank(),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("花了多少 (¥)", fontFamily = FontFamily.SansSerif) },
                    singleLine = true,
                    isError = isError && (amountStr.toDoubleOrNull() ?: 0.0) <= 0,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    label = { Text("什么时候 (MM-dd HH:mm)", fontFamily = FontFamily.SansSerif) },
                    singleLine = true,
                    isError = isError && runCatching { dateFormatMDHM.parse(dateStr) }.isFailure,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    val dateMillis = try { dateFormatMDHM.parse(dateStr)?.time ?: 0L } catch (e: Exception) { 0L }
                    if (merchantName.isNotBlank() && amount > 0 && dateMillis > 0) {
                        onEdit(merchantName, amount, dateMillis)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("改好了", color = IosBlue, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("算了", color = IosBlue, fontFamily = FontFamily.SansSerif) }
        }
    )
}

// ========================
// Existing Content Functions (Unchanged logic, just UI matching)
// ========================

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ItemListContent(
    items: List<Item>,
    onDelete: (Int) -> Unit,
    onEdit: (Item) -> Unit,
    onSell: (Item) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("空空如也 — 连一件吃灰的家当都没有？", color = IosTextSecondary, fontFamily = FontFamily.SansSerif)
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items, key = { it.id }) { item ->
                Box(modifier = Modifier.animateItemPlacement(tween(250))) {
                    ItemCard(
                        item = item, 
                        onDelete = { onDelete(item.id) },
                        onEdit = { onEdit(item) },
                        onSell = { onSell(item) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OverviewContent(items: List<Item>, allBills: List<AutoBill>, modifier: Modifier = Modifier) {
    val nowMillis = remember { System.currentTimeMillis() }

    // 一次性计算所有统计数据并缓存
    val statistics = remember(items, allBills, nowMillis) {
        val totalSpent = items.sumOf { it.price }
        val totalRecovered = items.filter { it.isSold }.sumOf { it.residualValue }
        val netSpend = max(totalSpent - totalRecovered, 0.0)

        val activeItems = items.count { !it.isSold }
        val soldItems = items.count { it.isSold }
        val billCount = allBills.size

        var totalDailyCost = 0.0
        var worstItemName: String? = null
        var worstDailyCost = 0.0
        items.forEach { item ->
            val endDate = if (item.isSold) item.soldDateMillis ?: nowMillis else nowMillis
            val diffMillis = max(endDate - item.purchaseDateMillis, 0)
            var daysPassed = TimeUnit.MILLISECONDS.toDays(diffMillis)
            if (daysPassed < 1L) daysPassed = 1L
            val netCost = if (item.isSold) max(item.price - item.residualValue, 0.0) else item.price
            val dailyCost = netCost / daysPassed
            totalDailyCost += dailyCost
            if (!item.isSold && dailyCost > worstDailyCost) {
                worstDailyCost = dailyCost
                worstItemName = item.name
            }
        }

        // 当月折旧估算
        val daysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
        val monthDepreciation = totalDailyCost * daysInMonth

        // 月度/年度账单汇总
        val monthlySums = mutableMapOf<String, Double>()
        val yearlySums = mutableMapOf<String, Double>()
        allBills.forEach { bill ->
            val date = Date(bill.timestampMillis)
            monthlySums[monthFormat.format(date)] = (monthlySums[monthFormat.format(date)] ?: 0.0) + bill.amount
            yearlySums[yearFormat.format(date)] = (yearlySums[yearFormat.format(date)] ?: 0.0) + bill.amount
        }

        val recentMonths = mutableListOf<Pair<String, Double>>()
        val thisMonthKey = monthFormat.format(nowMillis)
        val thisMonthSpending = monthlySums[thisMonthKey] ?: 0.0
        recentMonths.add(Pair("这个月已经烧了", thisMonthSpending))
        monthlySums.keys.filter { it != thisMonthKey }.sortedDescending().forEach { key ->
            val date = monthFormat.parse(key)
            val label = date?.let { monthLabelFormat.format(it) } ?: key
            recentMonths.add(Pair(label, monthlySums[key] ?: 0.0))
        }

        val recentYears = mutableListOf<Pair<String, Double>>()
        val thisYearKey = yearFormat.format(nowMillis)
        val thisYearSpending = yearlySums[thisYearKey] ?: 0.0
        recentYears.add(Pair("今年累计败掉", thisYearSpending))
        yearlySums.keys.filter { it != thisYearKey }.sortedDescending().forEach { key ->
            recentYears.add(Pair("${key}年", yearlySums[key] ?: 0.0))
        }

        listOf(
            totalSpent, totalRecovered, netSpend, totalDailyCost,
            worstItemName, worstDailyCost, monthDepreciation,
            activeItems, soldItems, billCount,
            thisMonthSpending, thisYearSpending,
            recentMonths, recentYears
        )
    }

    val totalSpent = statistics[0] as Double
    val totalRecovered = statistics[1] as Double
    val netSpend = statistics[2] as Double
    val totalDailyCost = statistics[3] as Double
    val worstItemName = statistics[4] as String?
    val worstDailyCost = statistics[5] as Double
    val monthDepreciation = statistics[6] as Double
    val activeItems = statistics[7] as Int
    val soldItems = statistics[8] as Int
    val billCount = statistics[9] as Int
    val thisMonthSpending = statistics[10] as Double
    val thisYearSpending = statistics[11] as Double
    @Suppress("UNCHECKED_CAST")
    val recentMonths = statistics[12] as List<Pair<String, Double>>
    @Suppress("UNCHECKED_CAST")
    val recentYears = statistics[13] as List<Pair<String, Double>>

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 1: 家当总账 + 最烧钱的家当
        item {
            StandardOverviewCard("家当总账") {
                OverviewRow("这些年败掉的总数", "¥${String.format("%.2f", totalSpent)}", IosTextPrimary, 18.sp)
                if (totalRecovered > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OverviewRow("在咸鱼上回的血", "¥${String.format("%.2f", totalRecovered)}", IosBlue, 16.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = IosDivider, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))
                OverviewRow("真正烧掉的钱", "¥${String.format("%.2f", netSpend)}", IosRed, 26.sp, true)
                Spacer(modifier = Modifier.height(16.dp))
                OverviewRow("每天一睁眼就亏掉", "¥${String.format("%.2f", totalDailyCost)}", IosTextPrimary, 16.sp)
                if (worstItemName != null && worstDailyCost > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "最烧钱的家当",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp,
                            color = IosTextSecondary
                        )
                        Text(
                            "$worstItemName 每天 ¥${String.format("%.2f", worstDailyCost)}",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IosRed.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Card 2: 花钱明细
        item {
            StandardOverviewCard("花钱流水") {
                OverviewRow("自动抓取账单", "$billCount 笔", IosTextPrimary, 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = IosDivider, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))
                ExpandableOverviewRow(
                    historyData = recentMonths,
                    valueColor = IosRed,
                    fontSize = 26.sp,
                    isBold = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = IosDivider, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))
                ExpandableOverviewRow(
                    historyData = recentYears,
                    valueColor = IosTextPrimary,
                    fontSize = 18.sp
                )
            }
        }

        // Card 3: 家当现状 + 月度折旧
        item {
            StandardOverviewCard("家当现状") {
                OverviewRow("还在吃灰的", "$activeItems 件", IosTextPrimary, 18.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OverviewRow("成功脱手的", "$soldItems 件", IosBlue, 16.sp)
                if (activeItems > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = IosDivider, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OverviewRow("本月预计折旧", "¥${String.format("%.2f", monthDepreciation)}", IosRed.copy(alpha = 0.8f), 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ExpandableOverviewRow(
    historyData: List<Pair<String, Double>>, 
    valueColor: Color, 
    fontSize: androidx.compose.ui.unit.TextUnit, 
    isBold: Boolean = false
) {
    if (historyData.isEmpty()) return
    val currentData = historyData.first()
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { isExpanded = !isExpanded }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(), 
            horizontalArrangement = Arrangement.SpaceBetween, 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currentData.first, 
                    fontFamily = FontFamily.SansSerif, 
                    fontSize = 15.sp, 
                    color = IosTextPrimary
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = IosTextSecondary,
                    modifier = Modifier.padding(start = 4.dp).size(16.dp)
                )
            }
            Text(
                text = "¥${String.format("%.2f", currentData.second)}", 
                fontFamily = FontFamily.SansSerif, 
                fontWeight = if (isBold) FontWeight.Black else FontWeight.SemiBold, 
                fontSize = fontSize, 
                color = valueColor
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .background(Color(0xFFF2F2F7), RoundedCornerShape(12.dp))
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                val pastData = historyData.drop(1)
                if (pastData.isEmpty()) {
                    Text(
                        text = "还没有更早的记录",
                        color = IosTextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    pastData.forEachIndexed { index, (label, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, color = IosTextSecondary, fontSize = 14.sp)
                            Text("¥${String.format("%.2f", value)}", color = IosTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        if (index < pastData.size - 1) {
                            Divider(color = Color(0x1A000000), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StandardOverviewCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp, 
                shape = RoundedCornerShape(16.dp), 
                spotColor = Color(0x0C000000), 
                ambientColor = Color.Transparent
            ),
        colors = CardDefaults.cardColors(containerColor = IosCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
            Text(
                text = title, 
                fontFamily = FontFamily.SansSerif, 
                fontSize = 14.sp, 
                fontWeight = FontWeight.SemiBold,
                color = IosTextSecondary
            )
            Spacer(modifier = Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
fun OverviewRow(label: String, value: String, valueColor: Color, fontSize: androidx.compose.ui.unit.TextUnit, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(), 
        horizontalArrangement = Arrangement.SpaceBetween, 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label, 
            fontFamily = FontFamily.SansSerif, 
            fontSize = 15.sp, 
            color = IosTextPrimary
        )
        Text(
            text = value, 
            fontFamily = FontFamily.SansSerif, 
            fontWeight = if (isBold) FontWeight.Black else FontWeight.SemiBold, 
            fontSize = fontSize, 
            color = valueColor
        )
    }
}

@Composable
fun ItemCard(item: Item, onDelete: () -> Unit, onEdit: () -> Unit, onSell: () -> Unit) {
    val dateStr = dateFormatYMD.format(Date(item.purchaseDateMillis))
    val soldDateStr = item.soldDateMillis?.let { dateFormatYMD.format(Date(it)) }
    
    val now = System.currentTimeMillis()
    val endDate = if (item.isSold) item.soldDateMillis ?: now else now
    val diffMillis = max(endDate - item.purchaseDateMillis, 0)
    var daysPassed = TimeUnit.MILLISECONDS.toDays(diffMillis)
    if (daysPassed < 1L) daysPassed = 1L
    
    val netCost = if (item.isSold) max(item.price - item.residualValue, 0.0) else item.price
    val dailyCost = netCost / daysPassed

    val alphaFactor by animateFloatAsState(targetValue = if (item.isSold) 0.5f else 1f, label = "alpha", animationSpec = tween(400))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .shadow(
                elevation = 8.dp, 
                shape = RoundedCornerShape(16.dp), 
                spotColor = Color(0x0C000000), 
                ambientColor = Color.Transparent
            ),
        colors = CardDefaults.cardColors(containerColor = IosCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name, 
                        fontFamily = FontFamily.SansSerif, 
                        fontSize = 19.sp, 
                        fontWeight = FontWeight.Bold,
                        color = IosTextPrimary.copy(alpha = alphaFactor)
                    )
                    if (item.isSold) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(color = Color(0xFFF2F2F7).copy(alpha = alphaFactor), shape = RoundedCornerShape(6.dp)) {
                            Text("已脱手", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontFamily = FontFamily.SansSerif, fontSize = 11.sp, color = IosTextSecondary.copy(alpha = alphaFactor))
                        }
                    }
                }
                Box(contentAlignment = Alignment.TopEnd) {
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { expanded = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "更多", tint = IosTextSecondary)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(IosCardBg)
                    ) {
                        DropdownMenuItem(
                            text = { Text("改一下", fontFamily = FontFamily.SansSerif) },
                            onClick = { expanded = false; onEdit() },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = "编辑", tint = IosBlue) }
                        )
                        DropdownMenuItem(
                            text = { Text("不要了", fontFamily = FontFamily.SansSerif, color = IosRed) },
                            onClick = { expanded = false; onDelete() },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = "删除", tint = IosRed) }
                        )
                    }
                }
            }
            
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("入手价: ¥${String.format("%.2f", item.price)}", fontFamily = FontFamily.SansSerif, fontSize = 15.sp, color = IosTextPrimary.copy(alpha = alphaFactor))
                    Text("入手日: $dateStr", fontFamily = FontFamily.SansSerif, fontSize = 14.sp, color = IosTextSecondary.copy(alpha = alphaFactor))
                }
                if (item.isSold) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("回血了: ¥${String.format("%.2f", item.residualValue)}", fontFamily = FontFamily.SansSerif, fontSize = 15.sp, color = IosTextSecondary.copy(alpha = alphaFactor))
                        if (soldDateStr != null) {
                            Text("脱手日: $soldDateStr", fontFamily = FontFamily.SansSerif, fontSize = 14.sp, color = IosTextSecondary.copy(alpha = alphaFactor))
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("陪伴了你 $daysPassed 天", fontFamily = FontFamily.SansSerif, fontSize = 14.sp, color = IosTextSecondary.copy(alpha = alphaFactor))
                    if (!item.isSold) {
                        TextButton(onClick = onSell, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(30.dp)) {
                            Text("挂了咸鱼没？", color = IosBlue, fontFamily = FontFamily.SansSerif, fontSize = 14.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("每天烧你", fontFamily = FontFamily.SansSerif, fontSize = 12.sp, color = IosTextSecondary.copy(alpha = alphaFactor))
                    Text(
                        "¥${String.format("%.2f", dailyCost)}",
                        fontFamily = FontFamily.SansSerif, fontSize = 28.sp,
                        color = if(item.isSold) IosTextPrimary.copy(alpha = alphaFactor) else IosBlue,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun AddItemDialog(onDismiss: () -> Unit, onAdd: (String, Double, Long, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var residualStr by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf(dateFormatYMD.format(Date())) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IosBg,
        shape = RoundedCornerShape(14.dp),
        title = { Text("录入新家当", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, color = IosTextPrimary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("给它起个名", fontFamily = FontFamily.SansSerif) }, singleLine = true, isError = isError && name.isBlank(), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                OutlinedTextField(value = priceStr, onValueChange = { priceStr = it }, label = { Text("花了多少钱", fontFamily = FontFamily.SansSerif) }, singleLine = true, isError = isError && (priceStr.toDoubleOrNull() ?: 0.0) <= 0, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                OutlinedTextField(value = dateStr, onValueChange = { dateStr = it }, label = { Text("什么时候败的", fontFamily = FontFamily.SansSerif) }, singleLine = true, isError = isError && runCatching { dateFormatYMD.parse(dateStr) }.isFailure, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val price = priceStr.toDoubleOrNull() ?: 0.0
                val dateMillis = try { dateFormatYMD.parse(dateStr)?.time ?: 0L } catch (e: Exception) { 0L }
                if (name.isNotBlank() && price > 0 && dateMillis > 0) onAdd(name, price, dateMillis, 0.0) else isError = true
            }) { Text("记下了", color = IosBlue, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("算了", color = IosBlue, fontFamily = FontFamily.SansSerif) } }
    )
}

@Composable
fun EditItemDialog(item: Item, onDismiss: () -> Unit, onEdit: (String, Double, Long) -> Unit) {
    var name by remember { mutableStateOf(item.name) }
    var priceStr by remember { mutableStateOf(if (item.price > 0) item.price.toString() else "") }
    var dateStr by remember { mutableStateOf(dateFormatYMD.format(Date(item.purchaseDateMillis))) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IosBg,
        shape = RoundedCornerShape(14.dp),
        title = { Text("改一下信息", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, color = IosTextPrimary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("叫什么", fontFamily = FontFamily.SansSerif) }, singleLine = true, isError = isError && name.isBlank(), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                OutlinedTextField(value = priceStr, onValueChange = { priceStr = it }, label = { Text("花了多少钱", fontFamily = FontFamily.SansSerif) }, singleLine = true, isError = isError && (priceStr.toDoubleOrNull() ?: 0.0) <= 0, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                OutlinedTextField(value = dateStr, onValueChange = { dateStr = it }, label = { Text("什么时候败的", fontFamily = FontFamily.SansSerif) }, singleLine = true, isError = isError && runCatching { dateFormatYMD.parse(dateStr) }.isFailure, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val price = priceStr.toDoubleOrNull() ?: 0.0
                val dateMillis = try { dateFormatYMD.parse(dateStr)?.time ?: 0L } catch (e: Exception) { 0L }
                if (name.isNotBlank() && price > 0 && dateMillis > 0) onEdit(name, price, dateMillis) else isError = true
            }) { Text("改好了", color = IosBlue, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("算了", color = IosBlue, fontFamily = FontFamily.SansSerif) } }
    )
}

@Composable
fun SellItemDialog(item: Item, onDismiss: () -> Unit, onSell: (Double, Long) -> Unit) {
    var priceStr by remember { mutableStateOf(if(item.residualValue > 0) item.residualValue.toString() else "") }
    var dateStr by remember { mutableStateOf(dateFormatYMD.format(Date())) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IosBg,
        shape = RoundedCornerShape(14.dp),
        title = { Text("终于卖了？", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, color = IosTextPrimary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column {
                Text("来算算【${item.name}】到底亏了你多少钱。", fontFamily = FontFamily.SansSerif, fontSize = 13.sp, color = IosTextSecondary, modifier = Modifier.padding(bottom = 12.dp), textAlign = TextAlign.Center)
                OutlinedTextField(value = priceStr, onValueChange = { priceStr = it }, label = { Text("卖了多少钱", fontFamily = FontFamily.SansSerif) }, singleLine = true, isError = isError && priceStr.isBlank(), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                OutlinedTextField(value = dateStr, onValueChange = { dateStr = it }, label = { Text("哪天出手的", fontFamily = FontFamily.SansSerif) }, singleLine = true, isError = isError && runCatching { dateFormatYMD.parse(dateStr) }.isFailure, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val price = priceStr.toDoubleOrNull() ?: 0.0
                val dateMillis = try { dateFormatYMD.parse(dateStr)?.time ?: 0L } catch (e: Exception) { 0L }
                if (dateMillis > 0) onSell(price, dateMillis) else isError = true
            }) { Text("面对现实", color = IosBlue, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("算了", color = IosBlue, fontFamily = FontFamily.SansSerif) } }
    )
}
