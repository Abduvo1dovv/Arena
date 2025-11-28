package com.example.arena.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arena.ui.theme.ArenaGreen
import com.example.arena.ui.theme.ArenaBlack

// 1. ARENA INPUT FIELD (Yangilandi: Icon qo'shildi)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArenaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null // Qo'shimcha ikonka (ko'zcha) uchun
) {
    Column {
        Text(
            text = label,
            color = ArenaGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ArenaGreen, RoundedCornerShape(12.dp)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = ArenaGreen,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.Gray
            ),
            shape = RoundedCornerShape(12.dp),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            singleLine = true,
            trailingIcon = trailingIcon // Icon shu yerga tushadi
        )
    }
}

// 2. ARENA BUTTON (Loading holati qo'shildi)
@Composable
fun ArenaButton(
    text: String,
    onClick: () -> Unit,
    isOutline: Boolean = false,
    isLoading: Boolean = false // Loading bormi?
) {
    Button(
        onClick = onClick,
        enabled = !isLoading, // Loading paytida bosib bo'lmaydi
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isOutline) Color.Transparent else ArenaGreen,
            disabledContainerColor = if (isOutline) Color.Transparent else ArenaGreen.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(50),
        border = if (isOutline) androidx.compose.foundation.BorderStroke(1.dp, ArenaGreen) else null
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = if (isOutline) ArenaGreen else ArenaBlack,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                color = if (isOutline) ArenaGreen else ArenaBlack,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}