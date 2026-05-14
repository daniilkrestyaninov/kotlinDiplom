package com.example.diplom

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
            try {
                profileData = userService.getUserProfile(user.id)
            } catch (_: Exception) {}

            // Use recipes from profile data if available
            if (profileData?.recipes != null) {
                myRecipes = profileData!!.recipes!!
            } else {
                // Fallback
                isLoadingRecipes = true
                try {
                    myRecipes = userService.getUserRecipes(user.id)
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
        // Profile header card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 1.dp,
                color = Color.White
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .clickable {
                                if (!isLoggedIn) onLoginClick()
                                else if (user != null) navController.navigate("user_detail/${user.id}")
                            }
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar with upload capability
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(UmamiOrange.copy(alpha = 0.1f))
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
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            // Camera overlay
                            if (isLoggedIn) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Загрузить фото",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                user?.name ?: user?.username ?: "Личный профиль",
                                fontWeight = FontWeight.Bold,
                                color = UmamiOrange,
                                fontSize = 22.sp,
                                fontFamily = InterFontFamily
                            )
                            if (isLoggedIn && user != null) {
                                Text(
                                    "@${user.username}",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    fontFamily = InterFontFamily
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("${profileData?.stats?.recipesCount ?: 0} рецептов", fontSize = 12.sp, color = Color.Gray, fontFamily = InterFontFamily)
                                    Text(
                                        "${profileData?.stats?.followersCount ?: 0} подп.",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        fontFamily = InterFontFamily,
                                        modifier = Modifier.clickable {
                                            showFollowers = true
                                            scope.launch {
                                                try { followers = userService.getFollowers(user.id) } catch (_: Exception) {}
                                            }
                                        }
                                    )
                                    Text(
                                        "${profileData?.stats?.followingCount ?: 0} подп-к",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        fontFamily = InterFontFamily,
                                        modifier = Modifier.clickable {
                                            showFollowing = true
                                            scope.launch {
                                                try { following = userService.getFollowing(user.id) } catch (_: Exception) {}
                                            }
                                        }
                                    )
                                }
                            } else {
                                Text(
                                    "Войдите, чтобы сохранять рецепты",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    fontFamily = InterFontFamily
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }

                    // Bio display
                    if (isLoggedIn && !profileData?.bio.isNullOrBlank()) {
                        Text(
                            profileData!!.bio!!,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                            color = Color.DarkGray,
                            fontSize = 13.sp,
                            fontFamily = InterFontFamily
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Action buttons (edit profile, logout) - only for logged in users
        if (isLoggedIn && user != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = UmamiOrange),
                        border = androidx.compose.foundation.BorderStroke(1.dp, UmamiOrange)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Редактировать", fontFamily = InterFontFamily, fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Выйти", fontFamily = InterFontFamily, fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
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

                    ProfileMenuItem("Избранное", Icons.Default.Bookmark) {
                        if (!isLoggedIn) onLoginClick() else navController.navigate(Routes.FAVORITES)
                    }
                    HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 20.dp))

                    ProfileMenuItem("ИИ Шеф", Icons.Default.Chat) {
                        if (!isLoggedIn) onLoginClick() else navController.navigate(Routes.CHAT)
                    }
                    HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 20.dp))

                    ProfileMenuItem("Парсинг сайта", Icons.Default.Link) {
                        if (!isLoggedIn) onLoginClick() else navController.navigate(Routes.PARSE_RECIPE)
                    }
                    
                    if (isLoggedIn && user?.isVerified != true) {
                        HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 20.dp))
                        ProfileMenuItem("Подтвердить аккаунт", Icons.Default.Verified) {
                            showVerificationDialog = true
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
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Ошибка отправки заявки", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                showVerificationDialog = false
            }
        )
    }
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
                        maxLines = 1
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
