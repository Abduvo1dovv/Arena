package com.example.arena.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricAuth(private val context: Context) {

    fun authenticate(onSuccess: () -> Unit, onError: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricManager = BiometricManager.from(context)


        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) {
            onSuccess()
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("ARENA SECURITY")
            .setSubtitle("Scan fingerprint to access the grid")
            .setNegativeButtonText("Use Password")
            .build()

        val biometricPrompt = BiometricPrompt(
            context as FragmentActivity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError()
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }
}