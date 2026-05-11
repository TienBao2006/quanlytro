package com.example.quanlytro.screen

import com.example.quanlytro.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import android.content.Intent
import android.net.Uri

// ─────────────────────────────────────────────────────────────────────────────
// Màn hình quản lý hóa đơn cho CHỦ TRỌ
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceManageScreen(onBackClick: () -> Unit = {}) {
    var contracts by remember { mutableStateOf<List<ContractItem>>(emptyList()) }
    var invoices  by remember { mutableStateOf<List<InvoiceItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingInvoice   by remember { mutableStateOf<InvoiceItem?>(null) }
    var deletingInvoice  by remember { mutableStateOf<InvoiceItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var snackMsg by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    // sort: "month_desc" | "month_asc" | "room_asc"
    var sortMode by remember { mutableStateOf("month_desc") }
    var showSortMenu by remember { mutableStateOf(false) }

    fun loadContracts() {
        RetrofitClient.instance.getContractsByLandlord(UserSession.uid)
            .enqueue(object : Callback<ContractListResponse> {
                override fun onResponse(call: Call<ContractListResponse>, response: Response<ContractListResponse>) {
                    contracts = response.body()?.contracts
                        ?.filter { it.status == "agreed" } ?: emptyList()
                }
                override fun onFailure(call: Call<ContractListResponse>, t: Throwable) {}
            })
    }

    fun loadInvoices() {
        isLoading = true
        RetrofitClient.instance.getInvoicesByLandlord(UserSession.uid)
            .enqueue(object : Callback<InvoiceListResponse> {
                override fun onResponse(call: Call<InvoiceListResponse>, response: Response<InvoiceListResponse>) {
                    invoices  = response.body()?.invoices ?: emptyList()
                    isLoading = false
                }
                override fun onFailure(call: Call<InvoiceListResponse>, t: Throwable) {
                    isLoading = false
                }
            })
    }

    LaunchedEffect(Unit) { loadContracts(); loadInvoices() }

    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackbarHostState.showSnackbar(it); snackMsg = null }
    }

    if (showCreateDialog) {
        CreateInvoiceDialog(
            contracts = contracts,
            onDismiss = { showCreateDialog = false },
            onSuccess = { msg ->
                showCreateDialog = false
                snackMsg = msg
                loadInvoices()
            }
        )
    }

    // Dialog sửa hóa đơn
    editingInvoice?.let { inv ->
        EditInvoiceDialog(
            invoice = inv,
            onDismiss = { editingInvoice = null },
            onSuccess = { msg ->
                editingInvoice = null
                snackMsg = msg
                loadInvoices()
            }
        )
    }

    // Dialog xác nhận xóa
    deletingInvoice?.let { inv ->
        AlertDialog(
            onDismissRequest = { deletingInvoice = null },
            icon = { Icon(Icons.Default.DeleteForever, null, tint = Color(0xFFD32F2F)) },
            title = { Text("Xóa hóa đơn?") },
            text = { Text("Hóa đơn tháng ${inv.month.replace("-", "/")} của ${inv.room_name ?: ""} sẽ bị xóa vĩnh viễn.") },
            confirmButton = {
                Button(
                    onClick = {
                        RetrofitClient.instance.deleteInvoice(inv.id, UserSession.uid)
                            .enqueue(object : Callback<SimpleResponse> {
                                override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                                    deletingInvoice = null
                                    snackMsg = if (response.body()?.status == "success") "Đã xóa hóa đơn" else "Xóa thất bại"
                                    loadInvoices()
                                }
                                override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                                    deletingInvoice = null
                                    snackMsg = "Lỗi kết nối"
                                }
                            })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) { Text("Xóa") }
            },
            dismissButton = {
                OutlinedButton(onClick = { deletingInvoice = null }) { Text("Hủy") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Quản lý hóa đơn", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, null, tint = Color(0xFF007BFF))
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Tháng mới nhất") },
                                onClick = { sortMode = "month_desc"; showSortMenu = false },
                                leadingIcon = { if (sortMode == "month_desc") Icon(Icons.Default.Check, null, tint = Color(0xFF007BFF)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Tháng cũ nhất") },
                                onClick = { sortMode = "month_asc"; showSortMenu = false },
                                leadingIcon = { if (sortMode == "month_asc") Icon(Icons.Default.Check, null, tint = Color(0xFF007BFF)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Theo phòng (A-Z)") },
                                onClick = { sortMode = "room_asc"; showSortMenu = false },
                                leadingIcon = { if (sortMode == "room_asc") Icon(Icons.Default.Check, null, tint = Color(0xFF007BFF)) }
                            )
                        }
                    }
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, null, tint = Color(0xFF007BFF))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        // Lọc + sắp xếp — key theo searchQuery, sortMode, invoices để recompute đúng
        val filtered = remember(searchQuery, sortMode, invoices) {
            invoices
                .filter { inv ->
                    if (searchQuery.isBlank()) true
                    else {
                        val q = searchQuery.trim().lowercase()
                        inv.month.contains(q) ||
                                (inv.room_name?.lowercase()?.contains(q) == true)
                    }
                }
                .let { list ->
                    when (sortMode) {
                        "month_asc" -> list.sortedBy { it.month }
                        "room_asc"  -> list.sortedWith(compareBy({ it.room_name ?: "" }, { it.month }))
                        else        -> list.sortedByDescending { it.month }
                    }
                }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF007BFF))
            }
        } else if (invoices.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Receipt, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Chưa có hóa đơn nào", color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF))
                    ) { Text("Tạo hóa đơn") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8F9FA)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Thanh tìm kiếm
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Tìm theo tháng hoặc tên phòng...") },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, null, tint = Color.Gray)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF007BFF),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )
                }

                if (filtered.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Không tìm thấy hóa đơn nào", color = Color.Gray)
                        }
                    }
                } else {
                    // Nhóm theo tháng khi sort theo tháng, nhóm theo phòng khi sort theo phòng
                    if (sortMode == "room_asc") {
                        val grouped = filtered.groupBy { it.room_name ?: "Không rõ phòng" }
                        grouped.forEach { (room, list) ->
                            item {
                                Text(
                                    room,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF555555),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(list) { inv ->
                                InvoiceCard(
                                    invoice = inv, isLandlord = true,
                                    onMarkPaid = {
                                        val newStatus = if (inv.status == "paid") "unpaid" else "paid"
                                        RetrofitClient.instance.updateInvoiceStatus(inv.id, newStatus, UserSession.uid)
                                            .enqueue(object : Callback<SimpleResponse> {
                                                override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                                                    snackMsg = if (newStatus == "paid") "Đã đánh dấu đã thanh toán" else "Đã đánh dấu chưa thanh toán"
                                                    loadInvoices()
                                                }
                                                override fun onFailure(call: Call<SimpleResponse>, t: Throwable) { snackMsg = "Lỗi kết nối" }
                                            })
                                    },
                                    onEdit   = { editingInvoice = inv },
                                    onDelete = { deletingInvoice = inv }
                                )
                            }
                        }
                    } else {
                        val grouped = filtered.groupBy { it.month }
                        grouped.forEach { (month, list) ->
                            item {
                                Text(
                                    "Tháng ${month.replace("-", "/")}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF555555),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(list) { inv ->
                                InvoiceCard(
                                    invoice = inv, isLandlord = true,
                                    onMarkPaid = {
                                        val newStatus = if (inv.status == "paid") "unpaid" else "paid"
                                        RetrofitClient.instance.updateInvoiceStatus(inv.id, newStatus, UserSession.uid)
                                            .enqueue(object : Callback<SimpleResponse> {
                                                override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                                                    snackMsg = if (newStatus == "paid") "Đã đánh dấu đã thanh toán" else "Đã đánh dấu chưa thanh toán"
                                                    loadInvoices()
                                                }
                                                override fun onFailure(call: Call<SimpleResponse>, t: Throwable) { snackMsg = "Lỗi kết nối" }
                                            })
                                    },
                                    onEdit   = { editingInvoice = inv },
                                    onDelete = { deletingInvoice = inv }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Màn hình xem hóa đơn cho NGƯỜI THUÊ
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantInvoiceScreen(onBackClick: () -> Unit = {}) {
    var invoices  by remember { mutableStateOf<List<InvoiceItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var payingInvoice by remember { mutableStateOf<InvoiceItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var snackMsg by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    fun loadInvoices() {
        isLoading = true
        RetrofitClient.instance.getInvoicesByTenant(UserSession.uid)
            .enqueue(object : Callback<InvoiceListResponse> {
                override fun onResponse(call: Call<InvoiceListResponse>, response: Response<InvoiceListResponse>) {
                    invoices  = response.body()?.invoices ?: emptyList()
                    isLoading = false
                }
                override fun onFailure(call: Call<InvoiceListResponse>, t: Throwable) {
                    isLoading = false
                }
            })
    }

    LaunchedEffect(Unit) { loadInvoices() }

    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackbarHostState.showSnackbar(it); snackMsg = null }
    }

    // Dialog thanh toán
    payingInvoice?.let { inv ->
        PaymentDialog(
            invoice = inv,
            onDismiss = { payingInvoice = null },
            onSuccess = { msg ->
                payingInvoice = null
                snackMsg = msg
                loadInvoices()
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Hóa đơn của tôi", fontWeight = FontWeight.Bold) },
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
                CircularProgressIndicator(color = Color(0xFF007BFF))
            }
        } else if (invoices.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Receipt, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Chưa có hóa đơn nào", color = Color.Gray)
                }
            }
        } else {
            val filtered = remember(searchQuery, invoices) {
                invoices
                    .filter { inv ->
                        if (searchQuery.isBlank()) true
                        else {
                            val q = searchQuery.trim().lowercase()
                            inv.month.contains(q) || (inv.room_name?.lowercase()?.contains(q) == true)
                        }
                    }
                    .sortedByDescending { it.month }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8F9FA)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Tìm theo tháng hoặc tên phòng...") },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, null, tint = Color.Gray)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF007BFF),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )
                }

                if (filtered.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Không tìm thấy hóa đơn nào", color = Color.Gray)
                        }
                    }
                } else {
                    val grouped = filtered.groupBy { it.month }
                    grouped.forEach { (month, list) ->
                        item {
                            Text(
                                "Tháng ${month.replace("-", "/")}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF555555),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(list) { inv ->
                            InvoiceCard(
                                invoice = inv,
                                isLandlord = false,
                                onMarkPaid = {},
                                onPayClick = { payingInvoice = inv }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Card hiển thị 1 hóa đơn
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun InvoiceCard(
    invoice: InvoiceItem,
    isLandlord: Boolean,
    onMarkPaid: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onPayClick: () -> Unit = {}
) {
    val isPaid = invoice.status == "paid"
    val statusColor = if (isPaid) Color(0xFF4CAF50) else Color(0xFFFF9800)
    val statusLabel = if (isPaid) "Đã thanh toán" else "Chưa thanh toán"

    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Tháng ${invoice.month.replace("-", "/")}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    invoice.room_name?.let {
                        Text(it, fontSize = 12.sp, color = Color.Gray)
                    }
                    // Thời gian tạo
                    if (invoice.created_at.isNotBlank()) {
                        Text(
                            "Tạo lúc: ${formatDateTime(invoice.created_at)}",
                            fontSize = 11.sp,
                            color = Color(0xFFAAAAAA)
                        )
                    }
                }
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        statusLabel,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = Color(0xFFF0F0F0))
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tổng tiền", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    "${formatPrice(invoice.total)} VND",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF007BFF)
                )
            }

            TextButton(
                onClick = { expanded = !expanded },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    if (expanded) "Ẩn chi tiết" else "Xem chi tiết",
                    fontSize = 13.sp,
                    color = Color(0xFF007BFF)
                )
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    null,
                    tint = Color(0xFF007BFF),
                    modifier = Modifier.size(18.dp)
                )
            }

            if (expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    InvoiceRow("Tiền phòng", formatPrice(invoice.rent_price))
                    InvoiceRow(
                        "Điện (${invoice.electric_used.toInt()} kWh × ${formatPrice(invoice.electric_price)} đ)",
                        formatPrice(invoice.electric_cost),
                        sub = "Chỉ số: ${invoice.electric_old.toInt()} → ${invoice.electric_new.toInt()}"
                    )
                    InvoiceRow(
                        "Nước (${invoice.water_used.toInt()} m³ × ${formatPrice(invoice.water_price)} đ)",
                        formatPrice(invoice.water_cost),
                        sub = "Chỉ số: ${invoice.water_old.toInt()} → ${invoice.water_new.toInt()}"
                    )
                    if (invoice.other_fee > 0) {
                        InvoiceRow(
                            "Phí khác${if (!invoice.other_fee_note.isNullOrBlank()) " (${invoice.other_fee_note})" else ""}",
                            formatPrice(invoice.other_fee)
                        )
                    }
                }
            }

            if (isLandlord) {
                Spacer(Modifier.height(8.dp))
                // Nút đánh dấu thanh toán
                Button(
                    onClick = onMarkPaid,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPaid) Color(0xFFE0E0E0) else Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        if (isPaid) Icons.Default.Close else Icons.Default.Check,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isPaid) "Đánh dấu chưa thanh toán" else "Đánh dấu đã thanh toán",
                        color = if (isPaid) Color.DarkGray else Color.White
                    )
                }
                // Nút sửa / xóa
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1565C0)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1565C0))
                    ) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Sửa", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD32F2F))
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Xóa", fontSize = 13.sp)
                    }
                }
            } else if (!isPaid) {
                // Nút thanh toán cho người thuê
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onPayClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF))
                ) {
                    Icon(Icons.Default.Payment, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Thanh toán", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun InvoiceRow(label: String, value: String, sub: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, color = Color(0xFF555555))
            sub?.let { Text(it, fontSize = 11.sp, color = Color.Gray) }
        }
        Text("$value VND", fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dialog tạo hóa đơn — giao diện đẹp hơn với full-screen dialog
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInvoiceDialog(
    contracts: List<ContractItem>,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    var selectedContract by remember { mutableStateOf<ContractItem?>(null) }
    var month        by remember { mutableStateOf("") }
    var electricOld  by remember { mutableStateOf("") }
    var electricNew  by remember { mutableStateOf("") }
    var electricPrice by remember { mutableStateOf("") }
    var waterOld     by remember { mutableStateOf("") }
    var waterNew     by remember { mutableStateOf("") }
    var waterPrice   by remember { mutableStateOf("") }
    var otherFee     by remember { mutableStateOf("") }
    var otherFeeNote by remember { mutableStateOf("") }
    var isLoading    by remember { mutableStateOf(false) }
    var isLoadingPrev by remember { mutableStateOf(false) }
    var errorMsg     by remember { mutableStateOf("") }
    var contractExpanded by remember { mutableStateOf(false) }

    // Thời gian tạo hiện tại
    val createdAt = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
    }

    // Khi chọn hợp đồng → load hóa đơn gần nhất để prefill
    fun loadPreviousInvoice(contract: ContractItem) {
        isLoadingPrev = true
        // Prefill giá từ hợp đồng trước
        electricPrice = contract.electric_price.toInt().toString()
        waterPrice    = contract.water_price.toInt().toString()
        RetrofitClient.instance.getInvoicesByContract(UserSession.uid, contract.id)
            .enqueue(object : Callback<InvoiceListResponse> {
                override fun onResponse(call: Call<InvoiceListResponse>, response: Response<InvoiceListResponse>) {
                    isLoadingPrev = false
                    val latest = response.body()?.invoices?.firstOrNull() // đã sort DESC
                    if (latest != null) {
                        // Chỉ số cũ = chỉ số mới của kỳ trước
                        electricOld = latest.electric_new.toInt().toString()
                        waterOld    = latest.water_new.toInt().toString()
                        // Tháng tiếp theo
                        month = nextMonth(latest.month)
                    } else {
                        // Chưa có hóa đơn nào → tháng hiện tại
                        electricOld = ""
                        waterOld    = ""
                        month = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
                    }
                    electricNew = ""
                    waterNew    = ""
                }
                override fun onFailure(call: Call<InvoiceListResponse>, t: Throwable) {
                    isLoadingPrev = false
                }
            })
    }

    // Tính preview — dùng giá nhập tay nếu có, fallback về hợp đồng
    val ePrice = electricPrice.toDoubleOrNull() ?: (selectedContract?.electric_price ?: 0.0)
    val wPrice = waterPrice.toDoubleOrNull()    ?: (selectedContract?.water_price    ?: 0.0)
    val eUsed = (electricNew.toDoubleOrNull() ?: 0.0) - (electricOld.toDoubleOrNull() ?: 0.0)
    val wUsed = (waterNew.toDoubleOrNull() ?: 0.0) - (waterOld.toDoubleOrNull() ?: 0.0)
    val eCost = eUsed.coerceAtLeast(0.0) * ePrice
    val wCost = wUsed.coerceAtLeast(0.0) * wPrice
    val total = (selectedContract?.rent_price ?: 0.0) + eCost + wCost + (otherFee.toDoubleOrNull() ?: 0.0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFF8F9FA)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Header gradient ──────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF1565C0), Color(0xFF1E88E5))
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Tạo hóa đơn mới",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Ngày tạo: $createdAt",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp
                            )
                        }
                    }
                    // Nút đóng
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }

                // ── Nội dung cuộn ────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // Section: Chọn hợp đồng
                    InvoiceSectionCard(title = "Thông tin hợp đồng", icon = Icons.Default.Description) {
                        ExposedDropdownMenuBox(
                            expanded = contractExpanded,
                            onExpandedChange = { contractExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedContract?.let {
                                    "${it.tenant_name} — ${it.room_name ?: "Phòng ${it.id}"}"
                                } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Chọn hợp đồng") },
                                placeholder = { Text("Chọn hợp đồng...") },
                                trailingIcon = {
                                    if (isLoadingPrev)
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    else
                                        ExposedDropdownMenuDefaults.TrailingIcon(contractExpanded)
                                },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = contractExpanded,
                                onDismissRequest = { contractExpanded = false }
                            ) {
                                if (contracts.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Không có hợp đồng đang hoạt động", color = Color.Gray) },
                                        onClick = { contractExpanded = false }
                                    )
                                }
                                contracts.forEach { c ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(c.tenant_name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Text(
                                                    c.room_name ?: "Phòng ${c.id}",
                                                    fontSize = 12.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedContract = c
                                            contractExpanded = false
                                            loadPreviousInvoice(c)
                                        }
                                    )
                                }
                            }
                        }

                        // Hiển thị thông tin hợp đồng đã chọn
                        selectedContract?.let { c ->
                            Spacer(Modifier.height(6.dp))
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    ContractInfoChip("Tiền phòng", "${formatPrice(c.rent_price)} đ")
                                    ContractInfoChip("Điện/kWh", "${formatPrice(ePrice)} đ")
                                    ContractInfoChip("Nước/m³", "${formatPrice(wPrice)} đ")
                                }
                            }
                        }
                    }

                    // Section: Kỳ thanh toán
                    InvoiceSectionCard(title = "Kỳ thanh toán", icon = Icons.Default.CalendarMonth) {
                        OutlinedTextField(
                            value = month,
                            onValueChange = { month = it },
                            label = { Text("Tháng (YYYY-MM)") },
                            placeholder = { Text("2025-04") },
                            leadingIcon = {
                                Icon(Icons.Default.DateRange, null, tint = Color(0xFF1E88E5))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Section: Chỉ số điện
                    InvoiceSectionCard(title = "Chỉ số điện", icon = Icons.Default.ElectricBolt, iconTint = Color(0xFFFFA000)) {
                        OutlinedTextField(
                            value = electricPrice,
                            onValueChange = { electricPrice = it },
                            label = { Text("Đơn giá điện") },
                            suffix = { Text("đ/kWh", fontSize = 12.sp, color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = electricOld,
                                onValueChange = { electricOld = it },
                                label = { Text("Chỉ số cũ") },
                                suffix = { Text("kWh", fontSize = 12.sp, color = Color.Gray) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = electricNew,
                                onValueChange = { electricNew = it },
                                label = { Text("Chỉ số mới") },
                                suffix = { Text("kWh", fontSize = 12.sp, color = Color.Gray) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        if (eUsed > 0) {
                            Text(
                                "Tiêu thụ: ${"%.0f".format(eUsed)} kWh × ${formatPrice(ePrice)} đ  →  ${formatPrice(eCost)} VND",
                                fontSize = 12.sp,
                                color = Color(0xFFFFA000),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Section: Chỉ số nước
                    InvoiceSectionCard(title = "Chỉ số nước", icon = Icons.Default.Water, iconTint = Color(0xFF0288D1)) {
                        OutlinedTextField(
                            value = waterPrice,
                            onValueChange = { waterPrice = it },
                            label = { Text("Đơn giá nước") },
                            suffix = { Text("đ/m³", fontSize = 12.sp, color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = waterOld,
                                onValueChange = { waterOld = it },
                                label = { Text("Chỉ số cũ") },
                                suffix = { Text("m³", fontSize = 12.sp, color = Color.Gray) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = waterNew,
                                onValueChange = { waterNew = it },
                                label = { Text("Chỉ số mới") },
                                suffix = { Text("m³", fontSize = 12.sp, color = Color.Gray) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        if (wUsed > 0) {
                            Text(
                                "Tiêu thụ: ${"%.0f".format(wUsed)} m³ × ${formatPrice(wPrice)} đ  →  ${formatPrice(wCost)} VND",
                                fontSize = 12.sp,
                                color = Color(0xFF0288D1),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Section: Phí khác
                    InvoiceSectionCard(title = "Phí khác (tuỳ chọn)", icon = Icons.Default.AddCircleOutline, iconTint = Color(0xFF7B1FA2)) {
                        OutlinedTextField(
                            value = otherFee,
                            onValueChange = { otherFee = it },
                            label = { Text("Số tiền (VND)") },
                            leadingIcon = { Icon(Icons.Default.AttachMoney, null, tint = Color(0xFF7B1FA2)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = otherFeeNote,
                            onValueChange = { otherFeeNote = it },
                            label = { Text("Ghi chú") },
                            leadingIcon = { Icon(Icons.Default.Notes, null, tint = Color(0xFF7B1FA2)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Preview tổng hóa đơn
                    if (selectedContract != null) {
                        Surface(
                            color = Color(0xFF1565C0),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Summarize, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Xem trước hóa đơn", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Divider(color = Color.White.copy(alpha = 0.3f))
                                PreviewRowWhite("Tiền phòng", formatPrice(selectedContract!!.rent_price))
                                PreviewRowWhite(
                                    "Điện (${"%.0f".format(eUsed.coerceAtLeast(0.0))} kWh)",
                                    formatPrice(eCost)
                                )
                                PreviewRowWhite(
                                    "Nước (${"%.0f".format(wUsed.coerceAtLeast(0.0))} m³)",
                                    formatPrice(wCost)
                                )
                                if ((otherFee.toDoubleOrNull() ?: 0.0) > 0) {
                                    PreviewRowWhite(
                                        "Phí khác${if (otherFeeNote.isNotBlank()) " ($otherFeeNote)" else ""}",
                                        formatPrice(otherFee.toDoubleOrNull() ?: 0.0)
                                    )
                                }
                                Divider(color = Color.White.copy(alpha = 0.3f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("TỔNG CỘNG", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        "${formatPrice(total)} VND",
                                        color = Color(0xFFFFD54F),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                }
                            }
                        }
                    }

                    // Lỗi
                    if (errorMsg.isNotBlank()) {
                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(errorMsg, color = Color(0xFFD32F2F), fontSize = 13.sp)
                            }
                        }
                    }
                }

                // ── Footer buttons ────────────────────────────────────────────
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Hủy")
                        }

                        Button(
                            onClick = {
                                val c = selectedContract
                                if (c == null) { errorMsg = "Vui lòng chọn hợp đồng"; return@Button }
                                if (month.isBlank()) { errorMsg = "Vui lòng nhập tháng (YYYY-MM)"; return@Button }
                                val eO = electricOld.toDoubleOrNull()
                                val eN = electricNew.toDoubleOrNull()
                                val wO = waterOld.toDoubleOrNull()
                                val wN = waterNew.toDoubleOrNull()
                                if (eO == null || eN == null || wO == null || wN == null) {
                                    errorMsg = "Chỉ số điện/nước không hợp lệ"; return@Button
                                }
                                if (eN < eO) { errorMsg = "Chỉ số điện mới phải ≥ cũ"; return@Button }
                                if (wN < wO) { errorMsg = "Chỉ số nước mới phải ≥ cũ"; return@Button }

                                isLoading = true
                                errorMsg = ""
                                RetrofitClient.instance.createInvoice(
                                    contractId    = c.id,
                                    landlordId    = UserSession.uid,
                                    month         = month,
                                    electricOld   = eO,
                                    electricNew   = eN,
                                    electricPrice = ePrice,
                                    waterOld      = wO,
                                    waterNew      = wN,
                                    waterPrice    = wPrice,
                                    otherFee      = otherFee.toDoubleOrNull() ?: 0.0,
                                    otherFeeNote  = otherFeeNote
                                ).enqueue(object : Callback<CreateInvoiceResponse> {
                                    override fun onResponse(call: Call<CreateInvoiceResponse>, response: Response<CreateInvoiceResponse>) {
                                        isLoading = false
                                        val body = response.body()
                                        if (body?.status == "success") {
                                            onSuccess("Tạo hóa đơn thành công! Tổng: ${formatPrice(body.total ?: 0.0)} VND")
                                        } else {
                                            errorMsg = body?.message ?: "Lỗi không xác định"
                                        }
                                    }
                                    override fun onFailure(call: Call<CreateInvoiceResponse>, t: Throwable) {
                                        isLoading = false
                                        errorMsg = "Lỗi kết nối: ${t.message}"
                                    }
                                })
                            },
                            modifier = Modifier.weight(2f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.ReceiptLong, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Tạo hóa đơn", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Helper composables ────────────────────────────────────────────────────────

@Composable
private fun InvoiceSectionCard(
    title: String,
    icon: ImageVector,
    iconTint: Color = Color(0xFF1E88E5),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(iconTint.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF333333))
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun ContractInfoChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = Color(0xFF666666))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
    }
}

@Composable
private fun PreviewRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text("$value VND", fontSize = 12.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun PreviewRowWhite(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
        Text("$value VND", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

// ── Payment Dialog ────────────────────────────────────────────────────────────

enum class PaymentMethod(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    CASH("Tiền mặt (COD)", Icons.Default.Money),
    WALLET("Ví điện tử", Icons.Default.AccountBalanceWallet),
    QR_CODE("Quét mã QR", Icons.Default.QrCode)
}

@Composable
fun PaymentDialog(
    invoice: InvoiceItem,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    var selectedMethod by remember { mutableStateOf<PaymentMethod?>(null) }
    var showConfirm    by remember { mutableStateOf(false) }
    var showResult     by remember { mutableStateOf(false) }
    var txnId          by remember { mutableStateOf("") }
    var isLoading      by remember { mutableStateOf(false) }

    val paidTime = remember {
        java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
    }

    // Màn kết quả thành công
    if (showResult) {
        Dialog(onDismissRequest = {}) {
            Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFF4CAF50).copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(44.dp))
                    }
                    Text("Thanh toán thành công", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Divider()
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ResultRow("Số tiền", "${formatPrice(invoice.total)} VND")
                        ResultRow("Phương thức", selectedMethod?.label ?: "")
                        ResultRow("Mã giao dịch", txnId)
                        ResultRow("Thời gian", paidTime)
                    }
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { onSuccess("Thanh toán thành công") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) { Text("Đóng", fontWeight = FontWeight.Bold) }
                }
            }
        }
        return
    }

    // Popup xác nhận
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            icon = { Icon(Icons.Default.Payment, null, tint = Color(0xFF007BFF)) },
            title = { Text("Xác nhận thanh toán") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Bạn có chắc muốn thanh toán")
                    Text(
                        "${formatPrice(invoice.total)} VND",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF007BFF)
                    )
                    Text("qua ${selectedMethod?.label}?", color = Color.Gray, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isLoading = true
                        val generatedTxn = "TXN" + System.currentTimeMillis().toString().takeLast(6)
                        val methodKey = when (selectedMethod) {
                            PaymentMethod.CASH   -> "cash"
                            PaymentMethod.WALLET -> "wallet"
                            else -> ""
                        }
                        RetrofitClient.instance.updateInvoiceStatus(
                            invoiceId     = invoice.id,
                            status        = "paid",
                            userId        = UserSession.uid,
                            paymentMethod = methodKey,
                            txnId         = generatedTxn
                        ).enqueue(object : retrofit2.Callback<SimpleResponse> {
                                override fun onResponse(call: retrofit2.Call<SimpleResponse>, response: retrofit2.Response<SimpleResponse>) {
                                    isLoading = false
                                    if (response.body()?.status == "success") {
                                        txnId = generatedTxn
                                        showConfirm = false
                                        showResult = true
                                    }
                                }
                                override fun onFailure(call: retrofit2.Call<SimpleResponse>, t: Throwable) {
                                    isLoading = false
                                    showConfirm = false
                                }
                            })
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF))
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Xác nhận")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirm = false }) { Text("Hủy") }
            }
        )
        return
    }

    // Màn chọn phương thức
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFF8F9FA)
        ) {
            Column {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF007BFF), Color(0xFF1E88E5))),
                            RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payment, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Thanh toán hóa đơn", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tháng ${invoice.month.replace("-", "/")} — ${invoice.room_name ?: ""}",
                            color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }

                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Tổng tiền
                    Surface(color = Color(0xFFE3F2FD), shape = RoundedCornerShape(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tổng cần thanh toán", fontSize = 14.sp, color = Color(0xFF1565C0))
                            Text(
                                "${formatPrice(invoice.total)} VND",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF1565C0)
                            )
                        }
                    }

                    Text("Chọn phương thức thanh toán", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                    // Các lựa chọn
                    PaymentMethod.values().forEach { method ->
                        val isSelected = selectedMethod == method
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMethod = method },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFFE3F2FD) else Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFF007BFF) else Color(0xFFE0E0E0)
                            ),
                            shadowElevation = if (isSelected) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            if (isSelected) Color(0xFF007BFF) else Color(0xFFF0F0F0),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        method.icon, null,
                                        tint = if (isSelected) Color.White else Color(0xFF666666),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Text(
                                    method.label,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF007BFF) else Color(0xFF333333)
                                )
                                Spacer(Modifier.weight(1f))
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF007BFF), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Nút tiếp tục
                    Button(
                        onClick = { showConfirm = true },
                        enabled = selectedMethod != null,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF))
                    ) {
                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Tiếp tục", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = Color.Gray)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ── Edit Invoice Dialog ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditInvoiceDialog(
    invoice: InvoiceItem,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    var month         by remember { mutableStateOf(invoice.month) }
    var electricOld   by remember { mutableStateOf(invoice.electric_old.toInt().toString()) }
    var electricNew   by remember { mutableStateOf(invoice.electric_new.toInt().toString()) }
    var electricPrice by remember { mutableStateOf(invoice.electric_price.toInt().toString()) }
    var waterOld      by remember { mutableStateOf(invoice.water_old.toInt().toString()) }
    var waterNew      by remember { mutableStateOf(invoice.water_new.toInt().toString()) }
    var waterPrice    by remember { mutableStateOf(invoice.water_price.toInt().toString()) }
    var otherFee      by remember { mutableStateOf(if (invoice.other_fee > 0) invoice.other_fee.toInt().toString() else "") }
    var otherFeeNote  by remember { mutableStateOf(invoice.other_fee_note ?: "") }
    var isLoading     by remember { mutableStateOf(false) }
    var errorMsg      by remember { mutableStateOf("") }

    val ePrice = electricPrice.toDoubleOrNull() ?: 0.0
    val wPrice = waterPrice.toDoubleOrNull()    ?: 0.0
    val eUsed  = ((electricNew.toDoubleOrNull() ?: 0.0) - (electricOld.toDoubleOrNull() ?: 0.0)).coerceAtLeast(0.0)
    val wUsed  = ((waterNew.toDoubleOrNull()    ?: 0.0) - (waterOld.toDoubleOrNull()    ?: 0.0)).coerceAtLeast(0.0)
    val eCost  = eUsed * ePrice
    val wCost  = wUsed * wPrice
    val total  = invoice.rent_price + eCost + wCost + (otherFee.toDoubleOrNull() ?: 0.0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFF8F9FA)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF1565C0), Color(0xFF1E88E5))),
                            RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Sửa hóa đơn", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(
                                "${invoice.room_name ?: ""} — ${invoice.month.replace("-", "/")}",
                                color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }

                // Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tháng
                    InvoiceSectionCard("Kỳ thanh toán", Icons.Default.CalendarMonth) {
                        OutlinedTextField(
                            value = month, onValueChange = { month = it },
                            label = { Text("Tháng (YYYY-MM)") },
                            leadingIcon = { Icon(Icons.Default.DateRange, null, tint = Color(0xFF1E88E5)) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Điện
                    InvoiceSectionCard("Chỉ số điện", Icons.Default.ElectricBolt, iconTint = Color(0xFFFFA000)) {
                        OutlinedTextField(
                            value = electricPrice, onValueChange = { electricPrice = it },
                            label = { Text("Đơn giá điện") },
                            suffix = { Text("đ/kWh", fontSize = 12.sp, color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true, shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = electricOld, onValueChange = { electricOld = it },
                                label = { Text("Chỉ số cũ") },
                                suffix = { Text("kWh", fontSize = 12.sp, color = Color.Gray) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true, shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = electricNew, onValueChange = { electricNew = it },
                                label = { Text("Chỉ số mới") },
                                suffix = { Text("kWh", fontSize = 12.sp, color = Color.Gray) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true, shape = RoundedCornerShape(10.dp)
                            )
                        }
                        if (eUsed > 0) Text(
                            "Tiêu thụ: ${"%.0f".format(eUsed)} kWh × ${formatPrice(ePrice)} đ → ${formatPrice(eCost)} VND",
                            fontSize = 12.sp, color = Color(0xFFFFA000), modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Nước
                    InvoiceSectionCard("Chỉ số nước", Icons.Default.Water, iconTint = Color(0xFF0288D1)) {
                        OutlinedTextField(
                            value = waterPrice, onValueChange = { waterPrice = it },
                            label = { Text("Đơn giá nước") },
                            suffix = { Text("đ/m³", fontSize = 12.sp, color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true, shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = waterOld, onValueChange = { waterOld = it },
                                label = { Text("Chỉ số cũ") },
                                suffix = { Text("m³", fontSize = 12.sp, color = Color.Gray) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true, shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = waterNew, onValueChange = { waterNew = it },
                                label = { Text("Chỉ số mới") },
                                suffix = { Text("m³", fontSize = 12.sp, color = Color.Gray) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true, shape = RoundedCornerShape(10.dp)
                            )
                        }
                        if (wUsed > 0) Text(
                            "Tiêu thụ: ${"%.0f".format(wUsed)} m³ × ${formatPrice(wPrice)} đ → ${formatPrice(wCost)} VND",
                            fontSize = 12.sp, color = Color(0xFF0288D1), modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Phí khác
                    InvoiceSectionCard("Phí khác (tuỳ chọn)", Icons.Default.AddCircleOutline, iconTint = Color(0xFF7B1FA2)) {
                        OutlinedTextField(
                            value = otherFee, onValueChange = { otherFee = it },
                            label = { Text("Số tiền (VND)") },
                            leadingIcon = { Icon(Icons.Default.AttachMoney, null, tint = Color(0xFF7B1FA2)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true, shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = otherFeeNote, onValueChange = { otherFeeNote = it },
                            label = { Text("Ghi chú") },
                            leadingIcon = { Icon(Icons.Default.Notes, null, tint = Color(0xFF7B1FA2)) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Preview
                    Surface(color = Color(0xFF1565C0), shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Summarize, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Xem trước", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Divider(color = Color.White.copy(alpha = 0.3f))
                            PreviewRowWhite("Tiền phòng", formatPrice(invoice.rent_price))
                            PreviewRowWhite("Điện (${"%.0f".format(eUsed)} kWh)", formatPrice(eCost))
                            PreviewRowWhite("Nước (${"%.0f".format(wUsed)} m³)", formatPrice(wCost))
                            if ((otherFee.toDoubleOrNull() ?: 0.0) > 0)
                                PreviewRowWhite("Phí khác", formatPrice(otherFee.toDoubleOrNull() ?: 0.0))
                            Divider(color = Color.White.copy(alpha = 0.3f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("TỔNG CỘNG", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${formatPrice(total)} VND", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                    }

                    if (errorMsg.isNotBlank()) {
                        Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(errorMsg, color = Color(0xFFD32F2F), fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Footer
                Surface(color = Color.White, shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Hủy") }

                        Button(
                            onClick = {
                                val eO = electricOld.toDoubleOrNull()
                                val eN = electricNew.toDoubleOrNull()
                                val wO = waterOld.toDoubleOrNull()
                                val wN = waterNew.toDoubleOrNull()
                                if (month.isBlank()) { errorMsg = "Vui lòng nhập tháng"; return@Button }
                                if (eO == null || eN == null || wO == null || wN == null) { errorMsg = "Chỉ số không hợp lệ"; return@Button }
                                if (eN < eO) { errorMsg = "Chỉ số điện mới phải ≥ cũ"; return@Button }
                                if (wN < wO) { errorMsg = "Chỉ số nước mới phải ≥ cũ"; return@Button }
                                isLoading = true; errorMsg = ""
                                RetrofitClient.instance.updateInvoice(
                                    invoiceId    = invoice.id,
                                    landlordId   = UserSession.uid,
                                    month        = month,
                                    electricOld  = eO, electricNew = eN, electricPrice = ePrice,
                                    waterOld     = wO, waterNew    = wN, waterPrice    = wPrice,
                                    otherFee     = otherFee.toDoubleOrNull() ?: 0.0,
                                    otherFeeNote = otherFeeNote
                                ).enqueue(object : Callback<SimpleResponse> {
                                    override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                                        isLoading = false
                                        if (response.body()?.status == "success")
                                            onSuccess("Cập nhật hóa đơn thành công! Tổng: ${formatPrice(total)} VND")
                                        else
                                            errorMsg = response.body()?.message ?: "Lỗi không xác định"
                                    }
                                    override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                                        isLoading = false; errorMsg = "Lỗi kết nối"
                                    }
                                })
                            },
                            modifier = Modifier.weight(2f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            enabled = !isLoading
                        ) {
                            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            else {
                                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Lưu thay đổi", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

fun formatDateTime(raw: String): String {    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = sdf.parse(raw) ?: return raw
        SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(date)
    } catch (e: Exception) { raw }
}

/** Tính tháng tiếp theo từ chuỗi "YYYY-MM" */
fun nextMonth(month: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.time = sdf.parse(month) ?: return month
        cal.add(Calendar.MONTH, 1)
        sdf.format(cal.time)
    } catch (e: Exception) { month }
}
