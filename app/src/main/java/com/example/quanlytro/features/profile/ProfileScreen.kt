package package com.example.quanlytro.features.profile
import com.example.quanlytro.data.remote.*
import com.example.quanlytro.data.model.*

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.InputStream
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userRole: String = "Người thuê",
    onBackClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onExploreClick: () -> Unit = {},
    onManageClick: () -> Unit = {},
    onChatListClick: () -> Unit = {},
    onMyContractClick: () -> Unit = {},
    onMyInvoiceClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf(UserSession.fullName) }
    var email    by remember { mutableStateOf(UserSession.email) }
    var phone    by remember { mutableStateOf(UserSession.phone) }
    var address  by remember { mutableStateOf(UserSession.address) }
    var dob      by remember { mutableStateOf(UserSession.dob) }
    var idCard   by remember { mutableStateOf(UserSession.idCard) }
    var avatar   by remember { mutableStateOf(UserSession.avatar) }
    val snackState = remember { SnackbarHostState() }
    var snackMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        RetrofitClient.instance.getUserByUid(UserSession.uid)
            .enqueue(object : Callback<UserLookupResponse> {
                override fun onResponse(call: Call<UserLookupResponse>, response: Response<UserLookupResponse>) {
                    response.body()?.let { u ->
                        UserSession.fullName = u.fullName ?: UserSession.fullName
                        UserSession.email    = u.email    ?: ""
                        UserSession.address  = u.address  ?: ""
                        UserSession.dob      = u.dob      ?: ""
                        UserSession.idCard   = u.id_card  ?: ""
                        UserSession.avatar   = u.avatar   ?: ""
                        fullName = UserSession.fullName
                        email    = UserSession.email
                        phone    = UserSession.phone
                        address  = UserSession.address
                        dob      = UserSession.dob
                        idCard   = UserSession.idCard
                        avatar   = UserSession.avatar
                    }
                }
                override fun onFailure(call: Call<UserLookupResponse>, t: Throwable) {}
            })
    }

    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackState.showSnackbar(it); snackMsg = null }
    }

    if (showEditDialog) {
        EditProfileDialog(
            fullName = fullName, email = email, phone = phone,
            address = address, dob = dob, idCard = idCard, avatar = avatar,
            onDismiss = { showEditDialog = false },
            onSave = { fn, em, ph, addr, d, ic, av ->
                RetrofitClient.instance.updateProfile(UserSession.uid, fn, em, ph, addr, d, ic, av)
                    .enqueue(object : Callback<SimpleResponse> {
                        override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                            if (response.body()?.status == "success") {
                                UserSession.fullName = fn; UserSession.email = em
                                UserSession.phone = ph; UserSession.address = addr
                                UserSession.dob = d; UserSession.idCard = ic
                                UserSession.avatar = av
                                fullName = fn; email = em; phone = ph
                                address = addr; dob = d; idCard = ic; avatar = av
                                snackMsg = "Cập nhật thành công"
                                showEditDialog = false
                            } else {
                                snackMsg = "Lỗi: ${response.body()?.message}"
                            }
                        }
                        override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                            snackMsg = "Lỗi kết nối"
                        }
                    })
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Hồ sơ cá nhân", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            BottomNavBar(
                initialSelected = if (userRole == "Chủ trọ") 3 else 2,
                userRole = userRole,
                onExploreClick = onExploreClick,
                onProfileClick = {},
                onManageClick = onManageClick,
                onChatListClick = onChatListClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F9FA))
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // Avatar
            Box(contentAlignment = Alignment.BottomEnd) {
                val bmp = remember(avatar) {
                    if (avatar.isBlank()) null
                    else try {
                        val bytes = Base64.decode(
                            if (avatar.contains(",")) avatar.substringAfter(",") else avatar,
                            Base64.DEFAULT
                        )
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } catch (e: Exception) { null }
                }
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(100.dp).clip(CircleShape)
                            .border(2.dp, Color(0xFF007BFF), CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier.size(100.dp).clip(CircleShape)
                            .background(Color(0xFFE3F2FD))
                            .border(2.dp, Color(0xFF007BFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, null, tint = Color(0xFF1976D2), modifier = Modifier.size(60.dp))
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF007BFF),
                    modifier = Modifier.size(28.dp).clickable { showEditDialog = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(fullName.ifBlank { "Người dùng" }, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B34))
            Text("Vai trò: $userRole", fontSize = 14.sp, color = Color.Gray)
            Spacer(Modifier.height(16.dp))

            // Thông tin cá nhân — dạng menu item
            ProfileSection("TÀI KHOẢN") {
                ProfileMenuItem(
                    icon = Icons.Default.Person,
                    title = "Thông tin cá nhân",
                    subtitle = if (phone.isNotBlank()) phone else "Chưa cập nhật",
                    iconContainerColor = Color(0xFFE8F5E9),
                    iconColor = Color(0xFF4CAF50),
                    onClick = { showEditDialog = true }
                )
            }

            Spacer(Modifier.height(8.dp))

            if (userRole == "Người thuê") {
                ProfileSection("HỢP ĐỒNG & HÓA ĐƠN") {
                    ProfileMenuItem(Icons.Default.Description, "Hợp đồng của tôi", "Xem các hợp đồng thuê phòng",
                        Color(0xFFE3F2FD), Color(0xFF007BFF), onMyContractClick)
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = Color(0xFFF0F0F0))
                    ProfileMenuItem(Icons.Default.Receipt, "Hóa đơn của tôi", "Xem hóa đơn điện, nước, tiền phòng",
                        Color(0xFFF3E5F5), Color(0xFF9C27B0), onMyInvoiceClick)
                }
                Spacer(Modifier.height(8.dp))
            }

            ProfileSection("HỆ THỐNG") {
                ProfileMenuItem(Icons.Default.Settings, "Cài đặt", "Thông báo, bảo mật, ngôn ngữ",
                    Color(0xFFF1F3F5), Color(0xFF495057), onSettingsClick)
            }

            Spacer(Modifier.height(24.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onLogoutClick() },
                shape = RoundedCornerShape(12.dp), color = Color.White
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = Color.Red)
                    Spacer(Modifier.width(8.dp))
                    Text("Đăng xuất", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    fullName: String, email: String, phone: String,
    address: String, dob: String, idCard: String, avatar: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var fn   by remember { mutableStateOf(fullName) }
    var em   by remember { mutableStateOf(email) }
    var ph   by remember { mutableStateOf(phone) }
    var addr by remember { mutableStateOf(address) }
    var d    by remember { mutableStateOf(dob) }
    var ic   by remember { mutableStateOf(idCard) }
    var av   by remember { mutableStateOf(avatar) }
    var showDatePicker by remember { mutableStateOf(false) }

    // DatePicker
    if (showDatePicker) {
        val cal = Calendar.getInstance()
        // parse ngày hiện tại nếu có
        if (d.isNotBlank()) {
            try {
                val parts = d.split("/")
                if (parts.size == 3) {
                    cal.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
                }
            } catch (_: Exception) {}
        }
        val dpState = rememberDatePickerState(initialSelectedDateMillis = cal.timeInMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { millis ->
                        val c = Calendar.getInstance().apply { timeInMillis = millis }
                        d = "%02d/%02d/%04d".format(
                            c.get(Calendar.DAY_OF_MONTH),
                            c.get(Calendar.MONTH) + 1,
                            c.get(Calendar.YEAR)
                        )
                    }
                    showDatePicker = false
                }) { Text("Chọn") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Hủy") } }
        ) { DatePicker(state = dpState) }
    }

    // Image picker
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val stream: InputStream? = context.contentResolver.openInputStream(it)
                val bytes = stream?.readBytes() ?: return@let
                stream.close()
                // Resize nếu cần
                val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                opts.inSampleSize = calculateInSampleSize(opts, 512, 512)
                opts.inJustDecodeBounds = false
                val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                val out = java.io.ByteArrayOutputStream()
                bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
                av = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            } catch (_: Exception) {}
        }
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Chỉnh sửa thông tin", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                HorizontalDivider()

                // Avatar picker
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val bmp = remember(av) {
                        if (av.isBlank()) null
                        else try {
                            val bytes = Base64.decode(av, Base64.DEFAULT)
                            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } catch (_: Exception) { null }
                    }
                    Box(contentAlignment = Alignment.BottomEnd) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(90.dp).clip(CircleShape)
                                    .border(2.dp, Color(0xFF007BFF), CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(90.dp).clip(CircleShape)
                                    .background(Color(0xFFE3F2FD))
                                    .border(2.dp, Color(0xFF007BFF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, null, tint = Color(0xFF1976D2), modifier = Modifier.size(54.dp))
                            }
                        }
                        Surface(
                            shape = CircleShape, color = Color(0xFF007BFF),
                            modifier = Modifier.size(28.dp).clickable { imagePicker.launch("image/*") }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                EditField("Họ tên",             fn,   Icons.Default.Person)     { fn   = it }
                EditField("Email",              em,   Icons.Default.Email)      { em   = it }
                EditField("Số điện thoại",      ph,   Icons.Default.Phone)      { ph   = it }
                EditField("Địa chỉ thường trú", addr, Icons.Default.LocationOn) { addr = it }

                // Ngày sinh với DatePicker
                OutlinedTextField(
                    value = d,
                    onValueChange = { d = it },
                    label = { Text("Ngày sinh", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Cake, null, tint = Color(0xFF007BFF), modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF007BFF))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    readOnly = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007BFF),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )

                EditField("CCCD/CMND", ic, Icons.Default.Badge) { ic = it }

                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                        Text("Hủy")
                    }
                    Button(
                        onClick = { onSave(fn, em, ph, addr, d, ic, av) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF))
                    ) { Text("Lưu") }
                }
            }
        }
    }
}

@Composable
fun ProfileInfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = Color(0xFF007BFF), modifier = Modifier.size(18.dp))
        Column {
            Text(label, fontSize = 11.sp, color = Color.Gray)
            Text(value, fontSize = 14.sp, color = Color(0xFF0D1B34), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun EditField(label: String, value: String, icon: ImageVector, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        leadingIcon = { Icon(icon, null, tint = Color(0xFF007BFF), modifier = Modifier.size(20.dp)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF007BFF),
            unfocusedBorderColor = Color(0xFFE0E0E0)
        )
    )
}

@Composable
fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            fontSize = 12.sp, fontWeight = FontWeight.Bold,
            color = Color.LightGray, letterSpacing = 1.sp
        )
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White) {
            Column(content = content)
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector, title: String, subtitle: String,
    iconContainerColor: Color, iconColor: Color, onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(iconContainerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.LightGray)
    }
}
