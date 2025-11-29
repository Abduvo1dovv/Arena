package com.example.arena.model

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val marketValue: Double = 0.0,
    val coins: Int = 0,
    val status: String = "ROOKIE",
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val avatarUrl: String = ""
)
