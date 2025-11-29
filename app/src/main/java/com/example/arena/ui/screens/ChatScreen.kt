package com.example.arena.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.arena.R
import com.example.arena.model.Message
import com.example.arena.model.User
import com.example.arena.ui.theme.ArenaBlack
import com.example.arena.ui.theme.ArenaGreen
import com.example.arena.ui.theme.ArenaRed
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(navController: NavController, targetUserId: String) {
    val context = LocalContext.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val chatId =
        if (currentUserId < targetUserId) "${currentUserId}_${targetUserId}" else "${targetUserId}_${currentUserId}"

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var targetUser by remember { mutableStateOf<User?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()


    var showDeleteDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    var messageToDelete by remember { mutableStateOf<Message?>(null) }
    var deleteForEveryone by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }


    LaunchedEffect(targetUserId) {
        val db = FirebaseFirestore.getInstance()
        try {
            targetUser = db.collection("users").document(targetUserId).get().await()
                .toObject(User::class.java)
            val chatRef = db.collection("chats").document(chatId)
            if (chatRef.get().await().exists() && chatRef.get().await()
                    .getString("lastSenderId") != currentUserId
            ) {
                chatRef.update("isRead", true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    DisposableEffect(chatId) {
        val db = FirebaseFirestore.getInstance()
        val listener = db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { s, _ ->
                if (s != null) {
                    messages = s.toObjects(Message::class.java)
                    scope.launch { if (messages.isNotEmpty()) listState.scrollToItem(messages.size - 1) }
                }
            }
        onDispose { listener.remove() }
    }

    fun sendMessage() {
        if (messageText.isBlank()) return
        val text = messageText
        messageText = ""
        val db = FirebaseFirestore.getInstance()
        val chatRef = db.collection("chats").document(chatId)
        val msgRef = chatRef.collection("messages").document()
        val msg = Message(msgRef.id, currentUserId, text, System.currentTimeMillis())
        val batch = db.batch()
        batch.set(msgRef, msg)
        batch.set(
            chatRef, mapOf(
                "id" to chatId, "participants" to listOf(currentUserId, targetUserId),
                "lastMessage" to text, "lastMessageTime" to System.currentTimeMillis(),
                "lastSenderId" to currentUserId, "isRead" to false
            )
        )
        batch.commit()
    }

    fun confirmDeleteMessage() {
        val msg = messageToDelete ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("chats").document(chatId).collection("messages").document(msg.id).delete()
        showDeleteDialog = false
        messageToDelete = null
        deleteForEveryone = false
    }

    fun clearChatHistory() {
        val db = FirebaseFirestore.getInstance()
        val messagesRef = db.collection("chats").document(chatId).collection("messages")
        messagesRef.get().addOnSuccessListener { snapshot ->
            val batch = db.batch()
            for (document in snapshot.documents) {
                batch.delete(document.reference)
            }
            val chatRef = db.collection("chats").document(chatId)
            batch.update(
                chatRef,
                mapOf(
                    "lastMessage" to "History cleared",
                    "lastMessageTime" to System.currentTimeMillis()
                )
            )
            batch.commit()
        }
        showClearHistoryDialog = false
        showMenu = false
    }


    Scaffold(
        containerColor = ArenaBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),

        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF004400),
                                ArenaBlack
                            )
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                        }
                        val avatarUrl =
                            "https://api.dicebear.com/9.x/notionists/png?seed=${targetUser?.uid ?: "x"}&backgroundColor=00ff41"
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF222222)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                targetUser?.username ?: "...",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(ArenaGreen, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    stringResource(R.string.online),
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                "More",
                                tint = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = Color(0xFF1A1A1A),
                            modifier = Modifier.border(
                                1.dp,
                                Color(0xFF333333),
                                RoundedCornerShape(4.dp)
                            )
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.block_user),
                                        color = ArenaRed
                                    )
                                },
                                onClick = {
                                    showMenu = false; Toast.makeText(
                                    context,
                                    "Coming Soon",
                                    Toast.LENGTH_SHORT
                                ).show()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.clear_history),
                                        color = Color.White
                                    )
                                },
                                onClick = { showClearHistoryDialog = true }
                            )
                        }
                    }
                }
            }
        },

        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ArenaBlack)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* Attach */ }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Add, "Add", tint = Color.Gray)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = {
                        Text(
                            stringResource(R.string.type_message),
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp, max = 100.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1A1A1A),
                        unfocusedContainerColor = Color(0xFF1A1A1A),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = ArenaGreen,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { sendMessage() },
                    modifier = Modifier
                        .size(48.dp)
                        .background(ArenaGreen, CircleShape)
                ) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        "Send",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.today),
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
                items(messages) { message ->
                    ChatBubbleNew(
                        message = message,
                        isMe = message.senderId == currentUserId,
                        onLongClick = {
                            messageToDelete = message; showDeleteDialog = true; deleteForEveryone =
                            true
                        })
                }
            }
        }
    }


    if (showDeleteDialog && messageToDelete != null) {
        val isMyMessage = messageToDelete?.senderId == currentUserId
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color(0xFF1A1A1A),
            title = {
                Text(
                    stringResource(R.string.delete_message),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        stringResource(R.string.delete_confirm),
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    if (isMyMessage) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                deleteForEveryone = !deleteForEveryone
                            }) {
                            Checkbox(
                                checked = deleteForEveryone,
                                onCheckedChange = { deleteForEveryone = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = ArenaGreen,
                                    uncheckedColor = Color.Gray
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "${stringResource(R.string.also_delete_for)} ${targetUser?.username ?: "User"}",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { confirmDeleteMessage() },
                    colors = ButtonDefaults.buttonColors(containerColor = ArenaRed)
                ) {
                    Text(
                        stringResource(R.string.delete),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                }) { Text(stringResource(R.string.cancel), color = Color.White) }
            }
        )
    }


    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            containerColor = Color(0xFF1A1A1A),
            title = {
                Text(
                    stringResource(R.string.clear_history),
                    color = ArenaRed,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    stringResource(R.string.clear_history_confirm),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { clearChatHistory() },
                    colors = ButtonDefaults.buttonColors(containerColor = ArenaRed)
                ) {
                    Text(
                        stringResource(R.string.clear_all),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showClearHistoryDialog = false
                }) { Text(stringResource(R.string.cancel), color = Color.White) }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubbleNew(message: Message, isMe: Boolean, onLongClick: () -> Unit) {
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 80.dp, max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 2.dp,
                        bottomEnd = if (isMe) 2.dp else 16.dp
                    )
                )
                .background(if (isMe) ArenaGreen else Color(0xFF2A2A2A))
                .combinedClickable(onClick = {}, onLongClick = onLongClick)
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp)
        ) {
            Text(
                text = message.text,
                color = if (isMe) Color.Black else Color.White,
                fontSize = 15.sp,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            Text(
                text = time,
                color = if (isMe) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 18.dp)
            )
        }
    }
}