package com.example.diplom

import com.example.diplom.R

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diplom.ui.theme.*
import com.example.diplom.data.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.navigation.Routes

@Composable
fun UmamiMainScreen(navController: NavController, currentUserId: String? = null, viewModel: RecipeViewModel = viewModel()) {
    val state by viewModel.state

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item { 
                FilterSection(
                    title = "Категории",
                    items = viewModel.categories.value,
                    selectedId = viewModel.selectedCategoryId,
                    onToggle = { viewModel.toggleCategory(it) }
                )
            }

            item {
                FilterDropdownRow(
                    kitchens = viewModel.kitchens.value,
                    selectedKitchenId = viewModel.selectedKitchenId,
                    onKitchenToggle = { viewModel.toggleKitchen(it) },
                    
                    cookingTypes = viewModel.cookingTypes.value,
                    selectedCookingId = viewModel.selectedCookingId,
                    onCookingToggle = { viewModel.toggleCookingType(it) },
                    
                    celebrations = viewModel.celebrations.value,
                    selectedCelebrationId = viewModel.selectedCelebrationId,
                    onCelebrationToggle = { viewModel.toggleCelebration(it) }
                )
            }
            
            when (val recipeState = state) {
                is RecipeState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = UmamiOrange)
                        }
                    }
                }
                is RecipeState.Success -> {
                    items(recipeState.recipes) { recipe ->
                        val isLiked = recipe.isLiked ?: recipe.likes?.any { it.userId == currentUserId } ?: false
                        val likesCount = recipe.likesCount ?: recipe.likes?.size ?: 0
                        
                        RecipePostCard(
                            recipe = recipe.copy(isLiked = isLiked, likesCount = likesCount),
                            onClick = { navController.navigate("recipe_detail/${recipe.id}") },
                            onLikeClick = { viewModel.toggleLike(recipe.id, isLiked, currentUserId) },
                            onCommentClick = { navController.navigate("recipe_detail/${recipe.id}?tab=comments") }
                        )
                    }
                }
                is RecipeState.Error -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Ошибка загрузки: ${recipeState.message}", color = Color.Red)
                        }
                    }
                }
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UmamiTopBar(
    isLoggedIn: Boolean = false,
    username: String? = null,
    onAuthClick: () -> Unit = {}
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        title = {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "UMAMI Logo",
                modifier = Modifier
                    .width(100.dp) // Adjusted width
                    .height(20.dp),
                contentScale = ContentScale.Fit
            )
        },
        actions = {
            // Notification Bell from drawable (contains its own circle and border)
            Image(
                painter = painterResource(id = R.drawable.notification),
                contentDescription = "Notifications",
                modifier = Modifier
                    .size(42.dp) // Slightly scale up from 36dp for better visibility
                    .clickable { /* TODO */ },
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = UmamiOrange,
                modifier = Modifier.height(36.dp).clickable { if (!isLoggedIn) onAuthClick() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = if (isLoggedIn) (username?.uppercase() ?: "ПРОФИЛЬ") else "ВОЙТИ / РЕГИСТРАЦИЯ",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = InterFontFamily
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Профиль",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
        }
    )
}

@Composable
fun FilterSection(
    title: String,
    items: List<Category>,
    selectedId: String?,
    onToggle: (String) -> Unit
) {
    if (items.isEmpty()) return

    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            title,
            color = UmamiOrange,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            fontFamily = InterFontFamily
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { category ->
                CategoryCard(
                    category = category,
                    isSelected = category.id == selectedId,
                    onClick = { onToggle(category.id) }
                )
            }
        }
    }
}

@Composable
fun FilterDropdownRow(
    kitchens: List<Category>,
    selectedKitchenId: String?,
    onKitchenToggle: (String) -> Unit,
    cookingTypes: List<Category>,
    selectedCookingId: String?,
    onCookingToggle: (String) -> Unit,
    celebrations: List<Category>,
    selectedCelebrationId: String?,
    onCelebrationToggle: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterDropdownItem(
            label = "Кухня",
            items = kitchens,
            selectedId = selectedKitchenId,
            onSelect = onKitchenToggle,
            modifier = Modifier.weight(1f)
        )
        FilterDropdownItem(
            label = "Тип",
            items = cookingTypes,
            selectedId = selectedCookingId,
            onSelect = onCookingToggle,
            modifier = Modifier.weight(1f)
        )
        FilterDropdownItem(
            label = "Праздник",
            items = celebrations,
            selectedId = selectedCelebrationId,
            onSelect = onCelebrationToggle,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun FilterDropdownItem(
    label: String,
    items: List<Category>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = items.find { it.id == selectedId }?.name ?: label
    val isSelected = selectedId != null

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) UmamiOrange.copy(alpha = 0.1f) else Color.White,
            border = BorderStroke(1.dp, if (isSelected) UmamiOrange else Color(0xFFE5E5E5)),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedName,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) UmamiOrange else Color.DarkGray,
                    fontFamily = InterFontFamily,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isSelected) UmamiOrange else Color.Gray
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            item.name, 
                            fontFamily = InterFontFamily,
                            color = if (item.id == selectedId) UmamiOrange else Color.Black,
                            fontWeight = if (item.id == selectedId) FontWeight.Bold else FontWeight.Normal
                        ) 
                    },
                    onClick = {
                        onSelect(item.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun CategoryCard(
    category: Category,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    // Маппинг иконок
    val iconRes = when {
        category.name.contains("Завтрак", ignoreCase = true) -> R.drawable.ic_pizza
        category.name.contains("Обед", ignoreCase = true) -> R.drawable.ic_pizza
        category.name.contains("Пицца", ignoreCase = true) -> R.drawable.ic_pizza
        else -> R.drawable.ic_pizza
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable { onClick() }
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isSelected) UmamiOrange.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
            border = if (isSelected) BorderStroke(2.dp, UmamiOrange) else null,
            shadowElevation = if (isSelected) 0.dp else 4.dp,
            modifier = Modifier.size(70.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = category.name,
                    contentScale = ContentScale.Fit,
                    alpha = if (isSelected) 1f else 0.7f
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            category.name,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isSelected) UmamiOrange else Color.Gray,
            fontFamily = InterFontFamily,
            maxLines = 1
        )
    }
}

@Composable
fun RecipePostCard(
    recipe: com.example.diplom.data.Recipe, 
    onClick: () -> Unit, 
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit
) {
    var comments by remember { mutableStateOf<List<com.example.diplom.data.Comment>?>(null) }
    
    LaunchedEffect(recipe.id) {
        try {
            comments = com.example.diplom.data.ApiClient.recipeService.getComments(recipe.id)
        } catch (e: Exception) {
            // ignore
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // User Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = com.example.diplom.data.normalizeImageUrl(recipe.User?.avatarUrl) ?: R.drawable.ic_avatar,
                        contentDescription = "Пользователь",
                        modifier = Modifier
                            .size(45.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            recipe.User?.name ?: "Anonymous",
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("@${recipe.User?.username ?: "unknown"}", fontSize = 12.sp, color = Color.Gray, fontFamily = InterFontFamily)
                    }
                }
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = UmamiGreen),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    modifier = Modifier.width(112.dp).height(36.dp)
                ) {
                    Text("Подписаться", fontSize = 12.sp, fontFamily = InterFontFamily, maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = com.example.diplom.data.normalizeImageUrl(recipe.imageUrl) ?: R.drawable.img_pasta,
                    contentDescription = recipe.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Bookmark
                Surface(
                    color = Color.White.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd)
                        .size(36.dp)
                ) {
                    IconButton(onClick = { }) {
                        Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Save")
                    }
                }

                // Badges
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BadgeItem(icon = Icons.Default.AccessTime, text = "${recipe.cookingTime ?: 0} мин")
                    BadgeItem(icon = Icons.Default.KeyboardArrowDown, text = recipe.difficulty ?: "Легко")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title & Description
            Text(
                recipe.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 26.sp,
                fontFamily = InterFontFamily
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                recipe.description ?: "Нет описания",
                fontSize = 14.sp,
                color = Color.Gray,
                fontFamily = InterFontFamily
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stats
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onLikeClick() }) {
                    val currentLikes = recipe.likesCount ?: recipe.likes?.size ?: 0
                    val currentIsLiked = recipe.isLiked == true

                    Icon(
                        if (currentIsLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        modifier = Modifier.size(24.dp),
                        tint = if (currentIsLiked) UmamiOrange else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(currentLikes.toString(), fontWeight = FontWeight.Bold, color = Color.DarkGray, fontFamily = InterFontFamily)
                }
                    val currentCommentsCount = comments?.size ?: recipe.commentsCount ?: 0
                StatItem(
                    icon = Icons.Outlined.ModeComment, 
                    count = currentCommentsCount.toString(),
                    modifier = Modifier.clickable { onCommentClick() }
                )
            }

            if (!comments.isNullOrEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFF0F0F0))
                CommentPreview(comment = comments!!.first())
            }
        }
    }
}

@Composable
fun BadgeItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(
        color = Color.White.copy(alpha = 0.9f),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 2.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            IconBadge(icon, contentDescription = null, size = 14.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
        }
    }
}

@Composable
private fun IconBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp) {
    Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(size))
}

@Composable
fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, count: String, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.width(6.dp))
        Text(count, fontWeight = FontWeight.Bold, color = Color.DarkGray, fontFamily = InterFontFamily)
    }
}

@Composable
fun CommentPreview(comment: com.example.diplom.data.Comment) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            AsyncImage(
                model = com.example.diplom.data.normalizeImageUrl(comment.author?.avatarUrl) ?: R.drawable.ic_avatar,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = comment.author?.name ?: comment.author?.username ?: "Пользователь",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                fontFamily = InterFontFamily
            )
            Spacer(modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = comment.content,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 42.dp),
            fontFamily = InterFontFamily,
            color = Color.DarkGray
        )
    }
}

@Composable
fun UmamiBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = UmamiWhite,
        tonalElevation = 8.dp,
        modifier = Modifier
            .height(80.dp)
            .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
    ) {
        NavigationBarItem(
            selected = currentRoute == Routes.MAIN,
            onClick = { onNavigate(Routes.MAIN) },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Главная", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = UmamiOrange,
                selectedTextColor = UmamiOrange,
                unselectedIconColor = Color.LightGray,
                unselectedTextColor = Color.LightGray,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = currentRoute == Routes.SEARCH,
            onClick = { onNavigate(Routes.SEARCH) },
            icon = { Icon(Icons.Default.Search, contentDescription = null) },
            label = { Text("Поиск", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = UmamiOrange,
                selectedTextColor = UmamiOrange,
                unselectedIconColor = Color.LightGray,
                unselectedTextColor = Color.LightGray,
                indicatorColor = Color.Transparent
            )
        )
        // Add item placeholder to offset for FAB
        Spacer(modifier = Modifier.weight(1f))
        NavigationBarItem(
            selected = currentRoute == Routes.FAVORITES,
            onClick = { onNavigate(Routes.FAVORITES) },
            icon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
            label = { Text("Избранное", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = UmamiOrange,
                selectedTextColor = UmamiOrange,
                unselectedIconColor = Color.LightGray,
                unselectedTextColor = Color.LightGray,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = currentRoute == Routes.PROFILE,
            onClick = { onNavigate(Routes.PROFILE) },
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("Кабинет", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = UmamiOrange,
                selectedTextColor = UmamiOrange,
                unselectedIconColor = Color.LightGray,
                unselectedTextColor = Color.LightGray,
                indicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
fun AddRecipeFab(onClick: () -> Unit = {}) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = UmamiOrange,
        contentColor = Color.White,
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
        modifier = Modifier
            .size(60.dp)
            .offset(y = 40.dp) // Position it in the middle of bottom nav
            .border(5.dp, UmamiCream, CircleShape)
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(30.dp))
    }
}

















