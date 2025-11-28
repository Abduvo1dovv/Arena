package com.example.arena

sealed class Screen(val route: String) {
    object Splash : Screen("splash_screen")
    object Login : Screen("login_screen")
    object Signup : Screen("signup_screen")
    object Home : Screen("home_screen")
    object CreateChallenge : Screen("create_challenge_screen")



    object Search : Screen("search_screen")
    object Profile : Screen("profile_screen")
    object Notifications : Screen("notifications_screen")
    object Leaderboard : Screen("leaderboard_screen")

    object Inbox : Screen("inbox_screen")
    object Feed : Screen("feed_screen")

    object Chat : Screen("chat_screen/{userId}") {
        fun createRoute(userId: String) = "chat_screen/$userId"
    }

    object UserDetail : Screen("user_detail_screen/{userId}") {
        fun createRoute(userId: String) = "user_detail_screen/$userId"
    }
    object ProofCamera : Screen("proof_camera_screen/{challengeId}") {
        fun createRoute(challengeId: String) = "proof_camera_screen/$challengeId"
    }
    object UserList : Screen("user_list_screen/{userId}/{listType}") {
        fun createRoute(userId: String, listType: String) = "user_list_screen/$userId/$listType"
    }
    object ProofManager {
        var capturedData: Pair<String, String>? = null // (ChallengeID, RasmURL)
    }

}