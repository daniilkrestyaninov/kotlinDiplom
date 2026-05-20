package com.example.diplom

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.diplom.data.AdminViewModel
import com.example.diplom.data.Recipe
import com.example.diplom.data.normalizeImageUrl
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRecipesScreen(navController: NavController, viewModel: AdminViewModel = viewModel()) {
    val recipesState by viewModel.recipes.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedRecipeIds = remember { mutableStateListOf<String>() }

    LaunchedEffect(searchQuery) {
        viewModel.searchRecipes(searchQuery)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isMultiSelectMode) {
                        Text("Выбрано: ${selectedRecipeIds.size}", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Управление рецептами", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    if (isMultiSelectMode) {
                        IconButton(onClick = {
                            isMultiSelectMode = false
                            selectedRecipeIds.clear()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Отмена")
                        }
                    } else {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }
                    }
                },
                actions = {
                    if (isMultiSelectMode) {
                        if (selectedRecipeIds.isNotEmpty()) {
                            IconButton(onClick = {
                                viewModel.bulkDeleteRecipes(selectedRecipeIds.toList()) { success ->
                                    if (success) {
                                        isMultiSelectMode = false
                                        selectedRecipeIds.clear()
                                        viewModel.searchRecipes(searchQuery) // refresh list
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить выбранные", tint = Color.Red)
                            }
                        }
                    } else {
                        IconButton(onClick = {
                            isMultiSelectMode = true
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Выбор нескольких")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Поиск рецептов...", fontFamily = InterFontFamily) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            if (recipesState.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Рецепты не найдены", fontFamily = InterFontFamily, color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                    items(recipesState) { recipe ->
                        val isSelected = selectedRecipeIds.contains(recipe.id.toString())
                        AdminRecipeItem(
                            recipe = recipe,
                            isSelected = isSelected,
                            isMultiSelect = isMultiSelectMode,
                            onClick = {
                                if (isMultiSelectMode) {
                                    if (isSelected) {
                                        selectedRecipeIds.remove(recipe.id.toString())
                                    } else {
                                        selectedRecipeIds.add(recipe.id.toString())
                                    }
                                } else {
                                    navController.navigate("recipe_detail/${recipe.id}")
                                }
                            },
                            onLongClick = {
                                if (!isMultiSelectMode) {
                                    isMultiSelectMode = true
                                    selectedRecipeIds.add(recipe.id.toString())
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdminRecipeItem(
    recipe: Recipe,
    isSelected: Boolean,
    isMultiSelect: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) UmamiOrange.copy(alpha = 0.1f) else Color.White
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelect) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = 8.dp),
                    colors = CheckboxDefaults.colors(checkedColor = UmamiOrange)
                )
            }
            
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5))
            ) {
                if (!recipe.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = normalizeImageUrl(recipe.imageUrl),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(32.dp).align(Alignment.Center)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    recipe.title,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    recipe.User?.username?.let { "Автор: $it" } ?: "Автор: Umami AI",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    recipe.cookingTime?.let {
                        Text("🕒 $it мин", fontSize = 11.sp, color = Color.DarkGray, fontFamily = InterFontFamily)
                    }
                    recipe.difficulty?.let {
                        Text("⚡ $it", fontSize = 11.sp, color = UmamiOrange, fontFamily = InterFontFamily)
                    }
                }
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.LightGray
            )
        }
    }
}
