package com.example.arena.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.arena.R
import com.example.arena.model.User
import com.example.arena.ui.theme.ArenaBlack
import com.example.arena.ui.theme.ArenaGreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    // Cloudinary Init
    LaunchedEffect(Unit) {
        try {
            MediaManager.init(context, mapOf("cloud_name" to "dl1vh3hen", "secure" to true))
        } catch (e: Exception) {
        }
    }

    var username by remember { mutableStateOf("") }
    // Hozirgi rasm URL (Firebase dan kelgan)
    var currentAvatarUrl by remember { mutableStateOf("") }
    // Yangi tanlangan rasm URI (Galereyadan)
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Galereyani ochish
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    // User ma'lumotlarini yuklash
    LaunchedEffect(Unit) {
        if (uid != null) {
            isLoading = true
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("users").document(uid).get().await()
            val user = doc.toObject(User::class.java)
            if (user != null) {
                username = user.username
                // Avatar URLni qanday nom bilan saqlaganingizga qarab o'zgartiring.
                // Agar User modelda avatarUrl bo'lmasa, vaqtincha DiceBear ishlatamiz.
                // Lekin bu yerda biz rasm yuklashni qilyapmiz, shuning uchun User modelga `avatarUrl` qo'shish tavsiya etiladi.
                // Hozircha man taxminiy `photoUrl` deb oldim, agar sizda yo'q bo'lsa User modelga qo'shing.
                // Agar User model o'zgarmas bo'lsa, quyidagi qatorni o'zingizga moslang.
                currentAvatarUrl =
                    "https://api.dicebear.com/9.x/notionists/png?seed=${user.uid}&backgroundColor=00ff41"

                // Agar rostdan ham bazada rasm bo'lsa:
                if (doc.contains("avatarUrl")) {
                    currentAvatarUrl = doc.getString("avatarUrl") ?: currentAvatarUrl
                }
            }
            isLoading = false
        }
    }

    // Helper: URI dan File yasash
    fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("avatar", ".jpg", context.cacheDir)
            val outputStream = FileOutputStream(tempFile)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveProfile() {
        if (uid == null) return
        isSaving = true
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)

        if (selectedImageUri != null) {
            // 1. Rasmni faylga aylantirish
            val file = uriToFile(context, selectedImageUri!!)
            if (file != null) {
                // 2. Cloudinaryga yuklash
                MediaManager.get().upload(file.path)
                    .unsigned("arena_upload") // Upload Preset nomi
                    .option("resource_type", "image")
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String?) {}
                        override fun onProgress(
                            requestId: String?,
                            bytes: Long,
                            totalBytes: Long
                        ) {
                        }

                        override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                            val secureUrl = resultData?.get("secure_url") as? String
                            if (secureUrl != null) {
                                // 3. Bazaga yozish (Rasm + Ism)
                                userRef.update(
                                    mapOf(
                                        "username" to username,
                                        "avatarUrl" to secureUrl
                                    )
                                ).addOnSuccessListener {
                                    isSaving = false
                                    Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT)
                                        .show()
                                    navController.popBackStack()
                                }
                            }
                        }

                        override fun onError(requestId: String?, error: ErrorInfo?) {
                            isSaving = false
                            Toast.makeText(context, "Upload Error", Toast.LENGTH_SHORT).show()
                        }

                        override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                    })
                    .dispatch()
            }
        } else {
            // Faqat ismni yangilash
            userRef.update("username", username)
                .addOnSuccessListener {
                    isSaving = false
                    Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }
        }
    }

    // UI
    Box(modifier = Modifier
        .fillMaxSize()
        .background(ArenaBlack)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Brush.verticalGradient(colors = listOf(Color(0xFF003300), ArenaBlack)))
                .align(Alignment.TopCenter)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // HEADER
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = stringResource(R.string.edit_profile),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // AVATAR
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .size(120.dp)
                    .clickable { galleryLauncher.launch("image/*") }
            ) {
                // Rasm ko'rsatish (Yangi tanlangan yoki eski)
                AsyncImage(
                    model = selectedImageUri ?: currentAvatarUrl,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFF222222))
                        .border(2.dp, ArenaGreen, CircleShape),
                    contentScale = ContentScale.Crop
                )

                // Kamera ikonka
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(ArenaGreen, CircleShape)
                        .border(2.dp, ArenaBlack, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = ArenaBlack,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.change_photo), color = ArenaGreen, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(40.dp))

            // USERNAME INPUT
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.username), color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ArenaGreen,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = ArenaGreen,
                    focusedLabelColor = ArenaGreen,
                    unfocusedLabelColor = Color.Gray
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.weight(1f))

            // SAVE BUTTON
            Button(
                onClick = { saveProfile() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ArenaGreen),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = ArenaBlack, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = if (isSaving) stringResource(R.string.saving) else stringResource(R.string.save_changes),
                        color = ArenaBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}