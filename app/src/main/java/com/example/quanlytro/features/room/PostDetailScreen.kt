package package com.example.quanlytro.features.room
import com.example.quanlytro.data.remote.*
import com.example.quanlytro.data.model.*

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postJson: String,
    onBackClick: () -> Unit,
    onChatClick: (String, String) -> Unit = { _, _ -> },
    onBookingClick: (Int, String, Int) -> Unit = { _, _, _ -> }
) {
    val decodedJson = try {
        URLDecoder.decode(postJson, StandardCharsets.UTF_8.toString())
    } catch (e: Exception) { postJson }

    val post = Gson().fromJson(decodedJson, PostResponse::class.java)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedImageIndex by remember { mutableIntStateOf(0) }
    var resolvedUserId by remember { mutableStateOf("") }
    var resolvedUserName by remember { mutableStateOf(post.contact_name ?: "Chủ trọ") }
    var bookingStatus by remember { mutableStateOf("none") } // none | pending | confirmed | rejected
    var showCancelDialog by remember { mutableStateOf(false) }

    // Fetch lại post mới nhất để có available_rooms chính xác
    var freshPost by remember { mutableStateOf(post) }
    LaunchedEffect(post.id) {
        RetrofitClient.instance.getPostById(post.id).enqueue(object : retrofit2.Callback<List<PostResponse>> {
            override fun onResponse(call: retrofit2.Call<List<PostResponse>>, response: retrofit2.Response<List<PostResponse>>) {
                response.body()?.firstOrNull()?.let { freshPost = it }
            }
            override fun onFailure(call: retrofit2.Call<List<PostResponse>>, t: Throwable) {}
        })
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Hủy đặt phòng?") },
            text = { Text("Yêu cầu đặt phòng của bạn sẽ bị hủy.") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    RetrofitClient.instance.cancelBooking(post.id, UserSession.uid)
                        .enqueue(object : retrofit2.Callback<SimpleResponse> {
                            override fun onResponse(call: retrofit2.Call<SimpleResponse>, response: retrofit2.Response<SimpleResponse>) {
                                if (response.body()?.status == "success") bookingStatus = "none"
                            }
                            override fun onFailure(call: retrofit2.Call<SimpleResponse>, t: Throwable) {}
                        })
                }) { Text("Hủy đặt phòng", color = Color.Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Không") }
            }
        )
    }

    // Check trạng thái booking của user hiện tại với bài này
    LaunchedEffect(post.id) {
        if (UserSession.uid.isNotEmpty() && UserSession.role != "Chủ trọ") {
            RetrofitClient.instance.checkBooking(post.id, UserSession.uid)
                .enqueue(object : retrofit2.Callback<BookingStatusResponse> {
                    override fun onResponse(call: retrofit2.Call<BookingStatusResponse>, response: retrofit2.Response<BookingStatusResponse>) {
                        bookingStatus = response.body()?.status ?: "none"
                    }
                    override fun onFailure(call: retrofit2.Call<BookingStatusResponse>, t: Throwable) {}
                })
        }
    }

    // Luôn lookup uid và tên thực từ phone để đảm bảo dùng MySQL uid
    LaunchedEffect(post.contact_phone) {
        if (!post.contact_phone.isNullOrEmpty()) {
            RetrofitClient.instance.getUserByPhone(post.contact_phone)
                .enqueue(object : retrofit2.Callback<UserLookupResponse> {
                    override fun onResponse(call: retrofit2.Call<UserLookupResponse>, response: retrofit2.Response<UserLookupResponse>) {
                        val body = response.body()
                        if (!body?.uid.isNullOrEmpty()) {
                            resolvedUserId = body!!.uid!!
                            resolvedUserName = body.fullName ?: post.contact_name ?: "Chủ trọ"
                        }
                    }
                    override fun onFailure(call: retrofit2.Call<UserLookupResponse>, t: Throwable) {}
                })
        }
    }

    val displayLocation = post.location.replace("+", " ")

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osm_pref", 0))
        Configuration.getInstance().userAgentValue = "QuanLyTroApp/1.0"
    }

    var geoPoint by remember { mutableStateOf<GeoPoint?>(null) }

    LaunchedEffect(displayLocation) {
        scope.launch(Dispatchers.IO) {
            try {
                if (post.map_url?.contains(",") == true && post.map_url.split(",").size == 2) {
                    val parts = post.map_url.split(",")
                    val lat = parts[0].toDoubleOrNull()
                    val lon = parts[1].toDoubleOrNull()
                    if (lat != null && lon != null) {
                        withContext(Dispatchers.Main) { geoPoint = GeoPoint(lat, lon) }
                        return@launch
                    }
                }

                val searchUrl = "https://nominatim.openstreetmap.org/search?format=json&q=${displayLocation.replace(" ", "+")}"
                val url = URL(searchUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", "QuanLyTroApp/1.0")
                val response = connection.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)
                if (jsonArray.length() > 0) {
                    val first = jsonArray.getJSONObject(0)
                    withContext(Dispatchers.Main) { geoPoint = GeoPoint(first.getDouble("lat"), first.getDouble("lon")) }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết phòng", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = { /* Share */ }) { Icon(Icons.Default.Share, null, tint = Color(0xFF007BFF)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp, color = Color.White) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val phoneNumber = post.contact_phone?.trim() ?: ""
                            if (phoneNumber.isNotEmpty()) {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF007BFF))
                    ) {
                        Icon(Icons.Default.Phone, null, tint = Color(0xFF007BFF))
                        Spacer(Modifier.width(8.dp))
                        Text("Gọi điện", color = Color(0xFF007BFF))
                    }

                    val canChat = resolvedUserId.isNotEmpty() && resolvedUserId != UserSession.uid
                    Button(
                        onClick = {
                            if (canChat) onChatClick(resolvedUserId, resolvedUserName)
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = canChat,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF007BFF),
                            disabledContainerColor = Color(0xFFBDBDBD)
                        )
                    ) {
                        Icon(Icons.Default.Chat, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Nhắn tin", fontWeight = FontWeight.Bold)
                    }

                    // Nút Đặt phòng (chỉ hiện cho người thuê, khi phòng còn trống)
                    val isAvailableToBook = (freshPost.available ?: 1) == 1 && UserSession.role != "Chủ trọ"
                    if (isAvailableToBook) {
                        when (bookingStatus) {
                            "pending" -> {
                                Surface(
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFFF8E1)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.AccessTime, null, tint = Color(0xFFF57F17), modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Chờ xác nhận", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFF57F17))
                                    }
                                }
                                OutlinedButton(
                                    onClick = { showCancelDialog = true },
                                    modifier = Modifier.height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                    border = BorderStroke(1.dp, Color.Red)
                                ) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Hủy", fontWeight = FontWeight.Bold)
                                }
                            }
                            "confirmed" -> {
                                Surface(
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFE8F5E9)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Đã xác nhận", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2E7D32))
                                    }
                                }
                            }
                            else -> {
                                Button(
                                    onClick = { onBookingClick(post.id, post.title, freshPost.total_rooms ?: 1) },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                ) {
                                    Icon(Icons.Default.CalendarToday, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Đặt phòng", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val imageCount = post.images?.size ?: 0
        val pagerState = rememberPagerState(pageCount = { imageCount })
        LaunchedEffect(pagerState.currentPage) {
            selectedImageIndex = pagerState.currentPage
        }
        // Auto-slide mỗi 3 giây, dừng khi chỉ có 1 ảnh
        LaunchedEffect(imageCount) {
            if (imageCount > 1) {
                while (true) {
                    kotlinx.coroutines.delay(3000)
                    val next = (pagerState.currentPage + 1) % imageCount
                    pagerState.animateScrollToPage(next)
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding).background(Color.White)) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                    if (!post.images.isNullOrEmpty()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            Base64Image(post.images[page], modifier = Modifier.fillMaxSize())
                        }
                        // Badge số ảnh
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "${pagerState.currentPage + 1}/${post.images.size} Ảnh",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        // Dot indicator
                        Row(
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            repeat(post.images.size) { index ->
                                Box(
                                    modifier = Modifier.size(8.dp).clip(CircleShape)
                                        .background(if (index == pagerState.currentPage) Color.White else Color.White.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(post.title, modifier = Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B34), lineHeight = 26.sp)
                        Icon(Icons.Outlined.FavoriteBorder, null, tint = Color(0xFF007BFF), modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.height(8.dp))

                    // Trạng thái phòng
                    val isAvailable = (freshPost.available ?: 1) == 1
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = if (isAvailable) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(8.dp).clip(CircleShape)
                                        .background(if (isAvailable) Color(0xFF4CAF50) else Color(0xFFE53935))
                                )
                                Text(
                                    if (isAvailable) "Còn phòng" else "Hết phòng",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAvailable) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                        }
                        if (isAvailable && (freshPost.available_rooms ?: 0) > 0) {
                            Surface(
                                color = Color(0xFFE3F2FD),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    "${freshPost.available_rooms} phòng trống",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1565C0)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column {
                            Text("Giá thuê", color = Color.Gray, fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("${formatPrice(post.price)}đ", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007BFF))
                                Text("/tháng", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 2.dp))
                            }
                        }

                        Column {
                            Text("Diện tích", color = Color.Gray, fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("${post.area}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B34))
                                Text(" m²", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 2.dp))
                            }
                        }
                    }
                }
            }

            // Banner trạng thái booking
            if (bookingStatus == "pending" || bookingStatus == "confirmed" || bookingStatus == "rejected") {
                item {
                    val (bgColor, iconTint, icon, title, desc) = when (bookingStatus) {
                        "confirmed" -> listOf(
                            Color(0xFFE8F5E9), Color(0xFF4CAF50), Icons.Default.CheckCircle,
                            "Đặt phòng đã được xác nhận", "Chủ trọ đã xác nhận yêu cầu của bạn."
                        )
                        "rejected" -> listOf(
                            Color(0xFFFFEBEE), Color(0xFFE53935), Icons.Default.Cancel,
                            "Yêu cầu bị từ chối", "Chủ trọ đã từ chối yêu cầu đặt phòng của bạn."
                        )
                        else -> listOf(
                            Color(0xFFFFF8E1), Color(0xFFF57F17), Icons.Default.AccessTime,
                            "Đang chờ xác nhận", "Yêu cầu đặt phòng của bạn đang chờ chủ trọ xác nhận."
                        )
                    }
                    @Suppress("UNCHECKED_CAST")
                    Surface(
                        color = bgColor as Color,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(icon as androidx.compose.ui.graphics.vector.ImageVector, null, tint = iconTint as Color, modifier = Modifier.size(28.dp))
                            Column {
                                Text(title as String, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0D1B34))
                                Text(desc as String, fontSize = 12.sp, color = Color.Gray, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(36.dp), color = Color(0xFFE3F2FD), shape = RoundedCornerShape(8.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.LocationOn, null, tint = Color(0xFF007BFF), modifier = Modifier.size(18.dp)) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        val parts = displayLocation.split(",")
                        val city = parts.lastOrNull()?.trim() ?: ""
                        val district = parts.getOrNull(parts.size - 2)?.trim() ?: ""
                        Text("$district, $city", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(displayLocation, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(160.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { ctx ->
                                MapView(ctx).apply {
                                    setTileSource(TileSourceFactory.MAPNIK)
                                    setMultiTouchControls(false)
                                    controller.setZoom(16.0)
                                }
                            },
                            update = { view ->
                                geoPoint?.let {
                                    view.controller.setCenter(it)
                                    view.overlays.clear()
                                    val marker = Marker(view)
                                    marker.position = it
                                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    view.overlays.add(marker)
                                    view.invalidate()
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 2.dp
                        ) {
                            Text("Xem bản đồ", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("Thông tin liên hệ", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable {
                            val phoneNumber = post.contact_phone?.trim() ?: ""
                            if (phoneNumber.isNotEmpty()) {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
                                context.startActivity(intent)
                            }
                        },
                        color = Color(0xFFF8F9FA),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(Color(0xFFE3F2FD), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, null, tint = Color(0xFF007BFF))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(post.contact_name ?: "Chưa có tên", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(post.contact_phone ?: "Chưa có số điện thoại", color = Color(0xFF007BFF), fontSize = 14.sp)
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.PhoneEnabled, null, tint = Color(0xFF4CAF50))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("Mô tả", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(post.description.ifEmpty { "Căn hộ nội thất cao cấp đầy đủ tiện nghi..." }, fontSize = 14.sp, color = Color(0xFF4A4A4A), lineHeight = 22.sp)
                    Text("Xem thêm", color = Color(0xFF007BFF), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tiện ích", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(16.dp))

                    val actualAmenities = post.amenities?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                    if (actualAmenities.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            actualAmenities.chunked(2).forEach { rowItems ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    rowItems.forEach { name ->
                                        AmenityChip(name, Modifier.weight(1f))
                                    }
                                    if (rowItems.size == 1) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else {
                        Text("Không có thông tin tiện ích", fontSize = 13.sp, color = Color.Gray)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(30.dp)) }
        }
    }
}

@Composable
fun AmenityChip(name: String, modifier: Modifier) {
    val icon = when {
        name.contains("Wifi") -> Icons.Default.Wifi
        name.contains("đậu xe") -> Icons.Default.LocalParking
        name.contains("Điều hòa") -> Icons.Default.AcUnit
        name.contains("Ban công") -> Icons.Default.Balcony
        name.contains("Bếp") -> Icons.Default.Restaurant
        name.contains("Tủ lạnh") -> Icons.Default.Kitchen
        name.contains("Máy giặt") -> Icons.Default.LocalLaundryService
        name.contains("Vệ sinh") -> Icons.Default.Wc
        name.contains("Thang máy") -> Icons.Default.Elevator
        name.contains("giờ giấc") -> Icons.Default.AccessTime
        else -> Icons.Default.CheckCircle
    }
    Surface(
        modifier = modifier,
        color = Color(0xFFF0F7FF),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color(0xFF007BFF), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(name, fontSize = 13.sp, color = Color(0xFF0D1B34), fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
