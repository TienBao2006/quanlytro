package package com.example.quanlytro.features.auth
import com.example.quanlytro.data.remote.*
import com.example.quanlytro.data.model.*

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

/**
 * Màn hình xác thực số điện thoại qua Firebase Phone Auth (SMS OTP).
 * Sau khi xác thực thành công, gọi [onVerified] với số điện thoại đã xác thực.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAuthScreen(
    onVerified: (phone: String) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val auth = remember { FirebaseAuth.getInstance() }

    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var step by remember { mutableStateOf(1) } // 1 = nhập SĐT, 2 = nhập OTP

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
    )

    // Callback Firebase
    val callbacks = remember {
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto-retrieval hoặc instant verify (thiết bị test)
                isLoading = true
                auth.signInWithCredential(credential)
                    .addOnSuccessListener {
                        isLoading = false
                        val formattedPhone = formatPhoneForDisplay(phone)
                        onVerified(formattedPhone)
                    }
                    .addOnFailureListener { e ->
                        isLoading = false
                        Toast.makeText(context, "Lỗi xác thực: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                isLoading = false
                Toast.makeText(context, "Gửi OTP thất bại: ${e.message}", Toast.LENGTH_LONG).show()
            }

            override fun onCodeSent(
                vId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                isLoading = false
                verificationId = vId
                resendToken = token
                step = 2
                Toast.makeText(context, "Mã OTP đã gửi đến $phone", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun sendOtp() {
        val e164 = toE164(phone)
        if (e164 == null) {
            Toast.makeText(context, "Số điện thoại không hợp lệ (VD: 0912345678)", Toast.LENGTH_SHORT).show()
            return
        }
        // Log để kiểm tra số E.164 thực sự gửi đi
        android.util.Log.d("PhoneAuth", "Sending OTP to: $e164")
        Toast.makeText(context, "Đang gửi đến: $e164", Toast.LENGTH_SHORT).show()
        isLoading = true
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(e164)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .apply { resendToken?.let { setForceResendingToken(it) } }
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp() {
        val vid = verificationId
        if (vid == null) {
            Toast.makeText(context, "Vui lòng gửi OTP trước", Toast.LENGTH_SHORT).show()
            return
        }
        if (otp.length != 6) {
            Toast.makeText(context, "Mã OTP gồm 6 chữ số", Toast.LENGTH_SHORT).show()
            return
        }
        isLoading = true
        val credential = PhoneAuthProvider.getCredential(vid, otp)
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                isLoading = false
                onVerified(formatPhoneForDisplay(phone))
            }
            .addOnFailureListener { e ->
                isLoading = false
                Toast.makeText(context, "Mã OTP không đúng hoặc đã hết hạn", Toast.LENGTH_SHORT).show()
            }
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
                    Icon(Icons.Default.Phone, null, tint = Color.White, modifier = Modifier.size(38.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    if (step == 1) "Xác thực số điện thoại" else "Nhập mã OTP",
                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    if (step == 1) "Firebase sẽ gửi SMS đến số của bạn"
                    else "Mã 6 số đã gửi đến $phone",
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
                        // Bước 1: nhập SĐT
                        Text("Số điện thoại", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B34))
                        Text(
                            "Nhập số điện thoại Việt Nam (bắt đầu bằng 0)",
                            fontSize = 13.sp, color = Color(0xFF7E8CA0)
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            placeholder = { Text("VD: 0912345678", color = Color(0xFFB0BEC5)) },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Phone, null, tint = Color(0xFF42A5F5)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedBorderColor = Color(0xFF1565C0)
                            ),
                            singleLine = true
                        )
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = { sendOtp() },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            enabled = !isLoading && phone.isNotBlank()
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Gửi mã OTP", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Bước 2: nhập OTP
                        Text("Mã xác thực", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B34))
                        Text(
                            "Nhập mã 6 số Firebase đã gửi qua SMS",
                            fontSize = 13.sp, color = Color(0xFF7E8CA0)
                        )
                        OutlinedTextField(
                            value = otp,
                            onValueChange = { if (it.length <= 6) otp = it },
                            placeholder = { Text("______", color = Color(0xFFB0BEC5), letterSpacing = 8.sp) },
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
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 24.sp,
                                letterSpacing = 8.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                        Spacer(Modifier.height(4.dp))
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
                            onClick = { sendOtp() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            Text("Gửi lại mã OTP", color = Color(0xFF1565C0), fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

/** Chuyển SĐT Việt Nam sang định dạng E.164 (+84...) */
private fun toE164(phone: String): String? {
    val digits = phone.trim().filter { it.isDigit() }
    return when {
        digits.startsWith("0") && digits.length == 10 -> "+84${digits.drop(1)}"
        digits.startsWith("84") && digits.length == 11 -> "+$digits"
        digits.startsWith("+84") -> digits
        else -> null
    }
}

/** Trả về SĐT dạng 0xxxxxxxxx để lưu DB */
private fun formatPhoneForDisplay(phone: String): String {
    val digits = phone.trim().filter { it.isDigit() }
    return when {
        digits.startsWith("84") && digits.length == 11 -> "0${digits.drop(2)}"
        else -> phone.trim()
    }
}
