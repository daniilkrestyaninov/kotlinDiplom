package com.example.diplom

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.diplom.data.RecipeState
import com.example.diplom.data.RecipeViewModel
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiOrange
import com.example.diplom.ui.theme.UmamiGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UmamiSearchScreen(navController: NavController, currentUserId: String? = null, viewModel: RecipeViewModel = viewModel()) {
    var searchQuery by remember { mutableStateOf(viewModel.searchQuery) }
    val state by viewModel.state

    LaunchedEffect(currentUserId) {
        viewModel.currentUserId = currentUserId
        viewModel.fetchRecipes(currentUserId)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Text(
                text = "Поиск рецептов",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
        }

        // Search field
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.searchQuery = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                placeholder = { Text("Название рецепта или ингредиент...", fontFamily = InterFontFamily, color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                viewModel.searchQuery = ""
                                viewModel.fetchRecipes(currentUserId, forceRefresh = true)
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Очистить", tint = Color.Gray, modifier = Modifier.size(20.dp))
                            }
                        }
                        Button(
                            onClick = { viewModel.fetchRecipes(currentUserId, forceRefresh = true) },
                            colors = ButtonDefaults.buttonColors(containerColor = UmamiGreen),
                            modifier = Modifier.padding(end = 4.dp).height(36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Найти", fontFamily = InterFontFamily, fontSize = 12.sp)
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
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Category filters
        item {
            FilterSection(
                title = "Категории:",
                items = viewModel.categories.value,
                selectedId = viewModel.selectedCategoryId,
                onToggle = { viewModel.toggleCategory(it) }
            )
        }

        // Dropdown filters
        item {
            Text(
                "Фильтры:",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
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

        // Active filters indicator
        item {
            val hasFilters = viewModel.selectedCategoryId != null ||
                viewModel.selectedKitchenId != null ||
                viewModel.selectedCookingId != null ||
                viewModel.selectedCelebrationId != null ||
                searchQuery.isNotBlank()

            if (hasFilters) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val count = listOfNotNull(
                        viewModel.selectedCategoryId,
                        viewModel.selectedKitchenId,
                        viewModel.selectedCookingId,
                        viewModel.selectedCelebrationId
                    ).size + if (searchQuery.isNotBlank()) 1 else 0

                    Text(
                        "Активных фильтров: $count",
                        fontSize = 12.sp,
                        color = UmamiOrange,
                        fontFamily = InterFontFamily
                    )
                    TextButton(onClick = {
                        searchQuery = ""
                        viewModel.searchQuery = ""
                        viewModel.selectedCategoryId = null
                        viewModel.selectedKitchenId = null
                        viewModel.selectedCookingId = null
                        viewModel.selectedCelebrationId = null
                        viewModel.fetchRecipes(currentUserId, forceRefresh = true)
                    }) {
                        Text("Сбросить всё", fontSize = 12.sp, color = Color.Gray, fontFamily = InterFontFamily)
                    }
                }
            }
        }

        when (val recipeState = state) {
            is RecipeState.Success -> {
                item {
                    val count = recipeState.recipes.size
                    val word = when {
                        count % 10 == 1 && count % 100 != 11 -> "рецепт"
                        count % 10 in 2..4 && count % 100 !in 12..14 -> "рецепта"
                        else -> "рецептов"
                    }
                    Text(
                        "Найдено $count $word",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }

                if (recipeState.recipes.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.SearchOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Ничего не найдено", fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Попробуйте изменить запрос или фильтры", color = Color.Gray, fontFamily = InterFontFamily, fontSize = 13.sp)
                        }
                    }
                }

                items(recipeState.recipes) { recipe ->
                    val isLiked = recipe.isLiked ?: recipe.likes?.any { it.userId == currentUserId } ?: false
                    val likesCount = recipe.likesCount ?: recipe.likes?.size ?: 0
                    val isFavorited = recipe.isFavorited ?: false
                    val context = androidx.compose.ui.platform.LocalContext.current

                    RecipePostCard(
                        recipe = recipe.copy(isLiked = isLiked, likesCount = likesCount, isFavorited = isFavorited),
                        navController = navController,
                        currentUserId = currentUserId,
                        isFavorited = isFavorited,
                        onLikeClick = { viewModel.toggleLike(recipe.id.toString(), isLiked, currentUserId) },
                        onCommentClick = { navController.navigate("recipe_detail/${recipe.id}?tab=comments") },
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
            is RecipeState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = UmamiOrange)
                    }
                }
            }
            is RecipeState.Error -> {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = UmamiOrange,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Не удалось выполнить поиск", fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Проверьте подключение к интернету", color = Color.Gray, fontFamily = InterFontFamily)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.fetchRecipes(currentUserId, forceRefresh = true) },
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
