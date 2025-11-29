package com.example.arena.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.arena.R
import com.example.arena.Screen
import com.example.arena.model.Challenge
import com.example.arena.model.User
import com.example.arena.ui.theme.ArenaBlack
import com.example.arena.ui.theme.ArenaGreen
import com.example.arena.ui.theme.ArenaRed
import com.example.arena.utils.ProofManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var user by remember { mutableStateOf<User?>(null) }
    var challenges by remember { mutableStateOf<List<Challenge>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val marketValueAnim = remember { Animatable(0f) }

    var challengeListener by remember { mutableStateOf<ListenerRegistration?>(null) }
    var userListener by remember { mutableStateOf<ListenerRegistration?>(null) }

    // Notification badge
    var hasUnreadNotifications by remember { mutableStateOf(false) }

    val uid = FirebaseAuth.getInstance().currentUser?.uid

    // Real-time listener
    LaunchedEffect(uid) {
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()

            // User data
            userListener = db.collection("users").document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null && snapshot.exists()) {
                        user = snapshot.toObject(User::class.java)
                        scope.launch {
                            marketValueAnim.animateTo(
                                targetValue = user?.marketValue?.toFloat() ?: 0f,
                                animationSpec = tween(
                                    durationMillis = 1000,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        }
                    }
                }

            // Challenges
            challengeListener = db.collection("challenges")
                .whereEqualTo("userId", uid)
                .whereEqualTo("status", "ACTIVE")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        isLoading = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        challenges = snapshot.toObjects(Challenge::class.java)
                            .sortedBy { it.startTime }
                    }
                    isLoading = false
                }

            // Notifications Badge
            db.collection("notifications").whereEqualTo("userId", uid).whereEqualTo("isRead", false)
                .addSnapshotListener { s, _ -> hasUnreadNotifications = s != null && !s.isEmpty }

        } else {
            isLoading = false
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            challengeListener?.remove()
            userListener?.remove()
        }
    }

    // Submit Proof logic
    fun submitProofToTribunal(challengeId: String, proofUrl: String) {
        if (uid == null) return
        val challenge = challenges.find { it.id == challengeId } ?: return
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()
        val challengeRef = db.collection("challenges").document(challengeId)

        when (challenge.type) {
            "RECURRING" -> {
                val newDay = challenge.currentDay + 1
                if (newDay > challenge.totalDays) {
                    batch.update(
                        challengeRef,
                        mapOf(
                            "status" to "COMPLETED",
                            "proofUrl" to proofUrl,
                            "currentDay" to newDay
                        )
                    )
                    batch.update(
                        db.collection("users").document(uid),
                        mapOf(
                            "coins" to com.google.firebase.firestore.FieldValue.increment(challenge.rewardAmount.toLong()),
                            "marketValue" to com.google.firebase.firestore.FieldValue.increment(
                                challenge.totalDays * 0.5
                            )
                        )
                    )
                } else {
                    batch.update(
                        challengeRef,
                        mapOf(
                            "currentDay" to newDay,
                            "lastProofUrl" to proofUrl,
                            "lastProofTime" to System.currentTimeMillis()
                        )
                    )
                }
            }

            "SINGLE" -> {
                batch.update(challengeRef, mapOf("status" to "PENDING", "proofUrl" to proofUrl))
            }
        }
        batch.commit().addOnSuccessListener {
            // Bu yerdagi Textni ham kelajakda context.getString() bilan olish mumkin, lekin hozircha Toast.
            val msg =
                if (challenge.type == "RECURRING" && challenge.currentDay < challenge.totalDays) "DAY COMPLETED! KEEP GOING!" else "SENT TO TRIBUNAL!"
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && ProofManager.capturedData != null) {
                submitProofToTribunal(
                    ProofManager.capturedData!!.first,
                    ProofManager.capturedData!!.second
                )
                ProofManager.capturedData = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // UI
    Scaffold(
        containerColor = ArenaBlack,
        bottomBar = { /* MainActivity dagi BottomBar */ }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ArenaGreen)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. HEADER
                item {
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Net Worth (Tarjima)
                        Column {
                            Text(
                                text = stringResource(R.string.net_worth), // <--- TARJIMA
                                color = Color.Gray,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$${String.format("%,.2f", marketValueAnim.value)}",
                                color = ArenaGreen,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                letterSpacing = (-1).sp
                            )
                        }

                        // Icons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { navController.navigate(Screen.Inbox.route) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Send,
                                    contentDescription = "Chat",
                                    tint = Color.Green,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            IconButton(onClick = { navController.navigate(Screen.Notifications.route) }) {
                                Box {
                                    Icon(
                                        imageVector = Icons.Rounded.NotificationsNone,
                                        contentDescription = "Notifications",
                                        tint = Color.Green,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    if (hasUnreadNotifications) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(ArenaRed, CircleShape)
                                                .align(Alignment.TopEnd)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. PERFORMANCE TREND (Tarjima)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222222))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.performance_trend), // <--- TARJIMA
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row {
                                        Text(
                                            text = stringResource(R.string.last_30_days),
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        ) // <--- TARJIMA
                                        Text(
                                            text = "+${
                                                String.format(
                                                    "%.1f",
                                                    (user?.marketValue ?: 0.0) / 20
                                                )
                                            }%",
                                            color = ArenaGreen,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val width = size.width
                                val height = size.height

                                val path = Path().apply {
                                    moveTo(0f, height * 0.8f)
                                    cubicTo(
                                        width * 0.1f,
                                        height * 0.4f,
                                        width * 0.2f,
                                        height * 0.4f,
                                        width * 0.3f,
                                        height * 0.7f
                                    )
                                    cubicTo(
                                        width * 0.4f,
                                        height * 0.9f,
                                        width * 0.5f,
                                        height * 0.5f,
                                        width * 0.6f,
                                        height * 0.6f
                                    )
                                    cubicTo(
                                        width * 0.7f,
                                        height * 0.7f,
                                        width * 0.8f,
                                        height * 0.2f,
                                        width * 0.9f,
                                        height * 0.4f
                                    )
                                    lineTo(width, height * 0.3f)
                                }

                                drawPath(
                                    path = path,
                                    color = ArenaGreen,
                                    style = Stroke(width = 8f, cap = StrokeCap.Round)
                                )

                                val fillPath = Path().apply {
                                    addPath(path)
                                    lineTo(width, height)
                                    lineTo(0f, height)
                                    close()
                                }
                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            ArenaGreen.copy(alpha = 0.2f),
                                            Color.Transparent
                                        ),
                                        startY = 0f,
                                        endY = height
                                    )
                                )
                            }
                        }
                    }
                }

                // 3. ACTIVE OPERATIONS (Tarjima)
                item {
                    Text(
                        text = stringResource(R.string.active_operations), // <--- TARJIMA
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (challenges.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.no_active_challenges),
                                color = Color.Gray,
                                fontSize = 14.sp
                            ) // <--- TARJIMA
                        }
                    }
                } else {
                    items(challenges, key = { it.id }) { challenge ->
                        OperationRowItem(
                            challenge = challenge,
                            onClick = {
                                navController.navigate(Screen.ProofCamera.createRoute(challenge.id))
                            }
                        )
                        Divider(color = Color(0xFF222222), thickness = 1.dp)
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
fun OperationRowItem(challenge: Challenge, onClick: () -> Unit) {
    var timeLeftString by remember { mutableStateOf("...") }
    var isUrgent by remember { mutableStateOf(false) }

    LaunchedEffect(challenge) {
        while (true) {
            val now = System.currentTimeMillis()
            var diff: Long = 0

            if (challenge.type == "RECURRING") {
                try {
                    val parts = challenge.deadlineTime.split(":")
                    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 23
                    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 59

                    val cal = Calendar.getInstance()
                    cal.set(Calendar.HOUR_OF_DAY, hour)
                    cal.set(Calendar.MINUTE, minute)
                    cal.set(Calendar.SECOND, 0)

                    if (cal.timeInMillis <= now) {
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    }

                    diff = cal.timeInMillis - now
                } catch (e: Exception) {
                    diff = 0
                }
            } else {
                val endTime = challenge.startTime + (challenge.durationHours * 3600000L)
                diff = endTime - now
            }

            if (diff <= 0) {
                timeLeftString = "OVERDUE"
                isUrgent = true
            } else {
                val hours = diff / (1000 * 60 * 60)
                val minutes = (diff / (1000 * 60)) % 60
                timeLeftString = String.format("%02dh %02dm left", hours, minutes)
                isUrgent = hours < 3
            }
            delay(60000L)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = challenge.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (challenge.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = challenge.description,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (challenge.type == "RECURRING") {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Day ${challenge.currentDay}/${challenge.totalDays}",
                    color = ArenaGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = timeLeftString,
            color = if (isUrgent) ArenaRed else Color.Gray,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isUrgent) FontWeight.Bold else FontWeight.Normal
        )
    }
}