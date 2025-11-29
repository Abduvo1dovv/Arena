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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.arena.R
import com.example.arena.Screen
import com.example.arena.model.Challenge
import com.example.arena.model.User
import com.example.arena.ui.theme.ArenaBlack
import com.example.arena.ui.theme.ArenaGreen
import com.example.arena.ui.theme.ArenaRed
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    var user by remember { mutableStateOf<User?>(null) }
    var myHistory by remember { mutableStateOf<List<Challenge>>(emptyList()) }

    var followersCount by remember { mutableStateOf(0) }
    var followingCount by remember { mutableStateOf(0) }

    var isLoading by remember { mutableStateOf(true) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid


    var showDeleteDialog by remember { mutableStateOf(false) }
    var challengeToDelete by remember { mutableStateOf<Challenge?>(null) }


    LaunchedEffect(Unit) {
        if (currentUserId != null) {
            val db = FirebaseFirestore.getInstance()


            db.collection("users").document(currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null && snapshot.exists()) {
                        val u = snapshot.toObject(User::class.java)
                        user = u
                        followersCount = u?.followersCount ?: 0
                        followingCount = u?.followingCount ?: 0
                    }
                }


            db.collection("challenges")
                .whereEqualTo("userId", currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        myHistory = snapshot.toObjects(Challenge::class.java)
                            .sortedByDescending { it.startTime }
                    }
                    isLoading = false
                }
        }
    }

    fun performLogout() {
        FirebaseAuth.getInstance().signOut()
        navController.navigate(Screen.Login.route) { popUpTo(0) }
    }


    fun deleteChallenge() {
        val challenge = challengeToDelete ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("challenges").document(challenge.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Record Deleted", Toast.LENGTH_SHORT).show()
                showDeleteDialog = false
                challengeToDelete = null
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show()
            }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(ArenaBlack)) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(Brush.verticalGradient(colors = listOf(Color(0xFF004400), ArenaBlack)))
        )

        if (isLoading) {
            CircularProgressIndicator(
                color = ArenaGreen,
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (user != null) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.Gray
                        )
                    }
                }


                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {

                    item(span = { GridItemSpan(2) }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {


                            val avatarUrl = if (user!!.avatarUrl.isNotEmpty()) {
                                user!!.avatarUrl
                            } else {
                                "https://api.dicebear.com/9.x/notionists/png?seed=${user!!.uid}&backgroundColor=00ff41"
                            }

                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black)
                                    .border(2.dp, ArenaGreen.copy(alpha = 0.6f), CircleShape)
                            ) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = user!!.username,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = "@${user!!.username.lowercase().replace(" ", "_")}",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }


                    item(span = { GridItemSpan(2) }) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { navController.navigate(Screen.EditProfile.route) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(45.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFF1A1A1A
                                    )
                                ),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    stringResource(R.string.edit_profile),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Button(
                                onClick = { performLogout() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(45.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFF1A1A1A
                                    )
                                ),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    stringResource(R.string.log_out),
                                    color = ArenaRed,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }


                    item(span = { GridItemSpan(2) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatBox("followers", formatNumber(followersCount)) {
                                if (followersCount > 0) navController.navigate(
                                    Screen.UserList.createRoute(
                                        currentUserId!!,
                                        "followers"
                                    )
                                )
                            }
                            StatBox("following", formatNumber(followingCount)) {
                                if (followingCount > 0) navController.navigate(
                                    Screen.UserList.createRoute(
                                        currentUserId!!,
                                        "following"
                                    )
                                )
                            }
                            StatBox("net_worth", "$${user!!.marketValue.toInt()}", isGreen = true)
                        }
                    }


                    item(span = { GridItemSpan(2) }) {
                        Text(
                            text = stringResource(R.string.challenge_history),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }


                    if (myHistory.isEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                "No history yet.",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        items(myHistory) { challenge ->
                            HistoryCard(
                                challenge = challenge,
                                onLongClick = {
                                    challengeToDelete = challenge
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }


        if (showDeleteDialog && challengeToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = Color(0xFF1A1A1A),
                title = {
                    Text(
                        "DELETE RECORD?",
                        color = ArenaGreen,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                },
                text = {
                    Text(
                        "This challenge will be permanently removed from your history.",
                        color = Color.Gray
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { deleteChallenge() },
                        colors = ButtonDefaults.buttonColors(containerColor = ArenaRed)
                    ) {
                        Text("DELETE", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("CANCEL", color = Color.White)
                    }
                }
            )
        }
    }
}

@Composable
fun RowScope.StatBox(
    resourceIdName: String,
    value: String,
    isGreen: Boolean = false,
    onClick: () -> Unit = {}
) {
    val textColor = if (isGreen) ArenaGreen else Color.White

    val label = when (resourceIdName) {
        "followers" -> stringResource(R.string.followers)
        "following" -> stringResource(R.string.following)
        "net_worth" -> stringResource(R.string.net_worth)
        else -> resourceIdName
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 4.dp)
            .aspectRatio(1.1f)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF111111))
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(20.dp))
            .clickable { onClick() },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = textColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryCard(
    challenge: Challenge,
    onLongClick: () -> Unit
) {
    val isWin = challenge.status == "COMPLETED"
    val badgeColor = if (isWin) ArenaGreen else ArenaRed
    val badgeText = if (isWin) "WIN" else if (challenge.status == "FAILED") "LOSS" else "PENDING"
    val hasImage = challenge.proofUrl.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF111111))
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = { },
                onLongClick = onLongClick
            )
    ) {
        if (hasImage) {

            val imageUrl = if (challenge.proofUrl.endsWith(".mp4")) challenge.proofUrl.replace(
                ".mp4",
                ".jpg"
            ) else challenge.proofUrl

            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.6f
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF222222),
                                Color.Black
                            )
                        )
                    )
            )
        }

        Text(
            text = challenge.title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(badgeColor, RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = badgeText,
                color = Color.Black,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatNumber(count: Int): String {
    return when {
        count >= 1000000 -> String.format("%.1fM", count / 1000000.0)
        count >= 1000 -> String.format("%.1fK", count / 1000.0)
        else -> count.toString()
    }
}