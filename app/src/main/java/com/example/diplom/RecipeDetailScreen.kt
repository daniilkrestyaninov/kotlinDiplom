package com.example.diplom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UmamiRecipeDetailScreen(
    navController: NavController, 
    recipeId: String, 
    initialTab: String = "",
    currentUserId: String? = null,
    viewModel: com.example.diplom.data.RecipeDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(if (initialTab == "comments") 3 else 0) }
    val tabs = listOf("Ингредиенты", "Шаги", "Питание", "Отзывы")

    LaunchedEffect(recipeId) {
        viewModel.loadRecipe(recipeId)
    }

    val state by viewModel.state

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Рецепт", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, color = UmamiOrange) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val isLiked = if (state is com.example.diplom.data.RecipeDetailState.Success) {
                        val r = (state as com.example.diplom.data.RecipeDetailState.Success).recipe
                        r.isLiked ?: r.likes?.any { it.userId == currentUserId } ?: false
                    } else false
                    
                    IconButton(onClick = { viewModel.toggleLike(recipeId, isLiked, currentUserId) }) {
                        Icon(Icons.Default.Star, contentDescription = "Favorite", tint = if (isLiked) UmamiOrange else Color.Gray)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            when (val currentState = state) {
                is com.example.diplom.data.RecipeDetailState.Loading -> {
                    item { CircularProgressIndicator(color = UmamiOrange, modifier = Modifier.padding(20.dp).fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)) }
                }
                is com.example.diplom.data.RecipeDetailState.Error -> {
                    item { Text("Ошибка: ${currentState.message}", color = Color.Red, modifier = Modifier.padding(20.dp)) }
                }
                is com.example.diplom.data.RecipeDetailState.Success -> {
                    val recipe = currentState.recipe
                    val comments = currentState.comments
                    
                    item {
                        if (recipe.imageUrl != null) {
                            coil.compose.AsyncImage(
                                model = recipe.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color.LightGray),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(UmamiOrange.copy(alpha = 0.5f))
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            recipe.title,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                        if (!recipe.description.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                recipe.description,
                                fontFamily = InterFontFamily,
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DetailStat("${recipe.cookingTime ?: 0} мин", "готовка")
                            DetailStat("${recipe.portion ?: "—"} порц.", "выход")
                            DetailStat("${recipe.calorific ?: "—"}", "ккал")
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color(0xFFF5F5F5),
                            indicator = { },
                            divider = { },
                            modifier = Modifier.clip(RoundedCornerShape(24.dp))
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    modifier = Modifier
                                        .background(if (selectedTab == index) UmamiOrange else Color.Transparent)
                                        .clip(RoundedCornerShape(24.dp)),
                                    text = { 
                                        Text(
                                            title, 
                                            color = if (selectedTab == index) Color.White else Color.Gray,
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ) 
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    item {
                        when (selectedTab) {
                            0 -> {
                                // Ингредиенты
                                Column {
                                    Text("Ингредиенты", fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (recipe.ingredients.isNullOrEmpty()) {
                                        Text("Ингредиенты не указаны", fontFamily = InterFontFamily, color = Color.Gray)
                                    } else {
                                        recipe.ingredients.forEach { ingredient ->
                                            Surface(
                                                color = Color(0xFFF9F9F9),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        ingredient.name ?: "—",
                                                        fontFamily = InterFontFamily,
                                                        fontWeight = FontWeight.Medium,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    val qty = ingredient.pivot?.quantity ?: ""
                                                    val note = ingredient.pivot?.note ?: ""
                                                    val detail = listOfNotNull(
                                                        qty.takeIf { it.isNotBlank() },
                                                        note.takeIf { it.isNotBlank() }
                                                    ).joinToString(" · ")
                                                    if (detail.isNotEmpty()) {
                                                        Text(detail, fontFamily = InterFontFamily, color = UmamiOrange, fontSize = 13.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            1 -> {
                                // Шаги
                                Column {
                                    Text("Шаги приготовления", fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (recipe.steps.isNullOrEmpty()) {
                                        Text("Шаги не указаны", fontFamily = InterFontFamily, color = Color.Gray)
                                    } else {
                                        recipe.steps.sortedBy { it.stepNumber }.forEach { step ->
                                            Surface(
                                                color = Color(0xFFF9F9F9),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text(
                                                        "Шаг ${step.stepNumber}",
                                                        fontWeight = FontWeight.Bold,
                                                        color = UmamiOrange,
                                                        fontFamily = InterFontFamily,
                                                        fontSize = 14.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(step.description, fontFamily = InterFontFamily, fontSize = 14.sp)
                                                    if (!step.imageUrl.isNullOrEmpty()) {
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        coil.compose.AsyncImage(
                                                            model = step.imageUrl,
                                                            contentDescription = "Шаг ${step.stepNumber}",
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(150.dp)
                                                                .clip(RoundedCornerShape(12.dp)),
                                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> {
                                // Питание
                                Column {
                                    Text("Пищевая ценность", fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        color = Color(0xFFF9F9F9),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            NutritionRow("Калории", "${recipe.calorific ?: "—"} ккал")
                                            NutritionRow("Порции", "${recipe.portion ?: "—"}")
                                            NutritionRow("Время готовки", "${recipe.cookingTime ?: "—"} мин")
                                            NutritionRow("Сложность", recipe.difficulty ?: "—")
                                            if (recipe.kitchen != null) NutritionRow("Кухня", recipe.kitchen.name)
                                            if (recipe.typeCooking != null) NutritionRow("Тип готовки", recipe.typeCooking.name)
                                            if (recipe.celebration != null) NutritionRow("Праздник", recipe.celebration.name)
                                        }
                                    }
                                }
                            }
                            3 -> {
                                Column {
                                    Text("Отзывы (${comments.size})", fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    comments.forEach { comment ->
                                        Surface(
                                            color = Color(0xFFF9F9F9),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(comment.author?.name ?: comment.author?.username ?: "Пользователь", fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = InterFontFamily)
                                                Text(comment.content, fontSize = 14.sp, fontFamily = InterFontFamily, modifier = Modifier.padding(top = 4.dp))
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    var newComment by remember { mutableStateOf("") }
                                    OutlinedTextField(
                                        value = newComment,
                                        onValueChange = { newComment = it },
                                        placeholder = { Text("Написать отзыв...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(24.dp),
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                if (newComment.isNotBlank()) {
                                                    viewModel.postComment(recipeId, newComment, 5)
                                                    newComment = ""
                                                }
                                            }) {
                                                Icon(Icons.Default.Send, contentDescription = "Send", tint = UmamiOrange)
                                            }
                                        }
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

@Composable
fun DetailStat(value: String, label: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E5E5)),
        color = Color.White,
        modifier = Modifier.width(100.dp).height(70.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(value, color = UmamiOrange, fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = InterFontFamily)
            Text(label, color = Color.Gray, fontSize = 12.sp, fontFamily = InterFontFamily)
        }
    }
}

@Composable
fun NutritionRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontFamily = InterFontFamily, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
