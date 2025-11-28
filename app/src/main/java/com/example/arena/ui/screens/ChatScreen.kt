package com.example.arena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.arena.model.Message
import com.example.arena.model.User
import com.example.arena.ui.theme.ArenaBlack
import com.example.arena.ui.theme.ArenaGreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ChatScreen(navController: NavController, targetUserId: String) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val chatId = if (currentUserId < targetUserId) "${currentUserId}_${targetUserId}" else "${targetUserId}_${currentUserId}"

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var targetUser by remember { mutableStateOf<User?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // User Info
    LaunchedEffect(targetUserId) {
        val db = FirebaseFirestore.getInstance()
        val doc = db.collection("users").document(targetUserId).get().await()
        targetUser = doc.toObject(User::class.java)

        // O'qildi qilish
        val chatRef = db.collection("chats").document(chatId)
        val chatSnap = chatRef.get().await()
        if(chatSnap.exists() && chatSnap.getString("lastSenderId") != currentUserId) {
            chatRef.update("isRead", true)
        }
    }

    // Messages Listener
    DisposableEffect(chatId) {
        val db = FirebaseFirestore.getInstance()
        val listener = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    messages = snapshot.toObjects(Message::class.java)
                    scope.launch {
                        if (messages.isNotEmpty()) listState.scrollToItem(messages.size - 1)
                    }
                }
            }
        onDispose { listener.remove() }
    }

    fun sendMessage() {
        if (messageText.isBlank()) return
        val textToSend = messageText
        messageText = ""

        val db = FirebaseFirestore.getInstance()
        val chatRef = db.collection("chats").document(chatId)
        val messageRef = chatRef.collection("messages").document()

        val message = Message(
            id = messageRef.id,
            senderId = currentUserId,
            text = textToSend,
            timestamp = System.currentTimeMillis()
        )

        val batch = db.batch()
        batch.set(messageRef, message)

        val chatRoomData = mapOf(
            "id" to chatId,
            "participants" to listOf(currentUserId, targetUserId),
            "lastMessage" to textToSend,
            "lastMessageTime" to System.currentTimeMillis(),
            "lastSenderId" to currentUserId,
            "isRead" to false
        )
        batch.set(chatRef, chatRoomData)
        batch.commit()
    }

    Scaffold(
        containerColor = ArenaBlack,
        contentWindowInsets = WindowInsets.ime, // Klaviatura joyini hisobga oladi
        topBar = {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Brush.verticalGradient(colors = listOf(Color(0xFF003300), ArenaBlack)))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }

                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF222222)), contentAlignment = Alignment.Center) {
                        Text(text = targetUser?.username?.take(1)?.uppercase() ?: "?", color = ArenaGreen, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = targetUser?.username ?: "Loading...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(text = "Online", color = ArenaGreen, fontSize = 12.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Xabarlar
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { message ->
                    val isMe = message.senderId == currentUserId
                    ChatBubble(message, isMe)
                }
            }

            // Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111111))
                    .padding(12.dp)
                    .navigationBarsPadding(), // Pastki gesture bar uchun
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Message...", color = Color.Gray) },
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF222222),
                        unfocusedContainerColor = Color(0xFF222222),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = ArenaGreen,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { sendMessage() }, modifier = Modifier.size(48.dp).background(ArenaGreen, CircleShape)) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = ArenaBlack)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: Message, isMe: Boolean) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (isMe) 18.dp else 4.dp, bottomEnd = if (isMe) 4.dp else 18.dp))
                .background(if (isMe) ArenaGreen else Color(0xFF333333))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(text = message.text, color = if (isMe) ArenaBlack else Color.White, fontWeight = FontWeight.Normal, fontSize = 15.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}