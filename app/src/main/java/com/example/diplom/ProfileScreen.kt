package com.example.diplom

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.diplom.data.*
import com.example.diplom.ui.navigation.Routes
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiCream
import com.example.diplom.ui.theme.UmamiOrange
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UmamiProfileScreen(
    navController: NavController,
    isLoggedIn: Boolean,
    onLoginClick: () -> Unit,
    user: User?,
    authViewModel: AuthViewModel? = null
) {
    val scope = rememberCoroutineScope()
    val userService = ApiClient.userService
    val context = LocalContext.current
    val db = remember { com.example.diplom.data.local.UmamiDatabase.getDatabase(context) }
    val dao = remember { db.dao() }
    val gson = remember { com.google.gson.Gson() }

    var profileData by remember { mutableStateOf<UserProfile?>(null) }
    var myRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var isLoadingRecipes by remember { mutableStateOf(false) }

    // Edit profile state
    var showEditDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Followers/following
    var showFollowers by remember { mutableStateOf(false) }
    var showFollowing by remember { mutableStateOf(false) }
    var followers by remember { mutableStateOf<List<User>>(emptyList()) }
    var following by remember { mutableStateOf<List<User>>(emptyList()) }
    
    // Verification
    var showVerificationDialog by remember { mutableStateOf(false) }
    var showAppealDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var verificationRequest by remember { mutableStateOf<VerificationRequest?>(null) }
    var showPendingInfoDialog by remember { mutableStateOf(false) }
    var showRejectionDetailsDialog by remember { mutableStateOf(false) }

    // Avatar upload
    var currentAvatarUrl by remember(user?.avatarUrl) { mutableStateOf(user?.avatarUrl) }

    val avatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                    val bytes = inputStream.readBytes()
                    inputStream.close()

                    val requestFile = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                    val imagePart = MultipartBody.Part.createFormData("image", "avatar.jpg", requestFile)
                    val folderPart = "avatars".toRequestBody("text/plain".toMediaTypeOrNull())

                    val uploadResult = ApiClient.recipeService.uploadImage(imagePart, folderPart)
                    val newAvatarUrl = uploadResult.url

                    // Update profile with new avatar URL
                    userService.updateProfile(UpdateProfileRequest(avatarUrl = newAvatarUrl))
                    currentAvatarUrl = newAvatarUrl
                    authViewModel?.refreshProfile()

                    // Refresh profile data
                    if (user != null) {
                        profileData = try { userService.getUserProfile(user.id) } catch (_: Exception) { profileData }
                    }

                    android.widget.Toast.makeText(context, "Аватар обновлён", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    android.util.Log.e("Profile", "Avatar upload failed", e)
                    android.widget.Toast.makeText(context, "Ошибка загрузки аватара", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Load profile data
    LaunchedEffect(user?.id) {
        if (user != null) {
            // 1. Load cached profile & recipes first
            try {
                val cached = dao.getUserAccount()
                if (cached != null) {
                    if (!cached.profileJson.isNullOrBlank()) {
                        val cachedProfile = gson.fromJson(cached.profileJson, UserProfile::class.java)
                        profileData = cachedProfile
                        if (cachedProfile.recipes != null) {
                            myRecipes = cachedProfile.recipes
                        }
                    }
                }
                
                // Also load cached individual personal recipes if list is empty
                if (myRecipes.isEmpty()) {
                    val cachedMyRecipes = dao.getCachedMyRecipes()
                    if (cachedMyRecipes.isNotEmpty()) {
                        myRecipes = cachedMyRecipes.map {
                            gson.fromJson(it.recipeJson, Recipe::class.java)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileScreen", "Failed to load cached profile/recipes", e)
            }

            // 2. Fetch fresh profile data
            try {
                val freshProfile = userService.getUserProfile(user.id)
                profileData = freshProfile
                
                // Cache full UserProfile in user_account
                try {
                    val currentCached = dao.getUserAccount()
                    if (currentCached != null) {
                        dao.saveUserAccount(
                            currentCached.copy(
                                profileJson = gson.toJson(freshProfile),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    } else {
                        dao.saveUserAccount(
                            com.example.diplom.data.local.LocalUserAccount(
                                id = user.id,
                                userJson = gson.toJson(user),
                                profileJson = gson.toJson(freshProfile)
                            )
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ProfileScreen", "Failed to cache UserProfile", e)
                }
            } catch (_: Exception) {}

            // Fetch verification request
            try {
                verificationRequest = userService.getMyVerificationRequest()
            } catch (_: Exception) {}

            // Use recipes from profile data if available
            if (profileData?.recipes != null) {
                myRecipes = profileData!!.recipes!!
                // Cache my recipes
                try {
                    dao.clearMyRecipesCache()
                    dao.insertMyRecipes(myRecipes.map { r ->
                        com.example.diplom.data.local.CachedMyRecipe(
                            id = r.id,
                            recipeJson = gson.toJson(r)
                        )
                    })
                } catch (e: Exception) {
                    android.util.Log.e("ProfileScreen", "Failed to cache my recipes", e)
                }
            } else {
                // Fallback
                isLoadingRecipes = true
                try {
                    val fetched = userService.getUserRecipes(user.id)
                    myRecipes = fetched
                    // Cache my recipes
                    try {
                        dao.clearMyRecipesCache()
                        dao.insertMyRecipes(myRecipes.map { r ->
                            com.example.diplom.data.local.CachedMyRecipe(
                                id = r.id,
                                recipeJson = gson.toJson(r)
                            )
                        })
                    } catch (e: Exception) {
                        android.util.Log.e("ProfileScreen", "Failed to cache my recipes", e)
                    }
                } catch (_: Exception) {}
                isLoadingRecipes = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(UmamiCream),
        contentPadding = PaddingValues(16.dp)
    ) {
        // Premium Profile Card
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
                    ) {
                        // Quick Action Buttons (Edit Profile & Logout) in the top-right corner of the banner
                        if (isLoggedIn && user != null) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 16.dp, end = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Edit Profile Box Button
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color.White.copy(alpha = 0.25f), CircleShape)
                                        .clickable { showEditDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Редактировать",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                // Logout Box Button
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color.White.copy(alpha = 0.25f), CircleShape)
                                        .clickable { showLogoutDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Logout,
                                        contentDescription = "Выйти",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 2. Overlapping Circular Avatar
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .offset(y = (-45).dp) // Half overlap
                            .size(96.dp)
                            .border(4.dp, Color.White, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .then(
                                if (isLoggedIn) Modifier.clickable { avatarLauncher.launch("image/*") }
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val avatarToShow = currentAvatarUrl ?: user?.avatarUrl
                        if (avatarToShow != null) {
                            AsyncImage(
                                model = normalizeImageUrl(avatarToShow),
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
                        // Camera upload overlay
                        if (isLoggedIn) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Изменить фото",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // 3. User Info (Name & Username)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-30).dp)
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Display Name
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = user?.name ?: user?.username ?: "Личный профиль",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black,
                                fontSize = 24.sp,
                                fontFamily = InterFontFamily
                            )
                            if (user?.isVerified == true || profileData?.isVerified == true) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Верифицирован",
                                    tint = Color(0xFF2196F3),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        
                        // @username
                        if (isLoggedIn && user != null) {
                            Text(
                                text = "@${user.username}",
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                fontFamily = InterFontFamily
                            )
                        } else {
                            Text(
                                text = "Войдите в аккаунт, чтобы сохранять и делиться рецептами",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                fontFamily = InterFontFamily,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // Blocked notification inside the card if user is blocked
                        if (isLoggedIn && user?.isBlocked == true) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFFFFEBEE),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Block, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Ваш аккаунт заблокирован", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Text(
                                        "Взаимодействие с контентом ограничено. Вы можете оспорить блокировку.",
                                        fontSize = 12.sp,
                                        color = Color.DarkGray,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    TextButton(
                                        onClick = { showAppealDialog = true },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Оспорить", color = Color.Red, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 4. Premium Stat Columns (Recipes, Followers, Following)
                        if (isLoggedIn && user != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF9F9F9), RoundedCornerShape(20.dp))
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Stat item: Recipes
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        navController.navigate("user_detail/${user.id}")
                                    }
                                ) {
                                    Text(
                                        text = "${profileData?.stats?.recipesCount ?: 0}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = UmamiOrange,
                                        fontFamily = InterFontFamily
                                    )
                                    Text(
                                        text = "Рецепты",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        fontFamily = InterFontFamily
                                    )
                                }
                                
                                // Vertical divider
                                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFE5E5E5)))

                                // Stat item: Following (подп.)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        showFollowers = true
                                        scope.launch {
                                            try { followers = userService.getFollowers(user.id) } catch (_: Exception) {}
                                        }
                                    }
                                ) {
                                    Text(
                                        text = "${profileData?.stats?.followersCount ?: 0}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = UmamiOrange,
                                        fontFamily = InterFontFamily
                                    )
                                    Text(
                                        text = "Подписки",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        fontFamily = InterFontFamily
                                    )
                                }

                                // Vertical divider
                                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFE5E5E5)))

                                // Stat item: Followers (подп-к)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        showFollowing = true
                                        scope.launch {
                                            try { following = userService.getFollowing(user.id) } catch (_: Exception) {}
                                        }
                                    }
                                ) {
                                    Text(
                                        text = "${profileData?.stats?.followingCount ?: 0}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = UmamiOrange,
                                        fontFamily = InterFontFamily
                                    )
                                    Text(
                                        text = "Подписчики",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        fontFamily = InterFontFamily
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // 5. Bio Container with Quote style
                        val bioText = if (isLoggedIn && !profileData?.bio.isNullOrBlank()) {
                            profileData!!.bio!!
                        } else if (isLoggedIn) {
                            "Ты не ты когда ты... голоден! 🍫"
                        } else {
                            "Войдите, чтобы делиться рецептами и подписываться на авторов! 🍕"
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

        // Menu items
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 1.dp,
                color = Color.White
            ) {
                Column {
                    if (user != null && (user.role?.lowercase() == "admin" || user.role?.lowercase() == "moderator")) {
                        ProfileMenuItem("Панель управления", Icons.Default.AdminPanelSettings) {
                            navController.navigate(Routes.ADMIN_PANEL)
                        }
                        HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 20.dp))
                    }

                    ProfileMenuItem("ИИ Шеф", Icons.Default.Chat) {
                        if (!isLoggedIn) onLoginClick() else navController.navigate(Routes.CHAT)
                    }
                    HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 20.dp))

                    ProfileMenuItem("Парсинг сайта", Icons.Default.Link) {
                        if (!isLoggedIn) onLoginClick() else navController.navigate(Routes.PARSE_RECIPE)
                    }
                    


                    if (isLoggedIn) {
                        HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 20.dp))
                        ProfileMenuItem("Удалить аккаунт", Icons.Default.Delete) {
                            showDeleteAccountDialog = true
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // My recipes section (including private)
        if (isLoggedIn && user != null) {
            item {
                Text(
                    "Мои рецепты",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    fontFamily = InterFontFamily,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (isLoadingRecipes) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = UmamiOrange)
                    }
                }
            } else if (myRecipes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Restaurant, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("У вас пока нет рецептов", color = Color.Gray, fontFamily = InterFontFamily)
                        }
                    }
                }
            } else {
                items(myRecipes, key = { it.id }) { recipe ->
                    MyRecipeCard(recipe = recipe, navController = navController)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    // Dialogs
    if (showFollowers) {
        UserListDialog("Подписчики", followers, user?.id, { showFollowers = false }, { _, _ -> })
    }
    if (showFollowing) {
        UserListDialog("Подписки", following, user?.id, { showFollowing = false }, { _, _ -> })
    }
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Выйти из аккаунта?", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
            text = { Text("Вы уверены, что хотите выйти?", fontFamily = InterFontFamily) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    authViewModel?.logout()
                }) {
                    Text("Выйти", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
    if (showEditDialog && user != null) {
        EditProfileDialog(
            user = user,
            bio = profileData?.bio ?: "",
            onDismiss = { showEditDialog = false },
            onSave = { newName, newBio ->
                scope.launch {
                    try {
                        userService.updateProfile(UpdateProfileRequest(name = newName, bio = newBio))
                        authViewModel?.refreshProfile()
                        // Refresh
                        profileData = try { userService.getUserProfile(user.id) } catch (_: Exception) { profileData }
                        android.widget.Toast.makeText(context, "Профиль обновлён", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Ошибка обновления профиля", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                showEditDialog = false
            }
        )
    }

    if (showVerificationDialog) {
        RequestVerificationDialog(
            onDismiss = { showVerificationDialog = false },
            onSubmit = { fullName, info ->
                scope.launch {
                    try {
                        userService.requestVerification(mapOf("full_name" to fullName, "info" to info))
                        android.widget.Toast.makeText(context, "Заявка отправлена", android.widget.Toast.LENGTH_SHORT).show()
                        try {
                            verificationRequest = userService.getMyVerificationRequest()
                        } catch (_: Exception) {}
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Ошибка отправки заявки", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                showVerificationDialog = false
            }
        )
    }

    if (showPendingInfoDialog) {
        AlertDialog(
            onDismissRequest = { showPendingInfoDialog = false },
            title = { Text("Заявка на рассмотрении", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
            text = { Text("Ваша заявка на верификацию аккаунта находится на рассмотрении у модераторов платформы. Обычно это занимает не более 24 часов.", fontFamily = InterFontFamily) },
            confirmButton = {
                TextButton(onClick = { showPendingInfoDialog = false }) {
                    Text("Хорошо", color = UmamiOrange, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showRejectionDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showRejectionDetailsDialog = false },
            title = { Text("Заявка отклонена", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("К сожалению, ваша предыдущая заявка на верификацию была отклонена модератором.", fontFamily = InterFontFamily)
                    val notes = verificationRequest?.adminNotes ?: verificationRequest?.reason ?: "Причина не указана."
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Причина: $notes",
                            color = Color(0xFFC62828),
                            fontFamily = InterFontFamily,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text("Вы можете исправить указанные замечания и подать заявку повторно.", fontFamily = InterFontFamily, fontSize = 13.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRejectionDetailsDialog = false
                        showVerificationDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UmamiOrange)
                ) {
                    Text("Подать заново")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectionDetailsDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }

    if (showAppealDialog) {
        AppealDialog(
            onDismiss = { showAppealDialog = false },
            onSubmit = { message ->
                scope.launch {
                    try {
                        userService.createAppeal(mapOf("message" to message))
                        android.widget.Toast.makeText(context, "Апелляция отправлена", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Ошибка отправки апелляции", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                showAppealDialog = false
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Удалить аккаунт?", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
            text = { Text("Вы уверены, что хотите удалить свой аккаунт? Это действие необратимо и сотрет все ваши рецепты и данные.", fontFamily = InterFontFamily) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAccountDialog = false
                    authViewModel?.deleteAccount(
                        onSuccess = {
                            android.widget.Toast.makeText(context, "Аккаунт успешно удален", android.widget.Toast.LENGTH_LONG).show()
                        },
                        onError = { error ->
                            android.widget.Toast.makeText(context, "Ошибка удаления: $error", android.widget.Toast.LENGTH_LONG).show()
                        }
                    )
                }) {
                    Text("Удалить", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun AppealDialog(onDismiss: () -> Unit, onSubmit: (message: String) -> Unit) {
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Оспорить блокировку", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Опишите причину, по которой мы должны разблокировать ваш аккаунт.", fontSize = 13.sp, color = Color.Gray)
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    placeholder = { Text("Ваше сообщение...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (message.isNotBlank()) onSubmit(message) },
                colors = ButtonDefaults.buttonColors(containerColor = UmamiOrange)
            ) {
                Text("Отправить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun RequestVerificationDialog(onDismiss: () -> Unit, onSubmit: (name: String, info: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Подтверждение аккаунта", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Укажите ваше реальное ФИО и краткую информацию о себе (например, шеф-повар, блогер).", fontSize = 13.sp, color = Color.Gray)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ФИО") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = info,
                    onValueChange = { info = it },
                    label = { Text("Дополнительно") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSubmit(name, info) },
                colors = ButtonDefaults.buttonColors(containerColor = UmamiOrange)
            ) {
                Text("Отправить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun EditProfileDialog(
    user: User,
    bio: String,
    onDismiss: () -> Unit,
    onSave: (name: String, bio: String) -> Unit
) {
    var nameField by remember { mutableStateOf(user.name ?: "") }
    var bioField by remember { mutableStateOf(bio) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Редактировать профиль", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nameField,
                    onValueChange = { nameField = it },
                    label = { Text("Имя", fontFamily = InterFontFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = UmamiOrange,
                        focusedLabelColor = UmamiOrange
                    )
                )
                OutlinedTextField(
                    value = bioField,
                    onValueChange = { bioField = it },
                    label = { Text("О себе", fontFamily = InterFontFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = UmamiOrange,
                        focusedLabelColor = UmamiOrange
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(nameField, bioField) },
                colors = ButtonDefaults.buttonColors(containerColor = UmamiOrange)
            ) {
                Text("Сохранить", fontFamily = InterFontFamily)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", fontFamily = InterFontFamily)
            }
        }
    )
}

@Composable
fun MyRecipeCard(recipe: Recipe, navController: NavController) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate(Routes.recipeDetail(recipe.id.toString())) },
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!recipe.imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = normalizeImageUrl(recipe.imageUrl),
                    contentDescription = recipe.title,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(UmamiOrange.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🍽", fontSize = 24.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        recipe.title,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (recipe.isPrivate == true) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFFF3E0)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = UmamiOrange,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Приватный", fontSize = 10.sp, color = UmamiOrange, fontFamily = InterFontFamily)
                            }
                        }
                    }
                }
                if (!recipe.description.isNullOrEmpty()) {
                    Text(
                        recipe.description,
                        fontFamily = InterFontFamily,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (recipe.cookingTime != null) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Text(" ${recipe.cookingTime} мин", fontSize = 11.sp, color = Color.Gray, fontFamily = InterFontFamily)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Text(" ${recipe.likesCount ?: 0}", fontSize = 11.sp, color = Color.Gray, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Text(" ${recipe.viewsCount ?: 0}", fontSize = 11.sp, color = Color.Gray, fontFamily = InterFontFamily)
                }
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
