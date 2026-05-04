package com.example.quanlytro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(receiverId: String, receiverName: String, onBackClick: () -> Unit) {
    val senderId = UserSession.uid
    // Load avatar của receiver
    var receiverAvatar by remember { mutableStateOf("") }
    LaunchedEffect(receiverId) {
        RetrofitClient.instance.getUserByUid(receiverId)
            .enqueue(object : Callback<UserLookupResponse> {
                override fun onResponse(call: Call<UserLookupResponse>, response: Response<UserLookupResponse>) {
                    receiverAvatar = response.body()?.avatar ?: ""
                }
                override fun onFailure(call: Call<UserLookupResponse>, t: Throwable) {}
            })
    }

    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var isSending by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Gửi tin chào tự động nếu là lần đầu chat (chủ trọ gửi cho khách)
    // senderId = khách thuê, receiverId = chủ trọ
    LaunchedEffect(senderId, receiverId) {
        if (senderId.isEmpty() || receiverId.isEmpty()) return@LaunchedEffect
        RetrofitClient.instance.sendWelcomeMessage(
            landlordId = receiverId,
            tenantId = senderId
        ).enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {}
            override fun onFailure(call: Call<String>, t: Throwable) {}
        })
    }

    // Polling mỗi 3 giây
    LaunchedEffect(senderId, receiverId) {
        if (senderId.isEmpty()) return@LaunchedEffect
        while (true) {
            RetrofitClient.instance.getMessages(senderId, receiverId)
                .enqueue(object : Callback<List<ChatMessage>> {
                    override fun onResponse(call: Call<List<ChatMessage>>, response: Response<List<ChatMessage>>) {
                        response.body()?.let { messages = it }
                    }
                    override fun onFailure(call: Call<List<ChatMessage>>, t: Throwable) {}
                })
            delay(3000)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    fun sendMessage() {
        val text = messageText.trim()
        if (text.isEmpty() || senderId.isEmpty() || isSending) return
        isSending = true
        messageText = ""

        RetrofitClient.instance.sendMessage(senderId, receiverId, text)
            .enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    isSending = false
                    Log.d("ChatDebug", "Send response: ${response.code()} body=${response.body()}")
                    RetrofitClient.instance.getMessages(senderId, receiverId)
                        .enqueue(object : Callback<List<ChatMessage>> {
                            override fun onResponse(call: Call<List<ChatMessage>>, response: Response<List<ChatMessage>>) {
                                response.body()?.let { messages = it }
                            }
                            override fun onFailure(call: Call<List<ChatMessage>>, t: Throwable) {}
                        })
                }
                override fun onFailure(call: Call<String>, t: Throwable) {
                    isSending = false
                    Log.e("ChatDebug", "Send failed: ${t.message}")
                }
            })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(name = receiverName, avatar = receiverAvatar, size = 36)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(receiverName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Nhắn tin trực tiếp", fontSize = 11.sp, color = Color(0xFF4CAF50))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
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
                .background(Color(0xFFF0F4F8))
        ) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Bắt đầu cuộc trò chuyện", color = Color.Gray, fontSize = 15.sp)
                        Text("với $receiverName", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        MysqlChatBubble(msg, isMe = msg.sender_id == senderId)
                    }
                }
            }

            Surface(tonalElevation = 4.dp, color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Nhập tin nhắn...", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF1F5F9),
                            focusedContainerColor = Color(0xFFF1F5F9),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color(0xFF007BFF)
                        ),
                        maxLines = 4
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { sendMessage() },
                        enabled = messageText.isNotBlank() && !isSending,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (messageText.isNotBlank()) Color(0xFF007BFF) else Color(0xFFE0E0E0),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null)
                    }
                }
            }
        }
    }
}

@Composable
fun MysqlChatBubble(message: ChatMessage, isMe: Boolean) {
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val bgColor = if (isMe) Color(0xFF007BFF) else Color.White
    val contentColor = if (isMe) Color.White else Color(0xFF0D1B34)
    val shape = if (isMe) RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
                else RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)

    val timeStr = try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = sdf.parse(message.created_at)
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date ?: Date())
    } catch (e: Exception) { "" }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            color = bgColor,
            shape = shape,
            shadowElevation = if (isMe) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(message.message, color = contentColor, fontSize = 15.sp, lineHeight = 20.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = timeStr,
                    color = contentColor.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
