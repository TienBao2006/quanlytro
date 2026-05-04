package com.example.quanlytro.screen

import com.example.quanlytro.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
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
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateContractScreen(
    booking: BookingItem,
    landlordName: String,
    landlordPhone: String,
    onBackClick: () -> Unit = {},
    onSuccess: () -> Unit = {}
) {
    // Fetch post info để prefill
    var postData by remember { mutableStateOf<PostResponse?>(null) }
    LaunchedEffect(booking.post_id) {
        RetrofitClient.instance.getPosts().enqueue(object : Callback<List<PostResponse>> {
            override fun onResponse(call: Call<List<PostResponse>>, response: Response<List<PostResponse>>) {
                postData = response.body()?.find { it.id == booking.post_id }
            }
            override fun onFailure(call: Call<List<PostResponse>>, t: Throwable) {}
        })
    }

    // ── Thông tin người thuê (prefill từ booking) ──
    var tenantName    by remember { mutableStateOf(booking.full_name) }
    var tenantPhone   by remember { mutableStateOf(booking.phone) }
    var tenantIdCard  by remember { mutableStateOf(booking.id_card) }
    var tenantAddress by remember { mutableStateOf(booking.address ?: "") }

    // ── Thông tin chủ trọ ──
    var lLandlordName    by remember { mutableStateOf(landlordName) }
    var lLandlordPhone   by remember { mutableStateOf(landlordPhone) }
    var lLandlordAddress by remember { mutableStateOf("") }

    // ── Thông tin phòng — prefill khi postData load xong ──
    var roomName    by remember { mutableStateOf(booking.room_number ?: booking.post_title) }
    var roomAddress by remember { mutableStateOf("") }
    var roomArea    by remember { mutableStateOf("") }
    var amenities   by remember { mutableStateOf("") }

    // Cập nhật khi postData có
    LaunchedEffect(postData) {
        postData?.let { p ->
            if (roomAddress.isBlank()) roomAddress = p.location
            if (roomArea.isBlank() && p.area > 0) roomArea = p.area.toString()
            if (amenities.isBlank() && !p.amenities.isNullOrBlank()) amenities = p.amenities
        }
    }

    // ── Chi phí ──
    var rentPrice     by remember { mutableStateOf("") }
    var deposit       by remember { mutableStateOf("") }
    var electricPrice by remember { mutableStateOf("") }
    var waterPrice    by remember { mutableStateOf("") }
    var otherFee      by remember { mutableStateOf("") }
    var otherFeeNote  by remember { mutableStateOf("") }

    LaunchedEffect(postData) {
        postData?.let { p ->
            if (rentPrice.isBlank() && p.price > 0) rentPrice = p.price.toLong().toString()
        }
    }

    // ── Thời gian ──
    var startDate       by remember { mutableStateOf("") }
    var durationMonths  by remember { mutableStateOf("12") }
    var paymentDay      by remember { mutableStateOf("5") }
    var paymentMethod   by remember { mutableStateOf("cash") }
    var latePaymentRule by remember { mutableStateOf("Phạt 0.1%/ngày nếu trễ hạn thanh toán") }

    // ── Quy định ──
    var rules by remember { mutableStateOf("Không gây ồn ào sau 22h\nKhông nuôi thú cưng\nKhông sửa chữa khi chưa được phép\nGiữ gìn tài sản chung") }

    // ── Chấm dứt ──
    var terminationDays    by remember { mutableStateOf("30") }
    var depositReturnCond  by remember { mutableStateOf("Hoàn trả tiền cọc trong 7 ngày sau khi kết thúc hợp đồng nếu không có thiệt hại") }

    var isLoading  by remember { mutableStateOf(false) }
    var errorMsg   by remember { mutableStateOf<String?>(null) }
    val snackState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }
    var contractDone by remember { mutableStateOf(false) }

    // ── Màn hình tạo hợp đồng thành công ──────────────────────────────────
    if (contractDone) {
        ContractSuccessScreen(
            tenantName  = tenantName,
            tenantPhone = tenantPhone,
            roomName    = roomName,
            startDate   = startDate,
            onBackHome  = onSuccess
        )
        return
    }

    // DatePicker state
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance().apply { timeInMillis = millis }
                        startDate = "%04d-%02d-%02d".format(
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH) + 1,
                            cal.get(Calendar.DAY_OF_MONTH)
                        )
                    }
                    showDatePicker = false
                }) { Text("Chọn") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Hủy") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    LaunchedEffect(errorMsg) {
        errorMsg?.let { snackState.showSnackbar(it); errorMsg = null }
    }

    fun submit() {
        val missing = mutableListOf<String>()
        if (tenantName.isBlank())     missing.add("Họ tên người thuê")
        if (tenantPhone.isBlank())    missing.add("SĐT người thuê")
        if (tenantIdCard.isBlank())   missing.add("CCCD/CMND người thuê")
        if (lLandlordName.isBlank())  missing.add("Họ tên chủ trọ")
        if (lLandlordPhone.isBlank()) missing.add("SĐT chủ trọ")
        if (roomAddress.isBlank())    missing.add("Địa chỉ phòng")
        if (rentPrice.isBlank())      missing.add("Giá thuê")
        if (deposit.isBlank())        missing.add("Tiền cọc")
        if (startDate.isBlank())      missing.add("Ngày bắt đầu")
        if (durationMonths.isBlank()) missing.add("Thời hạn thuê")
        if (missing.isNotEmpty()) {
            errorMsg = "Thiếu: ${missing.joinToString(", ")}"; return
        }
        isLoading = true
        RetrofitClient.instance.createContract(
            bookingId               = booking.id,
            postId                  = booking.post_id,
            landlordId              = UserSession.uid,
            tenantId                = booking.user_id,
            tenantName              = tenantName,
            tenantPhone             = tenantPhone,
            tenantIdCard            = tenantIdCard,
            tenantAddress           = tenantAddress,
            landlordName            = lLandlordName,
            landlordPhone           = lLandlordPhone,
            landlordAddress         = lLandlordAddress,
            roomName                = roomName,
            roomAddress             = roomAddress,
            roomArea                = roomArea.toDoubleOrNull() ?: 0.0,
            amenities               = amenities,
            rentPrice               = rentPrice.replace(",", "").toDoubleOrNull() ?: 0.0,
            deposit                 = deposit.replace(",", "").toDoubleOrNull() ?: 0.0,
            electricPrice           = electricPrice.replace(",", "").toDoubleOrNull() ?: 0.0,
            waterPrice              = waterPrice.replace(",", "").toDoubleOrNull() ?: 0.0,
            otherFee                = otherFee.replace(",", "").toDoubleOrNull() ?: 0.0,
            otherFeeNote            = otherFeeNote,
            startDate               = startDate,
            durationMonths          = durationMonths.toIntOrNull() ?: 12,
            paymentDay              = paymentDay.toIntOrNull() ?: 5,
            paymentMethod           = paymentMethod,
            latePaymentRule         = latePaymentRule,
            rules                   = rules,
            terminationNoticeDays   = terminationDays.toIntOrNull() ?: 30,
            depositReturnCondition  = depositReturnCond
        ).enqueue(object : Callback<SimpleResponse> {
            override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                isLoading = false
                val body = response.body()
                if (body?.status == "success") contractDone = true
                else errorMsg = body?.message ?: "Tạo hợp đồng thất bại"
            }
            override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                isLoading = false; errorMsg = "Lỗi kết nối: ${t.message}"
            }
        })
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackState) },
        topBar = {
            TopAppBar(
                title = { Text("Tạo hợp đồng thuê", fontWeight = FontWeight.Bold) },
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F9FA))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Thông tin chung
            ContractSection(title = "1. Thông tin chung", icon = Icons.Default.DateRange) {
                // Ngày bắt đầu: vừa nhập tay vừa chọn lịch
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Ngày bắt đầu thuê *", fontSize = 13.sp) },
                    placeholder = { Text("VD: 2025-01-01", fontSize = 13.sp, color = Color.LightGray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
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
                ContractField("Thời hạn thuê (tháng) *", durationMonths, { durationMonths = it }, "VD: 12", KeyboardType.Number)
            }

            // 2. Người thuê
            ContractSection(title = "2. Thông tin người thuê", icon = Icons.Default.Person) {
                ContractField("Họ tên *", tenantName, { tenantName = it })
                ContractField("Số điện thoại *", tenantPhone, { tenantPhone = it }, keyboardType = KeyboardType.Phone)
                ContractField("CCCD/CMND *", tenantIdCard, { tenantIdCard = it }, keyboardType = KeyboardType.Number)
                ContractField("Địa chỉ thường trú", tenantAddress, { tenantAddress = it })
            }

            // 3. Chủ trọ
            ContractSection(title = "3. Thông tin chủ trọ", icon = Icons.Default.AccountCircle) {
                ContractField("Họ tên chủ trọ *", lLandlordName, { lLandlordName = it })
                ContractField("Số điện thoại *", lLandlordPhone, { lLandlordPhone = it }, keyboardType = KeyboardType.Phone)
                ContractField("Địa chỉ", lLandlordAddress, { lLandlordAddress = it })
            }

            // 4. Phòng trọ
            ContractSection(title = "4. Thông tin phòng trọ", icon = Icons.Default.Home) {
                ContractField("Tên/Mã phòng", roomName, { roomName = it })
                ContractField("Địa chỉ phòng *", roomAddress, { roomAddress = it })
                ContractField("Diện tích (m²)", roomArea, { roomArea = it }, keyboardType = KeyboardType.Decimal)
                ContractFieldMultiline("Trang thiết bị", amenities, { amenities = it })
            }

            // 5. Chi phí
            ContractSection(title = "5. Chi phí thuê", icon = Icons.Default.MonetizationOn) {
                ContractField("Giá thuê/tháng (VND) *", rentPrice, { rentPrice = it }, keyboardType = KeyboardType.Number)
                ContractField("Tiền cọc (VND) *", deposit, { deposit = it }, keyboardType = KeyboardType.Number)
                ContractField("Tiền điện (đ/kWh)", electricPrice, { electricPrice = it }, keyboardType = KeyboardType.Number)
                ContractField("Tiền nước (đ/m³)", waterPrice, { waterPrice = it }, keyboardType = KeyboardType.Number)
                ContractField("Phí khác (VND)", otherFee, { otherFee = it }, keyboardType = KeyboardType.Number)
                ContractField("Ghi chú phí khác", otherFeeNote, { otherFeeNote = it }, "VD: wifi, rác, giữ xe")
            }

            // 6. Thanh toán
            ContractSection(title = "6. Thời gian & thanh toán", icon = Icons.Default.Schedule) {
                ContractField("Ngày thanh toán hàng tháng", paymentDay, { paymentDay = it }, "VD: 5", KeyboardType.Number)
                Text("Hình thức thanh toán", fontSize = 13.sp, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("cash" to "Tiền mặt", "transfer" to "Chuyển khoản").forEach { (value, label) ->
                        FilterChip(
                            selected = paymentMethod == value,
                            onClick = { paymentMethod = value },
                            label = { Text(label, fontSize = 13.sp) }
                        )
                    }
                }
                ContractField("Quy định trễ hạn", latePaymentRule, { latePaymentRule = it })
            }

            // 7. Quy định
            ContractSection(title = "7. Quy định & điều khoản", icon = Icons.Default.Description) {
                ContractFieldMultiline("Nội quy", rules, { rules = it }, minLines = 4)
            }

            // 8. Chấm dứt hợp đồng
            ContractSection(title = "8. Điều kiện chấm dứt hợp đồng", icon = Icons.Default.Cancel) {
                ContractField("Báo trước (ngày)", terminationDays, { terminationDays = it }, keyboardType = KeyboardType.Number)
                ContractFieldMultiline("Điều kiện hoàn cọc", depositReturnCond, { depositReturnCond = it })
            }

            // Nút xác nhận
            Button(
                onClick = { submit() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF)),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Xác nhận tạo hợp đồng", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun ContractSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, tint = Color(0xFF007BFF), modifier = Modifier.size(20.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0D1B34))
            }
            HorizontalDivider(color = Color(0xFFF0F0F0))
            content()
        }
    }
}

@Composable
fun ContractField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        placeholder = if (placeholder.isNotBlank()) ({ Text(placeholder, fontSize = 13.sp, color = Color.LightGray) }) else null,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF007BFF),
            unfocusedBorderColor = Color(0xFFE0E0E0)
        )
    )
}

@Composable
fun ContractFieldMultiline(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    minLines: Int = 2
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        placeholder = if (placeholder.isNotBlank()) ({ Text(placeholder, fontSize = 13.sp, color = Color.LightGray) }) else null,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        minLines = minLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF007BFF),
            unfocusedBorderColor = Color(0xFFE0E0E0)
        )
    )
}

// ── Màn hình tạo hợp đồng thành công ──────────────────────────────────────
@Composable
fun ContractSuccessScreen(
    tenantName: String,
    tenantPhone: String,
    roomName: String,
    startDate: String,
    onBackHome: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFFE8F5E9), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(60.dp))
            }

            Text(
                "Tạo hợp đồng thành công!",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color(0xFF0D1B34),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Surface(color = Color(0xFFE3F2FD), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ContractSuccessRow("Người thuê", tenantName)
                    ContractSuccessRow("Số điện thoại", tenantPhone)
                    if (roomName.isNotBlank()) ContractSuccessRow("Phòng", roomName)
                    if (startDate.isNotBlank()) ContractSuccessRow("Ngày bắt đầu", startDate)
                }
            }

            Surface(color = Color(0xFFFFF8E1), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Info, null, tint = Color(0xFFF57F17), modifier = Modifier.size(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Chờ người thuê xác nhận", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFF57F17))
                        Text(
                            "Hợp đồng đã được tạo. Người thuê sẽ nhận được thông báo và cần xác nhận hợp đồng.",
                            fontSize = 13.sp,
                            color = Color(0xFF795548),
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onBackHome,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF))
            ) {
                Icon(Icons.Default.Home, null)
                Spacer(Modifier.width(8.dp))
                Text("Về quản lý đặt phòng", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun ContractSuccessRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = Color.Gray)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0D1B34))
    }
}
