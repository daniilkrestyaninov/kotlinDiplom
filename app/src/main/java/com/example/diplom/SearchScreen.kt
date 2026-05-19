package com.example.diplom

import androidx.compose.animation.*
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
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFE5E5E5),
                focusedBorderColor = UmamiOrange,
                unfocusedContainerColor = Color(0xFFFAFAFA),
                focusedContainerColor = Color.White
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = Color.White,
            contentColor = UmamiOrange,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                    color = UmamiOrange
                )
            },
            divider = {}
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { searchViewModel.setTab(0) },
                text = { Text("Рецепты", fontFamily = InterFontFamily, fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { searchViewModel.setTab(1) },
                text = { Text("Люди", fontFamily = InterFontFamily, fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal) }
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
                        // Show "Discover" or "Recent" or Categories
                        if (activeTab == 0) {
                            CategoryDiscovery(recipeViewModel)
                        } else {
                            EmptySearchState(Icons.Default.Group, "Найдите интересных авторов")
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryDiscovery(viewModel: RecipeViewModel) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp)) {
        item {
            Text("Популярные категории", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.categories.value.take(12).forEach { category ->
                    FilterChip(
                        selected = viewModel.selectedCategoryId == category.id.toString(),
                        onClick = { viewModel.toggleCategory(category.id.toString()) },
                        label = { Text(category.name, fontFamily = InterFontFamily) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
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
                        Text(
                            text = user.name ?: user.username,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
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
