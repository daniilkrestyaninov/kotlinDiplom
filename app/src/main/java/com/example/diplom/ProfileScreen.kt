package com.example.diplom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.diplom.data.ApiClient
import com.example.diplom.data.User
import com.example.diplom.ui.navigation.Routes
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiCream
import com.example.diplom.ui.theme.UmamiOrange
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

    var showFollowers by remember { mutableStateOf(false) }
    var showFollowing by remember { mutableStateOf(false) }
    var followers by remember { mutableStateOf<List<User>>(emptyList()) }
    var following by remember { mutableStateOf<List<User>>(emptyList()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(UmamiCream),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (!isLoggedIn) onLoginClick()
                        else if (user != null) navController.navigate("user_detail/${user.id}")
                    },
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 1.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(UmamiOrange.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (user?.avatarUrl != null) {
                            AsyncImage(
                                model = com.example.diplom.data.normalizeImageUrl(user.avatarUrl),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            androidx.compose.material3.Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = UmamiOrange
                            )
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
                            user?.name ?: user?.username ?: "Войти",
                            fontWeight = FontWeight.Bold,
                            color = UmamiOrange,
                            fontSize = 34.sp,
                            lineHeight = 34.sp,
                            fontFamily = InterFontFamily
                        )
                    }
                    androidx.compose.material3.Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 1.dp,
                color = Color.White
            ) {
                Column {
                    ProfileMenuItem("Избранное") {
                        if (!isLoggedIn) onLoginClick() else navController.navigate(Routes.FAVORITES)
                    }
                    HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 20.dp))

                    ProfileMenuItem("ИИ Шеф") {
                        if (!isLoggedIn) onLoginClick() else navController.navigate(Routes.CHAT)
                    }
                    HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 20.dp))

                    ProfileMenuItem("Подписки") {
                        if (!isLoggedIn) onLoginClick() else {
                            showFollowing = true
                            scope.launch {
                                try {
                                    following = userService.getFollowing(user!!.id)
                                } catch (_: Exception) {
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 20.dp))

                    ProfileMenuItem("Подписчики") {
                        if (!isLoggedIn) onLoginClick() else {
                            showFollowers = true
                            scope.launch {
                                try {
                                    followers = userService.getFollowers(user!!.id)
                                } catch (_: Exception) {
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 20.dp))

                    ProfileMenuItem("Список покупок") { if (!isLoggedIn) onLoginClick() }
                    HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 20.dp))

                    ProfileMenuItem("Настройки") {}
                }
            }
        }
    }

    if (showFollowers) {
        UserListDialog("Подписчики", followers, user?.id, { showFollowers = false }, { _, _ -> })
    }
    if (showFollowing) {
        UserListDialog("Подписки", following, user?.id, { showFollowing = false }, { _, _ -> })
    }
}

