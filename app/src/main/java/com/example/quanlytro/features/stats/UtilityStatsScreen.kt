package package com.example.quanlytro.features.stats
import com.example.quanlytro.data.remote.*
import com.example.quanlytro.data.model.*

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtilityStatsScreen(onBackClick: () -> Unit = {}) {
    var invoices   by remember { mutableStateOf<List<InvoiceItem>>(emptyList()) }
    var isLoading  by remember { mutableStateOf(true) }
    var subTab     by remember { mutableStateOf(0) } // 0=Tất cả, 1=Điện, 2=Nước
    var selectedMonth by remember { mutableStateOf<String?>(null) } // Tháng được chọn để hiển thị header
    var monthExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        RetrofitClient.instance.getInvoicesByLandlord(UserSession.uid)
            .enqueue(object : Callback<InvoiceListResponse> {
                override fun onResponse(call: Call<InvoiceListResponse>, response: Response<InvoiceListResponse>) {
                    invoices  = response.body()?.invoices ?: emptyList()
                    isLoading = false
                    // Mặc định chọn tháng gần nhất
                    if (invoices.isNotEmpty() && selectedMonth == null) {
                        selectedMonth = invoices.maxByOrNull { it.month }?.month
                    }
                }
                override fun onFailure(call: Call<InvoiceListResponse>, t: Throwable) { isLoading = false }
            })
    }

    // ── Danh sách tháng có dữ liệu (sort DESC) ────────────────────────────────
    val availableMonths = invoices.map { it.month }.distinct().sortedDescending()

    // ── Tổng theo tháng được chọn ─────────────────────────────────────────────
    val monthInvoices = selectedMonth?.let { m -> invoices.filter { it.month == m } } ?: emptyList()
    val monthElectricUsed = monthInvoices.sumOf { it.electric_used }
    val monthElectricCost = monthInvoices.sumOf { it.electric_cost }
    val monthWaterUsed    = monthInvoices.sumOf { it.water_used }
    val monthWaterCost    = monthInvoices.sumOf { it.water_cost }

    // ── Nhóm theo phòng ───────────────────────────────────────────────────────
    val byRoom = invoices.groupBy { it.room_name ?: "Phòng không xác định" }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Thống kê Điện & Nước", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF0288D1))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF8F9FA)),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // ── Banner tổng quan theo tháng ──────────────────────────
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFF0277BD), Color(0xFF26C6DA)))
                            )
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Dropdown chọn tháng
                            ExposedDropdownMenuBox(
                                expanded = monthExpanded,
                                onExpandedChange = { monthExpanded = it }
                            ) {
                                Surface(
                                    modifier = Modifier.menuAnchor(),
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.CalendarMonth, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = selectedMonth?.replace("-", "/")?.let { "Tháng $it" } ?: "Chọn tháng",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Icon(
                                            if (monthExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            null, tint = Color.White, modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                ExposedDropdownMenu(
                                    expanded = monthExpanded,
                                    onDismissRequest = { monthExpanded = false }
                                ) {
                                    availableMonths.forEach { m ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "Tháng ${m.replace("-", "/")}",
                                                    fontWeight = if (m == selectedMonth) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (m == selectedMonth) Color(0xFF0288D1) else Color.Unspecified
                                                )
                                            },
                                            onClick = { selectedMonth = m; monthExpanded = false }
                                        )
                                    }
                                }
                            }

                            // Thống kê tháng được chọn
                            if (monthInvoices.isEmpty()) {
                                Text("Không có dữ liệu tháng này", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                            } else {
                                Text(
                                    "${monthInvoices.size} phòng có hóa đơn",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    BannerStatCard(
                                        icon  = Icons.Default.ElectricBolt,
                                        label = "Điện tháng này",
                                        value = "${"%.0f".format(monthElectricUsed)} kWh",
                                        sub   = "${formatPrice(monthElectricCost)} VND",
                                        color = Color(0xFFFFD54F),
                                        modifier = Modifier.weight(1f)
                                    )
                                    BannerStatCard(
                                        icon  = Icons.Default.Water,
                                        label = "Nước tháng này",
                                        value = "${"%.0f".format(monthWaterUsed)} m³",
                                        sub   = "${formatPrice(monthWaterCost)} VND",
                                        color = Color(0xFF80DEEA),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Sub-tab Tất cả/Điện/Nước ─────────────────────────────
                item {
                    TabRow(
                        selectedTabIndex = subTab,
                        containerColor   = Color.White,
                        contentColor     = Color(0xFF0288D1)
                    ) {
                        listOf("Tất cả", "Điện", "Nước").forEachIndexed { i, title ->
                            Tab(
                                selected = subTab == i,
                                onClick  = { subTab = i },
                                text = { Text(title, fontWeight = if (subTab == i) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }
                }

                if (invoices.isEmpty()) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ElectricBolt, null, tint = Color.LightGray, modifier = Modifier.size(56.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("Chưa có dữ liệu điện nước", color = Color.Gray)
                            }
                        }
                    }
                } else {
                    // ── VIEW: THEO PHÒNG ──────────────────────────────────
                    item { Spacer(Modifier.height(4.dp)) }
                    items(byRoom.toList()) { (roomName, roomInvoices) ->
                        RoomUtilityCard(
                            roomName = roomName,
                            invoices = roomInvoices,
                            tab = subTab,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Card tổng quan theo PHÒNG
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RoomUtilityCard(roomName: String, invoices: List<InvoiceItem>, tab: Int, modifier: Modifier = Modifier) {
    val totalE = invoices.sumOf { it.electric_used }
    val costE  = invoices.sumOf { it.electric_cost }
    val totalW = invoices.sumOf { it.water_used }
    val costW  = invoices.sumOf { it.water_cost }
    val latest = invoices.firstOrNull()
    val sortedInvoices = invoices.sortedByDescending { it.month }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.size(36.dp).background(Color(0xFFE1F5FE), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Home, null, tint = Color(0xFF0288D1), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(roomName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("${invoices.size} kỳ hóa đơn", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                latest?.let {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Kỳ gần nhất", fontSize = 10.sp, color = Color.Gray)
                        Text(it.month.replace("-", "/"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0288D1))
                    }
                }
            }

            Divider(color = Color(0xFFF0F0F0))

            // Tổng tích lũy (theo tab)
            if (tab == 0 || tab == 1) {
                // Điện
                Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFFFFF8E1), shape = RoundedCornerShape(10.dp)) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.ElectricBolt, null, tint = Color(0xFFFFA000), modifier = Modifier.size(13.dp))
                            Text("Điện (tích lũy)", fontSize = 11.sp, color = Color(0xFFFFA000), fontWeight = FontWeight.SemiBold)
                        }
                        Text("${"%.0f".format(totalE)} kWh", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${formatPrice(costE)} VND", fontSize = 11.sp, color = Color.Gray)
                        latest?.electric_price?.let { p ->
                            if (p > 0) Text("${formatPrice(p)} đ/kWh", fontSize = 10.sp, color = Color(0xFFFFA000))
                        }
                    }
                }
            }
            if (tab == 0 || tab == 2) {
                // Nước
                Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFFE1F5FE), shape = RoundedCornerShape(10.dp)) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Water, null, tint = Color(0xFF0288D1), modifier = Modifier.size(13.dp))
                            Text("Nước (tích lũy)", fontSize = 11.sp, color = Color(0xFF0288D1), fontWeight = FontWeight.SemiBold)
                        }
                        Text("${"%.0f".format(totalW)} m³", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${formatPrice(costW)} VND", fontSize = 11.sp, color = Color.Gray)
                        latest?.water_price?.let { p ->
                            if (p > 0) Text("${formatPrice(p)} đ/m³", fontSize = 10.sp, color = Color(0xFF0288D1))
                        }
                    }
                }
            }

            // Lịch sử các kỳ
            if (sortedInvoices.isNotEmpty()) {
                Divider(color = Color(0xFFF0F0F0))
                Text("Lịch sử các kỳ", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    sortedInvoices.forEach { inv ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                inv.month.replace("-", "/"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF333333),
                                modifier = Modifier.width(60.dp)
                            )
                            if (tab != 2) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ElectricBolt, null, tint = Color(0xFFFFA000), modifier = Modifier.size(12.dp))
                                    Text("${"%.0f".format(inv.electric_used)} kWh", fontSize = 12.sp, color = Color(0xFF555555))
                                }
                            }
                            if (tab == 0) Text("→", fontSize = 11.sp, color = Color.LightGray)
                            if (tab != 1) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Water, null, tint = Color(0xFF0288D1), modifier = Modifier.size(12.dp))
                                    Text("${"%.0f".format(inv.water_used)} m³", fontSize = 12.sp, color = Color(0xFF555555))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Banner stat card (dùng trong header)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BannerStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = color, fontSize = 12.sp)
        }
    }
}
