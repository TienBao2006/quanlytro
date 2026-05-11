package package com.example.quanlytro.features.room
import com.example.quanlytro.data.remote.*
import com.example.quanlytro.data.model.*

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyScreen(
    userRole: String = UserSession.role,
    onBackClick: () -> Unit = {},
    onPostDetailClick: (PostResponse) -> Unit = {}
) {
    val context = LocalContext.current
    var posts by remember { mutableStateOf<List<PostResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf("") }
    var userLat by remember { mutableStateOf(0.0) }
    var userLng by remember { mutableStateOf(0.0) }
    var selectedRadius by remember { mutableStateOf(5.0) }
    val radiusOptions = listOf(1.0, 3.0, 5.0, 10.0, 20.0)

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    @SuppressLint("MissingPermission")
    fun fetchLocation() {
        isLoading = true
        locationError = ""
        val cts = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    userLat = location.latitude
                    userLng = location.longitude
                    RetrofitClient.instance.getNearbyPosts(userLat, userLng, selectedRadius)
                        .enqueue(object : Callback<List<PostResponse>> {
                            override fun onResponse(call: Call<List<PostResponse>>, response: Response<List<PostResponse>>) {
                                isLoading = false
                                posts = response.body() ?: emptyList()
                            }
                            override fun onFailure(call: Call<List<PostResponse>>, t: Throwable) {
                                isLoading = false
                                locationError = "Lỗi kết nối server"
                            }
                        })
                } else {
                    isLoading = false
                    locationError = "Không lấy được vị trí, thử lại nhé"
                }
            }
            .addOnFailureListener {
                isLoading = false
                locationError = "Lỗi GPS: ${it.message}"
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            fetchLocation()
        } else {
            locationError = "Cần cấp quyền vị trí để tìm trọ gần đây"
        }
    }

    fun checkAndFetch() {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            fetchLocation()
        } else {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    // Tự động lấy vị trí khi mở màn hình
    LaunchedEffect(Unit) { checkAndFetch() }

    // Reload khi đổi bán kính
    LaunchedEffect(selectedRadius) {
        if (userLat != 0.0 && userLng != 0.0) fetchLocation()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Trọ gần đây", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (userLat != 0.0) {
                            Text(
                                "Trong vòng ${selectedRadius.toInt()} km",
                                fontSize = 12.sp,
                                color = Color(0xFF42A5F5)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { checkAndFetch() }) {
                        Icon(Icons.Default.MyLocation, null, tint = Color(0xFF1565C0))
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
            // Bộ chọn bán kính
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    // Chip chọn bán kính
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        radiusOptions.forEach { r ->
                            FilterChip(
                                selected = selectedRadius == r,
                                onClick = { selectedRadius = r },
                                label = { Text("${r.toInt()} km", fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1565C0),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                when {
                    isLoading -> item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(400.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color(0xFF1565C0))
                                Spacer(Modifier.height(12.dp))
                                Text("Đang tìm trọ gần bạn...", color = Color.Gray, fontSize = 14.sp)
                            }
                        }
                    }

                    locationError.isNotEmpty() -> item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(400.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(72.dp), tint = Color(0xFFE0E0E0))
                                Spacer(Modifier.height(12.dp))
                                Text(locationError, color = Color.Gray, fontSize = 14.sp)
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = { checkAndFetch() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Thử lại")
                                }
                            }
                        }
                    }

                    posts.isEmpty() && userLat != 0.0 -> item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(400.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(72.dp), tint = Color(0xFFE0E0E0))
                                Spacer(Modifier.height(12.dp))
                                Text("Không có trọ nào trong ${selectedRadius.toInt()} km", color = Color.Gray)
                                Spacer(Modifier.height(8.dp))
                                Text("Thử mở rộng bán kính tìm kiếm", color = Color(0xFF42A5F5), fontSize = 13.sp)
                            }
                        }
                    }

                    else -> {
                        item {
                            val hasCoords = posts.any { it.distance_km != null }
                            if (!hasCoords) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, null, tint = Color(0xFFF57C00), modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Các phòng chưa có tọa độ GPS. Hiển thị tất cả bài đăng.",
                                            fontSize = 12.sp, color = Color(0xFFF57C00)
                                        )
                                    }
                                }
                            }
                            Text(
                                if (hasCoords) "Tìm thấy ${posts.size} phòng trọ gần bạn"
                                else "Tất cả ${posts.size} phòng trọ",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                fontSize = 13.sp, color = Color.Gray
                            )
                        }
                        items(posts) { post ->
                            NearbyPostCard(post = post, onClick = { onPostDetailClick(post) })
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun NearbyPostCard(post: PostResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            // Ảnh nhỏ
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(Color(0xFFE3F2FD), RoundedCornerShape(12.dp))
            ) {
                if (!post.images.isNullOrEmpty()) {
                    Base64Image(
                        post.images[0],
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent, RoundedCornerShape(12.dp))
                    )
                } else {
                    Icon(
                        Icons.Default.LocationOn,
                        null,
                        tint = Color(0xFF90CAF9),
                        modifier = Modifier.align(Alignment.Center).size(36.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(post.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, color = Color(0xFF0D1B34))
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, Modifier.size(13.dp), Color(0xFF42A5F5))
                    Text(post.location, fontSize = 12.sp, color = Color.Gray, maxLines = 1, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${formatPrice(post.price)}đ/tháng",
                        color = Color(0xFF1565C0),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    // Badge khoảng cách
                    post.distance_km?.let { dist ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFE3F2FD)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.MyLocation, null, Modifier.size(11.dp), Color(0xFF1565C0))
                                Text(
                                    text = if (dist < 1.0) "${(dist * 1000).toInt()}m" else "${dist}km",
                                    fontSize = 11.sp,
                                    color = Color(0xFF1565C0),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
