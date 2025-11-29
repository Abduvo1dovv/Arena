package com.example.arena.model

data class Challenge(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val betAmount: Int = 0,
    val rewardAmount: Int = 0,

    val durationHours: Int = 24,
    val status: String = "ACTIVE",
    val startTime: Long = 0,
    val proofUrl: String = "",
    val validVotes: Int = 0,
    val fakeVotes: Int = 0,
    val voters: List<String> = emptyList(),

    val type: String = "SINGLE",
    val totalDays: Int = 1,
    val currentDay: Int = 1,
    val deadlineTime: String = "23:59"
)