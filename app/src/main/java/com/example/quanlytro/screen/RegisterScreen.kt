package com.example.quanlytro

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Người thuê") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Header gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(gradient)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.padding(8.dp).align(Alignment.TopStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("Tạo tài khoản", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Điền thông tin bên dưới", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            }
        }

        // Form
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF0F4F8))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .offset(y = (-28).dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(28.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Thông tin cá nhân", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B34))

                    // Full name
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        placeholder = { Text("Họ và tên", color = Color(0xFFB0BEC5)) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = Color(0xFF42A5F5)) },
                        shape = RoundedCornerShape(14.dp),
                        colors = authFieldColors(),
                        singleLine = true
                    )

                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("Email", color = Color(0xFFB0BEC5)) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = Color(0xFF42A5F5)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(14.dp),
                        colors = authFieldColors(),
                        singleLine = true
                    )

                    // Phone
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        placeholder = { Text("Số điện thoại", color = Color(0xFFB0BEC5)) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Phone, null, tint = Color(0xFF42A5F5)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(14.dp),
                        colors = authFieldColors(),
                        singleLine = true
                    )

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Mật khẩu", color = Color(0xFFB0BEC5)) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF42A5F5)) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    null, tint = Color(0xFF7E8CA0)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(14.dp),
                        colors = authFieldColors(),
                        singleLine = true
                    )

                    // Role selection
                    Text("Bạn là", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0D1B34))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RoleChip(
                            label = "Người thuê",
                            icon = Icons.Default.Person,
                            selected = role == "Người thuê",
                            onClick = { role = "Người thuê" },
                            modifier = Modifier.weight(1f)
                        )
                        RoleChip(
                            label = "Chủ trọ",
                            icon = Icons.Default.Home,
                            selected = role == "Chủ trọ",
                            onClick = { role = "Chủ trọ" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    // Register button
                    Button(
                        onClick = {
                            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                                Toast.makeText(context, "Vui lòng nhập đầy đủ", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isLoading = true
                            RetrofitClient.instance.registerUser(fullName, email, phone, password, role)
                                .enqueue(object : Callback<String> {
                                    override fun onResponse(call: Call<String>, response: Response<String>) {
                                        isLoading = false
                                        val body = response.body() ?: ""
                                        if (body.contains("success")) {
                                            Toast.makeText(context, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                                            onBackClick()
                                        } else {
                                            val msg = try {
                                                org.json.JSONObject(body).optString("message", body)
                                            } catch (e: Exception) { body }
                                            Toast.makeText(context, "Lỗi: $msg", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    override fun onFailure(call: Call<String>, t: Throwable) {
                                        isLoading = false
                                        Toast.makeText(context, "Lỗi kết nối server", Toast.LENGTH_SHORT).show()
                                    }
                                })
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Tạo tài khoản", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(
                        onClick = onBackClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Đã có tài khoản? ", color = Color(0xFF7E8CA0), fontSize = 14.sp)
                        Text("Đăng nhập", color = Color(0xFF1565C0), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) Color(0xFFE3F2FD) else Color(0xFFF8FAFC)
    val border = if (selected) Color(0xFF1565C0) else Color(0xFFE2E8F0)
    val textColor = if (selected) Color(0xFF1565C0) else Color(0xFF7E8CA0)

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .border(1.5.dp, border, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = bg
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = textColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = textColor, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = Color(0xFFF8FAFC),
    focusedContainerColor = Color(0xFFF8FAFC),
    unfocusedBorderColor = Color(0xFFE2E8F0),
    focusedBorderColor = Color(0xFF1565C0)
)
