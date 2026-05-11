package package com.example.quanlytro.features.stats
import com.example.quanlytro.data.remote.*
import com.example.quanlytro.data.model.*

import com.example.quanlytro.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevenueStatsScreen(onBackClick: () -> Unit = {}) {
    var isLoading    by remember { mutableStateOf(true) }
    var monthlyList  by remember { mutableStateOf<List<MonthlyRevenue>>(emptyList()) }
    var yearlyList   by remember { mutableStateOf<List<YearlyRevenue>>(emptyList()) }
    var selectedTab  by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        RetrofitClient.instance.getRevenueStats(UserSession.uid)
            .enqueue(object : Callback<RevenueStatsResponse> {
                override fun onResponse(call: Call<RevenueStatsResponse>, response: Response<RevenueStatsResponse>) {
                    response.body()?.let { monthlyList = it.monthly; yearlyList = it.yearly }
                    isLoading = false
                }
                override fun onFailure(call: Call<RevenueStatsResponse>, t: Throwable) { isLoading = false }
            })
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Thống kê doanh thu", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF0D1B34))
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).background(Color(0xFF0D1B34))) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A2B4A))
            ) {
                listOf("Theo tháng", "Theo năm").forEachIndexed { i, label ->
                    Box(
                        modifier = Modifier.weight(1f).padding(4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == i) Color(0xFF007BFF) else Color.Transparent)
                            .clickable { selectedTab = i }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = Color.White,
                            fontWeight = if (selectedTab == i) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp)
                    }
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF007BFF))
                }
            } else when (selectedTab) {
                0 -> MonthlyTab(monthlyList)
                1 -> YearlyTab(yearlyList, monthlyList)
            }
        }
    }
}

// ── Tab Theo tháng: chọn tháng → xem từng phòng ──────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyTab(monthlyList: List<MonthlyRevenue>) {
    // Tháng được chọn (mặc định tháng mới nhất)
    var selectedMonth by remember(monthlyList) {
        mutableStateOf(monthlyList.firstOrNull()?.month ?: "")
    }
    var roomStats    by remember { mutableStateOf<RoomStatsResponse?>(null) }
    var loadingRooms by remember { mutableStateOf(false) }
    var dropdownOpen by remember { mutableStateOf(false) }

    // Load phòng khi đổi tháng
    LaunchedEffect(selectedMonth) {
        if (selectedMonth.isEmpty()) return@LaunchedEffect
        loadingRooms = true
        RetrofitClient.instance.getRoomStats(UserSession.uid, selectedMonth)
            .enqueue(object : Callback<RoomStatsResponse> {
                override fun onResponse(call: Call<RoomStatsResponse>, response: Response<RoomStatsResponse>) {
                    roomStats = response.body()
                    loadingRooms = false
                }
                override fun onFailure(call: Call<RoomStatsResponse>, t: Throwable) { loadingRooms = false }
            })
    }

    val chartData = monthlyList.take(6).reversed()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF4F6FB)),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Tổng quan header
        item {
            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF0D1B34))
                .padding(horizontal = 16.dp, vertical = 8.dp)) {
                val cur = monthlyList.firstOrNull()
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DarkStatCard("Tháng này đã thu",
                        formatPrice(cur?.paid ?: 0.0) + " đ", Color(0xFF4ADE80), Modifier.weight(1f))
                    DarkStatCard("Còn nợ",
                        formatPrice(cur?.debt ?: 0.0) + " đ", Color(0xFFFF6B6B), Modifier.weight(1f))
                }
            }
        }

        // Biểu đồ cột dọc từng phòng trong tháng được chọn
        item {
            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF0D1B34)).padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    // Header: tên tháng + dropdown chọn tháng
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Phòng · ${if (selectedMonth.length == 7) "T${selectedMonth.substring(5).trimStart('0')}/${selectedMonth.substring(0,4)}" else selectedMonth}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box {
                            Surface(
                                onClick = { dropdownOpen = true },
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text("Chọn tháng", color = Color.White, fontSize = 12.sp)
                                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            DropdownMenu(expanded = dropdownOpen, onDismissRequest = { dropdownOpen = false }) {
                                monthlyList.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(formatMonthLabel(m.month)) },
                                        onClick = { selectedMonth = m.month; dropdownOpen = false }
                                    )
                                }
                            }
                        }
                    }

                    if (loadingRooms) {
                        Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF4ADE80), modifier = Modifier.size(32.dp))
                        }
                    } else {
                        val rooms = roomStats?.rooms ?: emptyList()
                        if (rooms.isNotEmpty()) {
                            RoomVerticalBarChart(
                                rooms = rooms,
                                modifier = Modifier.fillMaxWidth().height(200.dp)
                            )
                        } else {
                            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                Text("Không có dữ liệu", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Danh sách phòng
        if (loadingRooms) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF007BFF))
                }
            }
        } else {
            val rooms = roomStats?.rooms ?: emptyList()
            if (rooms.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Home, null, tint = Color.LightGray, modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Không có hóa đơn trong tháng này", color = Color.Gray)
                        }
                    }
                }
            } else {
                // Tổng tháng
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ChipStat("Tổng đã thu", formatPrice(roomStats?.total_paid ?: 0.0) + " đ",
                            Color(0xFF4CAF50), Color(0xFFE8F5E9), Modifier.weight(1f))
                        ChipStat("Tổng còn nợ", formatPrice(roomStats?.total_debt ?: 0.0) + " đ",
                            Color(0xFFE53935), Color(0xFFFFEBEE), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                }

                item {
                    Text(
                        "Chi tiết từng phòng",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B34),
                        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                    )
                }

                itemsIndexed(rooms) { _, room ->
                    RoomStatCard(room = room)
                }
            }
        }
    }
}

@Composable
fun RoomStatCard(room: RoomStatItem) {
    val isPaid = room.status == "paid"
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Icon phòng
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape)
                    .background(if (isPaid) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Home, null,
                    tint = if (isPaid) Color(0xFF4CAF50) else Color(0xFFE53935),
                    modifier = Modifier.size(22.dp))
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(room.room_name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(room.tenant_name, fontSize = 13.sp, color = Color.Gray)
                if (room.room_address.isNotBlank()) {
                    Text(room.room_address, fontSize = 11.sp, color = Color.LightGray, maxLines = 1)
                }
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(formatPrice(room.total) + " đ", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    color = Color(0xFF0D1B34))
                Surface(
                    color = if (isPaid) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (isPaid) "✅ Đã thu" else "⚠️ Chưa thu",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = if (isPaid) Color(0xFF4CAF50) else Color(0xFFE53935),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

// ── Tab Theo năm ──────────────────────────────────────────────────────────

@Composable
fun YearlyTab(yearlyList: List<YearlyRevenue>, monthlyList: List<MonthlyRevenue>) {
    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()

    // Danh sách năm có dữ liệu (từ monthlyList)
    val availableYears = remember(monthlyList) {
        monthlyList.map { it.month.substring(0, 4) }.distinct().sortedDescending()
    }
    var selectedYear by remember(availableYears) {
        mutableStateOf(availableYears.firstOrNull() ?: currentYear)
    }

    val selectedYearMonths = monthlyList.filter { it.month.startsWith(selectedYear) }.reversed()
    val yearData = yearlyList.find { it.year == selectedYear }

    val yearIndex = availableYears.indexOf(selectedYear)
    val canPrev   = yearIndex < availableYears.size - 1  // năm cũ hơn
    val canNext   = yearIndex > 0                         // năm mới hơn

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF4F6FB)),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Bộ chọn năm + biểu đồ
        item {
            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF0D1B34)).padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    // Điều hướng năm
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (canPrev) selectedYear = availableYears[yearIndex + 1] },
                            enabled = canPrev
                        ) {
                            Icon(Icons.Default.ChevronLeft, null,
                                tint = if (canPrev) Color.White else Color.White.copy(alpha = 0.3f))
                        }
                        Text(
                            "Năm $selectedYear",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        IconButton(
                            onClick = { if (canNext) selectedYear = availableYears[yearIndex - 1] },
                            enabled = canNext
                        ) {
                            Icon(Icons.Default.ChevronRight, null,
                                tint = if (canNext) Color.White else Color.White.copy(alpha = 0.3f))
                        }
                    }

                    // Tổng năm được chọn
                    if (yearData != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DarkStatCard("Đã thu năm $selectedYear",
                                formatPrice(yearData.paid) + " đ", Color(0xFF4ADE80), Modifier.weight(1f))
                            DarkStatCard("Còn nợ",
                                formatPrice(yearData.debt) + " đ", Color(0xFFFF6B6B), Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // Biểu đồ các tháng trong năm được chọn
                    if (selectedYearMonths.isNotEmpty()) {
                        Text("Các tháng trong năm $selectedYear",
                            color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp))
                        RevenueBarChart(
                            data = selectedYearMonths,
                            modifier = Modifier.fillMaxWidth().height(180.dp)
                        )
                    } else {
                        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            Text("Không có dữ liệu năm $selectedYear", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Chi tiết từng tháng trong năm được chọn
        if (selectedYearMonths.isNotEmpty()) {
            item {
                Text("Chi tiết từng tháng", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B34),
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp))
            }
            val maxM = selectedYearMonths.maxOf { it.paid }.takeIf { it > 0 } ?: 1.0
            itemsIndexed(selectedYearMonths) { index, m ->
                MonthlyDetailCard(
                    item = m,
                    maxPaid = maxM,
                    isBest = m == selectedYearMonths.maxByOrNull { it.paid },
                    prevPaid = if (index > 0) selectedYearMonths[index - 1].paid else null
                )
            }
        }

        // Tổng quan tất cả năm
        item {
            Text("Tổng quan tất cả năm", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B34),
                modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
        }

        if (yearlyList.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.BarChart, null, tint = Color.LightGray, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(8.dp)); Text("Chưa có dữ liệu", color = Color.Gray)
                    }
                }
            }
        } else {
            val maxPaid = yearlyList.maxOf { it.paid }.takeIf { it > 0 } ?: 1.0
            itemsIndexed(yearlyList) { _, year ->
                YearlyDetailCard(year = year, maxPaid = maxPaid, isSelected = year.year == selectedYear,
                    onClick = { selectedYear = year.year })
            }
        }
    }
}

// ── Bar Chart ─────────────────────────────────────────────────────────────

@Composable
fun RevenueBarChart(
    data: List<MonthlyRevenue>,
    modifier: Modifier = Modifier,
    selectedMonth: String = "",
    onBarClick: ((String) -> Unit)? = null
) {
    val maxVal = data.maxOf { it.total_billed }.takeIf { it > 0 } ?: 1.0
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(data) { animProgress.animateTo(1f, tween(900, easing = EaseOutCubic)) }

    val paidColor  = Color(0xFF4ADE80)
    val debtColor  = Color(0xFFFF6B6B)
    val selColor   = Color(0xFF60A5FA)
    val gridColor  = Color.White.copy(alpha = 0.08f)
    val labelColor = Color.White.copy(alpha = 0.6f)

    Canvas(modifier = modifier) {
        val barCount   = data.size
        val totalWidth = size.width
        val chartH     = size.height - 28.dp.toPx()
        val barW       = (totalWidth / barCount) * 0.5f

        repeat(4) { i ->
            val y = chartH * (1f - (i + 1) / 4f)
            drawLine(gridColor, Offset(0f, y), Offset(totalWidth, y), strokeWidth = 1.dp.toPx())
        }

        data.forEachIndexed { i, item ->
            val centerX = (i + 0.5f) * (totalWidth / barCount)
            val left    = centerX - barW / 2f
            val isSelected = item.month == selectedMonth

            val paidH = (item.paid / maxVal * chartH * animProgress.value).toFloat()
            val debtH = (item.debt / maxVal * chartH * animProgress.value).toFloat()

            if (item.debt > 0) {
                drawRoundRect(
                    color = debtColor.copy(alpha = 0.4f),
                    topLeft = Offset(left, chartH - paidH - debtH),
                    size = Size(barW, debtH),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            }

            val brush = if (isSelected)
                Brush.verticalGradient(listOf(selColor, Color(0xFF3B82F6)), chartH - paidH, chartH)
            else
                Brush.verticalGradient(listOf(paidColor, Color(0xFF22C55E)), chartH - paidH, chartH)

            drawRoundRect(
                brush = brush,
                topLeft = Offset(left, chartH - paidH),
                size = Size(barW, paidH.coerceAtLeast(4.dp.toPx())),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = (if (isSelected) Color.White else labelColor).toArgb()
                    textSize = 10.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    if (isSelected) isFakeBoldText = true
                }
                val label = if (item.month.length == 7) "T${item.month.substring(5).trimStart('0')}" else item.month
                canvas.nativeCanvas.drawText(label, centerX, size.height, paint)
            }
        }
    }
}

// ── Cards ─────────────────────────────────────────────────────────────────

@Composable
fun MonthlyDetailCard(item: MonthlyRevenue, maxPaid: Double, isBest: Boolean, prevPaid: Double?) {
    val changePercent = if (prevPaid != null && prevPaid > 0)
        Math.round((item.paid - prevPaid) / prevPaid * 1000) / 10.0 else null
    val isUp = (changePercent ?: 0.0) > 0

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFE8F5E9)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CalendarMonth, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(formatMonthLabel(item.month), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            if (isBest) {
                                Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(6.dp)) {
                                    Text("🏆 Cao nhất", color = Color(0xFFFF8F00), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                        if (changePercent != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isUp) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    null, tint = if (isUp) Color(0xFF4CAF50) else Color(0xFFE53935),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(" ${if (isUp) "+" else ""}${changePercent}% tháng trước",
                                    fontSize = 11.sp, color = if (isUp) Color(0xFF4CAF50) else Color(0xFFE53935))
                            }
                        }
                    }
                }
                Text(formatPrice(item.paid) + " đ", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0D1B34))
            }

            val totalBilled = item.total_billed.takeIf { it > 0 } ?: 1.0
            val paidFrac = (item.paid / totalBilled).toFloat().coerceIn(0f, 1f)
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFF0F0F0))) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (paidFrac > 0f) Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(paidFrac)
                        .background(Brush.horizontalGradient(listOf(Color(0xFF4CAF50), Color(0xFF81C784)))))
                    if (paidFrac < 1f) Box(modifier = Modifier.fillMaxHeight().weight(1f).background(Color(0xFFFFCDD2)))
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ChipStat("Đã thu", formatPrice(item.paid) + " đ", Color(0xFF4CAF50), Color(0xFFE8F5E9), Modifier.weight(1f))
                if (item.debt > 0) ChipStat("Còn nợ", formatPrice(item.debt) + " đ", Color(0xFFE53935), Color(0xFFFFEBEE), Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun YearlyDetailCard(year: YearlyRevenue, maxPaid: Double, isSelected: Boolean = false, onClick: () -> Unit = {}) {
    val paidFrac = (year.paid / (year.total_billed.takeIf { it > 0 } ?: 1.0)).toFloat().coerceIn(0f, 1f)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE3F2FD) else Color.White
        ),
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 2.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF007BFF)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE3F2FD)), contentAlignment = Alignment.Center) {
                        Text(year.year.takeLast(2), fontWeight = FontWeight.Bold, color = Color(0xFF007BFF), fontSize = 14.sp)
                    }
                    Text("Năm ${year.year}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Text(formatPrice(year.paid) + " đ", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0D1B34))
            }
            Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFFF0F0F0))) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (paidFrac > 0f) Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(paidFrac)
                        .background(Brush.horizontalGradient(listOf(Color(0xFF007BFF), Color(0xFF4FC3F7)))))
                    if (paidFrac < 1f) Box(modifier = Modifier.fillMaxHeight().weight(1f).background(Color(0xFFFFCDD2)))
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ChipStat("Đã thu", formatPrice(year.paid) + " đ", Color(0xFF4CAF50), Color(0xFFE8F5E9), Modifier.weight(1f))
                if (year.debt > 0) ChipStat("Còn nợ", formatPrice(year.debt) + " đ", Color(0xFFE53935), Color(0xFFFFEBEE), Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun DarkStatCard(label: String, value: String, valueColor: Color, modifier: Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF1A2B4A)).padding(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun ChipStat(label: String, value: String, textColor: Color, bgColor: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(10.dp)).background(bgColor).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, fontSize = 11.sp, color = textColor.copy(alpha = 0.7f))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

fun formatMonthLabel(month: String): String =
    if (month.length == 7) "Tháng ${month.substring(5).trimStart('0')}/${month.substring(0, 4)}" else month

@Composable
fun RoomVerticalBarChart(rooms: List<RoomStatItem>, modifier: Modifier = Modifier) {
    if (rooms.isEmpty()) return
    val maxTotal = rooms.maxOf { it.total }.takeIf { it > 0 } ?: 1.0
    val animProgress = remember(rooms) { Animatable(0f) }
    LaunchedEffect(rooms) { animProgress.animateTo(1f, tween(900, easing = EaseOutCubic)) }

    val paidColor  = Color(0xFF4ADE80)
    val debtColor  = Color(0xFFFF6B6B)
    val gridColor  = Color.White.copy(alpha = 0.08f)
    val labelColor = Color.White.copy(alpha = 0.7f)

    Canvas(modifier = modifier) {
        val count      = rooms.size
        val totalWidth = size.width
        val chartH     = size.height - 30.dp.toPx()
        val barW       = (totalWidth / count) * 0.55f

        // Grid lines
        repeat(4) { i ->
            val y = chartH * (1f - (i + 1) / 4f)
            drawLine(gridColor, Offset(0f, y), Offset(totalWidth, y), strokeWidth = 1.dp.toPx())
        }

        rooms.forEachIndexed { i, room ->
            val centerX = (i + 0.5f) * (totalWidth / count)
            val left    = centerX - barW / 2f

            val totalH = (room.total / maxTotal * chartH * animProgress.value).toFloat()
            val paidH  = (room.paid  / maxTotal * chartH * animProgress.value).toFloat()
            val debtH  = (totalH - paidH).coerceAtLeast(0f)

            // Phần nợ (trên cùng)
            if (debtH > 0f) {
                drawRoundRect(
                    color = debtColor.copy(alpha = 0.5f),
                    topLeft = Offset(left, chartH - totalH),
                    size = Size(barW, debtH),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            }

            // Phần đã thu (dưới)
            if (paidH > 0f) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(paidColor, Color(0xFF22C55E)),
                        startY = chartH - paidH, endY = chartH
                    ),
                    topLeft = Offset(left, chartH - paidH),
                    size = Size(barW, paidH.coerceAtLeast(4.dp.toPx())),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            } else {
                // Phòng chưa thu gì — vẽ cột đỏ nhỏ
                drawRoundRect(
                    color = debtColor.copy(alpha = 0.5f),
                    topLeft = Offset(left, chartH - totalH),
                    size = Size(barW, totalH.coerceAtLeast(4.dp.toPx())),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            }

            // Nhãn tên phòng
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = labelColor.toArgb()
                    textSize = 9.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                val label = room.room_name.take(6)
                canvas.nativeCanvas.drawText(label, centerX, size.height, paint)
            }
        }
    }
}
