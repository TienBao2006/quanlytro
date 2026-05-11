package package com.example.quanlytro.features.notification
import com.example.quanlytro.data.remote.*
import com.example.quanlytro.data.model.*

import com.example.quanlytro.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(onBackClick: () -> Unit = {}) {
    var contracts by remember { mutableStateOf<List<ContractItem>>(emptyList()) }
    var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var snackMsg by remember { mutableStateOf<String?>(null) }
    val snackState = remember { SnackbarHostState() }
    var selectedContract by remember { mutableStateOf<ContractItem?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0=Chung, 1=Hóa đơn, 2=Chủ trọ, 3=Hợp đồng, 4=Sắp hết hạn

    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackState.showSnackbar(it); snackMsg = null }
    }

    fun loadAll() {
        isLoading = true
        // Load notifications
        RetrofitClient.instance.getNotifications(UserSession.uid)
            .enqueue(object : Callback<NotificationListResponse> {
                override fun onResponse(call: Call<NotificationListResponse>, response: Response<NotificationListResponse>) {
                    notifications = response.body()?.notifications ?: emptyList()
                    isLoading = false
                }
                override fun onFailure(call: Call<NotificationListResponse>, t: Throwable) { isLoading = false }
            })
        // Load contracts (cho người thuê)
        RetrofitClient.instance.getContractsByTenant(UserSession.uid)
            .enqueue(object : Callback<ContractListResponse> {
                override fun onResponse(call: Call<ContractListResponse>, response: Response<ContractListResponse>) {
                    contracts = response.body()?.contracts ?: emptyList()
                }
                override fun onFailure(call: Call<ContractListResponse>, t: Throwable) {}
            })
    }

    fun respond(contractId: Int, action: String) {
        RetrofitClient.instance.confirmContract(contractId, UserSession.uid, action)
            .enqueue(object : Callback<SimpleResponse> {
                override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                    val body = response.body()
                    if (body?.status == "success") {
                        snackMsg = if (action == "agreed") "Đã xác nhận hợp đồng" else "Đã từ chối hợp đồng"
                        selectedContract = null
                        loadAll()
                    } else {
                        snackMsg = "Lỗi: ${body?.message ?: "Không cập nhật được"}"
                    }
                }
                override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                    snackMsg = "Lỗi kết nối: ${t.message}"
                }
            })
    }

    fun respondCancel(contractId: Int, action: String) {
        RetrofitClient.instance.respondCancelContract(contractId, UserSession.uid, "tenant", action)
            .enqueue(object : Callback<SimpleResponse> {
                override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                    val body = response.body()
                    if (body?.status == "success") {
                        snackMsg = if (action == "accept") "Đã đồng ý hủy hợp đồng" else "Đã từ chối yêu cầu hủy"
                        selectedContract = null
                        loadAll()
                    } else {
                        snackMsg = "Lỗi: ${body?.message ?: "Không cập nhật được"}"
                    }
                }
                override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                    snackMsg = "Lỗi kết nối: ${t.message}"
                }
            })
    }

    LaunchedEffect(Unit) { loadAll() }

    selectedContract?.let { contract ->
        ContractDetailDialog(
            contract  = contract,
            onAgree   = { respond(contract.id, "agreed") },
            onReject  = { respond(contract.id, "rejected") },
            onAcceptCancel = { respondCancel(contract.id, "accept") },
            onRejectCancel = { respondCancel(contract.id, "reject") },
            onDismiss = { selectedContract = null }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackState) },
        topBar = {
            TopAppBar(
                title = { Text("Thông báo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Tab row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF007BFF)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Chung", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Notifications, null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Hóa đơn", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Receipt, null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Chủ trọ", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Campaign, null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Hợp đồng", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Description, null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("Sắp hết hạn", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF007BFF))
                }
            } else when (selectedTab) {
                0 -> {
                    // Tab chung — booking_response và các loại khác
                    val knownTypes = setOf("payment", "invoice", "landlord_notice", "contract_expiring")
                    val generalNotifs = notifications.filter { it.type !in knownTypes }
                    if (generalNotifs.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Notifications, null, modifier = Modifier.size(72.dp), tint = Color.LightGray)
                                Spacer(Modifier.height(12.dp))
                                Text("Chưa có thông báo chung", color = Color.Gray, fontSize = 15.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA)),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(generalNotifs, key = { it.id }) { notif ->
                                SwipeToDeleteNotif(notif, onDelete = { notifications = notifications.filter { it.id != notif.id } }) {
                                    PaymentNotificationCard(notif)
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Tab hóa đơn
                    val invoiceNotifs = notifications.filter { it.type == "payment" || it.type == "invoice" }
                    if (invoiceNotifs.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ReceiptLong, null, modifier = Modifier.size(72.dp), tint = Color.LightGray)
                                Spacer(Modifier.height(12.dp))
                                Text("Chưa có thông báo hóa đơn", color = Color.Gray, fontSize = 15.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA)),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(invoiceNotifs, key = { it.id }) { notif ->
                                SwipeToDeleteNotif(notif, onDelete = { notifications = notifications.filter { it.id != notif.id } }) {
                                    PaymentNotificationCard(notif)
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Tab thông báo chủ trọ
                    val landlordNotifs = notifications.filter { it.type == "landlord_notice" }
                    if (landlordNotifs.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Campaign, null, modifier = Modifier.size(72.dp), tint = Color.LightGray)
                                Spacer(Modifier.height(12.dp))
                                Text("Chưa có thông báo từ chủ trọ", color = Color.Gray, fontSize = 15.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA)),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(landlordNotifs, key = { it.id }) { notif ->
                                SwipeToDeleteNotif(notif, onDelete = { notifications = notifications.filter { it.id != notif.id } }) {
                                    PaymentNotificationCard(notif)
                                }
                            }
                        }
                    }
                }
                3 -> {
                    if (contracts.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Description, null, modifier = Modifier.size(72.dp), tint = Color.LightGray)
                                Spacer(Modifier.height(12.dp))
                                Text("Không có hợp đồng nào", color = Color.Gray, fontSize = 15.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA)),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(contracts, key = { it.id }) { contract ->
                                ContractNotificationCard(
                                    contract = contract,
                                    onViewDetail = { selectedContract = contract }
                                )
                            }
                        }
                    }
                }
                4 -> {
                    // Tab sắp hết hạn
                    val expiringNotifs = notifications.filter { it.type == "contract_expiring" }
                    if (expiringNotifs.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(72.dp), tint = Color.LightGray)
                                Spacer(Modifier.height(12.dp))
                                Text("Không có hợp đồng nào sắp hết hạn", color = Color.Gray, fontSize = 15.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA)),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(expiringNotifs, key = { it.id }) { notif ->
                                SwipeToDeleteNotif(notif, onDelete = { notifications = notifications.filter { it.id != notif.id } }) {
                                    ExpiringContractNotificationCard(notif)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContractNotificationCard(contract: ContractItem, onViewDetail: () -> Unit) {
    val (statusColor, statusText) = when (contract.status) {
        "agreed"                    -> Color(0xFF4CAF50) to "Đã xác nhận"
        "rejected"                  -> Color(0xFFE53935) to "Đã từ chối"
        "cancelled"                 -> Color(0xFF9E9E9E) to "Đã hủy"
        "cancel_requested"          -> Color(0xFFFF5722) to "Chủ yêu cầu hủy"
        "cancel_requested_by_tenant"-> Color(0xFFFF9800) to "Bạn đã yêu cầu hủy"
        else                        -> Color(0xFFFF9800) to "Chờ xác nhận"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFE3F2FD)) {
                        Icon(Icons.Default.Description, null, tint = Color(0xFF007BFF),
                            modifier = Modifier.padding(10.dp).size(24.dp))
                    }
                    Column {
                        Text("Hợp đồng thuê phòng", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Từ: ${contract.landlord_name}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Surface(color = statusColor.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp)) {
                    Text(statusText, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
                }
            }

            HorizontalDivider(color = Color(0xFFF0F0F0))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Home, null, tint = Color(0xFF007BFF), modifier = Modifier.size(15.dp))
                Text(contract.room_name ?: "Phòng trọ", fontSize = 13.sp, color = Color(0xFF0D1B34))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.MonetizationOn, null, tint = Color.Gray, modifier = Modifier.size(15.dp))
                Text("${formatPrice(contract.rent_price)}đ/tháng", fontSize = 13.sp, color = Color.Gray)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.DateRange, null, tint = Color.Gray, modifier = Modifier.size(15.dp))
                Text("Bắt đầu: ${contract.start_date}  •  ${contract.duration_months} tháng", fontSize = 13.sp, color = Color.Gray)
            }

            if (contract.status == "active") {
                OutlinedButton(
                    onClick = onViewDetail,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Xem hợp đồng")
                }
            } else if (contract.status == "cancel_requested") {
                // Chủ yêu cầu hủy — khách cần phản hồi
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFFF5722), modifier = Modifier.size(16.dp))
                        Text("Chủ trọ yêu cầu hủy hợp đồng này", fontSize = 12.sp, color = Color(0xFFFF5722))
                    }
                }
                OutlinedButton(
                    onClick = onViewDetail,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5722))
                ) {
                    Icon(Icons.Default.NotificationImportant, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Phản hồi yêu cầu hủy")
                }
            } else {
                TextButton(onClick = onViewDetail, contentPadding = PaddingValues(0.dp)) {
                    Text("Xem chi tiết ▼", fontSize = 12.sp, color = Color(0xFF007BFF))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractDetailDialog(
    contract: ContractItem,
    onAgree: () -> Unit,
    onReject: () -> Unit,
    onAcceptCancel: () -> Unit = {},
    onRejectCancel: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val (statusColor, statusText) = when (contract.status) {
        "agreed"                    -> Color(0xFF4CAF50) to "Đã xác nhận"
        "rejected"                  -> Color(0xFFE53935) to "Đã từ chối"
        "cancelled"                 -> Color(0xFF9E9E9E) to "Đã hủy"
        "cancel_requested"          -> Color(0xFFFF5722) to "Chủ yêu cầu hủy"
        "cancel_requested_by_tenant"-> Color(0xFFFF9800) to "Bạn yêu cầu hủy"
        else                        -> Color(0xFFFF9800) to "Chờ xác nhận"
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Description, null, tint = Color(0xFF007BFF), modifier = Modifier.size(28.dp))
                    Column {
                        Text("Hợp đồng thuê phòng", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Mã HĐ: #${contract.id}  •  ${contract.created_at.take(10)}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                HorizontalDivider()

                ContractDialogSection("Thông tin chủ trọ") {
                    ContractDialogRow("Họ tên", contract.landlord_name)
                    ContractDialogRow("Điện thoại", contract.landlord_phone)
                    if (!contract.landlord_address.isNullOrBlank()) ContractDialogRow("Địa chỉ", contract.landlord_address)
                }

                ContractDialogSection("Thông tin người thuê") {
                    ContractDialogRow("Họ tên", contract.tenant_name)
                    ContractDialogRow("Điện thoại", contract.tenant_phone)
                    ContractDialogRow("CCCD/CMND", contract.tenant_id_card)
                    if (!contract.tenant_address.isNullOrBlank()) ContractDialogRow("Thường trú", contract.tenant_address)
                }

                ContractDialogSection("Thông tin phòng") {
                    ContractDialogRow("Phòng", contract.room_name ?: "-")
                    ContractDialogRow("Địa chỉ", contract.room_address ?: "-")
                    if (contract.room_area > 0) ContractDialogRow("Diện tích", "${contract.room_area} m²")
                    if (!contract.amenities.isNullOrBlank()) ContractDialogRow("Trang thiết bị", contract.amenities)
                }

                ContractDialogSection("Chi phí") {
                    ContractDialogRow("Giá thuê/tháng", "${formatPrice(contract.rent_price)}đ")
                    ContractDialogRow("Tiền cọc", "${formatPrice(contract.deposit)}đ")
                    if (contract.electric_price > 0) ContractDialogRow("Tiền điện", "${formatPrice(contract.electric_price)}đ/kWh")
                    if (contract.water_price > 0) ContractDialogRow("Tiền nước", "${formatPrice(contract.water_price)}đ/m³")
                    if (contract.other_fee > 0) ContractDialogRow("Phí khác", "${formatPrice(contract.other_fee)}đ (${contract.other_fee_note ?: ""})")
                }

                ContractDialogSection("Thời gian & thanh toán") {
                    ContractDialogRow("Ngày bắt đầu", contract.start_date)
                    ContractDialogRow("Thời hạn", "${contract.duration_months} tháng")
                    ContractDialogRow("Thanh toán ngày", "${contract.payment_day} hàng tháng")
                    ContractDialogRow("Hình thức", if (contract.payment_method == "cash") "Tiền mặt" else "Chuyển khoản")
                    if (!contract.late_payment_rule.isNullOrBlank()) ContractDialogRow("Trễ hạn", contract.late_payment_rule)
                }

                if (!contract.rules.isNullOrBlank()) {
                    ContractDialogSection("Quy định") {
                        Text(contract.rules, fontSize = 13.sp, color = Color(0xFF0D1B34), lineHeight = 20.sp)
                    }
                }

                ContractDialogSection("Chấm dứt hợp đồng") {
                    ContractDialogRow("Báo trước", "${contract.termination_notice_days} ngày")
                    if (!contract.deposit_return_condition.isNullOrBlank())
                        ContractDialogRow("Hoàn cọc", contract.deposit_return_condition)
                }

                HorizontalDivider()

                when (contract.status) {
                    "active" -> {
                        Text("Bạn có đồng ý với các điều khoản trong hợp đồng này không?",
                            fontSize = 13.sp, color = Color.Gray, lineHeight = 18.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = onReject, modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                            ) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Từ chối", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onAgree, modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Đồng ý", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    "cancel_requested" -> {
                        // Chủ yêu cầu hủy — khách phản hồi
                        Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Warning, null, tint = Color(0xFFFF5722), modifier = Modifier.size(18.dp))
                                Text("Chủ trọ muốn hủy hợp đồng này. Bạn có đồng ý không?",
                                    fontSize = 13.sp, color = Color(0xFFBF360C), lineHeight = 18.sp)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = onRejectCancel, modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50))
                            ) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Không đồng ý", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Button(
                                onClick = onAcceptCancel, modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                            ) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Đồng ý hủy", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                    else -> {
                        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                            Text("Đóng")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContractDialogSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF007BFF))
        content()
    }
}

@Composable
fun ContractDialogRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.width(110.dp))
        Text(value, fontSize = 12.sp, color = Color(0xFF0D1B34), fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}

@Composable
fun PaymentNotificationCard(notif: NotificationItem) {
    val isPayment = notif.type == "payment"
    val isLandlordNotice = notif.type == "landlord_notice"
    val iconColor = when {
        isPayment       -> Color(0xFF4CAF50)
        isLandlordNotice -> Color(0xFFFF9800)
        else            -> Color(0xFF007BFF)
    }
    val bgColor = when {
        isPayment       -> Color(0xFFE8F5E9)
        isLandlordNotice -> Color(0xFFFFF3E0)
        else            -> Color(0xFFE3F2FD)
    }
    val icon = when {
        isPayment       -> Icons.Default.Payments
        isLandlordNotice -> Icons.Default.Campaign
        else            -> Icons.Default.Notifications
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(bgColor, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        notif.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF0D1B34),
                        modifier = Modifier.weight(1f)
                    )
                    if (notif.is_read == 0) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF007BFF), androidx.compose.foundation.shape.CircleShape)
                        )
                    }
                }
                Text(
                    notif.message,
                    fontSize = 13.sp,
                    color = Color(0xFF555555),
                    lineHeight = 19.sp
                )
                Text(
                    formatDateTime(notif.created_at),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ExpiringContractNotificationCard(notif: NotificationItem) {
    // Màu cảnh báo dựa theo nội dung (ngày còn lại)
    val isUrgent = notif.title.contains("ngày mai") || notif.title.contains("1 ngày") || notif.title.contains("3 ngày")
    val accentColor = if (isUrgent) Color(0xFFE53935) else Color(0xFFFF9800)
    val bgColor     = if (isUrgent) Color(0xFFFFEBEE) else Color(0xFFFFF3E0)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(bgColor, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        notif.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = accentColor,
                        modifier = Modifier.weight(1f)
                    )
                    if (notif.is_read == 0) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(accentColor, androidx.compose.foundation.shape.CircleShape)
                        )
                    }
                }
                Text(
                    notif.message,
                    fontSize = 13.sp,
                    color = Color(0xFF555555),
                    lineHeight = 19.sp
                )
                Text(
                    formatDateTime(notif.created_at),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteNotif(
    notif: NotificationItem,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                // Gọi API xóa
                RetrofitClient.instance.deleteNotification(notif.id, UserSession.uid)
                    .enqueue(object : retrofit2.Callback<SimpleResponse> {
                        override fun onResponse(call: retrofit2.Call<SimpleResponse>, response: retrofit2.Response<SimpleResponse>) {
                            if (response.body()?.status == "success") onDelete()
                        }
                        override fun onFailure(call: retrofit2.Call<SimpleResponse>, t: Throwable) {}
                    })
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                    Color(0xFFE53935) else Color.Transparent,
                label = "swipe_bg"
            )
            val scale by animateFloatAsState(
                targetValue = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) 1f else 0.75f,
                label = "swipe_scale"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(16.dp))
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Xóa",
                    tint = Color.White,
                    modifier = Modifier.scale(scale).size(24.dp)
                )
            }
        }
    ) {
        content()
    }
}
