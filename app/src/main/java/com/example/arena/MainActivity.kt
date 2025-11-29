package com.example.arena

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.arena.ui.components.ArenaBottomBar
import com.example.arena.ui.screens.ChatScreen
import com.example.arena.ui.screens.CreateChallengeScreen
import com.example.arena.ui.screens.EditProfileScreen
import com.example.arena.ui.screens.FeedScreen
import com.example.arena.ui.screens.HomeScreen
import com.example.arena.ui.screens.InboxScreen
import com.example.arena.ui.screens.LeaderboardScreen
import com.example.arena.ui.screens.LoginScreen
import com.example.arena.ui.screens.NotificationsScreen
import com.example.arena.ui.screens.ProfileScreen
import com.example.arena.ui.screens.ProofCameraScreen
import com.example.arena.ui.screens.SearchScreen
import com.example.arena.ui.screens.SettingsScreen
import com.example.arena.ui.screens.SignUpScreen
import com.example.arena.ui.screens.SplashScreen
import com.example.arena.ui.screens.UserDetailScreen
import com.example.arena.ui.screens.UserListScreen
import com.example.arena.ui.theme.ARENATheme
import com.example.arena.ui.theme.ArenaBlack

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        com.example.arena.utils.LocaleHelper.loadLocale(this)
        enableEdgeToEdge()
        setContent {
            ARENATheme {
                val navController = rememberNavController()


                val navBackStackEntry = navController.currentBackStackEntryAsState().value
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomBar = currentRoute in listOf(
                    Screen.Home.route,
                    Screen.Search.route,
                    Screen.Leaderboard.route,
                    Screen.Profile.route,
                    Screen.Feed.route,

                    )

                Scaffold(
                    containerColor = ArenaBlack,

                    contentWindowInsets = WindowInsets.ime,
                    bottomBar = {
                        if (showBottomBar) {
                            ArenaBottomBar(navController = navController)
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Splash.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Splash.route) { SplashScreen(navController) }
                        composable(Screen.Login.route) { LoginScreen(navController) }
                        composable(Screen.Signup.route) { SignUpScreen(navController) }


                        composable(Screen.Home.route) { HomeScreen(navController) }
                        composable(Screen.Search.route) { SearchScreen(navController) }
                        composable(Screen.Feed.route) { FeedScreen(navController) }
                        composable(Screen.Profile.route) { ProfileScreen(navController) }


                        composable(Screen.CreateChallenge.route) {
                            CreateChallengeScreen(
                                navController
                            )
                        }


                        composable(Screen.Leaderboard.route) { LeaderboardScreen(navController) }
                        composable(Screen.Notifications.route) { NotificationsScreen(navController) }
                        composable(Screen.Inbox.route) { InboxScreen(navController) }


                        composable(
                            route = Screen.UserDetail.route,
                            arguments = listOf(navArgument("userId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId")
                            if (userId != null) UserDetailScreen(navController, userId)
                        }

                        composable(
                            route = Screen.ProofCamera.route,
                            arguments = listOf(navArgument("challengeId") {
                                type = NavType.StringType
                            })
                        ) { backStackEntry ->
                            val challengeId =
                                backStackEntry.arguments?.getString("challengeId") ?: ""
                            ProofCameraScreen(
                                navController = navController,
                                challengeId = challengeId,
                                onPhotoCaptured = {}
                            )
                        }

                        composable(
                            route = Screen.UserList.route,
                            arguments = listOf(
                                navArgument("userId") { type = NavType.StringType },
                                navArgument("listType") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId") ?: ""
                            val listType = backStackEntry.arguments?.getString("listType") ?: ""
                            UserListScreen(navController, userId, listType)
                        }

                        composable(
                            route = Screen.Chat.route,
                            arguments = listOf(navArgument("userId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId") ?: ""
                            if (userId.isNotEmpty()) {

                                ChatScreen(navController = navController, targetUserId = userId)
                            }
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(navController)
                        }

                        composable(Screen.EditProfile.route) {
                            EditProfileScreen(navController)
                        }
                    }
                }
            }
        }
    }
}