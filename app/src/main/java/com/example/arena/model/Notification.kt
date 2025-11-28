package com.example.arena.model

data class Notification(
    val id: String = "",
    val userId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val type: String = "INVEST",
    val amount: Double = 0.0,
    val timestamp: Long = 0,
    val isRead: Boolean = false
)