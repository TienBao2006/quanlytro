package package com.example.quanlytro.features.landlord
import com.example.quanlytro.data.remote.*
import com.example.quanlytro.data.model.*

import com.example.quanlytro.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandlordNoticeScreen(onBackClick: () -> Unit = {}) {
    var posts by remember { mutableStateOf<List<PostResponse>>(emptyList()) }
    var notices by remember { mutableStateOf<List<LandlordNoticeItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }
    var snackMsg by remember { mutableStateOf<String?>(null) }
    val snackState = remember { SnackbarHostState() }

    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackState.showSnackbar(it); snackMsg = null }
    }

    fun loadData() {
        isLoading = true
        // Load posts của chủ trọ
        RetrofitClient.instance.getPosts().enqueue(object : Callback<List<PostResponse>> {
            override fun onResponse(call: Call<List<PostResponse>>, response: Response<List<PostResponse>>) {
                posts = (response.body() ?: emptyList()).filter { it.user_id == UserSession.uid }
            }
            override fun onFailure(call: Call<List<PostResponse>>, t: Throwable) {}
        })
        // Load lịch sử thông báo đã gửi
        RetrofitClient.instance.getLandlordNotices(UserSession.uid)
            .enqueue(object : Callback<LandlordNoticeListResponse> {
                override fun onResponse(call: Call<LandlordNoticeListResponse>, response: Response<LandlordNoticeListResponse>) {
                    notices = response.body()?.notices ?: emptyList()
                    isLoading = false
                }
                override fun onFailure(call: Call<LandlordNoticeListResponse>, t: Throwable) { isLoading = false }
            })
    }

    LaunchedEffect(Unit) { loadData() }

    if (showDialog) {
        SendNoticeDialog(
            posts = posts,
            onDismiss = { showDialog = false },
            onSent = { msg ->
                snackMsg = msg
                showDialog = false
                loadData()
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackState) },
        topBar = {
            TopAppBar(
                title = { Text("Thông báo nhà trọ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(Icons.Default.Add, null, tint = Color(0xFF007BFF))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = Color(0xFF007BFF),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Campaign, null, modifier = Modifier.size(20.dp))
                    Text("Gửi thông báo", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF007BFF))
            }
        } else if (notices.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(innerPadding).background(Color(0xFFF8F9FA)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Campaign, null, modifier = Modifier.size(72.dp), tint = Color.LightGray)
                    Text("Chưa có thông báo nào", color = Color.Gray, fontSize = 15.sp)
                    Text("Nhấn nút bên dưới để gửi thông báo\nđến tất cả người thuê",
                        color = Color.LightGray, fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).background(Color(0xFFF8F9FA)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Lịch sử thông báo đã gửi", fontWeight = FontWeight.Bold,
                        fontSize = 15.sp, color = Color(0xFF0D1B34))
                }
                items(notices, key = { it.id }) { notice ->
                    NoticeHistoryCard(notice)
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun NoticeHistoryCard(notice: LandlordNoticeItem) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFE3F2FD), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Campaign, null, tint = Color(0xFF007BFF), modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text(notice.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0D1B34))
                        Text(notice.post_title ?: "Nhà trọ", fontSize = 12.sp, color = Color(0xFF007BFF))
                    }
                }
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.People, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(12.dp))
                        Text("${notice.tenant_count} người", fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    }
                }
            }
            Text(notice.message, fontSize = 13.sp, color = Color(0xFF555555), lineHeight = 19.sp)
            Text(formatDateTime(notice.created_at), fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendNoticeDialog(
    posts: List<PostResponse>,
    onDismiss: () -> Unit,
    onSent: (String) -> Unit
) {
    var selectedPost by remember { mutableStateOf<PostResponse?>(posts.firstOrNull()) }
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }

    // Gợi ý tiêu đề nhanh
    val quickTitles = listOf("Thông báo sửa chữa", "Thông báo mất điện", "Thông báo mất nước",
        "Thông báo tăng giá", "Thông báo nội quy", "Thông báo khác")

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Campaign, null, tint = Color(0xFF007BFF), modifier = Modifier.size(26.dp))
                    Text("Gửi thông báo đến người thuê", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }

                HorizontalDivider()

                // Chọn nhà trọ
                if (posts.size > 1) {
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = selectedPost?.title ?: "Chọn nhà trọ",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Nhà trọ") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            posts.forEach { post ->
                                DropdownMenuItem(
                                    text = { Text(post.title, fontSize = 14.sp) },
                                    onClick = { selectedPost = post; expanded = false }
                                )
                            }
                        }
                    }
                } else if (posts.size == 1) {
                    Surface(color = Color(0xFFF0F4FF), shape = RoundedCornerShape(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Home, null, tint = Color(0xFF007BFF), modifier = Modifier.size(16.dp))
                            Text(posts[0].title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0D1B34))
                        }
                    }
                } else {
                    Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800), modifier = Modifier.size(16.dp))
                            Text("Bạn chưa có nhà trọ nào", fontSize = 13.sp, color = Color(0xFFFF9800))
                        }
                    }
                }

                // Gợi ý tiêu đề nhanh
                Text("Chọn nhanh:", fontSize = 12.sp, color = Color.Gray)
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(quickTitles) { qt ->
                        FilterChip(
                            selected = title == qt,
                            onClick = { title = qt },
                            label = { Text(qt, fontSize = 11.sp) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }

                // Tiêu đề
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tiêu đề thông báo") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Title, null, modifier = Modifier.size(18.dp)) }
                )

                // Nội dung
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Nội dung thông báo") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 5
                )

                errorMsg?.let {
                    Text(it, color = Color.Red, fontSize = 12.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Hủy") }

                    Button(
                        onClick = {
                            val post = selectedPost ?: posts.firstOrNull()
                            if (post == null) { errorMsg = "Vui lòng chọn nhà trọ"; return@Button }
                            if (title.isBlank()) { errorMsg = "Vui lòng nhập tiêu đề"; return@Button }
                            if (message.isBlank()) { errorMsg = "Vui lòng nhập nội dung"; return@Button }
                            isSending = true
                            errorMsg = null
                            RetrofitClient.instance.sendLandlordNotice(
                                UserSession.uid, post.id, title.trim(), message.trim()
                            ).enqueue(object : Callback<SendNoticeResponse> {
                                override fun onResponse(call: Call<SendNoticeResponse>, response: Response<SendNoticeResponse>) {
                                    isSending = false
                                    val body = response.body()
                                    if (body?.status == "success") {
                                        onSent("Đã gửi đến ${body.sent_to ?: 0} người thuê")
                                    } else {
                                        errorMsg = body?.message ?: "Gửi thất bại"
                                    }
                                }
                                override fun onFailure(call: Call<SendNoticeResponse>, t: Throwable) {
                                    isSending = false
                                    errorMsg = "Lỗi kết nối: ${t.message}"
                                }
                            })
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF)),
                        enabled = !isSending && posts.isNotEmpty()
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Send, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Gửi", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
