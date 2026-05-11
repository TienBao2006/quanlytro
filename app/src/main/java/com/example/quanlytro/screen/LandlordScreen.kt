package com.example.quanlytro

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandlordScreen(
    userRole: String = "Chủ trọ",
    onBackClick: () -> Unit = {},
    onExploreClick: () -> Unit = {},
    onBookingManageClick: () -> Unit = {},
    onContractListClick: () -> Unit = {},
    onInvoiceClick: () -> Unit = {},
    onUtilityClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onNoticeClick: () -> Unit = {},
    onTenantManageClick: () -> Unit = {},
    onChatListClick: () -> Unit = {},
    onStatsClick: () -> Unit = {}
) {
    var bookings by remember { mutableStateOf<List<BookingItem>>(emptyList()) }
    var isLoadingBookings by remember { mutableStateOf(false) }
    var snackMsg by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var unreadCount by remember { mutableStateOf(0) }

    var totalRooms by remember { mutableStateOf(0) }
    var emptyRooms by remember { mutableStateOf(0) }
    var activeContracts by remember { mutableStateOf(0) }
    var pendingCancelCount by remember { mutableStateOf(0) }

    var currentRevenue by remember { mutableStateOf(0.0) }
    var percentChange by remember { mutableStateOf(0.0) }
    var currentMonth by remember { mutableStateOf("") }
    var monthlyList by remember { mutableStateOf<List<MonthlyRevenue>>(emptyList()) }
    var selectedMonthIndex by remember { mutableStateOf(0) } // 0 = tháng mới nhất

    fun loadUnreadCount() {
        RetrofitClient.instance.countNotifications(UserSession.uid)
            .enqueue(object : Callback<NotificationCountResponse> {
                override fun onResponse(call: Call<NotificationCountResponse>, response: Response<NotificationCountResponse>) {
                    unreadCount = response.body()?.count ?: 0
                }
                override fun onFailure(call: Call<NotificationCountResponse>, t: Throwable) {}
            })
    }

    fun loadStats() {
        val uid = UserSession.uid
        Log.d("LandlordStats", "loadStats uid='$uid'")
        if (uid.isBlank()) {
            Log.e("LandlordStats", "uid is blank, skip")
            return
        }
        RetrofitClient.instance.getLandlordStats(uid).enqueue(object : Callback<LandlordStatsResponse> {
            override fun onResponse(call: Call<LandlordStatsResponse>, response: Response<LandlordStatsResponse>) {
                val body = response.body()
                Log.d("LandlordStats", "url=${call.request().url()} code=${response.code()} body=$body")
                body?.let {
                    totalRooms = it.total_rooms
                    emptyRooms = it.empty_rooms
                    activeContracts = it.active_contracts
                    Log.d("LandlordStats", "set: total=$totalRooms empty=$emptyRooms active=$activeContracts")
                } ?: Log.e("LandlordStats", "body null, errorBody=${response.errorBody()?.string()}")
            }
            override fun onFailure(call: Call<LandlordStatsResponse>, t: Throwable) {
                Log.e("LandlordStats", "onFailure: ${t.message}")
            }
        })
        // Load số hợp đồng đang chờ phản hồi hủy
        RetrofitClient.instance.getContractsByLandlord(uid).enqueue(object : Callback<ContractListResponse> {
            override fun onResponse(call: Call<ContractListResponse>, response: Response<ContractListResponse>) {
                pendingCancelCount = response.body()?.contracts
                    ?.count { it.status == "cancel_requested_by_tenant" } ?: 0
            }
            override fun onFailure(call: Call<ContractListResponse>, t: Throwable) {}
        })
        // Load doanh thu
        RetrofitClient.instance.getRevenueStats(uid).enqueue(object : Callback<RevenueStatsResponse> {
            override fun onResponse(call: Call<RevenueStatsResponse>, response: Response<RevenueStatsResponse>) {
                response.body()?.let {
                    monthlyList = it.monthly
                    selectedMonthIndex = 0
                    if (it.monthly.isNotEmpty()) {
                        currentRevenue = it.monthly[0].revenue
                        currentMonth   = it.monthly[0].month
                    } else {
                        currentRevenue = it.current_revenue
                        currentMonth   = it.current_month
                    }
                    percentChange = it.percent_change
                }
            }
            override fun onFailure(call: Call<RevenueStatsResponse>, t: Throwable) {}
        })
    }

    fun loadBookings() {
        val uid = UserSession.uid
        if (uid.isBlank()) return
        isLoadingBookings = true
        RetrofitClient.instance.getLandlordBookings(uid).enqueue(object : Callback<BookingListResponse> {
            override fun onResponse(call: Call<BookingListResponse>, response: Response<BookingListResponse>) {
                isLoadingBookings = false
                bookings = response.body()?.bookings ?: emptyList()
            }
            override fun onFailure(call: Call<BookingListResponse>, t: Throwable) {
                isLoadingBookings = false
            }
        })
    }

    fun updateBooking(bookingId: Int, status: String) {
        RetrofitClient.instance.updateBookingStatus(bookingId, status, UserSession.uid)
            .enqueue(object : Callback<SimpleResponse> {
                override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                    snackMsg = if (status == "confirmed") "Đã xác nhận đặt phòng" else "Đã từ chối đặt phòng"
                    loadBookings()
                }
                override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                    snackMsg = "Lỗi kết nối"
                }
            })
    }

    LaunchedEffect(Unit) { loadBookings(); loadStats(); loadUnreadCount() }

    LaunchedEffect(snackMsg) {
        snackMsg?.let {
            snackbarHostState.showSnackbar(it)
            snackMsg = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Quản lý chủ trọ", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { onNotificationClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Thông báo",
                            modifier = Modifier.size(26.dp)
                        )
                        if (unreadCount > 0) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 4.dp, end = 4.dp)
                                    .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp),
                                color = Color(0xFFE53935),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = if (unreadCount > 99) "99+" else "$unreadCount",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            BottomNavBar(
                initialSelected = 2,
                userRole = userRole,
                onExploreClick = onExploreClick,
                onProfileClick = onProfileClick,
                onManageClick = {},
                onChatListClick = onChatListClick
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F9FA))
        ) {
            item { RevenueOverview(
                currentMonth = currentMonth,
                revenue = currentRevenue,
                percentChange = percentChange,
                canGoPrev = selectedMonthIndex < monthlyList.size - 1,
                canGoNext = selectedMonthIndex > 0,
                onPrev = {
                    val next = selectedMonthIndex + 1
                    if (next < monthlyList.size) {
                        selectedMonthIndex = next
                        currentMonth = monthlyList[next].month
                        currentRevenue = monthlyList[next].revenue
                        val prevRev = if (next + 1 < monthlyList.size) monthlyList[next + 1].revenue else 0.0
                        percentChange = if (prevRev > 0) ((currentRevenue - prevRev) / prevRev * 100).let { Math.round(it * 10) / 10.0 } else 0.0
                    }
                },
                onNext = {
                    val prev = selectedMonthIndex - 1
                    if (prev >= 0) {
                        selectedMonthIndex = prev
                        currentMonth = monthlyList[prev].month
                        currentRevenue = monthlyList[prev].revenue
                        val prevRev = if (selectedMonthIndex + 1 < monthlyList.size) monthlyList[selectedMonthIndex + 1].revenue else 0.0
                        percentChange = if (prevRev > 0) ((currentRevenue - prevRev) / prevRev * 100).let { Math.round(it * 10) / 10.0 } else 0.0
                    }
                }
            ) }
            item { StatsRow(totalRooms = totalRooms, emptyRooms = emptyRooms, tenants = activeContracts) }
            item {
                Text(
                    "Công cụ quản lý",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            item { ManagementGrid(onBookingManageClick = onBookingManageClick, onContractListClick = onContractListClick, onInvoiceClick = onInvoiceClick, onUtilityClick = onUtilityClick, onNoticeClick = onNoticeClick, onTenantManageClick = onTenantManageClick, onStatsClick = onStatsClick, pendingCount = bookings.count { it.status == "pending" }, pendingCancelCount = pendingCancelCount) }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun RevenueOverview(
    currentMonth: String = "",
    revenue: Double = 0.0,
    percentChange: Double = 0.0,
    canGoPrev: Boolean = false,
    canGoNext: Boolean = false,
    onPrev: () -> Unit = {},
    onNext: () -> Unit = {}
) {
    val monthLabel = if (currentMonth.length == 7) {
        "tháng ${currentMonth.substring(5).trimStart('0')}"
    } else "tháng này"

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B34))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Header với nút điều hướng
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrev,
                    enabled = canGoPrev,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ChevronLeft, null,
                        tint = if (canGoPrev) Color.White else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text("Doanh thu $monthLabel", color = Color.LightGray, fontSize = 14.sp)
                IconButton(
                    onClick = onNext,
                    enabled = canGoNext,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ChevronRight, null,
                        tint = if (canGoNext) Color.White else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(formatPrice(revenue), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(" VND", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (percentChange != 0.0) {
                val isUp = percentChange > 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isUp) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null,
                        tint = if (isUp) Color.Green else Color(0xFFFF5252),
                        modifier = Modifier.size(16.dp)
                    )
                    val sign = if (isUp) "+" else ""
                    Text(
                        " ${sign}${percentChange}% so với tháng trước",
                        color = if (isUp) Color.Green else Color(0xFFFF5252),
                        fontSize = 12.sp
                    )
                }
            } else {
                Text("Chưa có dữ liệu tháng trước", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun StatsRow(totalRooms: Int = 0, emptyRooms: Int = 0, tenants: Int = 0) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard("Phòng", "$totalRooms", Color(0xFFE3F2FD), Color(0xFF007BFF), Modifier.weight(1f))
        StatCard("Trống", "$emptyRooms", Color(0xFFFFF3E0), Color(0xFFFF9800), Modifier.weight(1f))
        StatCard("Khách", "$tenants", Color(0xFFE8F5E9), Color(0xFF4CAF50), Modifier.weight(1f))
    }
}

@Composable
fun StatCard(label: String, value: String, bgColor: Color, textColor: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(label.take(1), color = textColor, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ManagementGrid(onBookingManageClick: () -> Unit = {}, onContractListClick: () -> Unit = {}, onInvoiceClick: () -> Unit = {}, onUtilityClick: () -> Unit = {}, onNoticeClick: () -> Unit = {}, onTenantManageClick: () -> Unit = {}, onStatsClick: () -> Unit = {}, pendingCount: Int = 0, pendingCancelCount: Int = 0) {
    val items = listOf(
        ManagementAction("Điện & Nước", Icons.Default.FlashOn, Color(0xFFE1F5FE), Color(0xFF0288D1)),
        ManagementAction("Hóa đơn", Icons.Default.Receipt, Color(0xFFF3E5F5), Color(0xFF9C27B0)),
        ManagementAction("Khách thuê", Icons.Default.People, Color(0xFFE8F5E9), Color(0xFF4CAF50)),
        ManagementAction("Hợp đồng", Icons.Default.Description, Color(0xFFFFF3E0), Color(0xFFFF9800)),
        ManagementAction("Thông báo", Icons.Default.Campaign, Color(0xFFFFEBEE), Color(0xFFF44336)),
        ManagementAction("Thống kê", Icons.Default.BarChart, Color(0xFFE8EAF6), Color(0xFF3F51B5))
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Nút Yêu cầu đặt phòng nổi bật
        Surface(
            onClick = onBookingManageClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF007BFF),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.CalendarToday, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Column {
                        Text("Yêu cầu đặt phòng", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Xem và xác nhận yêu cầu từ khách", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
                if (pendingCount > 0) {
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.Red),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(pendingCount.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Icon(Icons.Default.ChevronRight, null, tint = Color.White)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        items.chunked(3).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { item ->
                    val onClick: () -> Unit = when (item.title) {
                        "Hợp đồng"   -> onContractListClick
                        "Hóa đơn"    -> onInvoiceClick
                        "Điện & Nước" -> onUtilityClick
                        "Thông báo"  -> onNoticeClick
                        "Khách thuê" -> onTenantManageClick
                        "Thống kê"   -> onStatsClick
                        else -> ({})
                    }
                    val badge = if (item.title == "Hợp đồng" && pendingCancelCount > 0) pendingCancelCount else 0
                    ActionCard(item, onClick, Modifier.weight(1f), badge)
                }
                if (rowItems.size < 3) {
                    repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ActionCard(item: ManagementAction, onClick: () -> Unit = {}, modifier: Modifier, badgeCount: Int = 0) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(item.bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.icon, contentDescription = null, tint = item.textColor)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            if (badgeCount > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp),
                    color = Color(0xFFE53935),
                    shape = CircleShape
                ) {
                    Text(
                        text = "$badgeCount",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RequestItem() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Build, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Hỏng vòi nước - P.302", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Anh Tuấn • 2 giờ trước", fontSize = 12.sp, color = Color.Gray)
            }
            Text("Mới", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}
data class ManagementAction(val title: String, val icon: ImageVector, val bgColor: Color, val textColor: Color)

@Composable
fun BookingRequestItem(
    booking: BookingItem,
    onConfirm: () -> Unit,
    onReject: () -> Unit
) {
    val statusColor = when (booking.status) {
        "confirmed" -> Color(0xFF4CAF50)
        "rejected"  -> Color(0xFFF44336)
        else        -> Color(0xFFFF9800)
    }
    val statusLabel = when (booking.status) {
        "confirmed" -> "Đã xác nhận"
        "rejected"  -> "Đã từ chối"
        else        -> "Chờ xác nhận"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFE3F2FD)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, null, tint = Color(0xFF007BFF), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(booking.full_name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(booking.phone, fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Surface(color = statusColor.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        statusLabel,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Home, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                Text(booking.post_title, fontSize = 13.sp, color = Color(0xFF0D1B34))
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.AccessTime, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                Text(booking.created_at, fontSize = 12.sp, color = Color.Gray)
            }

            if (booking.status == "pending") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF44336))
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Từ chối", fontSize = 13.sp)
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Xác nhận", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
