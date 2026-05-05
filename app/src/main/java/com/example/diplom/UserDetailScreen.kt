package com.example.diplom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.diplom.data.*
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiCream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UmamiUserDetailScreen(
    navController: NavController,
    userId: String,
    currentUserId: String?
) {
    val userService = ApiClient.userService
    val recipeService = ApiClient.recipeService

    var profileData by remember { mutableStateOf<UserProfile?>(null) }
    var friends by remember { mutableStateOf<List<User>>(emptyList()) }
    var userRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }

    LaunchedEffect(userId) {
        try {
            profileData = userService.getUserProfile(userId)
            friends = userService.getFollowing(userId)
            // If viewing own profile, we might want to see both public and private.
            // Our backend getAll now handles security (shows private only to owner).
            userRecipes = recipeService.getRecipes(userId = userId)
        } catch (e: Exception) {
            android.util.Log.e("UserDetail", "Load failed", e)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        containerColor = UmamiCream
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(Color(0xFFE0E0E0))) {
                            if (profileData?.avatarUrl != null) {
                                AsyncImage(
                                    model = normalizeImageUrl(profileData?.avatarUrl),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            profileData?.name ?: profileData?.username ?: "Загрузка...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            fontFamily = InterFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            ProfileStat(count = profileData?.stats?.recipesCount ?: 0, label = "Рецепты")
                            ProfileStat(count = profileData?.stats?.followingCount ?: 0, label = "Подписки")
                            ProfileStat(count = profileData?.stats?.followersCount ?: 0, label = "Подписчики")
                        }
                        if (userId == currentUserId) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF86947D)),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Редактировать профиль", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Друзья", fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = InterFontFamily, modifier = Modifier.padding(bottom = 16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(friends) { friend ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
                            Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(Color(0xFFE0E0E0))) {
                                if (friend.avatarUrl != null) {
                                    AsyncImage(model = normalizeImageUrl(friend.avatarUrl), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }
                            }
                            Text(friend.username, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = InterFontFamily)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                userRecipes.forEach { recipe ->
                    RecipePostCard(recipe, navController, currentUserId)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
