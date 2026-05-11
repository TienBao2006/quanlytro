package package com.example.quanlytro.features.auth
import com.example.quanlytro.data.remote.*
import com.example.quanlytro.data.model.*

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()

    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    fun register(fullName: String, email: String, phone: String, password: String, role: String, onSuccess: () -> Unit) {
        if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            errorMessage.value = "Vui lòng điền đầy đủ thông tin"
            return
        }
        isLoading.value = true
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: ""
                val user = hashMapOf("uid" to uid, "fullName" to fullName, "email" to email, "phone" to phone, "role" to role)
                db.getReference("users").child(uid).setValue(user)
                    .addOnSuccessListener { isLoading.value = false; onSuccess() }
                    .addOnFailureListener { isLoading.value = false; errorMessage.value = "Lỗi lưu dữ liệu: ${it.message}" }
            }
            .addOnFailureListener { isLoading.value = false; errorMessage.value = "Lỗi đăng ký: ${it.message}" }
    }

    fun login(email: String, password: String, onSuccess: (String) -> Unit) {
        if (email.isEmpty() || password.isEmpty()) {
            errorMessage.value = "Vui lòng nhập email và mật khẩu"
            return
        }
        isLoading.value = true
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: ""
                db.getReference("users").child(uid).get()
                    .addOnSuccessListener { snapshot ->
                        val role = snapshot.child("role").getValue(String::class.java) ?: "Người thuê"
                        isLoading.value = false
                        onSuccess(role)
                    }
                    .addOnFailureListener { isLoading.value = false; errorMessage.value = "Không thể lấy thông tin người dùng" }
            }
            .addOnFailureListener { isLoading.value = false; errorMessage.value = "Email hoặc mật khẩu không đúng" }
    }
}
