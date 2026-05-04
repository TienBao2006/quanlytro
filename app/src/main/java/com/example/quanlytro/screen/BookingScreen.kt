package com.example.quanlytro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    postId: Int,
    postTitle: String,
    totalRooms: Int = 1,
    onBackClick: () -> Unit,
    onBookingSuccess: () -> Unit
) {
    var fullName    by remember { mutableStateOf(UserSession.fullName) }
    var phone       by remember { mutableStateOf(UserSession.phone) }
    var roomNumber  by remember { mutableStateOf("1") }
    var roomDropdownExpanded by remember { mutableStateOf(false) }
    var bookedRooms by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Load danh sách phòng đã bị đặt
    LaunchedEffect(postId) {
        RetrofitClient.instance.getBookedRooms(postId).enqueue(object : Callback<BookedRoomsResponse> {
            override fun onResponse(call: Call<BookedRoomsResponse>, response: Response<BookedRoomsResponse>) {
                bookedRooms = response.body()?.booked_rooms?.toSet() ?: emptySet()
                // Nếu phòng 1 đã bị đặt, tự động chọn phòng trống đầu tiên
                if (bookedRooms.contains("1")) {
                    val firstFree = (1..totalRooms).firstOrNull { !bookedRooms.contains(it.toString()) }
                    roomNumber = firstFree?.toString() ?: "1"
                }
            }
            override fun onFailure(call: Call<BookedRoomsResponse>, t: Throwable) {}
        })
    }
    var idCard      by remember { mutableStateOf("") }
    var dob         by remember { mutableStateOf("") }
    var idIssueDate by remember { mutableStateOf("") }
    var startDate   by remember { mutableStateOf("") }
    var duration    by remember { mutableStateOf("") }
    var email       by remember { mutableStateOf("") }
    var address     by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMsg  by remember { mutableStateOf<String?>(null) }

    // Trạng thái sau khi đặt phòng thành công
    var bookingDone by remember { mutableStateOf(false) }

    // OTP
    var showOtpDialog by remember { mutableStateOf(false) }
    var otpCode       by remember { mutableStateOf("") }
    var otpError      by remember { mutableStateOf<String?>(null) }
    var otpLoading    by remember { mutableStateOf(false) }
    var otpDebug      by remember { mutableStateOf("") }

    // DatePicker state
    var showDobPicker        by remember { mutableStateOf(false) }
    var showIssueDatePicker  by remember { mutableStateOf(false) }
    var showStartDatePicker  by remember { mutableStateOf(false) }

    fun sendOtp() {
        if (phone.isBlank()) { errorMsg = "Vui lòng nhập số điện thoại"; return }
        isLoading = true
        RetrofitClient.instance.sendOtp(phone).enqueue(object : Callback<OtpResponse> {
            override fun onResponse(call: Call<OtpResponse>, response: Response<OtpResponse>) {
                isLoading = false
                val body = response.body()
                if (body?.status == "success") {
                    otpDebug = body.otp_debug ?: ""
                    otpCode = ""
                    otpError = null
                    showOtpDialog = true
                } else {
                    errorMsg = body?.message ?: "Gửi OTP thất bại"
                }
            }
            override fun onFailure(call: Call<OtpResponse>, t: Throwable) {
                isLoading = false
                errorMsg = "Lỗi kết nối: ${t.message}"
            }
        })
    }

    fun verifyAndBook() {
        otpLoading = true
        RetrofitClient.instance.verifyOtp(phone, otpCode).enqueue(object : Callback<SimpleResponse> {
            override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                val body = response.body()
                if (body?.status == "success") {
                    RetrofitClient.instance.createBooking(
                        postId, UserSession.uid, roomNumber, fullName, phone,
                        idCard, dob, idIssueDate, startDate, duration, email, address
                    ).enqueue(object : Callback<SimpleResponse> {
                        override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                            otpLoading = false
                            showOtpDialog = false
                            bookingDone = true  // Hiện màn hình chờ xác nhận
                        }
                        override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                            otpLoading = false
                            otpError = "Lỗi lưu đặt phòng"
                        }
                    })
                } else {
                    otpLoading = false
                    otpError = body?.message ?: "OTP không đúng"
                }
            }
            override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                otpLoading = false
                otpError = "Lỗi kết nối"
            }
        })
    }

    fun validate(): Boolean {
        if (fullName.isBlank())    { errorMsg = "Vui lòng nhập họ tên"; return false }
        if (phone.isBlank())       { errorMsg = "Vui lòng nhập số điện thoại"; return false }
        if (idCard.isBlank())      { errorMsg = "Vui lòng nhập số CCCD"; return false }
        if (dob.isBlank())         { errorMsg = "Vui lòng nhập ngày sinh"; return false }
        if (idIssueDate.isBlank()) { errorMsg = "Vui lòng nhập ngày cấp CCCD"; return false }
        if (startDate.isBlank())   { errorMsg = "Vui lòng nhập ngày bắt đầu thuê"; return false }
        if (duration.isBlank())    { errorMsg = "Vui lòng nhập thời hạn thuê"; return false }
        errorMsg = null
        return true
    }

    // ── Màn hình chờ xác nhận ──────────────────────────────────────────────
    if (bookingDone) {
        BookingPendingScreen(
            postTitle = postTitle,
            fullName = fullName,
            phone = phone,
            roomNumber = roomNumber,
            onBackHome = onBookingSuccess
        )
        return
    }

    // ── DatePicker dialogs ─────────────────────────────────────────────────
    if (showDobPicker) {
        DatePickerDialog(
            onDismiss = { showDobPicker = false },
            onDateSelected = { dob = it; showDobPicker = false }
        )
    }
    if (showIssueDatePicker) {
        DatePickerDialog(
            onDismiss = { showIssueDatePicker = false },
            onDateSelected = { idIssueDate = it; showIssueDatePicker = false }
        )
    }
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismiss = { showStartDatePicker = false },
            onDateSelected = { startDate = it; showStartDatePicker = false }
        )
    }

    // ── Form chính ─────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đặt phòng", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Banner phòng
            Surface(
                color = Color(0xFFE3F2FD),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Home, null, tint = Color(0xFF007BFF))
                    Column {
                        Text("Phòng đang đặt", fontSize = 12.sp, color = Color.Gray)
                        Text(postTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0D1B34))
                    }
                }
            }

            Text("Thông tin người đặt", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0D1B34))

            // Dropdown chọn mã phòng từ 1 đến totalRooms
            ExposedDropdownMenuBox(
                expanded = roomDropdownExpanded,
                onExpandedChange = { roomDropdownExpanded = !roomDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = "Phòng $roomNumber",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Mã phòng *", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.MeetingRoom, null, tint = Color(0xFF007BFF), modifier = Modifier.size(20.dp)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roomDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF007BFF),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )
                ExposedDropdownMenu(
                    expanded = roomDropdownExpanded,
                    onDismissRequest = { roomDropdownExpanded = false }
                ) {
                    (1..totalRooms).forEach { num ->
                        val isBooked = bookedRooms.contains(num.toString())
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Phòng $num",
                                        color = if (isBooked) Color.Gray else Color.Unspecified
                                    )
                                    if (isBooked) {
                                        Text("Đã đặt", fontSize = 11.sp, color = Color(0xFFE53935))
                                    }
                                }
                            },
                            onClick = {
                                if (!isBooked) {
                                    roomNumber = num.toString()
                                    roomDropdownExpanded = false
                                }
                            },
                            enabled = !isBooked
                        )
                    }
                }
            }

            BookingTextField(label = "Họ và tên *", value = fullName, onValueChange = { fullName = it }, icon = Icons.Default.Person)
            BookingTextField(label = "Số điện thoại *", value = phone, onValueChange = { phone = it }, icon = Icons.Default.Phone, keyboardType = KeyboardType.Phone)
            BookingTextField(label = "Số căn cước công dân *", value = idCard, onValueChange = { idCard = it }, icon = Icons.Default.Badge, keyboardType = KeyboardType.Number)

            // Ngày sinh — gõ hoặc chọn
            DateInputField(
                label = "Ngày sinh *",
                value = dob,
                onValueChange = { dob = it },
                onPickerClick = { showDobPicker = true }
            )

            // Ngày cấp CCCD — gõ hoặc chọn
            DateInputField(
                label = "Ngày cấp CCCD *",
                value = idIssueDate,
                onValueChange = { idIssueDate = it },
                onPickerClick = { showIssueDatePicker = true }
            )

            // Ngày bắt đầu thuê — gõ hoặc chọn
            DateInputField(
                label = "Ngày bắt đầu thuê *",
                value = startDate,
                onValueChange = { startDate = it },
                onPickerClick = { showStartDatePicker = true }
            )

            BookingTextField(
                label = "Thời hạn thuê (tháng) *",
                value = duration,
                onValueChange = { duration = it },
                icon = Icons.Default.Schedule,
                keyboardType = KeyboardType.Number
            )

            BookingTextField(label = "Email", value = email, onValueChange = { email = it }, icon = Icons.Default.Email, keyboardType = KeyboardType.Email)
            BookingTextField(label = "Thường trú", value = address, onValueChange = { address = it }, icon = Icons.Default.LocationOn, singleLine = false)

            if (errorMsg != null) {
                Text(errorMsg!!, color = Color.Red, fontSize = 13.sp)
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { if (validate()) sendOtp() },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF)),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.CheckCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Xác nhận & Gửi OTP", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    // ── OTP Dialog ─────────────────────────────────────────────────────────
    if (showOtpDialog) {
        Dialog(onDismissRequest = { showOtpDialog = false }) {
            Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Default.Sms, null, tint = Color(0xFF007BFF), modifier = Modifier.size(48.dp))
                    Text("Xác nhận OTP", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Mã OTP đã được gửi đến số\n$phone", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)

                    if (otpDebug.isNotEmpty()) {
                        Surface(color = Color(0xFFFFF9C4), shape = RoundedCornerShape(8.dp)) {
                            Text("OTP (test): $otpDebug", modifier = Modifier.padding(8.dp), fontSize = 13.sp, color = Color(0xFF795548), fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { if (it.length <= 6) otpCode = it },
                        label = { Text("Nhập mã OTP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = otpError != null
                    )

                    if (otpError != null) {
                        Text(otpError!!, color = Color.Red, fontSize = 13.sp)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { showOtpDialog = false }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Text("Hủy")
                        }
                        Button(
                            onClick = { if (otpCode.length == 6) verifyAndBook() else otpError = "Nhập đủ 6 số" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF)),
                            enabled = !otpLoading
                        ) {
                            if (otpLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Text("Xác nhận", fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(onClick = { sendOtp() }) {
                        Text("Gửi lại OTP", color = Color(0xFF007BFF), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ── Màn hình chờ xác nhận ──────────────────────────────────────────────────
@Composable
fun BookingPendingScreen(
    postTitle: String,
    fullName: String,
    phone: String,
    roomNumber: String,
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
            // Icon thành công
            Box(
                modifier = Modifier.size(100.dp).background(Color(0xFFE8F5E9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(60.dp))
            }

            Text("Đặt phòng thành công!", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFF0D1B34), textAlign = TextAlign.Center)

            Surface(color = Color(0xFFE3F2FD), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PendingInfoRow(label = "Phòng", value = postTitle)
                    if (roomNumber.isNotBlank()) PendingInfoRow(label = "Mã phòng", value = roomNumber)
                    PendingInfoRow(label = "Người đặt", value = fullName)
                    PendingInfoRow(label = "Số điện thoại", value = phone)
                }
            }

            Surface(color = Color(0xFFFFF8E1), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.AccessTime, null, tint = Color(0xFFF57F17), modifier = Modifier.size(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Đang chờ xác nhận", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFF57F17))
                        Text(
                            "Chủ trọ sẽ liên hệ với bạn qua số điện thoại để xác nhận trong vòng 24 giờ.",
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
                Text("Về trang chủ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun PendingInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = Color.Gray)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0D1B34))
    }
}

// ── DatePicker Dialog ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val today = Calendar.getInstance()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = today.timeInMillis,
        yearRange = 1900..today.get(Calendar.YEAR)
    )

    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    val cal = Calendar.getInstance().apply { timeInMillis = millis }
                    val day   = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
                    val month = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
                    val year  = cal.get(Calendar.YEAR)
                    onDateSelected("$day/$month/$year")
                }
            }) { Text("Chọn") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    ) {
        DatePicker(state = state)
    }
}

// ── Field ngày tháng: gõ tay + nút lịch ──────────────────────────────────
@Composable
fun DateInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onPickerClick: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("$label (dd/mm/yyyy)", fontSize = 13.sp) },
        leadingIcon = { Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF007BFF), modifier = Modifier.size(20.dp)) },
        trailingIcon = {
            IconButton(onClick = onPickerClick) {
                Icon(Icons.Default.DateRange, "Chọn ngày", tint = Color(0xFF007BFF))
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        placeholder = { Text("dd/mm/yyyy", color = Color.LightGray) },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            focusedBorderColor = Color(0xFF007BFF),
            unfocusedBorderColor = Color(0xFFE0E0E0)
        )
    )
}

// ── TextField thông thường ─────────────────────────────────────────────────
@Composable
fun BookingTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        leadingIcon = { Icon(icon, null, tint = Color(0xFF007BFF), modifier = Modifier.size(20.dp)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 3,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            focusedBorderColor = Color(0xFF007BFF),
            unfocusedBorderColor = Color(0xFFE0E0E0)
        )
    )
}
