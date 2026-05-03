package com.example.diplom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.diplom.data.*
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiOrange
import kotlinx.coroutines.launch

@Composable
fun UmamiFavoritesScreen(
    navController: NavController,
    currentUserId: String? = null
) {
    val service = ApiClient.userService
    val scope = rememberCoroutineScope()

    var favorites by remember { mutableStateOf<List<FavoriteItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            favorites = service.getFavorites()
            isLoading = false
        } catch (e: Exception) {
            error = e.message
            isLoading = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Избранное",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = UmamiOrange)
                }
            }
        } else if (error != null) {
            item {
                Text("Ошибка: $error", color = Color.Red, fontFamily = InterFontFamily)
            }
        } else if (favorites.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("У вас пока нет избранных рецептов", fontFamily = InterFontFamily, color = Color.Gray)
                    }
                }
            }
        } else {
            items(favorites, key = { it.id }) { fav ->
                val recipe = fav.recipe ?: return@items
                FavoriteRecipeCard(
                    recipe = recipe,
                    onClick = { navController.navigate("recipe_detail/${recipe.id}") },
                    onRemove = {
                        scope.launch {
                            try {
                                service.removeFavorite(recipe.id)
                                favorites = favorites.filter { it.id != fav.id }
                            } catch (e: Exception) {
                                android.util.Log.e("Favorites", "Remove failed", e)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FavoriteRecipeCard(recipe: Recipe, onClick: () -> Unit, onRemove: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
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
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(UmamiOrange.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🍽", fontSize = 28.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    recipe.title,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 2
                )
                if (!recipe.description.isNullOrEmpty()) {
                    Text(
                        recipe.description,
                        fontFamily = InterFontFamily,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 2
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    if (recipe.cookingTime != null) {
                        Text("${recipe.cookingTime} мин", fontSize = 12.sp, color = UmamiOrange, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium)
                    }
                    if (recipe.User != null) {
                        Text(" · ${recipe.User.username}", fontSize = 12.sp, color = Color.Gray, fontFamily = InterFontFamily)
                    }
                }
            }

            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Bookmark, contentDescription = "Убрать из избранного", tint = UmamiOrange)
            }
        }
    }
}
