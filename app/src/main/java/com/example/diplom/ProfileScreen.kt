package com.example.diplom

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.diplom.data.*
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiOrange
import com.example.diplom.ui.theme.UmamiCream
import kotlinx.coroutines.launch

@Composable
fun UmamiProfileScreen(
    navController: NavController,
    isLoggedIn: Boolean,
    onLoginClick: () -> Unit,
    user: User?
) {
    val scope = rememberCoroutineScope()
    val userService = ApiClient.userService

    // Profile data from API
    var profileData by remember { mutableStateOf<UserProfile?>(null) }
    var showFollowers by remember { mutableStateOf(false) }
    var showFollowing by remember { mutableStateOf(false) }
    var followers by remember { mutableStateOf<List<User>>(emptyList()) }
    var following by remember { mutableStateOf<List<User>>(emptyList()) }

    // Load profile details if logged in
    LaunchedEffect(isLoggedIn, user) {
        if (isLoggedIn && user != null) {
            try {
                profileData = userService.getUserProfile(user.id)
            } catch (e: Exception) {
                android.util.Log.e("Profile", "Load profile failed", e)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(UmamiCream),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            // Profile Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (!isLoggedIn) onLoginClick()
                    },
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 2.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoggedIn && user?.avatarUrl != null) {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.LightGray)
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(60.dp).clip(CircleShape).background(UmamiOrange.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoggedIn && user != null) {
                                Text(
                                    user.username.firstOrNull()?.uppercase() ?: "?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    color = UmamiOrange,
                                    fontFamily = InterFontFamily
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.size(40.dp),
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Личный профиль",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            fontFamily = InterFontFamily
                        )
                        Text(
                            text = if (isLoggedIn) (user?.name ?: user?.username ?: "Пользователь") else "Войти",
                            color = if (isLoggedIn) Color.Black else UmamiOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            fontFamily = InterFontFamily
                        )
                        if (isLoggedIn && user != null) {
                            Text(
                                "@${user.username}",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontFamily = InterFontFamily
                            )
                        }
                    }
                    
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Go",
                        tint = Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Stats row
        if (isLoggedIn && profileData != null) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 2.dp,
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStat(count = profileData!!.stats?.recipesCount ?: 0, label = "Рецепты")
                        ProfileStat(count = profileData!!.stats?.followersCount ?: 0, label = "Подписчики")
                        ProfileStat(count = profileData!!.stats?.followingCount ?: 0, label = "Подписки")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item {
            // Menu Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 2.dp,
                color = Color.White
            ) {
                Column {
                    ProfileMenuItem("Избранное", Icons.Default.Bookmark) {
                        if (!isLoggedIn) onLoginClick() else navController.navigate("favorites")
                    }
                    HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 20.dp))
                    ProfileMenuItem("Подписки", Icons.Default.PersonAdd) {
                        if (!isLoggedIn) {
                            onLoginClick()
                        } else if (user != null) {
                            showFollowing = true
                            scope.launch {
                                try {
                                    following = userService.getFollowing(user.id)
                                } catch (e: Exception) {
                                    android.util.Log.e("Profile", "Get following failed", e)
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 20.dp))
                    ProfileMenuItem("Подписчики", Icons.Default.People) {
                        if (!isLoggedIn) {
                            onLoginClick()
                        } else if (user != null) {
                            showFollowers = true
                            scope.launch {
                                try {
                                    followers = userService.getFollowers(user.id)
                                } catch (e: Exception) {
                                    android.util.Log.e("Profile", "Get followers failed", e)
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 20.dp))
                    ProfileMenuItem("Настройки", Icons.Default.Settings) {
                        if (!isLoggedIn) onLoginClick()
                    }
                }
            }
        }
    }

    // Followers dialog
    if (showFollowers) {
        UserListDialog(
            title = "Подписчики",
            users = followers,
            currentUserId = user?.id,
            onDismiss = { showFollowers = false },
            onFollowToggle = { targetId, isFollowing ->
                scope.launch {
                    try {
                        if (isFollowing) {
                            userService.unfollow(targetId)
                        } else {
                            userService.follow(targetId)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("Profile", "Follow toggle failed", e)
                    }
                }
            }
        )
    }

    // Following dialog
    if (showFollowing) {
        UserListDialog(
            title = "Подписки",
            users = following,
            currentUserId = user?.id,
            onDismiss = { showFollowing = false },
            onFollowToggle = { targetId, isFollowing ->
                scope.launch {
                    try {
                        if (isFollowing) {
                            userService.unfollow(targetId)
                        } else {
                            userService.follow(targetId)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("Profile", "Follow toggle failed", e)
                    }
                }
            }
        )
    }
}

@Composable
fun ProfileStat(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            count.toString(),
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = UmamiOrange,
            fontFamily = InterFontFamily
        )
        Text(label, color = Color.Gray, fontSize = 12.sp, fontFamily = InterFontFamily)
    }
}

@Composable
fun ProfileMenuItem(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.ChevronRight, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = UmamiOrange, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, fontSize = 16.sp, fontFamily = InterFontFamily)
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.Gray
        )
    }
}

@Composable
fun UserListDialog(
    title: String,
    users: List<User>,
    currentUserId: String?,
    onDismiss: () -> Unit,
    onFollowToggle: (targetId: String, isCurrentlyFollowing: Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "$title (${users.size})",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (users.isEmpty()) {
                Text("Список пуст", fontFamily = InterFontFamily, color = Color.Gray)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(users) { u ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!u.avatarUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = u.avatarUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.LightGray)
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(UmamiOrange.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        u.username.firstOrNull()?.uppercase() ?: "?",
                                        fontWeight = FontWeight.Bold,
                                        color = UmamiOrange,
                                        fontFamily = InterFontFamily
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    u.name ?: u.username,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = InterFontFamily,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "@${u.username}",
                                    color = Color.Gray,
                                    fontFamily = InterFontFamily,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть", color = UmamiOrange)
            }
        }
    )
}
