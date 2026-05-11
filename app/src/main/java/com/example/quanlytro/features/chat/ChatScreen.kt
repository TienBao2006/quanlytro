package package com.example.quanlytro.features.chat
import com.example.quanlytro.data.remote.*
import com.example.quanlytro.data.model.*

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

const val IMAGE_PREFIX = "[IMAGE]"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(receiverId: String, receiverName: String, onBackClick: () -> Unit) {
    val senderId = UserSession.uid
    val context = LocalContext.current

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

    // Chọn ảnh từ gallery
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@rememberLauncherForActivityResult
            val bytes = inputStream.readBytes()
            inputStream.close()

            // Nén ảnh xuống tối đa 800px và quality 70 để giảm kích thước
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@rememberLauncherForActivityResult
            val maxSize = 800
            val scale = minOf(maxSize.toFloat() / bmp.width, maxSize.toFloat() / bmp.height, 1f)
            val scaled = if (scale < 1f) {
                android.graphics.Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true)
            } else bmp

            val out = ByteArrayOutputStream()
            scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, out)
            val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            val imageMessage = "$IMAGE_PREFIX$base64"

            isSending = true
            RetrofitClient.instance.sendMessage(senderId, receiverId, imageMessage)
                .enqueue(object : Callback<String> {
                    override fun onResponse(call: Call<String>, response: Response<String>) {
                        isSending = false
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
                        Log.e("ChatDebug", "Send image failed: ${t.message}")
                    }
                })
        } catch (e: Exception) {
            Log.e("ChatDebug", "Image encode error: ${e.message}")
        }
    }

    LaunchedEffect(senderId, receiverId) {
        if (senderId.isEmpty() || receiverId.isEmpty()) return@LaunchedEffect
        RetrofitClient.instance.sendWelcomeMessage(landlordId = receiverId, tenantId = senderId)
            .enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {}
                override fun onFailure(call: Call<String>, t: Throwable) {}
            })
    }

    // Polling mỗi 3 giây + đánh dấu đã đọc
    LaunchedEffect(senderId, receiverId) {
        if (senderId.isEmpty()) return@LaunchedEffect
        while (true) {
            RetrofitClient.instance.getMessages(senderId, receiverId)
                .enqueue(object : Callback<List<ChatMessage>> {
                    override fun onResponse(call: Call<List<ChatMessage>>, response: Response<List<ChatMessage>>) {
                        response.body()?.let { newMessages ->
                            val hasUnread = newMessages.any { it.receiver_id == senderId && it.is_read == 0 }
                            if (hasUnread) {
                                RetrofitClient.instance.markMessagesRead(senderId, receiverId)
                                    .enqueue(object : Callback<SimpleResponse> {
                                        override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {}
                                        override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {}
                                    })
                            }
                            messages = newMessages
                        }
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
                    // Nút chọn ảnh
                    IconButton(
                        onClick = { imagePicker.launch("image/*") },
                        enabled = !isSending,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color(0xFFF1F5F9),
                            contentColor = Color(0xFF007BFF)
                        )
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "Gửi ảnh")
                    }
                    Spacer(Modifier.width(6.dp))
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

    val isImage = message.message.startsWith(IMAGE_PREFIX)

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        if (isImage) {
            // Bubble ảnh
            val base64 = message.message.removePrefix(IMAGE_PREFIX)
            val bitmap = remember(base64) {
                try {
                    val bytes = Base64.decode(base64, Base64.NO_WRAP)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (e: Exception) { null }
            }
            var showFullscreen by remember { mutableStateOf(false) }

            // Dialog xem ảnh toàn màn hình
            if (showFullscreen && bitmap != null) {
                Dialog(
                    onDismissRequest = { showFullscreen = false },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        dismissOnClickOutside = true
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.95f))
                            .clickable { showFullscreen = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Ảnh toàn màn hình",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                        IconButton(
                            onClick = { showFullscreen = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color.White)
                        }
                    }
                }
            }

            Column(horizontalAlignment = alignment) {
                Surface(
                    shape = shape,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .widthIn(max = 240.dp)
                        .clickable { showFullscreen = true }
                ) {
                    Box {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Ảnh",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .widthIn(max = 240.dp)
                                    .heightIn(max = 300.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(120.dp).background(Color(0xFFE0E0E0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Image, null, tint = Color.Gray)
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.padding(top = 3.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(timeStr, color = Color.Gray, fontSize = 10.sp)
                    if (isMe) {
                        Icon(
                            imageVector = if (message.is_read == 1) Icons.Default.DoneAll else Icons.Default.Done,
                            contentDescription = null,
                            tint = if (message.is_read == 1) Color(0xFF1565C0) else Color.Gray,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        } else {
            // Bubble text thường
            Surface(
                color = bgColor,
                shape = shape,
                shadowElevation = if (isMe) 0.dp else 1.dp,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(message.message, color = contentColor, fontSize = 15.sp, lineHeight = 20.sp)
                    Spacer(Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(timeStr, color = contentColor.copy(alpha = 0.6f), fontSize = 10.sp)
                        if (isMe) {
                            Icon(
                                imageVector = if (message.is_read == 1) Icons.Default.DoneAll else Icons.Default.Done,
                                contentDescription = null,
                                tint = if (message.is_read == 1) Color(0xFF90CAF9) else contentColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
