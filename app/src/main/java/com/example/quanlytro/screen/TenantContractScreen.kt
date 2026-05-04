package com.example.quanlytro.screen

import com.example.quanlytro.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantContractScreen(onBackClick: () -> Unit = {}) {
    var contracts by remember { mutableStateOf<List<ContractItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedContract by remember { mutableStateOf<ContractItem?>(null) }

    fun load() {
        isLoading = true
        RetrofitClient.instance.getContractsByTenant(UserSession.uid)
            .enqueue(object : Callback<ContractListResponse> {
                override fun onResponse(call: Call<ContractListResponse>, response: Response<ContractListResponse>) {
                    contracts = response.body()?.contracts ?: emptyList()
                    isLoading = false
                }
                override fun onFailure(call: Call<ContractListResponse>, t: Throwable) { isLoading = false }
            })
    }

    LaunchedEffect(Unit) { load() }

    selectedContract?.let { contract ->
        TenantContractDetailDialog(
            contract  = contract,
            onDismiss = { selectedContract = null; load() },
            onRefresh = { selectedContract = null; load() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hợp đồng của tôi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF007BFF))
            }
        } else if (contracts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Description, null, modifier = Modifier.size(72.dp), tint = Color.LightGray)
                    Spacer(Modifier.height(12.dp))
                    Text("Chưa có hợp đồng nào", color = Color.Gray, fontSize = 15.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).background(Color(0xFFF8F9FA)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(contracts, key = { it.id }) { contract ->
                    TenantContractCard(contract = contract, onClick = { selectedContract = contract })
                }
            }
        }
    }
}

@Composable
fun TenantContractCard(contract: ContractItem, onClick: () -> Unit) {
    val (statusColor, statusText) = when (contract.status) {
        "agreed"                     -> Color(0xFF4CAF50) to "Đã xác nhận"
        "rejected"                   -> Color(0xFFE53935) to "Đã từ chối"
        "cancelled"                  -> Color(0xFF9E9E9E) to "Đã hủy"
        "cancel_requested"           -> Color(0xFFFF5722) to "Chờ bạn xác nhận hủy"
        "cancel_requested_by_tenant" -> Color(0xFFFF9800) to "Chờ chủ xác nhận hủy"
        "renew_requested"            -> Color(0xFF2196F3) to "Chờ chủ xác nhận gia hạn"
        else                         -> Color(0xFFFF9800) to "Chờ xác nhận"
    }

    // Tính ngày còn lại
    val daysLeft = remember(contract.start_date, contract.duration_months) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val start = sdf.parse(contract.start_date) ?: return@remember null
            val cal = Calendar.getInstance().apply { time = start }
            cal.add(Calendar.MONTH, contract.duration_months)
            val endTime = cal.timeInMillis
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val diff = ((endTime - today) / (1000 * 60 * 60 * 24)).toInt()
            if (diff >= 0) diff else null
        } catch (e: Exception) { null }
    }
    val isExpiringSoon = contract.status == "agreed" && daysLeft != null && daysLeft <= 30

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpiringSoon) Color(0xFFFFFDE7) else Color.White
        ),
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
                    Text(statusText, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
                }
            }

            // Badge cảnh báo sắp hết hạn
            if (isExpiringSoon && daysLeft != null) {
                val warnColor = if (daysLeft <= 7) Color(0xFFE53935) else Color(0xFFFF9800)
                Surface(
                    color = warnColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Warning, null, tint = warnColor, modifier = Modifier.size(15.dp))
                        Text(
                            if (daysLeft <= 1) "Hết hạn ngày mai!" else "Còn $daysLeft ngày hết hạn",
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = warnColor
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF0F0F0))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Home, null, tint = Color(0xFF007BFF), modifier = Modifier.size(15.dp))
                Text(contract.room_name ?: "-", fontSize = 13.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(15.dp))
                Text("Chủ trọ: ${contract.landlord_name}", fontSize = 13.sp, color = Color.Gray)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${formatPrice(contract.rent_price)}đ/tháng", fontSize = 12.sp, color = Color(0xFF007BFF), fontWeight = FontWeight.Bold)
                Text("${contract.start_date}  •  ${contract.duration_months} tháng", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantContractDetailDialog(contract: ContractItem, onDismiss: () -> Unit, onRefresh: () -> Unit = {}) {
    val (statusColor, statusText) = when (contract.status) {
        "agreed"                     -> Color(0xFF4CAF50) to "Đã xác nhận"
        "rejected"                   -> Color(0xFFE53935) to "Đã từ chối"
        "cancelled"                  -> Color(0xFF9E9E9E) to "Đã hủy"
        "cancel_requested"           -> Color(0xFFFF5722) to "Chủ yêu cầu hủy"
        "cancel_requested_by_tenant" -> Color(0xFFFF9800) to "Chờ chủ xác nhận hủy"
        "renew_requested"            -> Color(0xFF2196F3) to "Chờ chủ xác nhận gia hạn"
        else                         -> Color(0xFFFF9800) to "Chờ xác nhận"
    }

    var showCancelConfirm by remember { mutableStateOf(false) }
    var showRenewDialog by remember { mutableStateOf(false) }
    var renewMonths by remember { mutableStateOf("3") }
    var isLoading by remember { mutableStateOf(false) }
    var renewSuccess by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    if (showRenewDialog) {
        AlertDialog(
            onDismissRequest = { showRenewDialog = false },
            title = { Text("Yêu cầu gia hạn hợp đồng") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nhập số tháng muốn gia hạn thêm:", fontSize = 14.sp)
                    OutlinedTextField(
                        value = renewMonths,
                        onValueChange = { if (it.all { c -> c.isDigit() }) renewMonths = it },
                        label = { Text("Số tháng") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val months = renewMonths.toIntOrNull() ?: 0
                        if (months <= 0) { errorMsg = "Số tháng không hợp lệ"; return@Button }
                        showRenewDialog = false
                        isLoading = true
                        RetrofitClient.instance.requestRenewContract(contract.id, UserSession.uid, months)
                            .enqueue(object : Callback<SimpleResponse> {
                                override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                                    isLoading = false
                                    val body = response.body()
                                    if (body?.status == "success") {
                                        renewSuccess = true
                                    } else {
                                        renewSuccess = false
                                        errorMsg = "Lỗi: ${body?.message ?: "Không nhận được phản hồi (${response.code()})"}"
                                    }
                                }
                                override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                                    isLoading = false
                                    errorMsg = "Lỗi kết nối: ${t.message}"
                                }
                            })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) { Text("Gửi yêu cầu") }
            },
            dismissButton = {
                TextButton(onClick = { showRenewDialog = false }) { Text("Hủy") }
            }
        )
    }

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
                        RetrofitClient.instance.cancelContract(contract.id, UserSession.uid, "tenant")
                            .enqueue(object : Callback<SimpleResponse> {
                                override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                                    isLoading = false
                                    if (response.body()?.status == "success") { onRefresh(); onDismiss() }
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
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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

                when (contract.status) {
                    "cancelled" -> {
                        // Đã hủy — không làm gì thêm
                    }
                    "cancel_requested_by_tenant" -> {
                        // Khách đã yêu cầu, đang chờ chủ xác nhận
                        Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.HourglassTop, null, tint = Color(0xFFFF9800), modifier = Modifier.size(18.dp))
                                Text("Đang chờ chủ trọ xác nhận yêu cầu hủy của bạn",
                                    fontSize = 13.sp, color = Color(0xFFE65100), lineHeight = 18.sp)
                            }
                        }
                    }
                    "renew_requested" -> {
                        Surface(color = Color(0xFFE3F2FD), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Autorenew, null, tint = Color(0xFF2196F3), modifier = Modifier.size(18.dp))
                                Text("Đang chờ chủ trọ xác nhận gia hạn thêm ${contract.renew_requested_months} tháng",
                                    fontSize = 13.sp, color = Color(0xFF1565C0), lineHeight = 18.sp)
                            }
                        }
                    }                    "cancel_requested" -> {
                        // Chủ yêu cầu hủy — khách phản hồi
                        Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Warning, null, tint = Color(0xFFFF5722), modifier = Modifier.size(18.dp))
                                Text("Chủ trọ muốn hủy hợp đồng. Bạn có đồng ý không?",
                                    fontSize = 13.sp, color = Color(0xFFBF360C), lineHeight = 18.sp)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    isLoading = true
                                    RetrofitClient.instance.respondCancelContract(contract.id, UserSession.uid, "tenant", "reject")
                                        .enqueue(object : Callback<SimpleResponse> {
                                            override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                                                isLoading = false
                                                if (response.body()?.status == "success") { onRefresh(); onDismiss() }
                                            }
                                            override fun onFailure(call: Call<SimpleResponse>, t: Throwable) { isLoading = false }
                                        })
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50)),
                                enabled = !isLoading
                            ) { Text("Không đồng ý", fontSize = 13.sp) }
                            Button(
                                onClick = {
                                    isLoading = true
                                    RetrofitClient.instance.respondCancelContract(contract.id, UserSession.uid, "tenant", "accept")
                                        .enqueue(object : Callback<SimpleResponse> {
                                            override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                                                isLoading = false
                                                if (response.body()?.status == "success") { onRefresh(); onDismiss() }
                                            }
                                            override fun onFailure(call: Call<SimpleResponse>, t: Throwable) { isLoading = false }
                                        })
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                enabled = !isLoading
                            ) { Text("Đồng ý hủy", fontSize = 13.sp) }
                        }
                    }
                    else -> {
                        // agreed — có thể yêu cầu gia hạn hoặc hủy
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                onClick = { showRenewDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                                enabled = !isLoading
                            ) {
                                Icon(Icons.Default.Autorenew, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Gia hạn", fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Banner lỗi
                if (errorMsg != null) {
                    Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Error, null, tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                            Text(errorMsg ?: "", fontSize = 13.sp, color = Color(0xFFB71C1C))
                        }
                    }
                }

                // Banner thành công sau khi gửi yêu cầu gia hạn
                if (renewSuccess) {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                            Column {
                                Text("Đã gửi yêu cầu gia hạn thành công!", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                Text("Đang chờ chủ trọ xác nhận.", fontSize = 12.sp, color = Color(0xFF388E3C))
                            }
                        }
                    }
                }

                TextButton(
                    onClick = { if (renewSuccess) onRefresh(); onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Đóng")
                }
            }
        }
    }
}
