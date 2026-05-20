package com.example.diplom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.diplom.data.*
import com.example.diplom.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UmamiUserDetailScreen(
    navController: NavController,
    userId: String,
    currentUserId: String?,
    isBlocked: Boolean = false
) {
    val userService = ApiClient.userService
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var profileData by remember { mutableStateOf<UserProfile?>(null) }
    var friends by remember { mutableStateOf<List<User>>(emptyList()) }
    var userRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var isFollowing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    fun loadData() {
        scope.launch {
            try {
                isLoading = true
                loadError = null
                profileData = userService.getUserProfile(userId)
                friends = try { userService.getFollowing(userId) } catch (_: Exception) { emptyList() }
                
                // Используем ТОЛЬКО этот запрос, так как он работал в Кабинете
                userRecipes = try { userService.getUserRecipes(userId) } catch (_: Exception) { emptyList() }
                
                // Check if following
                if (currentUserId != null && currentUserId != userId) {
                    try {
                        val followers = userService.getFollowers(userId)
                        isFollowing = followers.any { it.id == currentUserId }
                    } catch (_: Exception) {}
                }
                
                // Прокидываем статус подписки во все рецепты пользователя
                userRecipes = userRecipes.map { recipe ->
                    recipe.copy(User = recipe.User?.copy(isFollowing = isFollowing))
                }
            } catch (e: Exception) {
                android.util.Log.e("UserDetail", "Load failed", e)
                val msg = e.message ?: ""
                loadError = if (msg.contains("404") || msg.contains("403")) {
                    "Пользователь заблокирован или не найден"
                } else {
                    "Не удалось загрузить профиль"
                }
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(userId) {
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(profileData?.username ?: "Профиль", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (userId != currentUserId && currentUserId != null) {
                        var showReportDialog by remember { mutableStateOf(false) }
                        IconButton(onClick = { showReportDialog = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Жалоба", tint = Color.Gray)
                        }

                        if (showReportDialog) {
                            ReportDialog(
                                onDismiss = { showReportDialog = false },
                                onSubmit = { reason, desc ->
                                    scope.launch {
                                        try {
                                            ApiClient.reportService.createReport(
                                                ReportRequest(type = "user", reportedUserId = userId.toLongOrNull(), reason = reason, description = desc)
                                            )
                                            android.widget.Toast.makeText(context, "Жалоба отправлена", android.widget.Toast.LENGTH_SHORT).show()
                                            showReportDialog = false
                                        } catch (e: Exception) {
                                            android.util.Log.e("UserDetail", "Report failed", e)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            )
        },
        containerColor = UmamiCream
    ) { padding ->
        if (isLoading && profileData == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = UmamiOrange)
            }
        } else if (loadError != null && profileData == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = UmamiOrange,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        loadError ?: "Ошибка",
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Проверьте подключение к интернету", color = Color.Gray, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { loadData() },
                        colors = ButtonDefaults.buttonColors(containerColor = UmamiOrange)
                    ) {
                        Text("Попробовать снова", fontFamily = InterFontFamily)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 1. Cover Banner (Sunset Gradient)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                            .background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(UmamiOrange, Color(0xFFFF9E80))
                                )
                            )
                    )

                    // 2. Overlapping Circular Avatar
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .offset(y = (-45).dp) // Half overlap
                            .size(96.dp)
                            .border(4.dp, Color.White, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileData?.avatarUrl != null) {
                            AsyncImage(
                                model = normalizeImageUrl(profileData?.avatarUrl),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = UmamiOrange,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    // 3. User Info & Stats
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-30).dp)
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Display Name
                        Text(
                            text = profileData?.name ?: profileData?.username ?: "Пользователь",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            fontSize = 24.sp,
                            fontFamily = InterFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        // @username
                        Text(
                            text = "@${profileData?.username ?: "username"}",
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            fontFamily = InterFontFamily
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // 4. Premium Stat Columns (Recipes, Followers, Following)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF9F9F9), RoundedCornerShape(20.dp))
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfileStat(count = profileData?.stats?.recipesCount ?: 0, label = "Рецепты")
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFE5E5E5)))
                            ProfileStat(count = profileData?.stats?.followingCount ?: 0, label = "Подписки")
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFE5E5E5)))
                            ProfileStat(count = profileData?.stats?.followersCount ?: 0, label = "Подписчики")
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 5. Follow / Edit Button
                        if (userId == currentUserId) {
                            Button(
                                onClick = { /* Navigate to Edit Profile */ },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5F5F5)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text(
                                    "Редактировать профиль",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = InterFontFamily,
                                    fontSize = 15.sp
                                )
                            }
                        } else if (currentUserId != null) {
                            Button(
                                onClick = {
                                    if (isBlocked) {
                                        android.widget.Toast.makeText(context, "Действие недоступно: аккаунт заблокирован", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        scope.launch {
                                            try {
                                                if (isFollowing) userService.unfollow(userId)
                                                else userService.follow(userId)
                                                isFollowing = !isFollowing
                                                userRecipes = userRecipes.map { recipe ->
                                                    recipe.copy(User = recipe.User?.copy(isFollowing = isFollowing))
                                                }
                                                // Refresh stats
                                                profileData = userService.getUserProfile(userId)
                                            } catch (e: Exception) {}
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing) Color(0xFFF5F5F5) else UmamiOrange
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text(
                                    if (isFollowing) "Отписаться" else "Подписаться",
                                    color = if (isFollowing) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    fontFamily = InterFontFamily
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 6. Bio Container with Quote style
                        val bioText = if (!profileData?.bio.isNullOrBlank()) {
                            profileData!!.bio!!
                        } else {
                            "Ты не ты когда ты... голоден! 🍫"
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFFFF8F6),
                            shape = RoundedCornerShape(18.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, UmamiOrange.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = UmamiOrange.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = bioText,
                                    color = Color.DarkGray,
                                    fontSize = 14.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
                }

                if (friends.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Подписки", fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = InterFontFamily, modifier = Modifier.padding(bottom = 16.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(friends) { friend ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally, 
                                    modifier = Modifier.width(64.dp).clickable { 
                                        if (friend.id.isNotBlank()) {
                                            navController.navigate("user_detail/${friend.id}")
                                        }
                                    }
                                ) {
                                    Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(Color(0xFFF5F5F5))) {
                                        if (friend.avatarUrl != null) {
                                            AsyncImage(model = normalizeImageUrl(friend.avatarUrl), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        } else {
                                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(30.dp).align(Alignment.Center), tint = Color.LightGray)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(friend.username, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = InterFontFamily)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        if (userId == currentUserId) "Мои рецепты" else "Рецепты пользователя",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        fontFamily = InterFontFamily,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                if (userRecipes.isEmpty() && !isLoading) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.5f)
                        ) {
                            Text(
                                "У пользователя пока нет рецептов",
                                modifier = Modifier.padding(24.dp),
                                textAlign = TextAlign.Center,
                                color = Color.Gray,
                                fontFamily = InterFontFamily
                            )
                        }
                    }
                } else {
                    items(userRecipes) { recipe ->
                        RecipePostCard(recipe, navController, currentUserId)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
