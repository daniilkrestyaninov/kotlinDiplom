package com.example.diplom

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.diplom.data.*
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiOrange
import com.example.diplom.ui.theme.UmamiGreen
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    currentUserId: String? = null,
    isBlocked: Boolean = false,
    searchViewModel: SearchViewModel = viewModel(),
    recipeViewModel: RecipeViewModel = viewModel()
) {
    val searchQuery by searchViewModel.searchQuery
    val activeTab by searchViewModel.activeTab
    val searchState by searchViewModel.searchState

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Top Header
        Text(
            text = "Поиск",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchViewModel.onSearchQueryChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            placeholder = { 
                Text(
                    if (activeTab == 0) "Рецепты, ингредиенты..." else "Имя или логин пользователя...",
                    fontFamily = InterFontFamily, 
                    color = Color.Gray, 
                    fontSize = 14.sp
                ) 
            },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchViewModel.onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Очистить", tint = Color.Gray)
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = UmamiOrange,
                unfocusedContainerColor = Color(0xFFF1F3F5),
                focusedContainerColor = Color(0xFFF1F3F5),
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Custom Capsule Tab Switcher
        SearchTabSwitcher(
            activeTab = activeTab,
            onTabSelected = { searchViewModel.setTab(it) }
        )

        // Horizontal Category Row for Recipes Tab
        if (activeTab == 0) {
            val selectedCategoryId by searchViewModel.selectedCategoryId
            SearchCategoriesRow(
                categories = recipeViewModel.categories.value,
                selectedCategoryId = selectedCategoryId,
                onCategoryClick = { searchViewModel.toggleCategory(it) }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = searchState,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "SearchContent"
            ) { state ->
                when (state) {
                    is SearchResultState.Idle -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = UmamiOrange)
                        }
                    }
                    is SearchResultState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = UmamiOrange)
                        }
                    }
                    is SearchResultState.Success -> {
                        if (activeTab == 0) {
                            RecipeResults(state.recipes, navController, currentUserId, isBlocked, recipeViewModel)
                        } else {
                            UserResults(state.users, navController)
                        }
                    }
                    is SearchResultState.Error -> {
                        ErrorState(state.message) { searchViewModel.performSearch() }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchTabSwitcher(activeTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .background(Color(0xFFF1F3F5), RoundedCornerShape(24.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(if (activeTab == 0) UmamiOrange else Color.Transparent)
                .clickable { onTabSelected(0) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Рецепты",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                color = if (activeTab == 0) Color.White else Color.Gray,
                fontSize = 14.sp
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(if (activeTab == 1) UmamiOrange else Color.Transparent)
                .clickable { onTabSelected(1) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Люди",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                color = if (activeTab == 1) Color.White else Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun SearchCategoryRowCard(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(110.dp)
            .height(55.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!category.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = normalizeImageUrl(category.imageUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFFE0B2), Color(0xFFFFCC80))
                            )
                        )
                )
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isSelected) {
                            Brush.verticalGradient(
                                colors = listOf(UmamiOrange.copy(alpha = 0.7f), UmamiOrange.copy(alpha = 0.9f))
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                            )
                        }
                    )
            )
            
            Text(
                text = category.name,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(4.dp)
            )
        }
    }
}

@Composable
fun SearchCategoriesRow(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategoryClick: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = selectedCategoryId == category.id.toString()
            SearchCategoryRowCard(
                category = category,
                isSelected = isSelected,
                onClick = { onCategoryClick(category.id.toString()) }
            )
        }
    }
}

@Composable
fun RecipeResults(
    recipes: List<Recipe>, 
    navController: NavController, 
    currentUserId: String?,
    isBlocked: Boolean = false,
    viewModel: RecipeViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activeRecipes = recipes.filter { it.User?.isBlocked != true }
    if (activeRecipes.isEmpty()) {
        EmptySearchState(Icons.Default.RestaurantMenu, "Рецепты не найдены")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(activeRecipes) { recipe ->
                val isLiked = recipe.likes?.any { it.userId == currentUserId } ?: false
                val isFavorited = recipe.isFavorited ?: false
                
                RecipePostCard(
                    recipe = recipe.copy(isLiked = isLiked),
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
                    onFavoriteClick = { viewModel.toggleFavorite(recipe.id, isFavorited) }
                )
            }
        }
    }
}

@Composable
fun UserResults(users: List<User>, navController: NavController) {
    val activeUsers = users.filter { it.isBlocked != true }
    if (activeUsers.isEmpty()) {
        EmptySearchState(Icons.Default.PersonSearch, "Пользователи не найдены")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(activeUsers) { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (user.id.isNotBlank()) navController.navigate("user_detail/${user.id}") },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = normalizeImageUrl(user.avatarUrl) ?: R.drawable.ic_avatar,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = user.name ?: user.username,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (user.isVerified == true) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Верифицирован",
                                    tint = Color(0xFF2196F3),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "@${user.username}",
                            fontFamily = InterFontFamily,
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        if (!user.bio.isNullOrBlank()) {
                            Text(
                                text = user.bio,
                                fontFamily = InterFontFamily,
                                color = Color.DarkGray,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
                }
            }
        }
    }
}

@Composable
fun EmptySearchState(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text, fontFamily = InterFontFamily, color = Color.Gray, fontSize = 16.sp)
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = UmamiOrange)
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, fontFamily = InterFontFamily, color = Color.DarkGray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = UmamiOrange)) {
                Text("Попробовать снова", fontFamily = InterFontFamily)
            }
        }
    }
}
