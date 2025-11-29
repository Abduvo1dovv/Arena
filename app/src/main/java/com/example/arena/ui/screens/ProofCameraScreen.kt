package com.example.arena.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.VideoView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.navigation.NavController
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.arena.R
import com.example.arena.ui.theme.ArenaBlack
import com.example.arena.ui.theme.ArenaGreen
import com.example.arena.ui.theme.ArenaRed
import com.example.arena.utils.ProofManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ProofCameraScreen(
    navController: NavController,
    challengeId: String,
    onPhotoCaptured: (File) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 1. Cloudinary Init
    val CLOUD_NAME = "dl1vh3hen"
    val UPLOAD_PRESET = "arena_upload"
    LaunchedEffect(Unit) {
        try {
            MediaManager.init(context, mapOf("cloud_name" to CLOUD_NAME, "secure" to true))
        } catch (e: Exception) {
        }
    }

    // 2. Permissions
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )

    // Avtomatik ruxsat so'rash (faqat bir marta)
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    // State Variables
    var capturedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var capturedVideoFile by remember { mutableStateOf<File?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var uploadStatus by remember { mutableStateOf("UPLOADING VIDEO...") }

    // --- CAMERA ENGINE ---
    val previewView = remember { PreviewView(context) }

    // Video Capture uchun Recorder yaratish (remember ichida, qayta yaratilmasligi uchun)
    val videoCapture = remember {
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        VideoCapture.withOutput(recorder)
    }

    var activeRecording: Recording? by remember { mutableStateOf(null) }

    // Ruxsat berilganda kamerani yoqish
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    // Preview
                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    // Avval eskisini o'chirib, keyin ulaymiz (Crashdan himoya)
                    cameraProvider.unbindAll()

                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        videoCapture
                    )
                } catch (exc: Exception) {
                    Log.e("ArenaCamera", "Binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    // --- VIDEO YOZISH FUNKSIYASI (FIXED) ---
    @SuppressLint("MissingPermission") // <--- BU QATOR QIZIL CHIZIQNI YO'QOTADI
    fun toggleRecording() {
        if (isRecording) {
            // STOP RECORDING
            activeRecording?.stop()
            activeRecording = null
            isRecording = false
        } else {
            // START RECORDING
            val videoFile = File(
                getOutputDirectory(context),
                SimpleDateFormat(
                    "yyyy-MM-dd-HH-mm-ss-SSS",
                    Locale.US
                ).format(System.currentTimeMillis()) + ".mp4"
            )

            // STANDART FileOutputOptions (Import xatosi bo'lmaydi)
            val outputOptions = FileOutputOptions.Builder(videoFile).build()

            // Audio ruxsatini tekshiramiz
            val hasAudio = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PermissionChecker.PERMISSION_GRANTED

            try {
                // PendingRecording yaratish
                var pendingRecording = videoCapture.output
                    .prepareRecording(context, outputOptions)

                // Agar ruxsat bo'lsa audio qo'shamiz
                if (hasAudio) {
                    pendingRecording = pendingRecording.withAudioEnabled()
                }

                // Yozishni boshlash
                activeRecording =
                    pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
                        when (event) {
                            is VideoRecordEvent.Start -> {
                                isRecording = true
                            }

                            is VideoRecordEvent.Finalize -> {
                                if (!event.hasError()) {
                                    capturedVideoUri = Uri.fromFile(videoFile)
                                    capturedVideoFile = videoFile
                                } else {
                                    activeRecording?.close()
                                    isRecording = false
                                    Log.e("ArenaCamera", "Video capture failed: ${event.error}")
                                }
                            }
                        }
                    }
            } catch (e: SecurityException) {
                isRecording = false
                Log.e("ArenaCamera", "Permission error: ${e.message}")
            } catch (e: Exception) {
                isRecording = false
                Log.e("ArenaCamera", "Unknown error: ${e.message}")
            }
        }
    }

    // --- UI ---
    Box(modifier = Modifier
        .fillMaxSize()
        .background(ArenaBlack)) {
        if (capturedVideoUri != null) {
            // --- REVIEW VIDEO ---
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, ArenaGreen, RoundedCornerShape(16.dp))
                ) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(capturedVideoUri)
                                start()
                                setOnCompletionListener { start() }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (isUploading) {
                    CircularProgressIndicator(color = ArenaGreen)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(uploadStatus, color = ArenaGreen, fontWeight = FontWeight.Bold)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { capturedVideoUri = null; capturedVideoFile = null },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ArenaRed.copy(
                                    alpha = 0.2f
                                )
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ArenaRed)
                        ) {
                            Icon(Icons.Default.Close, null, tint = ArenaRed)
                            Text(stringResource(R.string.retake), color = ArenaRed)
                        }

                        Button(
                            onClick = {
                                if (capturedVideoFile != null) {
                                    isUploading = true
                                    MediaManager.get().upload(capturedVideoFile!!.path)
                                        .unsigned(UPLOAD_PRESET)
                                        .option("resource_type", "video")
                                        .callback(object : UploadCallback {
                                            override fun onStart(requestId: String?) {}
                                            override fun onProgress(
                                                requestId: String?,
                                                bytes: Long,
                                                totalBytes: Long
                                            ) {
                                            }

                                            override fun onSuccess(
                                                requestId: String?,
                                                resultData: Map<*, *>?
                                            ) {
                                                val secureUrl =
                                                    resultData?.get("secure_url") as? String
                                                if (secureUrl != null) {
                                                    ProofManager.capturedData =
                                                        Pair(challengeId, secureUrl)
                                                    android.os.Handler(android.os.Looper.getMainLooper())
                                                        .post {
                                                            navController.popBackStack()
                                                        }
                                                }
                                            }

                                            override fun onError(
                                                requestId: String?,
                                                error: ErrorInfo?
                                            ) {
                                                isUploading = false
                                                Log.e("Upload", "Error: ${error?.description}")
                                            }

                                            override fun onReschedule(
                                                requestId: String?,
                                                error: ErrorInfo?
                                            ) {
                                            }
                                        })
                                        .dispatch()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ArenaGreen)
                        ) {
                            Icon(Icons.Default.Check, null, tint = ArenaBlack)
                            Text(
                                stringResource(R.string.submit),
                                color = ArenaBlack,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            // --- CAMERA PREVIEW ---
            if (permissionsState.allPermissionsGranted) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            // PreviewViewni to'g'ri o'lchamda qaytarish
                            previewView.apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 50.dp)
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) Color.Red else Color.White)
                            .border(4.dp, Color.White, CircleShape)
                            .clickable { toggleRecording() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRecording) {
                            Icon(
                                Icons.Rounded.Stop,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 40.dp)
                                .background(ArenaRed, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                stringResource(R.string.rec),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Ruxsat yo'q bo'lsa - Loading
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ArenaBlack),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ArenaGreen)
                }
            }
        }
    }
}

fun getOutputDirectory(context: Context): File {
    val mediaDir =
        context.externalMediaDirs.firstOrNull()?.let { File(it, "ArenaVideos").apply { mkdirs() } }
    return if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
}