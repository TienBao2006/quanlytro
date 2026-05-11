package package com.example.quanlytro.features.profile
import com.example.quanlytro.data.remote.*
import com.example.quanlytro.data.model.*

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quanlytro.RetrofitClient
import com.example.quanlytro.SimpleResponse
import com.example.quanlytro.UserSession
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private const val PREFS_NAME = "settings_prefs"
private const val KEY_PUSH_NOTIF = "push_notifications"
private const val KEY_EMAIL_NOTIF = "email_notifications"
private const val KEY_DARK_MODE = "dark_mode"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var notificationsEnabled by remember { mutableStateOf(prefs.getBoolean(KEY_PUSH_NOTIF, true)) }
    var emailNotifications by remember { mutableStateOf(prefs.getBoolean(KEY_EMAIL_NOTIF, false)) }
    var darkMode by remember { mutableStateOf(prefs.getBoolean(KEY_DARK_MODE, false)) }

    var showChangePasswordDialog by remember { mutableStateOf(false) }

    val snackState = remember { SnackbarHostState() }
    var snackMsg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackState.showSnackbar(it); snackMsg = null }
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onResult = { msg -> snackMsg = msg }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cài đặt", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F9FA))
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection("THÔNG BÁO") {
                SettingsToggleItem(
                    icon = Icons.Default.Notifications,
                    iconContainerColor = Color(0xFFE3F2FD),
                    iconColor = Color(0xFF007BFF),
                    title = "Thông báo đẩy",
                    subtitle = "Nhận thông báo từ ứng dụng",
                    checked = notificationsEnabled,
                    onCheckedChange = {
                        notificationsEnabled = it
                        prefs.edit().putBoolean(KEY_PUSH_NOTIF, it).apply()
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = Color(0xFFF0F0F0))
                SettingsToggleItem(
                    icon = Icons.Default.Email,
                    iconContainerColor = Color(0xFFF3E5F5),
                    iconColor = Color(0xFF9C27B0),
                    title = "Thông báo email",
                    subtitle = "Nhận thông báo qua email",
                    checked = emailNotifications,
                    onCheckedChange = {
                        emailNotifications = it
                        prefs.edit().putBoolean(KEY_EMAIL_NOTIF, it).apply()
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            SettingsSection("GIAO DIỆN") {
                SettingsToggleItem(
                    icon = Icons.Default.DarkMode,
                    iconContainerColor = Color(0xFF212121).copy(alpha = 0.1f),
                    iconColor = Color(0xFF212121),
                    title = "Chế độ tối",
                    subtitle = if (darkMode) "Đang bật" else "Đang tắt",
                    checked = darkMode,
                    onCheckedChange = {
                        darkMode = it
                        prefs.edit().putBoolean(KEY_DARK_MODE, it).apply()
                        snackMsg = if (it) "Chế độ tối đã bật (khởi động lại để áp dụng)" else "Chế độ tối đã tắt"
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            SettingsSection("BẢO MẬT") {
                SettingsNavItem(
                    icon = Icons.Default.Lock,
                    iconContainerColor = Color(0xFFE8F5E9),
                    iconColor = Color(0xFF4CAF50),
                    title = "Đổi mật khẩu",
                    subtitle = "Cập nhật mật khẩu tài khoản",
                    onClick = { showChangePasswordDialog = true }
                )
            }

            Spacer(Modifier.height(8.dp))

            SettingsSection("VỀ ỨNG DỤNG") {
                SettingsNavItem(
                    icon = Icons.Default.Info,
                    iconContainerColor = Color(0xFFF1F3F5),
                    iconColor = Color(0xFF495057),
                    title = "Phiên bản",
                    subtitle = "1.0.0"
                )
                HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = Color(0xFFF0F0F0))
                SettingsNavItem(
                    icon = Icons.Default.Policy,
                    iconContainerColor = Color(0xFFF1F3F5),
                    iconColor = Color(0xFF495057),
                    title = "Chính sách bảo mật",
                    subtitle = "Xem điều khoản và chính sách"
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onResult: (String) -> Unit
) {
    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var oldVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đổi mật khẩu", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (errorMsg.isNotBlank()) {
                    Text(errorMsg, color = Color.Red, fontSize = 13.sp)
                }
                PasswordField("Mật khẩu hiện tại", oldPass, oldVisible,
                    onValueChange = { oldPass = it },
                    onToggleVisible = { oldVisible = !oldVisible })
                PasswordField("Mật khẩu mới", newPass, newVisible,
                    onValueChange = { newPass = it },
                    onToggleVisible = { newVisible = !newVisible })
                PasswordField("Xác nhận mật khẩu mới", confirmPass, confirmVisible,
                    onValueChange = { confirmPass = it },
                    onToggleVisible = { confirmVisible = !confirmVisible })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    errorMsg = ""
                    when {
                        oldPass.isBlank() || newPass.isBlank() || confirmPass.isBlank() ->
                            errorMsg = "Vui lòng điền đầy đủ thông tin"
                        newPass.length < 6 ->
                            errorMsg = "Mật khẩu mới phải ít nhất 6 ký tự"
                        newPass != confirmPass ->
                            errorMsg = "Mật khẩu xác nhận không khớp"
                        else -> {
                            isLoading = true
                            RetrofitClient.instance.changePassword(UserSession.uid, oldPass, newPass)
                                .enqueue(object : Callback<SimpleResponse> {
                                    override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                                        isLoading = false
                                        val body = response.body()
                                        if (body?.status == "success") {
                                            onResult("Đổi mật khẩu thành công")
                                            onDismiss()
                                        } else {
                                            errorMsg = body?.message ?: "Có lỗi xảy ra"
                                        }
                                    }
                                    override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                                        isLoading = false
                                        errorMsg = "Lỗi kết nối"
                                    }
                                })
                        }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF)),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Xác nhận")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Hủy")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    visible: Boolean,
    onValueChange: (String) -> Unit,
    onToggleVisible: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF007BFF), modifier = Modifier.size(20.dp)) },
        trailingIcon = {
            IconButton(onClick = onToggleVisible) {
                Icon(
                    if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    null, tint = Color.Gray
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
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
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
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
fun SettingsToggleItem(
    icon: ImageVector,
    iconContainerColor: Color,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
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
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF007BFF))
        )
    }
}

@Composable
fun SettingsNavItem(
    icon: ImageVector,
    iconContainerColor: Color,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
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
