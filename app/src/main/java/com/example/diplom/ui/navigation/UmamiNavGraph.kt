package com.example.diplom.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.diplom.UmamiMainScreen
import com.example.diplom.UmamiTopBar
import com.example.diplom.UmamiBottomNavigation
import com.example.diplom.AddRecipeFab
import com.example.diplom.SearchScreen
import com.example.diplom.UmamiProfileScreen
import com.example.diplom.UmamiRecipeDetailScreen
import com.example.diplom.UmamiChatScreen
import com.example.diplom.UmamiUserDetailScreen
import com.example.diplom.AuthModal
import com.example.diplom.AddRecipeScreen
import com.example.diplom.UmamiFavoritesScreen
import com.example.diplom.data.TokenManager
import com.example.diplom.data.AuthViewModel
import com.example.diplom.data.AuthState
import androidx.compose.material3.FabPosition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.*
import com.example.diplom.AdminPanelScreen
import com.example.diplom.VerificationScreen

object Routes {
    const val MAIN = "main"
    const val SEARCH = "search"
    const val FAVORITES = "favorites"
    const val PROFILE = "profile"
    const val RECIPE_DETAIL = "recipe_detail/{recipeId}?tab={tab}"
    const val CHAT = "chat"
    const val ADD_RECIPE = "add_recipe?recipeId={recipeId}&draftJson={draftJson}"
    const val PARSE_RECIPE = "parse_recipe"
    const val USER_DETAIL = "user_detail/{userId}"
    const val NOTIFICATIONS = "notifications"
    const val ADMIN_PANEL = "admin_panel"
    const val ADMIN_VERIFICATIONS = "admin_verifications"
    const val ADMIN_USERS = "admin_users"
    const val ADMIN_REPORTS = "admin_reports"
    const val ADMIN_AUDIT_LOGS = "admin_audit_logs"
    
    fun recipeDetail(recipeId: String, tab: String = "") = "recipe_detail/$recipeId?tab=$tab"
    fun userDetail(userId: String) = "user_detail/$userId"
}

@Composable
fun UmamiApp(tokenManager: TokenManager) {
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.provideFactory(tokenManager))
    val authState by authViewModel.state
    val isLoggedIn = authState is AuthState.Success
    val username = if (authState is AuthState.Success) (authState as AuthState.Success).user.username else null
    val currentUserId = if (authState is AuthState.Success) (authState as AuthState.Success).user.id else null
    val avatarUrl = if (authState is AuthState.Success) (authState as AuthState.Success).user.avatarUrl else null

    val notificationViewModel: com.example.diplom.data.NotificationViewModel = viewModel()
    val unreadCount by notificationViewModel.unreadCount.collectAsState()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            notificationViewModel.refreshUnreadCount()
        }
    }

    var showAuthModal by remember { mutableStateOf(false) }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBarAndTopBar = currentRoute in listOf(
        Routes.MAIN, Routes.SEARCH, Routes.FAVORITES, Routes.PROFILE
    )

    Scaffold(
        topBar = {
            if (showBottomBarAndTopBar) {
                UmamiTopBar(
                    isLoggedIn = isLoggedIn,
                    username = username,
                    avatarUrl = avatarUrl,
                    isVerified = if (authState is AuthState.Success) (authState as AuthState.Success).user.isVerified ?: false else false,
                    unreadNotifications = unreadCount,
                    onAuthClick = { showAuthModal = true },
                    onNotificationClick = {
                        if (isLoggedIn) {
                            navController.navigate(Routes.NOTIFICATIONS)
                        } else {
                            showAuthModal = true
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (showBottomBarAndTopBar) {
                UmamiBottomNavigation(
                    currentRoute = currentRoute ?: Routes.MAIN,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            navController.graph.startDestinationRoute?.let { startRoute ->
                                popUpTo(startRoute) { saveState = true }
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (showBottomBarAndTopBar) {
                AddRecipeFab(onClick = {
                    if (isLoggedIn) {
                        navController.navigate(Routes.ADD_RECIPE)
                    } else {
                        showAuthModal = true
                    }
                })
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.MAIN,
            modifier = Modifier.padding(paddingValues),
            enterTransition = { fadeIn(animationSpec = tween(150)) },
            exitTransition = { fadeOut(animationSpec = tween(150)) },
            popEnterTransition = { fadeIn(animationSpec = tween(150)) },
            popExitTransition = { fadeOut(animationSpec = tween(150)) }
        ) {
            composable(Routes.MAIN) {
                UmamiMainScreen(navController = navController, currentUserId = currentUserId)
            }
            composable(Routes.SEARCH) {
                SearchScreen(navController = navController, currentUserId = currentUserId)
            }
            composable(Routes.FAVORITES) {
                if (!isLoggedIn) {
                    com.example.diplom.LoginRequiredScreen(onLoginClick = { showAuthModal = true })
                } else {
                    UmamiFavoritesScreen(navController = navController, currentUserId = currentUserId)
                }
            }
            composable(Routes.PROFILE) {
                UmamiProfileScreen(navController = navController, isLoggedIn = isLoggedIn, onLoginClick = { showAuthModal = true }, user = if (authState is AuthState.Success) (authState as AuthState.Success).user else null, authViewModel = authViewModel)
            }
            composable(
                route = Routes.RECIPE_DETAIL,
                arguments = listOf(
                    navArgument("recipeId") { type = NavType.StringType },
                    navArgument("tab") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getString("recipeId") ?: ""
                val tab = backStackEntry.arguments?.getString("tab") ?: ""
                UmamiRecipeDetailScreen(navController = navController, recipeId = recipeId, initialTab = tab, currentUserId = currentUserId)
            }
            composable(
                route = Routes.ADD_RECIPE,
                arguments = listOf(
                    navArgument("recipeId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("draftJson") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getString("recipeId")
                val draftJson = backStackEntry.arguments?.getString("draftJson")
                AddRecipeScreen(navController = navController, recipeId = recipeId, draftJson = draftJson)
            }
            composable(Routes.CHAT) {
                if (!isLoggedIn) {
                    com.example.diplom.LoginRequiredScreen(onLoginClick = { showAuthModal = true })
                } else {
                    UmamiChatScreen(navController = navController)
                }
            }
            composable(Routes.USER_DETAIL, arguments = listOf(navArgument("userId") { type = NavType.StringType })) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                UmamiUserDetailScreen(navController = navController, userId = userId, currentUserId = currentUserId)
            }
            composable(Routes.PARSE_RECIPE) {
                com.example.diplom.UmamiParseRecipeScreen(navController = navController)
            }
            composable(Routes.NOTIFICATIONS) {
                if (!isLoggedIn) {
                    com.example.diplom.LoginRequiredScreen(onLoginClick = { showAuthModal = true })
                } else {
                    com.example.diplom.NotificationScreen(navController = navController)
                }
            }
            composable(Routes.ADMIN_PANEL) {
                AdminPanelScreen(navController = navController)
            }
            composable(Routes.ADMIN_VERIFICATIONS) {
                VerificationScreen(navController = navController)
            }
            composable(Routes.ADMIN_USERS) {
                com.example.diplom.AdminUsersScreen(navController = navController)
            }
            composable(Routes.ADMIN_REPORTS) {
                com.example.diplom.AdminReportsScreen(navController = navController)
            }
            composable(Routes.ADMIN_AUDIT_LOGS) {
                com.example.diplom.AdminAuditLogsScreen(navController = navController)
            }
        }
        
        if (showAuthModal) {
            AuthModal(
                viewModel = authViewModel,
                onDismiss = { showAuthModal = false },
                onSuccess = { showAuthModal = false }
            )
        }
    }
}
