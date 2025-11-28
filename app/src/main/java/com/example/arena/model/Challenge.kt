package com.example.arena.model

data class Challenge(
    val id: String = "",
    val userId: String = "",
    val title: String = "",

    // CreateChallengeScreen da ishlatilayotgan yangi maydon
    val description: String = "",

    val betAmount: Int = 0,
    val rewardAmount: Int = 0,

    // Xatolik bermasligi uchun bu maydonni qaytaramiz
    val durationHours: Int = 24,

    val status: String = "ACTIVE",
    val startTime: Long = 0,
    val proofUrl: String = "",
    val validVotes: Int = 0,
    val fakeVotes: Int = 0,
    val voters: List<String> = emptyList(),

    // --- YANGI QO'SHILGAN MAYDONLAR (RECURRING MODE UCHUN) ---
    val type: String = "SINGLE", // "SINGLE" yoki "RECURRING"
    val totalDays: Int = 1,      // Necha kun davom etadi
    val currentDay: Int = 1,     // Hozir nechanchi kunda
    val deadlineTime: String = "23:59" // Tugash vaqti
)