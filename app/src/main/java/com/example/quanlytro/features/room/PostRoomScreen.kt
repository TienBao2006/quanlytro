package package com.example.quanlytro.features.room
import com.example.quanlytro.data.remote.*
import com.example.quanlytro.data.model.*

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PostRoomScreen(
    postJson: String? = null,
    onBackClick: () -> Unit = {},
    onPostSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUserId = UserSession.uid

    val existingPost = remember {
        postJson?.let {
            try {
                val decoded = URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
                Gson().fromJson(decoded, PostResponse::class.java)
            } catch (e: Exception) { null }
        }
    }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osm_pref", 0))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    var title by remember { mutableStateOf(existingPost?.title ?: "") }

    // Fetch lại post mới nhất từ server để có available_rooms chính xác
    var freshPost by remember { mutableStateOf(existingPost) }
    LaunchedEffect(existingPost?.id) {
        val id = existingPost?.id ?: return@LaunchedEffect
        RetrofitClient.instance.getPostById(id).enqueue(object : retrofit2.Callback<List<PostResponse>> {
            override fun onResponse(call: retrofit2.Call<List<PostResponse>>, response: retrofit2.Response<List<PostResponse>>) {
                response.body()?.firstOrNull()?.let { freshPost = it }
            }
            override fun onFailure(call: retrofit2.Call<List<PostResponse>>, t: Throwable) {}
        })
    }

    var selectedProvince by remember { mutableStateOf("") }
    var selectedDistrict by remember { mutableStateOf("") }
    var streetAddress by remember { mutableStateOf("") }
    
    LaunchedEffect(existingPost) {
        existingPost?.location?.let { loc ->
            val parts = loc.split(",").map { it.trim() }
            if (parts.size >= 3) {
                selectedProvince = parts.last()
                selectedDistrict = parts[parts.size - 2]
                streetAddress = parts.dropLast(2).joinToString(", ")
            } else {
                streetAddress = loc
            }
        }
    }

    var price by remember { mutableStateOf(existingPost?.price?.toLong()?.toString() ?: "") }
    var area by remember { mutableStateOf(existingPost?.area?.toLong()?.toString() ?: "") }
    var description by remember { mutableStateOf(existingPost?.description ?: "") }
    var contactName by remember { mutableStateOf(existingPost?.contact_name ?: UserSession.fullName) }
    var contactPhone by remember { mutableStateOf(existingPost?.contact_phone ?: UserSession.phone) }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }

    var existingImages by remember { mutableStateOf(existingPost?.images ?: emptyList()) }
    
    var selectedAmenities by remember { 
        mutableStateOf(existingPost?.amenities?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: setOf<String>())
    }
    var isLoading by remember { mutableStateOf(false) }
    var totalRooms by remember { mutableStateOf(existingPost?.total_rooms?.toString() ?: existingPost?.available_rooms?.toString() ?: "1") }

    var currentGeoPoint by remember {
        mutableStateOf(
            existingPost?.map_url?.split(",")?.let {
                if (it.size == 2) GeoPoint(it[0].toDoubleOrNull() ?: 10.762622, it[1].toDoubleOrNull() ?: 106.660172)
                else GeoPoint(10.762622, 106.660172)
            } ?: GeoPoint(10.762622, 106.660172)
        )
    }

    val scrollState = rememberScrollState()
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }

    val amenitiesList = listOf(
        "Wifi miễn phí" to Icons.Default.Wifi,
        "An ninh 24/7" to Icons.Default.Security,
        "Tự do giờ giấc" to Icons.Default.AccessTime,
        "Bếp riêng" to Icons.Default.Restaurant
    )

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> 
        if (uris.size + selectedImages.size + existingImages.size <= 10) {
            selectedImages = selectedImages + uris
        } else {
            Toast.makeText(context, "Tối đa 10 ảnh", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingPost == null) "Đăng tin mới" else "Sửa bài đăng", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.Close, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = Color.White) {
                Button(
                    onClick = {
                        val fullAddress = listOf(streetAddress, selectedDistrict, selectedProvince)
                            .filter { it.isNotBlank() }.joinToString(", ")
                        if (title.isEmpty() || fullAddress.isEmpty() || price.isEmpty() || contactPhone.isEmpty()) {
                            Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true

                        val coordsString = "${currentGeoPoint.latitude},${currentGeoPoint.longitude}"
                        val amenitiesString = selectedAmenities.joinToString(",")
                        val priceVal = price.toDoubleOrNull() ?: 0.0
                        val areaVal = area.toDoubleOrNull() ?: 0.0
                        val totalRoomsVal = totalRooms.toIntOrNull()?.coerceAtLeast(1) ?: 1

                        scope.launch {
                            try {
                                val base64Images = withContext(Dispatchers.IO) {
                                    val newlySelected = selectedImages.mapNotNull { uri ->
                                        try {
                                            val inputStream = context.contentResolver.openInputStream(uri)
                                            val original = BitmapFactory.decodeStream(inputStream) ?: return@mapNotNull null

                                            val maxSide = 800
                                            val scale = Math.min(maxSide.toFloat() / original.width, maxSide.toFloat() / original.height)
                                            val finalBitmap = if (scale < 1f) {
                                                Bitmap.createScaledBitmap(original, (original.width * scale).toInt(), (original.height * scale).toInt(), true)
                                            } else {
                                                original
                                            }

                                            val out = ByteArrayOutputStream()
                                            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
                                            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                                        } catch (e: Exception) { null }
                                    }
                                    existingImages + newlySelected
                                }
                                
                                val imagesJson = Gson().toJson(base64Images)

                                var postSuccess = false
                                var errorMsg = ""

                                suspendCancellableCoroutine<Unit> { cont ->
                                    val call = if (existingPost == null) {
                                        RetrofitClient.instance.uploadPost(
                                            currentUserId, title, fullAddress, priceVal, areaVal,
                                            description, amenitiesString, coordsString,
                                            contactName, contactPhone, imagesJson,
                                            totalRoomsVal
                                        )
                                    } else {
                                        RetrofitClient.instance.updatePost(
                                            existingPost.id, currentUserId, title, fullAddress, priceVal, areaVal,
                                            description, amenitiesString, coordsString,
                                            contactName, contactPhone, imagesJson,
                                            totalRoomsVal
                                        )
                                    }

                                    call.enqueue(object : Callback<String> {
                                        override fun onResponse(call: Call<String>, response: Response<String>) {
                                            val body = response.body()?.trim() ?: ""
                                            postSuccess = response.isSuccessful && (body == "OK" || body.startsWith("OK"))
                                            if (!postSuccess) errorMsg = body.ifEmpty { "Error ${response.code()}" }
                                            cont.resume(Unit)
                                        }
                                        override fun onFailure(call: Call<String>, t: Throwable) {
                                            cont.resumeWithException(t)
                                        }
                                    })
                                }

                                isLoading = false
                                if (postSuccess) {
                                    Toast.makeText(context, if (existingPost == null) "Đăng bài thành công!" else "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                                    onPostSuccess()
                                } else {
                                    Toast.makeText(context, "Thất bại: $errorMsg", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF)),
                    enabled = !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text(if (existingPost == null) "Đăng tin ngay" else "Cập nhật bài viết", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = Color(0xFFF8F9FA)
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                Text("Hình ảnh (Tối đa 10 ảnh)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)).background(Color.White).clickable { launcher.launch("image/*") }, contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddPhotoAlternate, null, tint = Color(0xFF007BFF), modifier = Modifier.size(28.dp))
                                Spacer(Modifier.height(4.dp))
                                Text("Thêm ảnh", fontSize = 12.sp, color = Color(0xFF007BFF), fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    items(existingImages) { base64 ->
                        Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp))) {
                            Base64Image(base64, Modifier.fillMaxSize())
                            Box(
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).background(Color.White.copy(alpha = 0.9f), CircleShape).clickable { existingImages = existingImages.filter { it != base64 } },
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(14.dp)) }
                        }
                    }

                    items(selectedImages) { uri ->
                        Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp))) {
                            ImageFromUri(uri, Modifier.fillMaxSize())
                            Box(
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).background(Color.White.copy(alpha = 0.9f), CircleShape).clickable { selectedImages = selectedImages.filter { it != uri } },
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(14.dp)) }
                        }
                    }
                }
            }

            SectionCard(title = "Thông tin cơ bản", icon = Icons.Default.Info) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CustomTextField(value = title, onValueChange = { title = it }, label = "Tiêu đề bài đăng", placeholder = "VD: Căn hộ Studio ngay trung tâm Quận 1")
                    
                    Text("Địa chỉ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    CustomTextField(value = streetAddress, onValueChange = { streetAddress = it }, label = "Số nhà, tên đường", placeholder = "Số 123, Đường ABC...")
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            CustomTextField(value = selectedDistrict, onValueChange = { selectedDistrict = it }, label = "Xã/Phường", placeholder = "Phường 1")
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CustomTextField(value = selectedProvince, onValueChange = { selectedProvince = it }, label = "Tỉnh/Thành phố", placeholder = "TP. HCM")
                        }
                    }
                    
                    Button(
                        onClick = {
                            val fullAddress = listOf(streetAddress, selectedDistrict, selectedProvince).filter { it.isNotBlank() }.joinToString(", ")
                            if (fullAddress.isNotBlank()) {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val addresses = geocoder.getFromLocationName(fullAddress, 1)
                                        if (addresses != null && addresses.isNotEmpty()) {
                                            withContext(Dispatchers.Main) {
                                                currentGeoPoint = GeoPoint(addresses[0].latitude, addresses[0].longitude)
                                            }
                                        }
                                    } catch (e: Exception) {}
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE3F2FD), contentColor = Color(0xFF007BFF)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Map, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cập nhật vị trí trên bản đồ")
                    }
                }
            }

            SectionCard(title = "Vị trí trên bản đồ", icon = Icons.Default.LocationOn) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))) {
                    AndroidView(
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                controller.setZoom(15.0)
                            }
                        },
                        update = { view ->
                            view.controller.setCenter(currentGeoPoint)
                            view.overlays.clear()
                            val marker = Marker(view)
                            marker.position = currentGeoPoint
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.isDraggable = true
                            marker.setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                                override fun onMarkerDrag(m: Marker?) {}
                                override fun onMarkerDragEnd(m: Marker?) {
                                    m?.position?.let { currentGeoPoint = it }
                                }
                                override fun onMarkerDragStart(m: Marker?) {}
                            })
                            view.overlays.add(marker)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            SectionCard(title = "Chi tiết & Giá", icon = Icons.Default.Sell) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            CustomTextField(value = price, onValueChange = { price = it }, label = "Giá thuê (VNĐ/tháng)", placeholder = "5000000", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CustomTextField(value = area, onValueChange = { area = it }, label = "Diện tích (m²)", placeholder = "30", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                    }
                    CustomTextField(value = description, onValueChange = { description = it }, label = "Mô tả chi tiết", placeholder = "Mô tả về phòng, quy định, tiện ích xung quanh...", minLines = 4)
                }
            }

            SectionCard(title = "Tiện ích", icon = Icons.Default.List) {
                val rows = amenitiesList.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowItems.forEach { (name, icon) ->
                                val isSelected = selectedAmenities.contains(name)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedAmenities = if (isSelected) selectedAmenities - name else selectedAmenities + name
                                    },
                                    label = { Text(name, fontSize = 13.sp) },
                                    leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFE3F2FD),
                                        selectedLabelColor = Color(0xFF007BFF),
                                        selectedLeadingIconColor = Color(0xFF007BFF)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (isSelected) Color(0xFF007BFF) else Color(0xFFE2E8F0),
                                        enabled = true,
                                        selected = isSelected
                                    )
                                )
                            }
                        }
                    }
                }
            }

            SectionCard(title = "Số phòng", icon = Icons.Default.MeetingRoom) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CustomTextField(
                        value = totalRooms,
                        onValueChange = { totalRooms = it },
                        label = "Tổng số phòng",
                        placeholder = "1",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    val total = totalRooms.toIntOrNull() ?: 0
                    // Đã thuê = total_rooms - available_rooms (lấy từ server, chỉ có khi sửa bài)
                    val rented = if (freshPost != null) {
                        val serverTotal = freshPost!!.total_rooms ?: freshPost!!.available_rooms ?: 1
                        val serverAvailable = freshPost!!.available_rooms ?: 1
                        (serverTotal - serverAvailable).coerceAtLeast(0)
                    } else 0
                    val remaining = (total - rented).coerceAtLeast(0)
                    Surface(color = Color(0xFFF0F4FF), shape = RoundedCornerShape(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$total", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007BFF))
                                Text("Tổng phòng", fontSize = 12.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$rented", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                                Text("Đã thuê", fontSize = 12.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$remaining", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                Text("Còn trống", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            SectionCard(title = "Thông tin liên hệ", icon = Icons.Default.ContactPhone) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CustomTextField(value = contactName, onValueChange = { contactName = it }, label = "Tên người liên hệ", placeholder = "Nguyễn Văn A")
                    CustomTextField(value = contactPhone, onValueChange = { contactPhone = it }, label = "Số điện thoại", placeholder = "0901234567", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SectionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Color(0xFF007BFF), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0D1B34))
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    minLines: Int = 1
) {
    Column {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = Color.LightGray, fontSize = 14.sp) },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF007BFF),
                unfocusedBorderColor = Color(0xFFE2E8F0)
            ),
            keyboardOptions = keyboardOptions,
            minLines = minLines,
            maxLines = if (minLines > 1) 10 else 1
        )
    }
}

@Composable
fun ImageFromUri(uri: Uri, modifier: Modifier) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) { null }
    }
    bitmap?.let { Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop) }
}
