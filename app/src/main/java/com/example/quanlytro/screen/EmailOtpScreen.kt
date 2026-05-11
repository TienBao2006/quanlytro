package com.example.quanlytro.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quanlytro.OtpResponse
import com.example.quanlytro.RetrofitClient
import com.example.quanlytro.SimpleResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Màn hình xác thực email qua OTP.
 * Gửi OTP đến email → người dùng nhập mã → xác thực xong gọi [onVerified].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailOtpScreen(
    onVerified: (email: String) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) } // 1 = nhập email, 2 = nhập OTP
    var isLoading by remember { mutableStateOf(false) }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
    )

    fun sendOtp() {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(context, "Email không hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }
        isLoading = true
        RetrofitClient.instance.sendOtp(email)
            .enqueue(object : Callback<OtpResponse> {
                override fun onResponse(call: Call<OtpResponse>, response: Response<OtpResponse>) {
                    isLoading = false
                    val body = response.body()
                    if (body?.status == "success") {
                        step = 2
                        Toast.makeText(context, "OTP đã gửi đến $email", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, body?.message ?: "Gửi OTP thất bại", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<OtpResponse>, t: Throwable) {
                    isLoading = false
                    Toast.makeText(context, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    fun verifyOtp() {
        if (otp.length != 6) {
            Toast.makeText(context, "Mã OTP gồm 6 chữ số", Toast.LENGTH_SHORT).show()
            return
        }
        isLoading = true
        RetrofitClient.instance.verifyOtp(email, otp)
            .enqueue(object : Callback<SimpleResponse> {
                override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                    isLoading = false
                    val body = response.body()
                    if (body?.status == "success") {
                        onVerified(email)
                    } else {
                        Toast.makeText(context, body?.message ?: "Mã OTP không đúng", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                    isLoading = false
                    Toast.makeText(context, "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
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
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Email, null, tint = Color.White, modifier = Modifier.size(38.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    if (step == 1) "Xác thực email" else "Nhập mã OTP",
                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    if (step == 1) "Mã OTP sẽ được gửi đến email của bạn"
                    else "Kiểm tra hộp thư của $email",
                    color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }

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
                    modifier = Modifier.padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (step == 1) {
                        Text("Email của bạn", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B34))
                        Text("Nhập email để nhận mã xác thực", fontSize = 13.sp, color = Color(0xFF7E8CA0))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = { Text("example@gmail.com", color = Color(0xFFB0BEC5)) },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Email, null, tint = Color(0xFF42A5F5)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedBorderColor = Color(0xFF1565C0)
                            ),
                            singleLine = true
                        )

                        Button(
                            onClick = { sendOtp() },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            enabled = !isLoading && email.isNotBlank()
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Gửi mã OTP", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text("Mã xác thực", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B34))
                        Text("Nhập mã 6 số đã gửi đến email", fontSize = 13.sp, color = Color(0xFF7E8CA0))

                        OutlinedTextField(
                            value = otp,
                            onValueChange = { if (it.length <= 6) otp = it },
                            placeholder = { Text("• • • • • •", color = Color(0xFFB0BEC5), letterSpacing = 8.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedBorderColor = Color(0xFF1565C0)
                            ),
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 24.sp,
                                letterSpacing = 8.sp,
                                textAlign = TextAlign.Center
                            )
                        )

                        Button(
                            onClick = { verifyOtp() },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            enabled = !isLoading && otp.length == 6
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Xác nhận", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        TextButton(
                            onClick = { otp = ""; sendOtp() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            Text("Gửi lại mã", color = Color(0xFF1565C0), fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
