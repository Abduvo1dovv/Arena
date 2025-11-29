package com.example.arena.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.arena.R
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
                trendingUsers =
                    trendSnapshot.toObjects(User::class.java).filter { it.uid != currentUserId }
                isLoading = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun investInUser(targetUser: User) {
        if (currentUserCoins < investmentCost) {
            Toast.makeText(
                context,
                "INSUFFICIENT FUNDS! NEED $investmentCost COINS",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()

        val myRef = db.collection("users").document(currentUserId!!)
        val targetRef = db.collection("users").document(targetUser.uid)

        batch.update(myRef, "coins", currentUserCoins - investmentCost)
        batch.update(targetRef, "marketValue", (targetUser.marketValue + 10.0))

        val notifRef = db.collection("notifications").document()
        batch.set(
            notifRef, Notification(
                id = notifRef.id,
                userId = targetUser.uid,
                senderId = currentUserId,
                senderName = currentUserName,
                type = "INVEST",
                amount = 10.0,
                timestamp = System.currentTimeMillis()
            )
        )

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
                searchResults =
                    snapshot.toObjects(User::class.java).filter { it.uid != currentUserId }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF003300), ArenaBlack)
                    )
                )
        )


        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {

                Text(
                    text = stringResource(R.string.the_market),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))


                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(Color(0xFF111111).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
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
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            cursorBrush = SolidColor(ArenaGreen),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {

                                    Text(
                                        stringResource(R.string.search),
                                        color = Color.Gray,
                                        fontSize = 16.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))


                Text(
                    text = "${stringResource(R.string.capital)}: $currentUserCoins",
                    color = ArenaGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.End)
                )
            }


            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                if (searchQuery.isEmpty()) {

                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                            Text(
                                stringResource(R.string.top_gainers),
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(trendingUsers) { user ->
                                    TrendingCard(user) {
                                        navController.navigate(Screen.UserDetail.createRoute(user.uid))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Text(
                                stringResource(R.string.all_listings),
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }


                    items(trendingUsers) { user ->
                        MarketUserRow(
                            user = user,
                            onInvest = { investInUser(user) },
                            onClick = { navController.navigate(Screen.UserDetail.createRoute(user.uid)) }
                        )
                        Divider(color = Color(0xFF222222), thickness = 0.5.dp)
                    }
                } else {

                    if (isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 50.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = ArenaGreen)
                            }
                        }
                    } else {
                        items(searchResults) { user ->
                            MarketUserRow(
                                user = user,
                                onInvest = { investInUser(user) },
                                onClick = {
                                    navController.navigate(
                                        Screen.UserDetail.createRoute(
                                            user.uid
                                        )
                                    )
                                }
                            )
                            Divider(color = Color(0xFF222222), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun TrendingCard(user: User, onClick: () -> Unit) {
    val avatarUrl =
        "https://api.dicebear.com/9.x/notionists/png?seed=${user.uid}&backgroundColor=00ff41"

    Card(
        modifier = Modifier
            .width(120.dp)
            .height(150.dp)
            .clickable { onClick() }
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF222222)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = user.username.take(10),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = "$${user.marketValue.toInt()}",
                color = ArenaGreen,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun MarketUserRow(user: User, onInvest: () -> Unit, onClick: () -> Unit) {
    val avatarUrl =
        "https://api.dicebear.com/9.x/notionists/png?seed=${user.uid}&backgroundColor=00ff41"
    val percent = (user.marketValue / 100.0 * 5.0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF222222)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = user.username,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "VAL: ",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "$${user.marketValue}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "+${String.format("%.1f", percent)}%",
                color = ArenaGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 12.dp)
            )


            Button(
                onClick = onInvest,
                colors = ButtonDefaults.buttonColors(containerColor = ArenaGreen.copy(alpha = 0.1f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, ArenaGreen),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {

                Text(
                    stringResource(R.string.invest),
                    color = ArenaGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}