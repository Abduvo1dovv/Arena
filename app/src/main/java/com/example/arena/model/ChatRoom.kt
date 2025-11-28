package com.example.arena.model

data class ChatRoom(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val lastSenderId: String = "",
    val isRead: Boolean = true
)