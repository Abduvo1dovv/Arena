package com.example.arena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.arena.R
import com.example.arena.Screen
import com.example.arena.model.ChatRoom
import com.example.arena.model.User
import com.example.arena.ui.theme.ArenaBlack
import com.example.arena.ui.theme.ArenaGreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ChatItemData(
    val chatRoom: ChatRoom,
    val otherUser: User?
)

@Composable
fun InboxScreen(navController: NavController) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val scope = rememberCoroutineScope()

    var chatList by remember { mutableStateOf<List<ChatItemData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // JONLI KUZATUV
    DisposableEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        if (currentUserId == null) return@DisposableEffect onDispose { }

        val listener = db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val rooms = snapshot.toObjects(ChatRoom::class.java)
                        .sortedByDescending { it.lastMessageTime }

                    scope.launch {
                        val fullList = mutableListOf<ChatItemData>()
                        for (room in rooms) {
                            val otherUserId = room.participants.find { it != currentUserId }
                            if (otherUserId != null) {
                                try {
                                    val userDoc =
                                        db.collection("users").document(otherUserId).get().await()
                                    val otherUser = userDoc.toObject(User::class.java)
                                    fullList.add(ChatItemData(room, otherUser))
                                } catch (ex: Exception) {
                                    fullList.add(ChatItemData(room, null))
                                }
                            }
                        }
                        chatList = fullList
                        isLoading = false
                    }
                } else {
                    isLoading = false
                }
            }
        onDispose { listener.remove() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaBlack)
    ) {
        // 1. ORQA FON (Rasmdagi yashil gradient)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF004400), // To'q yashil (Tepada)
                            ArenaBlack         // Qora (Pastda)
                        )
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // 2. HEADER (Inbox yozuvi va Edit icon)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.size(24.dp)) // O'rtaga olish uchun
                Text(
                    text = stringResource(R.string.inbox_title), // <--- R.string.inbox_title
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.Edit, // Qalam belgisi
                    contentDescription = "New Message",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { navController.navigate(Screen.Search.route) } // Yangi xabar yozish uchun Searchga o'tadi
                )
            }

            // 3. SEARCH BAR (Rasmdagi kabi)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(48.dp)
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                        cursorBrush = SolidColor(ArenaGreen),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    stringResource(R.string.search),
                                    color = Color.Gray,
                                    fontSize = 16.sp
                                ) // <--- R.string.search
                            }
                            innerTextField()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. LIST
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ArenaGreen)
                }
            } else if (chatList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.no_messages),
                        color = Color.Gray
                    ) // <--- R.string.no_messages
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    val filteredList = if (searchQuery.isEmpty()) chatList else chatList.filter {
                        it.otherUser?.username?.contains(searchQuery, ignoreCase = true) == true
                    }

                    items(filteredList) { item ->
                        InboxItem(item, currentUserId!!) {
                            if (item.otherUser != null) {
                                navController.navigate(Screen.Chat.createRoute(item.otherUser.uid))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InboxItem(item: ChatItemData, myId: String, onClick: () -> Unit) {
    val time = if (item.chatRoom.lastMessageTime > 0) {
        // Vaqt formati
        val diff = System.currentTimeMillis() - item.chatRoom.lastMessageTime
        when {
            diff < 60000 -> stringResource(R.string.just_now) // <--- R.string.just_now
            diff < 3600000 -> "${diff / 60000}m"
            diff < 86400000 -> "${diff / 3600000}h"
            else -> "${diff / 86400000}d"
        }
    } else ""

    val isUnread = (item.chatRoom.lastSenderId != myId) && !item.chatRoom.isRead

    val nameColor = Color.White
    val messageColor = if (isUnread) Color.White else Color.Gray
    val fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        val avatarUrl =
            "https://api.dicebear.com/9.x/notionists/png?seed=${item.otherUser?.uid ?: "x"}&backgroundColor=00ff41"
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFF222222)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Text Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.otherUser?.username ?: "Unknown",
                    color = nameColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = time,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.chatRoom.lastMessage,
                color = messageColor,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = fontWeight
            )
        }

        // Unread Indicator (Yashil nuqta)
        if (isUnread) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(ArenaGreen, CircleShape)
            )
        }
    }
}