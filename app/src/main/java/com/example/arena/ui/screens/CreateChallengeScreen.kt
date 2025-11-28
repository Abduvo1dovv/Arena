package com.example.arena.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource // <-- MUHIM
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.arena.R // <-- MUHIM
import com.example.arena.model.Challenge
import com.example.arena.ui.components.SlideToConfirmButton
import com.example.arena.ui.theme.ArenaBlack
import com.example.arena.ui.theme.ArenaGreen
import com.example.arena.ui.theme.ArenaRed
import com.example.arena.utils.AlarmReceiver
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChallengeScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var betAmount by remember { mutableFloatStateOf(10f) }
    var isRecurring by remember { mutableStateOf(false) }
    var selectedDurationVal by remember { mutableIntStateOf(24) }
    var selectedTime by remember { mutableStateOf("23:59") }
    var userCoins by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }

    val uid = FirebaseAuth.getInstance().currentUser?.uid

    // Notification Permission Launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted) {
                Toast.makeText(context, "ALERTS DISABLED: RISK OF LOSING MONEY!", Toast.LENGTH_LONG).show()
            }
        }
    )

    LaunchedEffect(uid) {
        if (uid != null) {
            // Android 13+ Notification Permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            try {
                val doc = FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
                userCoins = doc.getLong("coins")?.toInt() ?: 0
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val totalBet = if (isRecurring) (betAmount.toInt() * selectedDurationVal) else betAmount.toInt()
    val potentialReward = totalBet * 2

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour: Int, minute: Int ->
            selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        },
        23, 59, true
    )

    fun scheduleAlarm(context: Context, deadlineTimeStr: String, isDaily: Boolean, challengeTitle: String, challengeId: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val parts = deadlineTimeStr.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 23
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 59

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)

            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            // 1 soat oldin ogohlantirish
            val triggerTime = calendar.timeInMillis - (60 * 60 * 1000)
            val finalTriggerTime = if (triggerTime > System.currentTimeMillis()) triggerTime else System.currentTimeMillis() + 5000

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("TITLE", "⏳ 1 HOUR LEFT!")
                putExtra("MESSAGE", "Upload proof for '$challengeTitle' now or lose $$betAmount!")
                putExtra("CHALLENGE_ID", challengeId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                challengeId.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            if (isDaily) {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, finalTriggerTime, AlarmManager.INTERVAL_DAY, pendingIntent)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finalTriggerTime, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, finalTriggerTime, pendingIntent)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(ArenaBlack)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(350.dp)
                .background(Brush.verticalGradient(colors = listOf(ArenaGreen.copy(alpha = 0.3f), Color.Transparent)))
                .align(Alignment.TopCenter)
        )

        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            // HEADER
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.create_contract), // <--- TARJIMA
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(30.dp))

            // MISSION
            Text(stringResource(R.string.mission_objective), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) // <--- TARJIMA
            Spacer(modifier = Modifier.height(8.dp))
            GlassTextField(value = title, onValueChange = { if (it.length <= 50) title = it }, placeholder = "e.g. 100 Pushups")

            Spacer(modifier = Modifier.height(20.dp))

            // DETAILS
            Text(stringResource(R.string.details), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) // <--- TARJIMA
            Spacer(modifier = Modifier.height(8.dp))
            GlassTextField(value = description, onValueChange = { if (it.length <= 200) description = it }, placeholder = "Optional details...", isSingleLine = false, height = 100.dp)

            Spacer(modifier = Modifier.height(24.dp))

            // MODE TYPE
            Text(stringResource(R.string.mode_type), color = ArenaGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace) // <--- TARJIMA
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModeSelectionButton(stringResource(R.string.one_shot), Icons.Rounded.AccessTime, !isRecurring) { isRecurring = false; selectedDurationVal = 24 } // <--- TARJIMA
                ModeSelectionButton(stringResource(R.string.daily_habit), Icons.Rounded.Repeat, isRecurring) { isRecurring = true; selectedDurationVal = 30 } // <--- TARJIMA
            }

            Spacer(modifier = Modifier.height(24.dp))

            // DEADLINE & DURATION
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.deadline), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) // <--- TARJIMA
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(56.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp)).clickable { timePickerDialog.show() }.padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                        Text(selectedTime, color = Color.White, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    // DAYS or HOURS
                    Text(if (isRecurring) "DAYS" else "HOURS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        if (isRecurring) {
                            DurationChip("7D", 7, selectedDurationVal) { selectedDurationVal = it }
                            DurationChip("30D", 30, selectedDurationVal) { selectedDurationVal = it }
                            DurationChip("60D", 60, selectedDurationVal) { selectedDurationVal = it }
                        } else {
                            DurationChip("24H", 24, selectedDurationVal) { selectedDurationVal = it }
                            DurationChip("3D", 72, selectedDurationVal) { selectedDurationVal = it }
                            DurationChip("1W", 168, selectedDurationVal) { selectedDurationVal = it }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // BET AMOUNT
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                // DAILY STAKE or BET AMOUNT
                val betText = if (isRecurring) stringResource(R.string.daily_stake) else stringResource(R.string.bet_amount) // <--- TARJIMA
                Text(betText, color = Color.White, fontWeight = FontWeight.Bold)

                Text("$${String.format("%.0f", betAmount)}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Slider(value = betAmount, onValueChange = { val multiplier = if (isRecurring) selectedDurationVal else 1; if ((it * multiplier) <= userCoins) betAmount = it }, valueRange = 10f..100f, steps = 9, colors = SliderDefaults.colors(thumbColor = ArenaGreen, activeTrackColor = ArenaGreen, inactiveTrackColor = Color(0xFF333333)))
            Text("Max: $userCoins", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.align(Alignment.End))

            Spacer(modifier = Modifier.height(30.dp))

            // RISKS & REWARDS
            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF111111), RoundedCornerShape(16.dp)).border(1.dp, Color(0xFF222222), RoundedCornerShape(16.dp)).padding(20.dp)) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.total_risk), color = Color.Gray, fontSize = 16.sp) // <--- TARJIMA
                        Text("- $${totalBet}", color = ArenaRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0xFF222222))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.potential_win), color = Color.Gray, fontSize = 16.sp) // <--- TARJIMA
                        Text("+ $${potentialReward}", color = ArenaGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // BUTTON
            Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                SlideToConfirmButton(
                    text = if (isLoading) "CREATING..." else stringResource(R.string.slide_to_initiate), // <--- TARJIMA
                    onConfirm = {
                        if (title.isEmpty() || userCoins < totalBet || uid == null) {
                            Toast.makeText(context, "INVALID DATA / FUNDS", Toast.LENGTH_SHORT).show()
                            return@SlideToConfirmButton
                        }
                        isLoading = true
                        scope.launch {
                            try {
                                val db = FirebaseFirestore.getInstance()
                                val batch = db.batch()
                                val newChallengeRef = db.collection("challenges").document()
                                val challenge = Challenge(
                                    id = newChallengeRef.id, userId = uid, title = title, description = description,
                                    betAmount = totalBet, rewardAmount = potentialReward, status = "ACTIVE", startTime = System.currentTimeMillis(),
                                    type = if (isRecurring) "RECURRING" else "SINGLE", totalDays = if (isRecurring) selectedDurationVal else 1,
                                    durationHours = if (isRecurring) 24 else selectedDurationVal, deadlineTime = selectedTime, currentDay = 1
                                )
                                batch.set(newChallengeRef, challenge)
                                batch.update(db.collection("users").document(uid), "coins", FieldValue.increment(-totalBet.toLong()))
                                batch.commit().await()
                                scheduleAlarm(context, selectedTime, isRecurring, title, newChallengeRef.id)
                                Toast.makeText(context, "CONTRACT LIVE!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            } catch (e: Exception) {
                                isLoading = false
                                Toast.makeText(context, "ERROR: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

// YORDAMCHI KOMPONENTLAR (O'ZGARISHSIZ)
@Composable
fun GlassTextField(value: String, onValueChange: (String) -> Unit, placeholder: String, isSingleLine: Boolean = true, height: Dp = 56.dp) {
    Box(modifier = Modifier.fillMaxWidth().height(height).background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = if (isSingleLine) Alignment.CenterStart else Alignment.TopStart) {
        if (value.isEmpty()) Text(placeholder, color = Color.Gray, fontSize = 14.sp)
        androidx.compose.foundation.text.BasicTextField(value = value, onValueChange = onValueChange, textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp), singleLine = isSingleLine, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun DurationChip(text: String, value: Int, selectedValue: Int, onSelect: (Int) -> Unit) {
    Box(modifier = Modifier.width(60.dp).height(40.dp).clip(RoundedCornerShape(50)).background(Color(0xFF1A1A1A)).border(1.dp, if (value == selectedValue) ArenaGreen else Color(0xFF333333), RoundedCornerShape(50)).clickable { onSelect(value) }, contentAlignment = Alignment.Center) {
        Text(text, color = if (value == selectedValue) ArenaGreen else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun RowScope.ModeSelectionButton(text: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(12.dp)).background(if (isSelected) ArenaGreen.copy(alpha = 0.1f) else Color(0xFF1A1A1A)).border(1.dp, if (isSelected) ArenaGreen else Color(0xFF333333), RoundedCornerShape(12.dp)).clickable { onClick() }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Icon(icon, null, tint = if (isSelected) ArenaGreen else Color.White, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = if (isSelected) ArenaGreen else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}