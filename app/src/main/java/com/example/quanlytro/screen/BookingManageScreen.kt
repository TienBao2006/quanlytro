package com.example.quanlytro.screen

import com.example.quanlytro.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingManageScreen(
    onBackClick: () -> Unit = {},
    onCreateContract: (BookingItem) -> Unit = {}
) {
    var bookings by remember { mutableStateOf<List<BookingItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Chờ, 1=Đã xác nhận, 2=Từ chối

    fun loadBookings() {
        isLoading = true
        RetrofitClient.instance.getLandlordBookings(UserSession.uid)
            .enqueue(object : Callback<BookingListResponse> {
                override fun onResponse(call: Call<BookingListResponse>, response: Response<BookingListResponse>) {
                    bookings = response.body()?.bookings ?: emptyList()
                    isLoading = false
                }
                override fun onFailure(call: Call<BookingListResponse>, t: Throwable) {
                    isLoading = false
                }
            })
    }

    LaunchedEffect(Unit) { loadBookings() }

    fun updateStatus(bookingId: Int, status: String) {
        RetrofitClient.instance.updateBookingStatus(bookingId, status, UserSession.uid)
            .enqueue(object : Callback<SimpleResponse> {
                override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                    loadBookings()
                }
                override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {}
            })
    }

    fun deleteBooking(bookingId: Int) {
        RetrofitClient.instance.deleteBooking(bookingId)
            .enqueue(object : Callback<SimpleResponse> {
                override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                    loadBookings()
                }
                override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {}
            })
    }

    val tabs = listOf("Chờ xác nhận", "Đã xác nhận", "Từ chối")
    val statusMap = listOf("pending", "confirmed", "rejected")

    val pendingCount = bookings.count { it.status == "pending" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yêu cầu đặt phòng", fontWeight = FontWeight.Bold) },
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
            // Tab bar
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF007BFF)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(title, fontSize = 13.sp)
                                if (index == 0 && pendingCount > 0) {
                                    Box(
                                        modifier = Modifier.size(18.dp).clip(CircleShape).background(Color.Red),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(pendingCount.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF007BFF))
                }
            } else {
                val filtered = bookings.filter { it.status == statusMap[selectedTab] }

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Inbox, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                            Spacer(Modifier.height(12.dp))
                            Text("Không có yêu cầu nào", color = Color.Gray, fontSize = 15.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered, key = { it.id }) { booking ->
                            BookingCard(
                                booking = booking,
                                onConfirm = { updateStatus(booking.id, "confirmed") },
                                onReject  = { updateStatus(booking.id, "rejected") },
                                onCreateContract = { onCreateContract(booking) },
                                onDelete = { deleteBooking(booking.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookingCard(
    booking: BookingItem,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
    onCreateContract: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var showDetail by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa booking?") },
            text = { Text("Booking của ${booking.full_name} sẽ bị xóa vĩnh viễn.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Xóa", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Hủy") }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: tên + trạng thái
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFE3F2FD)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(booking.full_name.take(1).uppercase(), color = Color(0xFF007BFF), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Column {
                        Text(booking.full_name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(booking.phone, fontSize = 13.sp, color = Color.Gray)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (booking.has_contract == 1) {
                        Surface(
                            color = Color(0xFF6A1B9A).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                "📄 Đã có HĐ",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6A1B9A)
                            )
                        }
                    }
                    StatusBadge(booking.status)
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(Modifier.height(12.dp))

            // Thông tin phòng
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Home, null, tint = Color(0xFF007BFF), modifier = Modifier.size(16.dp))
                Text(booking.post_title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0D1B34))
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Schedule, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                Text(booking.created_at.take(16), fontSize = 12.sp, color = Color.Gray)
            }

            // Chi tiết mở rộng
            if (showDetail) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!booking.room_number.isNullOrBlank()) DetailRow("Mã phòng", booking.room_number)
                    DetailRow("CCCD", booking.id_card)
                    DetailRow("Ngày sinh", booking.dob)
                    DetailRow("Ngày cấp CCCD", booking.id_issue_date)
                    if (!booking.start_date.isNullOrBlank()) DetailRow("Ngày bắt đầu thuê", booking.start_date)
                    if (!booking.duration.isNullOrBlank()) DetailRow("Thời hạn thuê", "${booking.duration} tháng")
                    if (!booking.email.isNullOrBlank()) DetailRow("Email", booking.email)
                    if (!booking.address.isNullOrBlank()) DetailRow("Thường trú", booking.address)
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { showDetail = !showDetail },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    if (showDetail) "Thu gọn ▲" else "Xem chi tiết ▼",
                    fontSize = 12.sp,
                    color = Color(0xFF007BFF)
                )
            }

            // Nút hành động (chỉ hiện khi pending)
            if (booking.status == "pending") {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Từ chối", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Xác nhận", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Nút tạo hợp đồng (chỉ hiện khi confirmed)
            if (booking.status == "confirmed") {
                Spacer(Modifier.height(8.dp))
                if (booking.has_contract == 1) {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = Color(0xFF6A1B9A).copy(alpha = 0.15f),
                            disabledContentColor = Color(0xFF6A1B9A)
                        )
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Đã tạo hợp đồng", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onCreateContract,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF))
                    ) {
                        Icon(Icons.Default.Description, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Tạo hợp đồng", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (color, text) = when (status) {
        "confirmed" -> Color(0xFF4CAF50) to "Đã xác nhận"
        "rejected"  -> Color(0xFFE53935) to "Từ chối"
        else        -> Color(0xFFF57F17) to "Chờ xác nhận"
    }
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(1f))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0D1B34), modifier = Modifier.weight(2f))
    }
}
