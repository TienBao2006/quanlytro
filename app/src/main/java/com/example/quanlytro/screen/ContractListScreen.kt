package com.example.quanlytro.screen

import com.example.quanlytro.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractListScreen(onBackClick: () -> Unit = {}) {
    var contracts by remember { mutableStateOf<List<ContractItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var debugMsg by remember { mutableStateOf("") }
    var selectedContract by remember { mutableStateOf<ContractItem?>(null) }
    var editingContract by remember { mutableStateOf<ContractItem?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf("Tất cả", "Chờ xác nhận", "Đã xác nhận", "Từ chối", "Yêu cầu hủy", "Gia hạn", "Đã hủy")
    val statusFilter = listOf(null, "active", "agreed", "rejected", "cancel_requested_by_tenant", "renew_requested", "cancelled")

    fun load() {
        isLoading = true
        RetrofitClient.instance.getContractsByLandlord(UserSession.uid)
            .enqueue(object : Callback<ContractListResponse> {
                override fun onResponse(call: Call<ContractListResponse>, response: Response<ContractListResponse>) {
                    contracts = response.body()?.contracts ?: emptyList()
                    isLoading = false
                }
                override fun onFailure(call: Call<ContractListResponse>, t: Throwable) {
                    isLoading = false
                }
            })
    }

    LaunchedEffect(Unit) { load() }

    selectedContract?.let { contract ->
        LandlordContractDetailDialog(
            contract  = contract,
            onDismiss = { selectedContract = null },
            onEdit    = { editingContract = it; selectedContract = null },
            onRefresh = { selectedContract = null; load() }
        )
    }

    editingContract?.let { contract ->
        EditContractDialog(
            contract  = contract,
            onDismiss = { editingContract = null },
            onSuccess = { editingContract = null; load() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Danh sách hợp đồng", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).background(Color(0xFFF8F9FA))
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF007BFF),
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { i, title ->
                    val count = if (statusFilter[i] == null) contracts.size
                                else contracts.count { it.status == statusFilter[i] }
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        text = {
                            Text(
                                if (count > 0) "$title ($count)" else title,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF007BFF))
                }
            } else {
                val filtered = if (statusFilter[selectedTab] == null) contracts
                               else contracts.filter { it.status == statusFilter[selectedTab] }

                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Description, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                            Spacer(Modifier.height(12.dp))
                            Text("Chưa có hợp đồng nào", color = Color.Gray)
                            if (debugMsg.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(debugMsg, color = Color.Red, fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered, key = { it.id }) { contract ->
                            LandlordContractCard(
                                contract = contract,
                                onClick = { selectedContract = contract }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LandlordContractCard(contract: ContractItem, onClick: () -> Unit) {
    val (statusColor, statusText) = when (contract.status) {
        "agreed"                     -> Color(0xFF4CAF50) to "Đã xác nhận"
        "rejected"                   -> Color(0xFFE53935) to "Đã từ chối"
        "cancelled"                  -> Color(0xFF9E9E9E) to "Đã hủy"
        "cancel_requested"           -> Color(0xFFFF5722) to "Chờ khách xác nhận hủy"
        "cancel_requested_by_tenant" -> Color(0xFFFF9800) to "Khách yêu cầu hủy"
        "renew_requested"            -> Color(0xFF2196F3) to "Khách yêu cầu gia hạn"
        else                         -> Color(0xFFFF9800) to "Chờ xác nhận"
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Hợp đồng #${contract.id}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Ngày tạo: ${contract.created_at.take(10)}", fontSize = 12.sp, color = Color.Gray)
                }
                Surface(color = statusColor.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp)) {
                    Text(
                        statusText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF0F0F0))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Person, null, tint = Color(0xFF007BFF), modifier = Modifier.size(15.dp))
                Text(contract.tenant_name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("• ${contract.tenant_phone}", fontSize = 12.sp, color = Color.Gray)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Home, null, tint = Color.Gray, modifier = Modifier.size(15.dp))
                Text(contract.room_name ?: "-", fontSize = 13.sp, color = Color(0xFF0D1B34))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.MonetizationOn, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Text("${formatPrice(contract.rent_price)}đ/tháng", fontSize = 12.sp, color = Color.Gray)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.DateRange, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Text("${contract.start_date}  •  ${contract.duration_months} tháng", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandlordContractDetailDialog(
    contract: ContractItem,
    onDismiss: () -> Unit,
    onEdit: (ContractItem) -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    val (statusColor, statusText) = when (contract.status) {
        "agreed"                     -> Color(0xFF4CAF50) to "Đã xác nhận"
        "rejected"                   -> Color(0xFFE53935) to "Đã từ chối"
        "cancelled"                  -> Color(0xFF9E9E9E) to "Đã hủy"
        "cancel_requested"           -> Color(0xFFFF5722) to "Chờ khách xác nhận hủy"
        "cancel_requested_by_tenant" -> Color(0xFFFF9800) to "Khách yêu cầu hủy"
        "renew_requested"            -> Color(0xFF2196F3) to "Khách yêu cầu gia hạn"
        else                         -> Color(0xFFFF9800) to "Chờ xác nhận"
    }

    var showCancelConfirm by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val snackState = remember { SnackbarHostState() }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text("Xác nhận hủy hợp đồng") },
            text = { Text("Bạn có chắc muốn hủy hợp đồng #${contract.id}? Hành động này không thể hoàn tác.") },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelConfirm = false
                        isLoading = true
                        RetrofitClient.instance.cancelContract(contract.id, UserSession.uid, "landlord")
                            .enqueue(object : Callback<SimpleResponse> {
                                override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                                    isLoading = false
                                    if (response.body()?.status == "success") onRefresh()
                                }
                                override fun onFailure(call: Call<SimpleResponse>, t: Throwable) { isLoading = false }
                            })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) { Text("Hủy hợp đồng") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) { Text("Không") }
            }
        )
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Hợp đồng #${contract.id}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(contract.created_at.take(10), fontSize = 12.sp, color = Color.Gray)
                    }
                    Surface(color = statusColor.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp)) {
                        Text(statusText, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
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

                // Khách yêu cầu hủy — chủ phản hồi
                if (contract.status == "cancel_requested_by_tenant") {
                    Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFFF5722), modifier = Modifier.size(18.dp))
                            Text("Người thuê muốn hủy hợp đồng. Bạn có đồng ý không?",
                                fontSize = 13.sp, color = Color(0xFFBF360C), lineHeight = 18.sp)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                isLoading = true
                                RetrofitClient.instance.respondCancelContract(contract.id, UserSession.uid, "landlord", "reject")
                                    .enqueue(object : Callback<SimpleResponse> {
                                        override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                                            isLoading = false
                                            if (response.body()?.status == "success") onRefresh()
                                        }
                                        override fun onFailure(call: Call<SimpleResponse>, t: Throwable) { isLoading = false }
                                    })
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50)),
                            enabled = !isLoading
                        ) {
                            Text("Không đồng ý", fontSize = 13.sp)
                        }
                        Button(
                            onClick = {
                                isLoading = true
                                RetrofitClient.instance.respondCancelContract(contract.id, UserSession.uid, "landlord", "accept")
                                    .enqueue(object : Callback<SimpleResponse> {
                                        override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                                            isLoading = false
                                            if (response.body()?.status == "success") onRefresh()
                                        }
                                        override fun onFailure(call: Call<SimpleResponse>, t: Throwable) { isLoading = false }
                                    })
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            enabled = !isLoading
                        ) {
                            Text("Đồng ý hủy", fontSize = 13.sp)
                        }
                    }
                }

                // Khách yêu cầu gia hạn — chủ phản hồi
                if (contract.status == "renew_requested") {
                    Surface(color = Color(0xFFE3F2FD), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Autorenew, null, tint = Color(0xFF2196F3), modifier = Modifier.size(18.dp))
                            Text("Người thuê yêu cầu gia hạn thêm ${contract.renew_requested_months} tháng. Bạn có đồng ý không?",
                                fontSize = 13.sp, color = Color(0xFF1565C0), lineHeight = 18.sp)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                isLoading = true
                                RetrofitClient.instance.respondRenewContract(contract.id, UserSession.uid, "reject")
                                    .enqueue(object : Callback<SimpleResponse> {
                                        override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                                            isLoading = false
                                            if (response.body()?.status == "success") onRefresh()
                                        }
                                        override fun onFailure(call: Call<SimpleResponse>, t: Throwable) { isLoading = false }
                                    })
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935)),
                            enabled = !isLoading
                        ) { Text("Từ chối", fontSize = 13.sp) }
                        Button(
                            onClick = {
                                isLoading = true
                                RetrofitClient.instance.respondRenewContract(contract.id, UserSession.uid, "accept")
                                    .enqueue(object : Callback<SimpleResponse> {
                                        override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                                            isLoading = false
                                            if (response.body()?.status == "success") onRefresh()
                                        }
                                        override fun onFailure(call: Call<SimpleResponse>, t: Throwable) { isLoading = false }
                                    })
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            enabled = !isLoading
                        ) { Text("Đồng ý gia hạn", fontSize = 13.sp) }
                    }
                }

                // Nút Sửa/Yêu cầu hủy (chỉ khi chưa hủy và không đang chờ phản hồi)
                if (contract.status !in listOf("cancelled", "cancel_requested", "cancel_requested_by_tenant", "renew_requested")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCancelConfirm = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935)),
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.Cancel, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Yêu cầu hủy", fontSize = 13.sp)
                        }
                        Button(
                            onClick = { onEdit(contract) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF)),
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Sửa HĐ", fontSize = 13.sp)
                        }
                    }
                }

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Đóng")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContractDialog(contract: ContractItem, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var rentPrice       by remember { mutableStateOf(contract.rent_price.toLong().toString()) }
    var deposit         by remember { mutableStateOf(contract.deposit.toLong().toString()) }
    var electricPrice   by remember { mutableStateOf(if (contract.electric_price > 0) contract.electric_price.toLong().toString() else "") }
    var waterPrice      by remember { mutableStateOf(if (contract.water_price > 0) contract.water_price.toLong().toString() else "") }
    var otherFee        by remember { mutableStateOf(if (contract.other_fee > 0) contract.other_fee.toLong().toString() else "") }
    var otherFeeNote    by remember { mutableStateOf(contract.other_fee_note ?: "") }
    var startDate       by remember { mutableStateOf(contract.start_date) }
    var durationMonths  by remember { mutableStateOf(contract.duration_months.toString()) }
    var paymentDay      by remember { mutableStateOf(contract.payment_day.toString()) }
    var paymentMethod   by remember { mutableStateOf(contract.payment_method) }
    var latePaymentRule by remember { mutableStateOf(contract.late_payment_rule ?: "") }
    var rules           by remember { mutableStateOf(contract.rules ?: "") }
    var terminationDays by remember { mutableStateOf(contract.termination_notice_days.toString()) }
    var depositReturn   by remember { mutableStateOf(contract.deposit_return_condition ?: "") }
    var roomName        by remember { mutableStateOf(contract.room_name ?: "") }
    var roomAddress     by remember { mutableStateOf(contract.room_address ?: "") }
    var roomArea        by remember { mutableStateOf(if (contract.room_area > 0) contract.room_area.toString() else "") }
    var amenities       by remember { mutableStateOf(contract.amenities ?: "") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMsg  by remember { mutableStateOf<String?>(null) }
    val snackState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    LaunchedEffect(errorMsg) {
        errorMsg?.let { snackState.showSnackbar(it); errorMsg = null }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
                        startDate = "%04d-%02d-%02d".format(
                            cal.get(java.util.Calendar.YEAR),
                            cal.get(java.util.Calendar.MONTH) + 1,
                            cal.get(java.util.Calendar.DAY_OF_MONTH)
                        )
                    }
                    showDatePicker = false
                }) { Text("Chọn") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Hủy") } }
        ) { DatePicker(state = datePickerState) }
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackState) },
                containerColor = Color.Transparent
            ) { padding ->
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(padding)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Sửa hợp đồng #${contract.id}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    HorizontalDivider()

                    ContractSection(title = "Thông tin phòng", icon = Icons.Default.Home) {
                        ContractField("Tên/Mã phòng", roomName, { roomName = it })
                        ContractField("Địa chỉ phòng", roomAddress, { roomAddress = it })
                        ContractField("Diện tích (m²)", roomArea, { roomArea = it }, keyboardType = KeyboardType.Decimal)
                        ContractFieldMultiline("Trang thiết bị", amenities, { amenities = it })
                    }

                    ContractSection(title = "Chi phí thuê", icon = Icons.Default.MonetizationOn) {
                        ContractField("Giá thuê/tháng (VND) *", rentPrice, { rentPrice = it }, keyboardType = KeyboardType.Number)
                        ContractField("Tiền cọc (VND) *", deposit, { deposit = it }, keyboardType = KeyboardType.Number)
                        ContractField("Tiền điện (đ/kWh)", electricPrice, { electricPrice = it }, keyboardType = KeyboardType.Number)
                        ContractField("Tiền nước (đ/m³)", waterPrice, { waterPrice = it }, keyboardType = KeyboardType.Number)
                        ContractField("Phí khác (VND)", otherFee, { otherFee = it }, keyboardType = KeyboardType.Number)
                        ContractField("Ghi chú phí khác", otherFeeNote, { otherFeeNote = it })
                    }

                    ContractSection(title = "Thời gian & thanh toán", icon = Icons.Default.Schedule) {
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = { startDate = it },
                            label = { Text("Ngày bắt đầu", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF007BFF))
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF007BFF),
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            )
                        )
                        ContractField("Thời hạn (tháng)", durationMonths, { durationMonths = it }, keyboardType = KeyboardType.Number)
                        ContractField("Ngày thanh toán", paymentDay, { paymentDay = it }, keyboardType = KeyboardType.Number)
                        Text("Hình thức thanh toán", fontSize = 13.sp, color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("cash" to "Tiền mặt", "transfer" to "Chuyển khoản").forEach { (v, l) ->
                                FilterChip(selected = paymentMethod == v, onClick = { paymentMethod = v }, label = { Text(l, fontSize = 13.sp) })
                            }
                        }
                        ContractField("Quy định trễ hạn", latePaymentRule, { latePaymentRule = it })
                    }

                    ContractSection(title = "Quy định & điều khoản", icon = Icons.Default.Description) {
                        ContractFieldMultiline("Nội quy", rules, { rules = it }, minLines = 3)
                    }

                    ContractSection(title = "Điều kiện chấm dứt", icon = Icons.Default.Cancel) {
                        ContractField("Báo trước (ngày)", terminationDays, { terminationDays = it }, keyboardType = KeyboardType.Number)
                        ContractFieldMultiline("Điều kiện hoàn cọc", depositReturn, { depositReturn = it })
                    }

                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text("Hủy bỏ")
                        }
                        Button(
                            onClick = {
                                if (rentPrice.isBlank() || deposit.isBlank()) {
                                    errorMsg = "Vui lòng nhập giá thuê và tiền cọc"; return@Button
                                }
                                isLoading = true
                                RetrofitClient.instance.updateContract(
                                    contractId             = contract.id,
                                    landlordId             = UserSession.uid,
                                    rentPrice              = rentPrice.replace(",", "").toDoubleOrNull() ?: 0.0,
                                    deposit                = deposit.replace(",", "").toDoubleOrNull() ?: 0.0,
                                    electricPrice          = electricPrice.replace(",", "").toDoubleOrNull() ?: 0.0,
                                    waterPrice             = waterPrice.replace(",", "").toDoubleOrNull() ?: 0.0,
                                    otherFee               = otherFee.replace(",", "").toDoubleOrNull() ?: 0.0,
                                    otherFeeNote           = otherFeeNote,
                                    startDate              = startDate,
                                    durationMonths         = durationMonths.toIntOrNull() ?: 12,
                                    paymentDay             = paymentDay.toIntOrNull() ?: 5,
                                    paymentMethod          = paymentMethod,
                                    latePaymentRule        = latePaymentRule,
                                    rules                  = rules,
                                    terminationNoticeDays  = terminationDays.toIntOrNull() ?: 30,
                                    depositReturnCondition = depositReturn,
                                    roomName               = roomName,
                                    roomAddress            = roomAddress,
                                    roomArea               = roomArea.toDoubleOrNull() ?: 0.0,
                                    amenities              = amenities
                                ).enqueue(object : Callback<SimpleResponse> {
                                    override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                                        isLoading = false
                                        if (response.body()?.status == "success") onSuccess()
                                        else errorMsg = response.body()?.message ?: "Cập nhật thất bại"
                                    }
                                    override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                                        isLoading = false; errorMsg = "Lỗi kết nối: ${t.message}"
                                    }
                                })
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF)),
                            enabled = !isLoading
                        ) {
                            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Text("Lưu thay đổi")
                        }
                    }
                }
            }
        }
    }
}


