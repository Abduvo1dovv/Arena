package com.example.arena.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.arena.Screen
import com.example.arena.ui.theme.ArenaBlack
import com.example.arena.ui.theme.ArenaGreen

@Composable
fun ArenaBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = ArenaBlack,
        contentColor = ArenaGreen,
        tonalElevation = 0.dp,
        modifier = Modifier
            .height(60.dp)
            .border(
                0.5.dp,
                Color(0xFF222222),
                RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
            )
    ) {

        ArenaNavItem(
            icon = Icons.Default.Home,
            isSelected = currentRoute == Screen.Home.route,
            onClick = { navController.navigate(Screen.Home.route) }
        )


        ArenaNavItem(
            icon = Icons.Default.Search,
            isSelected = currentRoute == Screen.Search.route,
            onClick = { navController.navigate(Screen.Search.route) }
        )

        ArenaNavItem(
            icon = Icons.Rounded.AddBox,
            isSelected = currentRoute == Screen.CreateChallenge.route,
            onClick = { navController.navigate(Screen.CreateChallenge.route) }
        )


        ArenaNavItem(
            icon = Icons.Rounded.Gavel,
            isSelected = currentRoute == Screen.Feed.route,
            onClick = { navController.navigate(Screen.Feed.route) }
        )


        ArenaNavItem(
            icon = Icons.Default.Person,
            isSelected = currentRoute == Screen.Profile.route,
            onClick = { navController.navigate(Screen.Profile.route) }
        )
    }
}

@Composable
fun RowScope.ArenaNavItem(icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = if (isSelected) ArenaGreen else Color.Gray
            )
        },
        selected = isSelected,
        onClick = onClick,
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = Color.Transparent
        )
    )
}