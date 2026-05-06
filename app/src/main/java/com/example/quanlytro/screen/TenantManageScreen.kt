package com.example.quanlytro.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quanlytro.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantManageScreen(
    onBackClick: () -> Unit = {},
    onChatClick: (String, String) -> Unit = { _, _ -> }
) {
    var tenants by remember { mutableStateOf<List<TenantItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTenant by remember { mutableStateOf<TenantItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val snackState = remember { SnackbarHostState() }
    var snackMsg by remember { mutableStateOf<String?>(null) }

    fun load() {
        isLoading = true
        RetrofitClient.instance.getTenants(UserSession.uid)
            .enqueue(object : Callback<TenantListResponse> {
                override fun onResponse(call: Call<TenantListResponse>, response: Response<TenantListResponse>) {
                    tenants = response.body()?.tenants ?: emptyList()
                    isLoading = false
                }
                override fun onFailure(call: Call<TenantListResponse>, t: Throwable) {
                    isLoading = false
                }
            })
    }

    LaunchedEffect(Unit) { load() }

    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackState.showSnackbar(it); snackMsg = null }
    }

    selectedTenant?.let { tenant ->
        TenantDetailSheet(
            tenant    = tenant,
            onDismiss = { selectedTenant = null },
            onChat    = { onChatClick(tenant.tenant_id, tenant.tenant_name) },
            onRemind  = { msg ->
                RetrofitClient.instance.remindPayment(
                    contractId = tenant.contract_id,
                    landlordId = UserSession.uid,
                    tenantId   = tenant.tenant_id,
                    message    = msg
                ).enqueue(object : Callback<SimpleResponse> {
                    override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                        snackMsg = if (response.body()?.status == "success") "Đã gửi nhắc nhở" else "Gửi thất bại"
                    }
                    override fun onFailure(call: Call<SimpleResponse>, t: Throwable) { snackMsg = "Lỗi kết nối" }
                })
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackState) },
        topBar = {
            TopAppBar(
                title = { Text("Quản lý khách thuê", fontWeight = FontWeight.Bold) },
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
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Tìm theo tên, SĐT, phòng...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF007BFF),
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            // Summary chips
            if (tenants.isNotEmpty()) {
                val totalUnpaid = tenants.sumOf { it.unpaid_count }
                val totalPeople = tenants.sumOf { 1 + it.member_count }
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryChip("${tenants.size} phòng", Color(0xFF007BFF))
                    SummaryChip("$totalPeople người", Color(0xFF4CAF50))
                    if (totalUnpaid > 0)
                        SummaryChip("$totalUnpaid hóa đơn chưa trả", Color(0xFFE53935))
                }
                Spacer(Modifier.height(4.dp))
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF007BFF))
                }
            } else {
                val filtered = tenants.filter { t ->
                    searchQuery.isBlank() ||
                    t.tenant_name.contains(searchQuery, ignoreCase = true) ||
                    t.tenant_phone.contains(searchQuery) ||
                    (t.room_name?.contains(searchQuery, ignoreCase = true) == true)
                }

                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.People, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (searchQuery.isBlank()) "Chưa có khách thuê nào" else "Không tìm thấy kết quả",
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.contract_id }) { tenant ->
                            TenantCard(
                                tenant  = tenant,
                                onClick = { selectedTenant = tenant }
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun TenantCard(tenant: TenantItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    tenant.tenant_name.take(1).uppercase(),
                    color = Color(0xFF007BFF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(tenant.tenant_name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(tenant.tenant_phone, fontSize = 12.sp, color = Color.Gray)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Home, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                    Text(tenant.room_name ?: "-", fontSize = 12.sp, color = Color(0xFF0D1B34))
                    val totalPeople = 1 + tenant.member_count
                    if (totalPeople > 1) {
                        Text("·", fontSize = 12.sp, color = Color.Gray)
                        Icon(Icons.Default.People, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                        Text("$totalPeople người", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (tenant.unpaid_count > 0) {
                    Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(8.dp)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFE53935), modifier = Modifier.size(11.dp))
                            Text("${tenant.unpaid_count} chưa trả", color = Color(0xFFE53935), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp)) {
                        Text("Đã trả", color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                    }
                }
                Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TenantDetailSheet(
    tenant: TenantItem,
    onDismiss: () -> Unit,
    onChat: () -> Unit,
    onRemind: (String) -> Unit
) {
    val context = LocalContext.current
    var showInvoices by remember { mutableStateOf(false) }
    var invoices by remember { mutableStateOf<List<InvoiceItem>>(emptyList()) }
    var loadingInvoices by remember { mutableStateOf(false) }
    var showRemindDialog by remember { mutableStateOf(false) }
    var remindMsg by remember { mutableStateOf("") }

    // ── Room members state ──
    var members by remember { mutableStateOf<List<RoomMember>>(emptyList()) }
    var loadingMembers by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var memberName by remember { mutableStateOf("") }
    var memberPhone by remember { mutableStateOf("") }
    var memberIdCard by remember { mutableStateOf("") }
    var memberDob by remember { mutableStateOf("") }
    var memberNote by remember { mutableStateOf("") }
    var addingMember by remember { mutableStateOf(false) }
    var showDobPicker by remember { mutableStateOf(false) }
    val dobPickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    fun loadMembers() {
        loadingMembers = true
        RetrofitClient.instance.getRoomMembers(tenant.contract_id)
            .enqueue(object : Callback<RoomMemberListResponse> {
                override fun onResponse(call: Call<RoomMemberListResponse>, response: Response<RoomMemberListResponse>) {
                    members = response.body()?.members ?: emptyList()
                    loadingMembers = false
                }
                override fun onFailure(call: Call<RoomMemberListResponse>, t: Throwable) { loadingMembers = false }
            })
    }

    fun deleteMember(memberId: Int) {
        RetrofitClient.instance.deleteRoomMember(memberId, UserSession.uid)
            .enqueue(object : Callback<SimpleResponse> {
                override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) { loadMembers() }
                override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {}
            })
    }

    fun addMember() {
        if (memberName.isBlank()) return
        addingMember = true
        RetrofitClient.instance.addRoomMember(
            contractId = tenant.contract_id,
            landlordId = UserSession.uid,
            fullName   = memberName,
            phone      = memberPhone,
            idCard     = memberIdCard,
            dob        = memberDob,
            note       = memberNote
        ).enqueue(object : Callback<SimpleResponse> {
            override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                addingMember = false
                showAddMemberDialog = false
                memberName = ""; memberPhone = ""; memberIdCard = ""; memberDob = ""; memberNote = ""
                loadMembers()
            }
            override fun onFailure(call: Call<SimpleResponse>, t: Throwable) { addingMember = false }
        })
    }

    LaunchedEffect(tenant.contract_id) { loadMembers() }

    fun loadInvoices() {
        loadingInvoices = true
        RetrofitClient.instance.getInvoicesByContract(UserSession.uid, tenant.contract_id)
            .enqueue(object : Callback<InvoiceListResponse> {
                override fun onResponse(call: Call<InvoiceListResponse>, response: Response<InvoiceListResponse>) {
                    invoices = response.body()?.invoices ?: emptyList()
                    loadingInvoices = false
                }
                override fun onFailure(call: Call<InvoiceListResponse>, t: Throwable) { loadingInvoices = false }
            })
    }

    // BottomSheet thêm người ở cùng
    if (showAddMemberDialog) {
        ModalBottomSheet(
            onDismissRequest = { showAddMemberDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PersonAdd, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text("Thêm người ở cùng", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(tenant.room_name ?: "", fontSize = 13.sp, color = Color.Gray)
                    }
                }

                HorizontalDivider(color = Color(0xFFF0F0F0))

                // Fields
                MemberField("Họ tên *", memberName, { memberName = it })
                MemberField("Số điện thoại", memberPhone, { memberPhone = it }, keyboardType = KeyboardType.Phone)
                MemberField("CCCD/CMND", memberIdCard, { memberIdCard = it }, keyboardType = KeyboardType.Number)

                // Ngày sinh với DatePicker
                if (showDobPicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDobPicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                dobPickerState.selectedDateMillis?.let { millis ->
                                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
                                    memberDob = "%04d-%02d-%02d".format(
                                        cal.get(java.util.Calendar.YEAR),
                                        cal.get(java.util.Calendar.MONTH) + 1,
                                        cal.get(java.util.Calendar.DAY_OF_MONTH)
                                    )
                                }
                                showDobPicker = false
                            }) { Text("Chọn") }
                        },
                        dismissButton = { TextButton(onClick = { showDobPicker = false }) { Text("Hủy") } }
                    ) { DatePicker(state = dobPickerState) }
                }

                OutlinedTextField(
                    value = memberDob,
                    onValueChange = { memberDob = it },
                    label = { Text("Ngày sinh", fontSize = 13.sp) },
                    placeholder = { Text("VD: 1995-03-20", fontSize = 13.sp, color = Color.LightGray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showDobPicker = true }) {
                            Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF007BFF))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007BFF),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )

                MemberField("Ghi chú", memberNote, { memberNote = it })

                Button(
                    onClick = { addMember() },
                    enabled = memberName.isNotBlank() && !addingMember,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF))
                ) {
                    if (addingMember) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Thêm người ở cùng", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }

    if (showRemindDialog) {
        AlertDialog(
            onDismissRequest = { showRemindDialog = false },
            title = { Text("Nhắc thanh toán") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Gửi thông báo nhắc nhở đến ${tenant.tenant_name}", fontSize = 13.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = remindMsg,
                        onValueChange = { remindMsg = it },
                        placeholder = { Text("Nội dung nhắc (để trống = mặc định)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onRemind(remindMsg)
                    showRemindDialog = false
                    remindMsg = ""
                }) { Text("Gửi") }
            },
            dismissButton = {
                TextButton(onClick = { showRemindDialog = false }) { Text("Hủy") }
            }
        )
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFE3F2FD)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(tenant.tenant_name.take(1).uppercase(), color = Color(0xFF007BFF), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        }
                        Column {
                            Text(tenant.tenant_name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(tenant.room_name ?: "-", fontSize = 13.sp, color = Color.Gray)
                                Surface(color = Color(0xFF007BFF).copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
                                    Text("Đại diện", fontSize = 11.sp, color = Color(0xFF007BFF), fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color.Gray)
                    }
                }

                HorizontalDivider()

                // Thông tin cá nhân
                TenantDetailSection("Thông tin cá nhân") {
                    TenantDetailRow("Họ tên", tenant.tenant_name)
                    TenantDetailRow("SĐT", tenant.tenant_phone)
                    if (!tenant.tenant_id_card.isNullOrBlank()) TenantDetailRow("CCCD/CMND", tenant.tenant_id_card)
                    if (!tenant.dob.isNullOrBlank()) TenantDetailRow("Ngày sinh", tenant.dob)
                    if (!tenant.email.isNullOrBlank()) TenantDetailRow("Email", tenant.email)
                    if (!tenant.tenant_address.isNullOrBlank()) TenantDetailRow("Địa chỉ", tenant.tenant_address)
                }

                // Thông tin thuê
                TenantDetailSection("Thông tin thuê") {
                    TenantDetailRow("Phòng", tenant.room_name ?: "-")
                    if (!tenant.room_address.isNullOrBlank()) TenantDetailRow("Địa chỉ phòng", tenant.room_address)
                    TenantDetailRow("Ngày bắt đầu", tenant.start_date)
                    TenantDetailRow("Thời hạn", "${tenant.duration_months} tháng")
                    TenantDetailRow("Giá thuê", "${formatPrice(tenant.rent_price)}đ/tháng")
                }

                // ── Người ở cùng ──────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Người ở cùng", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF007BFF))
                            if (members.isNotEmpty()) {
                                Surface(color = Color(0xFF007BFF).copy(alpha = 0.1f), shape = CircleShape) {
                                    Text("${members.size}", fontSize = 11.sp, color = Color(0xFF007BFF), fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
                                }
                            }
                        }
                        TextButton(
                            onClick = { showAddMemberDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Thêm", fontSize = 13.sp)
                        }
                    }

                    if (loadingMembers) {
                        Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF007BFF), strokeWidth = 2.dp)
                        }
                    } else if (members.isEmpty()) {
                        Surface(color = Color(0xFFF5F5F5), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Text("Chưa có người ở cùng", color = Color.Gray, fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp))
                        }
                    } else {
                        members.forEach { member ->
                            RoomMemberRow(member = member, onDelete = { deleteMember(member.id) })
                        }
                    }
                }

                // Thanh toán tóm tắt
                TenantDetailSection("Thanh toán") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentSummaryBox("Đã trả", tenant.paid_count, Color(0xFF4CAF50), Modifier.weight(1f))
                        PaymentSummaryBox("Chưa trả", tenant.unpaid_count, Color(0xFFE53935), Modifier.weight(1f))
                    }
                    TextButton(
                        onClick = {
                            showInvoices = !showInvoices
                            if (showInvoices && invoices.isEmpty()) loadInvoices()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (showInvoices) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null, modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (showInvoices) "Ẩn hóa đơn" else "Xem chi tiết hóa đơn")
                    }

                    if (showInvoices) {
                        if (loadingInvoices) {
                            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF007BFF))
                            }
                        } else if (invoices.isEmpty()) {
                            Text("Chưa có hóa đơn nào", color = Color.Gray, fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 4.dp))
                        } else {
                            invoices.forEach { inv -> InvoiceRow(inv) }
                        }
                    }
                }

                HorizontalDivider()

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${tenant.tenant_phone}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Phone, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Gọi", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = onChat,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF007BFF))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Message, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Nhắn tin", fontSize = 13.sp)
                    }
                }

                if (tenant.unpaid_count > 0) {
                    Button(
                        onClick = { showRemindDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                    ) {
                        Icon(Icons.Default.NotificationsActive, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Nhắc thanh toán (${tenant.unpaid_count} tháng)", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentSummaryBox(label: String, count: Int, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$count", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 12.sp, color = color)
        }
    }
}

@Composable
private fun InvoiceRow(invoice: InvoiceItem) {
    val isPaid = invoice.status == "paid"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                if (isPaid) Icons.Default.CheckCircle else Icons.Default.Cancel,
                null,
                tint = if (isPaid) Color(0xFF4CAF50) else Color(0xFFE53935),
                modifier = Modifier.size(16.dp)
            )
            Text("Tháng ${invoice.month}", fontSize = 13.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${formatPrice(invoice.total)}đ", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Surface(
                color = if (isPaid) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    if (isPaid) "Đã trả" else "Chưa trả",
                    color = if (isPaid) Color(0xFF4CAF50) else Color(0xFFE53935),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun TenantDetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF007BFF))
        content()
    }
}

@Composable
private fun TenantDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.weight(0.4f))
        Text(value, fontSize = 13.sp, color = Color(0xFF0D1B34), modifier = Modifier.weight(0.6f))
    }
}

@Composable
private fun RoomMemberRow(member: RoomMember, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Xóa người ở cùng?") },
            text = { Text("${member.full_name} sẽ bị xóa khỏi danh sách.") },
            confirmButton = {
                TextButton(onClick = { showConfirm = false; onDelete() }) {
                    Text("Xóa", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Hủy") } }
        )
    }

    Surface(
        color = Color(0xFFF8F9FA),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                Text(member.full_name.take(1).uppercase(), color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(member.full_name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                if (member.phone.isNotBlank()) Text(member.phone, fontSize = 12.sp, color = Color.Gray)
                if (member.id_card.isNotBlank()) Text("CCCD: ${member.id_card}", fontSize = 12.sp, color = Color.Gray)
                if (member.note.isNotBlank()) Text(member.note, fontSize = 12.sp, color = Color(0xFF795548))
            }
            IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.DeleteOutline, null, tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun MemberField(
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
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF007BFF),
            unfocusedBorderColor = Color(0xFFE0E0E0)
        )
    )
}
