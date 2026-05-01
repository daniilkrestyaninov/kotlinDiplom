package com.example.diplom

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UmamiSearchScreen(navController: NavController, currentUserId: String? = null, viewModel: RecipeViewModel = viewModel()) {
    var searchQuery by remember { mutableStateOf("") }
    val state by viewModel.state

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

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                placeholder = { Text("Поиск", fontFamily = InterFontFamily, color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                trailingIcon = {
                    Button(
                        onClick = { /* TODO: Search action */ },
                        colors = ButtonDefaults.buttonColors(containerColor = com.example.diplom.ui.theme.UmamiGreen),
                        modifier = Modifier.padding(end = 4.dp).height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("Найти рецепт", fontFamily = InterFontFamily, fontSize = 12.sp)
                    }
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE5E5E5),
                    focusedBorderColor = UmamiOrange
                )
            )
        }

        item {
            FilterSection(
                title = "Категории:",
                items = viewModel.categories.value,
                selectedId = viewModel.selectedCategoryId,
                onToggle = { viewModel.toggleCategory(it) }
            )
        }

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

        when (val recipeState = state) {
            is RecipeState.Success -> {
                item {
                    Text(
                        "Найдено ${recipeState.recipes.size} рецепта",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                    )
                }
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
            is RecipeState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = UmamiOrange)
                    }
                }
            }
            is RecipeState.Error -> {
                item {
                    Text("Error: ${recipeState.message}", color = Color.Red, modifier = Modifier.padding(20.dp))
                }
            }
        }
    }
}
