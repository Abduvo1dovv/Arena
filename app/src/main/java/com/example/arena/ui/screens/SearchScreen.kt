package com.example.arena.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.arena.Screen
import com.example.arena.model.Notification
import com.example.arena.model.User
import com.example.arena.ui.theme.ArenaBlack
import com.example.arena.ui.theme.ArenaGreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

@Composable
fun SearchScreen(navController: NavController) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<User>>(emptyList()) }
    var trendingUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var currentUserName by remember { mutableStateOf("Unknown") }
    var currentUserCoins by remember { mutableIntStateOf(0) }
    val investmentCost = 50

    // MA'LUMOTLARNI YUKLASH
    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        if (currentUserId != null) {
            val userDoc = db.collection("users").document(currentUserId).get().await()
            currentUserName = userDoc.getString("username") ?: "Unknown"
            currentUserCoins = userDoc.getLong("coins")?.toInt() ?: 0

            try {
                val trendSnapshot = db.collection("users")
                    .orderBy("marketValue", Query.Direction.DESCENDING)
                    .limit(10)
                    .get().await()
                trendingUsers = trendSnapshot.toObjects(User::class.java).filter { it.uid != currentUserId }
                isLoading = false
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun investInUser(targetUser: User) {
        if (currentUserCoins < investmentCost) {
            Toast.makeText(context, "INSUFFICIENT FUNDS! NEED $investmentCost COINS", Toast.LENGTH_SHORT).show()
            return
        }
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()

        val myRef = db.collection("users").document(currentUserId!!)
        val targetRef = db.collection("users").document(targetUser.uid)

        batch.update(myRef, "coins", currentUserCoins - investmentCost)
        batch.update(targetRef, "marketValue", (targetUser.marketValue + 10.0))

        val notifRef = db.collection("notifications").document()
        batch.set(notifRef, Notification(
            id = notifRef.id, userId = targetUser.uid, senderId = currentUserId,
            senderName = currentUserName, type = "INVEST", amount = 10.0, timestamp = System.currentTimeMillis()
        ))

        batch.commit().addOnSuccessListener {
            currentUserCoins -= investmentCost
            Toast.makeText(context, "INVESTED $50!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length > 1) {
            isLoading = true
            try {
                val snapshot = FirebaseFirestore.getInstance().collection("users")
                    .whereGreaterThanOrEqualTo("username", searchQuery)
                    .whereLessThanOrEqualTo("username", searchQuery + "\uf8ff")
                    .get().await()
                searchResults = snapshot.toObjects(User::class.java).filter { it.uid != currentUserId }
            } catch (e: Exception) { e.printStackTrace() }
            isLoading = false
        } else {
            searchResults = emptyList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaBlack)
    ) {
        // 1. ORQA FON GRADIENTI (Eng tepadan boshlanadi)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp) // Ekran yarmigacha tushadi
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF003300), // To'q yashil (Tepada)
                            ArenaBlack         // Qora (Pastda)
                        )
                    )
                )
        )

        // 2. ASOSIY CONTENT (Scroll bo'ladigan qism)
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // SEARCH BAR QISMI
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding() // Soatdan pastga tushirish
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Search Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(Color(0xFF111111).copy(alpha = 0.8f), RoundedCornerShape(12.dp)) // Biroz shaffof
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(12.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            cursorBrush = SolidColor(ArenaGreen),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text("Search Gladiators...", color = Color.Gray, fontSize = 16.sp)
                                }
                                innerTextField()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Balans (Search bar ostida kichkina qilib)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "BALANCE: $currentUserCoins COINS",
                    color = ArenaGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            // SCROLLABLE CONTENT (Trending & List)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                if (searchQuery.isEmpty()) {
                    // TRENDING SECTION
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text("Trending", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(16.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(trendingUsers) { user ->
                                    TrendingUserItem(user) {
                                        navController.navigate(Screen.UserDetail.createRoute(user.uid))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                            Text("Top Movers", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // MARKET LIST
                    items(trendingUsers) { user ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            MarketUserRow(
                                user = user,
                                onInvest = { investInUser(user) },
                                onClick = { navController.navigate(Screen.UserDetail.createRoute(user.uid)) }
                            )
                        }
                    }
                } else {
                    // SEARCH RESULTS
                    if (isLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 50.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = ArenaGreen)
                            }
                        }
                    } else {
                        items(searchResults) { user ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                MarketUserRow(
                                    user = user,
                                    onInvest = { investInUser(user) },
                                    onClick = { navController.navigate(Screen.UserDetail.createRoute(user.uid)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- KOMPONENTLAR O'ZGARISHSIZ QOLADI ---
@Composable
fun TrendingUserItem(user: User, onClick: () -> Unit) {
    val avatarUrl = "https://api.dicebear.com/9.x/notionists/png?seed=${user.uid}&backgroundColor=00ff41"
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        AsyncImage(model = avatarUrl, contentDescription = null, modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFF222222)), contentScale = ContentScale.Crop)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = user.username.take(10), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "$${user.marketValue.toInt()}", color = ArenaGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun MarketUserRow(user: User, onInvest: () -> Unit, onClick: () -> Unit) {
    val avatarUrl = "https://api.dicebear.com/9.x/notionists/png?seed=${user.uid}&backgroundColor=00ff41"
    val percent = (1..5).random() + (0..9).random() * 0.1

    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = avatarUrl, contentDescription = null, modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF222222)), contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = user.username, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "$${user.marketValue}", color = Color.Gray, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "+${String.format("%.1f", percent)}%", color = ArenaGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Button(
            onClick = onInvest,
            colors = ButtonDefaults.buttonColors(containerColor = ArenaGreen),
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(text = "Invest", color = ArenaBlack, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}