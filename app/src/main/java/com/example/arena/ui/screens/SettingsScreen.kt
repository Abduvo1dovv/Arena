package com.example.arena.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.arena.R
import com.example.arena.Screen
import com.example.arena.ui.theme.ArenaBlack
import com.example.arena.ui.theme.ArenaGreen
import com.example.arena.ui.theme.ArenaRed
import com.example.arena.utils.LocaleHelper
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    var showLanguageDialog by remember { mutableStateOf(false) }

    // Hozirgi tilni bilib olamiz
    val currentLang = LocaleHelper.getCurrentLanguage(context)

    Scaffold(
        containerColor = ArenaBlack,
        topBar = {
            TopAppBar(
                title = {
                    // String resursdan o'qish
                    Text(
                        stringResource(R.string.settings_title),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ArenaBlack)
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            item {
                SettingsSectionTitle("Account")

                SettingsItem(
                    icon = Icons.Outlined.Person,
                    title = stringResource(R.string.edit_profile), // Resursdan o'qish
                    onClick = { /* Edit Profile */ }
                )

                SettingsItem(
                    icon = Icons.Outlined.Language,
                    title = stringResource(R.string.language),
                    subtitle = currentLang, // Hozirgi til
                    onClick = { showLanguageDialog = true }
                )
            }

            item {
                Divider(color = Color(0xFF222222), thickness = 8.dp)
                SettingsSectionTitle("Logins")
                SettingsItem(
                    icon = Icons.Default.Logout,
                    title = stringResource(R.string.log_out),
                    textColor = ArenaRed,
                    iconColor = ArenaRed,
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate(Screen.Login.route) { popUpTo(0) }
                    }
                )
            }
        }
    }

    // TILLAR DIALOGI
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("Select Language", color = Color.White) },
            text = {
                Column {
                    LanguageItem("English", currentLang == "English") {
                        LocaleHelper.setLocale(context, "en")
                        showLanguageDialog = false
                    }
                    LanguageItem("O'zbekcha", currentLang == "O'zbekcha") {
                        LocaleHelper.setLocale(context, "uz")
                        showLanguageDialog = false
                    }
                    LanguageItem("Русский", currentLang == "Русский") {
                        LocaleHelper.setLocale(context, "ru")
                        showLanguageDialog = false
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Cancel", color = ArenaGreen)
                }
            }
        )
    }
}

// Yordamchi komponentlar o'zgarishsiz qoladi...
@Composable
fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        color = Color.Gray,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    textColor: Color = Color.White,
    iconColor: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = textColor, fontSize = 16.sp)
            if (subtitle != null) {
                Text(text = subtitle, color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun LanguageItem(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = ArenaGreen,
                unselectedColor = Color.Gray
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = name, color = Color.White, fontSize = 16.sp)
    }
}