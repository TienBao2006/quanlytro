package com.example.quanlytro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    userRole: String = UserSession.role,
    onBackClick: () -> Unit,
    onChatClick: (String, String) -> Unit,
    onHomeClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onManageClick: () -> Unit = {}
) {
    val userId = UserSession.uid

    var conversations by remember { mutableStateOf<List<ChatConversation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // Polling danh sách chat mỗi 5 giây
    LaunchedEffect(userId) {
        if (userId.isEmpty()) { isLoading = false; return@LaunchedEffect }
        while (true) {
            RetrofitClient.instance.getChatList(userId)
                .enqueue(object : Callback<List<ChatConversation>> {
                    override fun onResponse(call: Call<List<ChatConversation>>, response: Response<List<ChatConversation>>) {
                        conversations = response.body() ?: emptyList()
                        isLoading = false
                    }
                    override fun onFailure(call: Call<List<ChatConversation>>, t: Throwable) {
                        isLoading = false
                    }
                })
            delay(5000)
        }
    }

    val filtered = conversations.filter {
        it.other_name.contains(searchQuery, ignoreCase = true) ||
                it.last_message.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tin nhắn", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            BottomNavBar(
                initialSelected = 1,
                userRole = userRole,
                onExploreClick = onHomeClick,
                onProfileClick = onProfileClick,
                onManageClick = onManageClick,
                onChatListClick = {}
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Tìm kiếm tin nhắn...") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFF8F9FA),
                    focusedContainerColor = Color(0xFFF8F9FA),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color(0xFF007BFF)
                )
            )

            when {
                isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF007BFF))
                }
                filtered.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Chat, null, modifier = Modifier.size(72.dp), tint = Color(0xFFE0E0E0))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (searchQuery.isEmpty()) "Chưa có tin nhắn nào" else "Không tìm thấy kết quả",
                            color = Color.Gray, fontSize = 15.sp
                        )
                        if (searchQuery.isEmpty()) {
                            Text(
                                "Nhắn tin với chủ trọ từ trang chi tiết phòng",
                                color = Color.LightGray, fontSize = 13.sp,
                                modifier = Modifier.padding(top = 4.dp, start = 32.dp, end = 32.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                else -> LazyColumn {
                    items(filtered, key = { it.chat_id }) { conv ->
                        ConversationItem(conv) { onChatClick(conv.other_id, conv.other_name) }
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 88.dp, end = 16.dp),
                            thickness = 0.5.dp,
                            color = Color(0xFFF0F0F0)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationItem(conv: ChatConversation, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(name = conv.other_name, avatar = conv.other_avatar, size = 56)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(conv.other_name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0D1B34))
                Text(text = formatMysqlTime(conv.last_time), fontSize = 11.sp, color = Color.Gray)
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = conv.last_message,
                fontSize = 13.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun formatMysqlTime(dateStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = sdf.parse(dateStr) ?: return ""
        val diff = System.currentTimeMillis() - date.time
        when {
            diff < 60_000 -> "Vừa xong"
            diff < 3_600_000 -> "${diff / 60_000} phút"
            diff < 86_400_000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(date)
        }
    } catch (e: Exception) { "" }
}

@Composable
fun UserAvatar(name: String, avatar: String, size: Int = 40) {
    val bmp = remember(avatar) {
        if (avatar.isBlank()) null
        else try {
            val bytes = android.util.Base64.decode(
                if (avatar.contains(",")) avatar.substringAfter(",") else avatar,
                android.util.Base64.DEFAULT
            )
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) { null }
    }
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(Color(0xFFE3F2FD)),
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) {
            androidx.compose.foundation.Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Text(
                text = name.take(1).uppercase(),
                color = Color(0xFF007BFF),
                fontWeight = FontWeight.Bold,
                fontSize = (size * 0.4f).sp
            )
        }
    }
}
