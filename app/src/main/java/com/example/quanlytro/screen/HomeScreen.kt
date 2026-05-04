package com.example.quanlytro

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    userRole: String = "Người thuê",
    refreshKey: Int = 0,
    onProfileClick: () -> Unit = {},
    onManageClick: () -> Unit = {},
    onPostClick: () -> Unit = {},
    onEditPostClick: (PostResponse) -> Unit = {},
    onPostDetailClick: (PostResponse) -> Unit = {},
    onChatListClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var allPosts by remember { mutableStateOf<List<PostResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // State cho tìm kiếm và lọc
    var searchQuery by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }
    
    // Tiêu chí lọc
    var minPrice by remember { mutableStateOf(0f) }
    var maxPrice by remember { mutableStateOf(20f) }
    var minArea by remember { mutableStateOf(0f) }
    var maxArea by remember { mutableStateOf(100f) }
    var selectedProvince by remember { mutableStateOf("Tất cả") }

    var postToDelete by remember { mutableStateOf<PostResponse?>(null) }

    fun loadPosts() {
        isLoading = true
        RetrofitClient.instance.getPosts().enqueue(object : Callback<List<PostResponse>> {
            override fun onResponse(call: Call<List<PostResponse>>, response: Response<List<PostResponse>>) {
                isLoading = false
                if (response.isSuccessful) {
                    allPosts = response.body() ?: emptyList()
                }
            }
            override fun onFailure(call: Call<List<PostResponse>>, t: Throwable) {
                isLoading = false
            }
        })
    }

    LaunchedEffect(refreshKey) {
        loadPosts()
    }

    // Logic lọc bài viết
    val filteredPosts = allPosts.filter { post ->
        val matchesSearch = post.title.contains(searchQuery, ignoreCase = true) || 
                           post.location.contains(searchQuery, ignoreCase = true)

        val priceInMillions = post.price / 1_000_000
        val matchesPrice = priceInMillions >= minPrice && priceInMillions <= maxPrice

        val matchesArea = post.area >= minArea && post.area <= maxArea

        val matchesProvince = if (selectedProvince == "Tất cả") true
                              else post.location.contains(selectedProvince, ignoreCase = true)

        matchesSearch && matchesPrice && matchesArea && matchesProvince
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Bộ lọc nâng cao", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = {
                        minPrice = 0f
                        maxPrice = 20f
                        minArea = 0f
                        maxArea = 100f
                        selectedProvince = "Tất cả"
                    }) { Text("Đặt lại", color = Color.Red) }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Khoảng giá (Triệu VNĐ)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                RangeSlider(
                    value = minPrice..maxPrice,
                    onValueChange = { minPrice = it.start; maxPrice = it.endInclusive },
                    valueRange = 0f..20f,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${minPrice.toInt()} triệu", fontSize = 13.sp, color = Color.Gray)
                    Text("${maxPrice.toInt()} triệu+", fontSize = 13.sp, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Diện tích (m²)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                RangeSlider(
                    value = minArea..maxArea,
                    onValueChange = { minArea = it.start; maxArea = it.endInclusive },
                    valueRange = 0f..100f,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${minArea.toInt()} m²", fontSize = 13.sp, color = Color.Gray)
                    Text("${maxArea.toInt()} m²+", fontSize = 13.sp, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Tỉnh / Thành phố", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(12.dp))

                var provinceExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = provinceExpanded,
                    onExpandedChange = { provinceExpanded = !provinceExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedProvince,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = provinceExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF1F5F9),
                            focusedContainerColor = Color(0xFFF1F5F9),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color(0xFF007BFF)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = provinceExpanded,
                        onDismissRequest = { provinceExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Tất cả") },
                            onClick = {
                                selectedProvince = "Tất cả"
                                provinceExpanded = false
                            }
                        )
                        vietNamData.keys.sorted().forEach { province ->
                            DropdownMenuItem(
                                text = { Text(province) },
                                onClick = {
                                    selectedProvince = province
                                    provinceExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF))
                ) {
                    Text("Áp dụng", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (postToDelete != null) {
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            title = { Text("Xác nhận xóa") },
            text = { Text("Bạn có chắc chắn muốn xóa bài đăng '${postToDelete?.title}' không?") },
            confirmButton = {
                TextButton(onClick = {
                    postToDelete?.let { post ->
                        RetrofitClient.instance.deletePost(post.id).enqueue(object : Callback<String> {
                            override fun onResponse(call: Call<String>, response: Response<String>) {
                                if (response.body() == "Thành công") {
                                    Toast.makeText(context, "Xóa bài thành công!", Toast.LENGTH_SHORT).show()
                                    loadPosts()
                                }
                            }
                            override fun onFailure(call: Call<String>, t: Throwable) {}
                        })
                    }
                    postToDelete = null
                }) { Text("Xóa", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { postToDelete = null }) { Text("Hủy") }
            }
        )
    }

    Scaffold(
        bottomBar = {
            BottomNavBar(
                initialSelected = 0,
                userRole = userRole,
                onProfileClick = onProfileClick,
                onManageClick = onManageClick,
                onChatListClick = onChatListClick
            )
        },
        floatingActionButton = {
            if (userRole == "Chủ trọ") {
                ExtendedFloatingActionButton(
                    onClick = onPostClick,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Đăng bài") },
                    containerColor = Color(0xFF007BFF),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF007BFF))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).background(Color(0xFFF8F9FA))
            ) {
                item { HomeHeader(onNotificationClick = onNotificationClick) }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            SearchField(value = searchQuery, onValueChange = { searchQuery = it })
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        val isFiltering = selectedProvince != "Tất cả" || minPrice > 0f || maxPrice < 20f || minArea > 0f || maxArea < 100f
                        Surface(
                            onClick = { showFilterSheet = true },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isFiltering) Color(0xFF007BFF) else Color.White,
                            contentColor = if (isFiltering) Color.White else Color.Gray,
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.FilterList, null)
                                if (isFiltering) {
                                    Box(modifier = Modifier.size(8.dp).background(Color.Red, CircleShape).align(Alignment.TopEnd).padding(4.dp))
                                }
                            }
                        }
                    }
                }

                if (filteredPosts.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Không có phòng nào phù hợp", color = Color.Gray, fontSize = 16.sp)
                                TextButton(onClick = {
                                    searchQuery = ""
                                    minPrice = 0f; maxPrice = 20f
                                    minArea = 0f; maxArea = 100f
                                    selectedProvince = "Tất cả"
                                }) {
                                    Text("Xem tất cả bài đăng")
                                }
                            }
                        }
                    }
                } else {
                    items(filteredPosts) { post ->
                        RecentCard(
                            post = post,
                            userRole = userRole,
                            onDelete = { postToDelete = it },
                            onEdit = { onEditPostClick(post) },
                            onPostClick = { onPostDetailClick(post) }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

// Giữ nguyên các Component phụ trợ bên dưới (HomeHeader, SearchField, RecentCard, vv.)

@Composable
fun HomeHeader(onNotificationClick: () -> Unit = {}) {
    val displayName = UserSession.fullName.ifBlank { "Người dùng" }
    var unreadCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        RetrofitClient.instance.countNotifications(UserSession.uid)
            .enqueue(object : retrofit2.Callback<NotificationCountResponse> {
                override fun onResponse(call: retrofit2.Call<NotificationCountResponse>, response: retrofit2.Response<NotificationCountResponse>) {
                    unreadCount = response.body()?.count ?: 0
                }
                override fun onFailure(call: retrofit2.Call<NotificationCountResponse>, t: Throwable) {}
            })
    }

    // Decode avatar base64
    val avatarBitmap = remember(UserSession.avatar) {
        val av = UserSession.avatar
        if (av.isBlank()) null
        else try {
            val bytes = android.util.Base64.decode(
                if (av.contains(",")) av.substringAfter(",") else av,
                android.util.Base64.DEFAULT
            )
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) { null }
    }

    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        // Avatar
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFE3F2FD)),
            contentAlignment = Alignment.Center
        ) {
            if (avatarBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = avatarBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Icon(Icons.Default.Person, null, tint = Color(0xFF1976D2), modifier = Modifier.size(32.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Xin chào,", fontSize = 14.sp, color = Color.Gray)
            Text(text = displayName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B34))
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable { onNotificationClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Notifications, null, modifier = Modifier.size(26.dp))
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth().height(56.dp),
        placeholder = { Text("Tìm tên phòng, địa chỉ...", color = Color.Gray) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White, unfocusedBorderColor = Color.Transparent, focusedBorderColor = Color(0xFF007BFF)),
        singleLine = true
    )
}

@Composable
fun FeaturedSection(posts: List<PostResponse>, userRole: String, onDelete: (PostResponse) -> Unit, onEdit: (PostResponse) -> Unit, onPostClick: (PostResponse) -> Unit) {
    if (posts.isEmpty()) return
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Tin nổi bật", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        LazyRow(contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(posts) { post ->
                FeaturedCard(post, userRole, onDelete, onEdit, onPostClick)
            }
        }
    }
}

@Composable
fun FeaturedCard(post: PostResponse, userRole: String, onDelete: (PostResponse) -> Unit, onEdit: (PostResponse) -> Unit, onPostClick: (PostResponse) -> Unit) {
    Card(
        modifier = Modifier.width(280.dp).clickable { onPostClick(post) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.height(200.dp)) {
            if (!post.images.isNullOrEmpty()) {
                Base64Image(post.images[0], modifier = Modifier.fillMaxSize())
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE3F2FD)))
            }
            // Gradient overlay phía dưới
            Box(
                modifier = Modifier.fillMaxSize().background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                        startY = 80f
                    )
                )
            )
            // Badge nổi bật
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFF6B35)
            ) {
                Text("⭐ Nổi bật", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
            // Action buttons cho chủ trọ
            if (userRole == "Chủ trọ") {
                Row(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallActionButton(icon = Icons.Default.Edit, tint = Color.White, onClick = { onEdit(post) })
                    SmallActionButton(icon = Icons.Default.Delete, tint = Color(0xFFFF6B6B), onClick = { onDelete(post) })
                }
            }
            // Thông tin ở dưới ảnh
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Text(text = post.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, Modifier.size(13.dp), Color.White.copy(alpha = 0.8f))
                    Text(text = post.location, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f), maxLines = 1)
                }
                Spacer(Modifier.height(4.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF007BFF)) {
                    Text(text = "${formatPrice(post.price)}đ/tháng", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RecentCard(post: PostResponse, userRole: String, onDelete: (PostResponse) -> Unit, onEdit: () -> Unit, onPostClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable { onPostClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            // Ảnh
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                if (!post.images.isNullOrEmpty()) {
                    Base64Image(post.images[0], modifier = Modifier.fillMaxSize())
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE3F2FD)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Home, null, tint = Color(0xFF90CAF9), modifier = Modifier.size(64.dp))
                    }
                }
                // Badge diện tích
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    Text("${post.area.toInt()} m²", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
                if (userRole == "Chủ trọ") {
                    Row(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SmallActionButton(icon = Icons.Default.Edit, tint = Color(0xFF007BFF), onClick = onEdit)
                        SmallActionButton(icon = Icons.Default.Delete, tint = Color.Red, onClick = { onDelete(post) })
                    }
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                // Tiêu đề
                Text(text = post.title, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, color = Color(0xFF0D1B34))
                Spacer(modifier = Modifier.height(6.dp))

                // Địa chỉ
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, Modifier.size(15.dp), Color(0xFF007BFF))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = post.location, fontSize = 13.sp, color = Color.Gray, maxLines = 1)
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Giá + diện tích
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "${formatPrice(post.price)}đ", color = Color(0xFF007BFF), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text(text = "/tháng", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    val isAvail = (post.available ?: 1) == 1
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isAvail) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(if (isAvail) Color(0xFF4CAF50) else Color(0xFFE53935)))
                            Text(
                                text = if (isAvail) "Còn ${post.available_rooms ?: 1} phòng" else "Hết phòng",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAvail) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }
                }

                // Divider
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(10.dp))

                // Thông tin liên hệ
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, Modifier.size(15.dp), Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = post.contact_name ?: "Chủ nhà", fontSize = 13.sp, color = Color(0xFF0D1B34), fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    if (!post.contact_phone.isNullOrEmpty()) {
                        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFE3F2FD)) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, null, Modifier.size(13.dp), Color(0xFF007BFF))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = post.contact_phone, fontSize = 12.sp, color = Color(0xFF007BFF), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmallActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = tint.copy(alpha = 0.1f),
        modifier = Modifier.size(28.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun BottomNavBar(
    initialSelected: Int = 0,
    userRole: String = "Người thuê",
    onExploreClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onManageClick: () -> Unit = {},
    onChatListClick: () -> Unit = {}
) {
    var selectedItem by remember { mutableStateOf(initialSelected) }
    val menuItems = mutableListOf("Trang chủ", "Tin nhắn").apply { if (userRole == "Chủ trọ") add("Quản lý"); add("Hồ sơ") }
    val menuIcons = mutableListOf(Icons.Default.Home, Icons.Default.Chat).apply { if (userRole == "Chủ trọ") add(Icons.Default.AdminPanelSettings); add(Icons.Default.Person) }
    NavigationBar(containerColor = Color.White) {
        menuItems.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(menuIcons[index], item) },
                label = { Text(item, fontSize = 10.sp) },
                selected = selectedItem == index,
                onClick = {
                    selectedItem = index
                    when (item) {
                        "Trang chủ" -> onExploreClick()
                        "Tin nhắn" -> onChatListClick()
                        "Quản lý" -> onManageClick()
                        "Hồ sơ" -> onProfileClick()
                    }
                }
            )
        }
    }
}
