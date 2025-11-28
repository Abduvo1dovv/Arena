package com.example.arena.ui.screens

import android.net.Uri
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.arena.model.Challenge
import com.example.arena.model.Notification
import com.example.arena.model.User
import com.example.arena.ui.theme.ArenaBlack
import com.example.arena.ui.theme.ArenaGreen
import com.example.arena.ui.theme.ArenaRed
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

@Composable
fun FeedScreen(navController: NavController) {
    val context = LocalContext.current
    var feedItems by remember { mutableStateOf<List<Challenge>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedMediaUrl by remember { mutableStateOf<String?>(null) }
    var feedListener by remember { mutableStateOf<ListenerRegistration?>(null) }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // Real-time feed listener
    LaunchedEffect(Unit) {
        if (currentUserId != null) {
            val db = FirebaseFirestore.getInstance()

            feedListener = db.collection("challenges")
                .whereEqualTo("status", "PENDING")
                .limit(20)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        error.printStackTrace()
                        isLoading = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        feedItems = snapshot.toObjects(Challenge::class.java)
                            .filter { !it.voters.contains(currentUserId) } // Ovoz berganlarni filterlash
                            .sortedByDescending { it.startTime }
                    }
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            feedListener?.remove()
        }
    }

    // Ovoz berish logikasi (Optimized)
    fun castVote(challenge: Challenge, isValid: Boolean) {
        if (currentUserId == null) {
            Toast.makeText(context, "LOGIN REQUIRED", Toast.LENGTH_SHORT).show()
            return
        }

        // Double voting oldini olish
        if (challenge.voters.contains(currentUserId)) {
            Toast.makeText(context, "ALREADY VOTED", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()
        val challengeRef = db.collection("challenges").document(challenge.id)

        // Ovozni qo'shish
        val newValidVotes = challenge.validVotes + (if (isValid) 1 else 0)
        val newFakeVotes = challenge.fakeVotes + (if (!isValid) 1 else 0)
        val voteField = if (isValid) "validVotes" else "fakeVotes"

        batch.update(
            challengeRef,
            mapOf(
                voteField to FieldValue.increment(1),
                "voters" to FieldValue.arrayUnion(currentUserId)
            )
        )

        // Threshold (3 ovoz kerak)
        val threshold = 3

        if (newValidVotes >= threshold) {
            // VALID - Pul va marketValue berish
            batch.update(challengeRef, "status", "COMPLETED")

            val ownerRef = db.collection("users").document(challenge.userId)
            batch.update(ownerRef, mapOf(
                "coins" to FieldValue.increment(challenge.rewardAmount.toLong()),
                "marketValue" to FieldValue.increment(15.0)
            ))

            // Notification yaratish
            val notifRef = db.collection("notifications").document()
            batch.set(notifRef, Notification(
                id = notifRef.id,
                userId = challenge.userId,
                senderId = "SYSTEM",
                senderName = "TRIBUNAL",
                type = "INVEST",
                amount = challenge.rewardAmount.toDouble(),
                timestamp = System.currentTimeMillis()
            ))
        } else if (newFakeVotes >= threshold) {
            // FAKE - Pul yo'qotish
            batch.update(challengeRef, "status", "FAILED")

            // Foydalanuvchiga yomon yangilik bildirishnomasi
            val notifRef = db.collection("notifications").document()
            batch.set(notifRef, Notification(
                id = notifRef.id,
                userId = challenge.userId,
                senderId = "SYSTEM",
                senderName = "TRIBUNAL",
                type = "UNFOLLOW",
                amount = 0.0,
                timestamp = System.currentTimeMillis()
            ))
        }

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(context, "VOTE RECORDED!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "ERROR: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    Box(modifier = Modifier.fillMaxSize().background(ArenaBlack)) {
        // Background gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Brush.verticalGradient(colors = listOf(Color(0xFF003300), ArenaBlack)))
        )

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.statusBarsPadding().padding(bottom = 20.dp)
            ) {
                Icon(Icons.Default.Gavel, contentDescription = null, tint = ArenaGreen)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("THE TRIBUNAL", color = Color.White, fontSize = 24.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("JUDGE. EARN. REPEAT.", color = Color.Gray, fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace)
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ArenaGreen)
                }
            } else if (feedItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NO CASES TO JUDGE", color = Color.Gray,
                            fontFamily = FontFamily.Monospace, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Check back later", color = Color.Gray.copy(alpha = 0.5f),
                            fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(feedItems, key = { it.id }) { challenge ->
                        val isMe = challenge.userId == currentUserId

                        FeedCard(
                            challenge = challenge,
                            isMe = isMe,
                            onVote = { isValid -> castVote(challenge, isValid) },
                            onMediaClick = { url -> selectedMediaUrl = url }
                        )
                    }
                }
            }
        }

        // Full screen video player
        if (selectedMediaUrl != null) {
            FullScreenVideoPlayer(
                videoUrl = selectedMediaUrl!!,
                onDismiss = { selectedMediaUrl = null }
            )
        }
    }
}

@Composable
fun FeedCard(
    challenge: Challenge,
    isMe: Boolean,
    onVote: (Boolean) -> Unit,
    onMediaClick: (String) -> Unit
) {
    var user by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(challenge.userId) {
        try {
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("users").document(challenge.userId).get().await()
            user = doc.toObject(User::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // User info
            Row(verticalAlignment = Alignment.CenterVertically) {
                val avatarUrl = "https://api.dicebear.com/9.x/notionists/png?seed=${user?.uid ?: "x"}&backgroundColor=00ff41"
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF222222)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "@${user?.username?.lowercase() ?: "unknown"}",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "submitted proof", color = Color.Gray, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Media preview
            if (challenge.proofUrl.isNotEmpty()) {
                val isVideo = challenge.proofUrl.contains(".mp4") ||
                        challenge.proofUrl.contains("/video/") ||
                        challenge.proofUrl.contains("resource_type/video")

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .clickable { onMediaClick(challenge.proofUrl) }
                ) {
                    // Thumbnail (Cloudinary auto-generates for videos)
                    val thumbnailUrl = if (isVideo) {
                        challenge.proofUrl.replace("/upload/", "/upload/w_600,h_800,c_fill/")
                    } else {
                        challenge.proofUrl
                    }

                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = "Proof",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    if (isVideo) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PlayCircle,
                                contentDescription = "Play",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }

                    // Tap to watch hint
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ZoomIn, contentDescription = null,
                                tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("TAP TO VIEW", color = Color.White, fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color(0xFF050505), RoundedCornerShape(12.dp))
                        .border(1.dp, ArenaRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("EVIDENCE MISSING", color = ArenaRed, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = challenge.title, color = Color.White, fontSize = 16.sp,
                fontWeight = FontWeight.Medium)

            if (challenge.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = challenge.description, color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Voting buttons
            if (isMe) {
                Text("AWAITING VERDICT...", color = ArenaGreen, fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { onVote(false) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF220000)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ArenaRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("FAKE ❌", color = ArenaRed, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onVote(true) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF002200)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ArenaGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("VALID ✅", color = ArenaGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun FullScreenVideoPlayer(videoUrl: String, onDismiss: () -> Unit) {
    val context = LocalContext.current

    // Cloudinary URL optimization
    val optimizedUrl = if (videoUrl.contains("/upload/")) {
        videoUrl.replace("/upload/", "/upload/q_auto,vc_auto/")
    } else {
        videoUrl
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse(optimizedUrl))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            decorFitsSystemWindows = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    }
                },
                modifier = Modifier.fillMaxSize().align(Alignment.Center)
            )

            // Play/Pause on tap
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                    }
            )

            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 24.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }

            // Loading indicator
            var isBuffering by remember { mutableStateOf(true) }

            DisposableEffect(exoPlayer) {
                val listener = object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        isBuffering = playbackState == Player.STATE_BUFFERING
                    }
                }
                exoPlayer.addListener(listener)
                onDispose { exoPlayer.removeListener(listener) }
            }

            if (isBuffering) {
                CircularProgressIndicator(
                    color = ArenaGreen,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}