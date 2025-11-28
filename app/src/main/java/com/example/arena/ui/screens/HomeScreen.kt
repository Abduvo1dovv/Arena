package com.example.arena.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.arena.Screen
import com.example.arena.model.Challenge
import com.example.arena.model.User
import com.example.arena.ui.theme.ArenaBlack
import com.example.arena.ui.theme.ArenaGreen
import com.example.arena.ui.theme.ArenaRed
import com.example.arena.utils.ProofManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    // Ma'lumotlarni yuklash
    LaunchedEffect(uid) {
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(uid).addSnapshotListener { s, _ ->
                if (s != null) { user = s.toObject(User::class.java); scope.launch { marketValueAnim.animateTo(user?.marketValue?.toFloat() ?: 0f, tween(1000)) } }
            }
            db.collection("challenges").whereEqualTo("userId", uid).whereEqualTo("status", "ACTIVE").addSnapshotListener { s, _ ->
                if (s != null) { challenges = s.toObjects(Challenge::class.java).sortedBy { it.startTime } }
                isLoading = false
            }
        } else { isLoading = false }
    }

    // Video yuklangandan keyin ishlaydigan logika
    fun submitProof(challengeId: String, proofUrl: String) {
        if (uid == null) return
        val challenge = challenges.find { it.id == challengeId } ?: return
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()
        val ref = db.collection("challenges").document(challengeId)

        if (challenge.type == "RECURRING") {
            val newDay = challenge.currentDay + 1
            if (newDay > challenge.totalDays) {
                batch.update(ref, mapOf("status" to "COMPLETED", "proofUrl" to proofUrl, "currentDay" to newDay))
                batch.update(db.collection("users").document(uid), mapOf("coins" to com.google.firebase.firestore.FieldValue.increment(challenge.rewardAmount.toLong())))
            } else {
                batch.update(ref, mapOf("currentDay" to newDay, "lastProofUrl" to proofUrl))
            }
        } else {
            batch.update(ref, mapOf("status" to "PENDING", "proofUrl" to proofUrl))
        }
        batch.commit().addOnSuccessListener { android.widget.Toast.makeText(context, "PROOF SENT!", android.widget.Toast.LENGTH_SHORT).show() }
    }

    // Proof Manager Listener
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && ProofManager.capturedData != null) {
                submitProof(ProofManager.capturedData!!.first, ProofManager.capturedData!!.second)
                ProofManager.capturedData = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(containerColor = ArenaBlack, bottomBar = { }) { padding ->
        if (isLoading) Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ArenaGreen) }
        else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Column { Text("Net Worth", color = Color.Gray, fontSize = 14.sp); Spacer(modifier = Modifier.height(4.dp)); Text("$${String.format("%,.2f", marketValueAnim.value)}", color = ArenaGreen, fontSize = 42.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif) }
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth().height(220.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222222))) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Performance Trend", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(20.dp))
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val path = Path().apply { moveTo(0f, size.height*0.8f); cubicTo(size.width*0.5f, size.height*0.5f, size.width*0.8f, size.height*0.2f, size.width, size.height*0.3f) }
                                drawPath(path, ArenaGreen, style = Stroke(width = 8f, cap = StrokeCap.Round))
                                drawPath(Path().apply { addPath(path); lineTo(size.width, size.height); lineTo(0f, size.height); close() }, Brush.verticalGradient(listOf(ArenaGreen.copy(0.2f), Color.Transparent)))
                            }
                        }
                    }
                }
                item { Text("Active Operations", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                if (challenges.isEmpty()) item { Text("No active challenges.", color = Color.Gray) }
                else items(challenges, key = { it.id }) { challenge ->
                    OperationRowItem(challenge) { navController.navigate(Screen.ProofCamera.createRoute(challenge.id)) }
                    Divider(color = Color(0xFF222222))
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
fun OperationRowItem(challenge: Challenge, onClick: () -> Unit) {
    var timeLeftString by remember { mutableStateOf("...") }
    LaunchedEffect(challenge) {
        while (true) {
            val diff = if (challenge.type == "RECURRING") {
                val parts = challenge.deadlineTime.split(":")
                val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, parts[0].toInt()); set(Calendar.MINUTE, parts[1].toInt()); set(Calendar.SECOND, 0) }
                if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis - System.currentTimeMillis()
            } else { (challenge.startTime + (challenge.durationHours * 3600000L)) - System.currentTimeMillis() }

            val hours = diff / 3600000; val minutes = (diff / 60000) % 60
            timeLeftString = if (diff > 0) String.format("%02dh %02dm left", hours, minutes) else "OVERDUE"
            delay(60000)
        }
    }
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(challenge.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (challenge.description.isNotEmpty()) Text(challenge.description, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (challenge.type == "RECURRING") Text("Day ${challenge.currentDay}/${challenge.totalDays}", color = ArenaGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Text(timeLeftString, color = if (timeLeftString == "OVERDUE") ArenaRed else Color.Gray, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
    }
}