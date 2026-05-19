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
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UmamiMainScreen(navController: NavController, currentUserId: String? = null, isBlocked: Boolean = false, viewModel: RecipeViewModel = viewModel()) {
    val state by viewModel.state
    val context = androidx.compose.ui.platform.LocalContext.current
    var showFullMenuBottomSheet by remember { mutableStateOf(false) }
    
    LaunchedEffect(currentUserId) {
        viewModel.currentUserId = currentUserId
        viewModel.fetchRecipes(currentUserId)
    }

    val isRefreshing by viewModel.isRefreshing

    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
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
            item {
                MenuOfTheWeekSection(
                    items = viewModel.menuOfTheWeek.value,
                    onRecipeClick = { id -> navController.navigate("recipe_detail/$id") },
                    onShowAllClick = { showFullMenuBottomSheet = true }
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
                    val activeRecipes = recipeState.recipes.filter { it.User?.isBlocked != true }
                    items(activeRecipes, key = { it.id }) { recipe ->
                        val isLiked = recipe.isLiked ?: recipe.likes?.any { it.userId == currentUserId } ?: false
                        val likesCount = recipe.likesCount ?: recipe.likes?.size ?: 0
                        val isFavorited = recipe.isFavorited ?: false
                        
                        RecipePostCard(
                            recipe = recipe.copy(isLiked = isLiked, likesCount = likesCount, isFavorited = isFavorited),
                            navController = navController,
                            currentUserId = currentUserId,
                            isFavorited = isFavorited,
                            isBlocked = isBlocked,
                            onLikeClick = {
                                if (isBlocked) {
                                    android.widget.Toast.makeText(context, "Действие недоступно: аккаунт заблокирован", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.toggleLike(recipe.id.toString(), isLiked, currentUserId)
                                }
                            },
                            onCommentClick = { navController.navigate("recipe_detail/${recipe.id}?tab=comments") },
                            onFollowClick = { 
                                if (isBlocked) {
                                    android.widget.Toast.makeText(context, "Действие недоступно: аккаунт заблокирован", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.toggleFollow(recipe.User!!.id.toString())
                                }
                            },
                            onFavoriteClick = {
                                if (currentUserId.isNullOrBlank()) {
                                    android.widget.Toast.makeText(context, "Нужно войти в аккаунт", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.toggleFavorite(recipe.id, isFavorited)
                                    val msg = if (isFavorited) "Удалено из избранного" else "Добавлено в избранное"
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
                is RecipeState.Error -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = UmamiOrange,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Не удалось загрузить рецепты",
                                color = Color.DarkGray,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFontFamily
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Проверьте подключение к интернету",
                                color = Color.Gray,
                                fontFamily = InterFontFamily
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.retry() },
                                colors = ButtonDefaults.buttonColors(containerColor = UmamiOrange)
                            ) {
                                Text("Попробовать снова", fontFamily = InterFontFamily)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFullMenuBottomSheet) {
        val daysOfWeekFull = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")
        ModalBottomSheet(
            onDismissRequest = { showFullMenuBottomSheet = false },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Меню на неделю",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    fontFamily = InterFontFamily,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    for (dayIndex in 1..7) {
                        val dayItems = viewModel.menuOfTheWeek.value.filter { it.day_of_week == dayIndex }
                        if (dayItems.isNotEmpty()) {
                            item {
                                Text(
                                    text = daysOfWeekFull[dayIndex - 1],
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    fontFamily = InterFontFamily,
                                    color = UmamiOrange,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(dayItems) { item ->
                                item.Recipe?.let { recipe ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { 
                                                navController.navigate("recipe_detail/${recipe.id}")
                                                showFullMenuBottomSheet = false
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = normalizeImageUrl(recipe.imageUrl),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = recipe.title ?: "Без названия",
                                                    fontFamily = InterFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                recipe.description?.let {
                                                    Text(
                                                        text = it,
                                                        fontFamily = InterFontFamily,
                                                        color = Color.Gray,
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
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
    avatarUrl: String? = null,
    isVerified: Boolean = false,
    unreadNotifications: Int = 0,
    onAuthClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
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
            // Notification Bell with Badge
            BadgedBox(
                badge = {
                    if (unreadNotifications > 0) {
                        Badge(
                            containerColor = Color(0xFFFF6B6B),
                            contentColor = Color.White
                        ) {
                            Text(unreadNotifications.toString())
                        }
                    }
                },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.notification),
                    contentDescription = "Notifications",
                    modifier = Modifier
                        .size(42.dp)
                        .clickable { onNotificationClick() },
                    contentScale = ContentScale.Fit
                )
            }
            
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
                    if (isLoggedIn && isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    if (isLoggedIn) {
                        if (!avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = com.example.diplom.data.normalizeImageUrl(avatarUrl),
                                contentDescription = "Аватар",
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Профиль",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Профиль",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
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
            Box(contentAlignment = Alignment.Center) {
                if (!category.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = com.example.diplom.data.normalizeImageUrl(category.imageUrl),
                        contentDescription = category.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)),
                        alpha = if (isSelected) 1f else 0.8f
                    )
                } else {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = category.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.padding(12.dp),
                        alpha = if (isSelected) 1f else 0.7f
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            category.name,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            fontFamily = InterFontFamily,
            maxLines = 1
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


















@Composable
fun MenuOfTheWeekSection(
    items: List<MenuOfTheWeekItem>, 
    onRecipeClick: (Long) -> Unit,
    onShowAllClick: () -> Unit
) {
    if (items.isEmpty()) return
    
    val calendar = java.util.Calendar.getInstance()
    val currentDay = when(calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
        java.util.Calendar.MONDAY -> 1
        java.util.Calendar.TUESDAY -> 2
        java.util.Calendar.WEDNESDAY -> 3
        java.util.Calendar.THURSDAY -> 4
        java.util.Calendar.FRIDAY -> 5
        java.util.Calendar.SATURDAY -> 6
        java.util.Calendar.SUNDAY -> 7
        else -> 1
    }
    
    val todayRecipes = items.filter { it.day_of_week == currentDay }
    if (todayRecipes.isEmpty()) return

    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Меню дня",
                color = Color.Black,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                fontFamily = InterFontFamily
            )
            Text(
                "Показать всё",
                color = UmamiOrange,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                fontFamily = InterFontFamily,
                modifier = Modifier.clickable { onShowAllClick() }
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(todayRecipes) { item ->
                item.Recipe?.let { recipe ->
                    MenuRecipeCard(recipe = recipe, onClick = { onRecipeClick(recipe.id) })
                }
            }
        }
    }
}

@Composable
fun MenuRecipeCard(recipe: Recipe, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .height(130.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            AsyncImage(
                model = normalizeImageUrl(recipe.imageUrl),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    recipe.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (recipe.cookingTime != null) {
                    Text(
                        "${recipe.cookingTime} мин",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }
    }
}
