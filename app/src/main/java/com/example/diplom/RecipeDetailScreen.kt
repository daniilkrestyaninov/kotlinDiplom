package com.example.diplom

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.diplom.data.*
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiOrange
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UmamiRecipeDetailScreen(
    navController: NavController,
    recipeId: String,
    initialTab: String = "",
    currentUserId: String? = null,
    isBlocked: Boolean = false,
    viewModel: RecipeDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedTab by remember { mutableIntStateOf(if (initialTab == "comments") 3 else 0) }
    val tabs = listOf("Ингредиенты", "Шаги", "Питание", "Отзывы")

    LaunchedEffect(recipeId, currentUserId) { 
        viewModel.loadRecipe(recipeId, currentUserId) 
    }

    var showFullScreenImage by remember { mutableStateOf<String?>(null) }
    val state by viewModel.state
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Рецепт", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, color = UmamiOrange) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    val isLiked = if (state is RecipeDetailState.Success) {
                        val r = (state as RecipeDetailState.Success).recipe
                        r.isLiked ?: r.likes?.any { it.userId == currentUserId } ?: false
                    } else false
                    
                    val isFavorited = if (state is RecipeDetailState.Success) {
                        (state as RecipeDetailState.Success).isFavorited
                    } else false

                    IconButton(onClick = {
                        if (currentUserId.isNullOrBlank()) {
                            Toast.makeText(context, "Нужно войти в аккаунт", Toast.LENGTH_SHORT).show()
                        } else if (isBlocked) {
                            Toast.makeText(context, "Действие недоступно: аккаунт заблокирован", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.toggleFavorite(recipeId, isFavorited)
                            val msg = if (isFavorited) "Удалено из избранного" else "Добавлено в избранное"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            if (isFavorited) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Избранное",
                            tint = if (isFavorited) UmamiOrange else Color.Gray
                        )
                    }

                    if (state is RecipeDetailState.Success) {
                        val recipe = (state as RecipeDetailState.Success).recipe
                        if (recipe.User?.id == currentUserId) {
                            var showDeleteDialog by remember { mutableStateOf(false) }
                            
                            IconButton(onClick = {
                                navController.navigate("add_recipe?recipeId=${recipe.id}")
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = Color.Gray)
                            }
                            
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.Red.copy(alpha = 0.7f))
                            }
                            
                            if (showDeleteDialog) {
                                AlertDialog(
                                    onDismissRequest = { showDeleteDialog = false },
                                    title = { Text("Удалить рецепт?", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
                                    text = { Text("Вы уверены? Это действие нельзя отменить.", fontFamily = InterFontFamily) },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                showDeleteDialog = false
                                                scope.launch {
                                                    try {
                                                        ApiClient.recipeService.deleteRecipe(recipeId)
                                                        Toast.makeText(context, "Рецепт удален", Toast.LENGTH_SHORT).show()
                                                        navController.popBackStack()
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Ошибка удаления", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        ) {
                                            Text("Удалить", color = Color.Red, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showDeleteDialog = false }) {
                                            Text("Отмена")
                                        }
                                    }
                                )
                            }
                        }
                    }

                    IconButton(onClick = {
                        if (currentUserId.isNullOrBlank()) {
                            Toast.makeText(context, "Нужно войти в аккаунт", Toast.LENGTH_SHORT).show()
                        } else if (isBlocked) {
                            Toast.makeText(context, "Действие недоступно: аккаунт заблокирован", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.toggleLike(recipeId, isLiked, currentUserId)
                            val msg = if (isLiked) "Лайк убран" else "Лайк поставлен"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Лайк",
                            tint = if (isLiked) UmamiOrange else Color.Gray
                        )
                    }

                    var showReportDialog by remember { mutableStateOf(false) }
                    IconButton(onClick = { 
                        if (currentUserId.isNullOrBlank()) {
                            Toast.makeText(context, "Нужно войти в аккаунт", Toast.LENGTH_SHORT).show()
                        } else {
                            showReportDialog = true 
                        }
                    }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Пожаловаться", tint = Color.Gray)
                    }

                    if (showReportDialog) {
                        ReportDialog(
                            onDismiss = { showReportDialog = false },
                            onSubmit = { reason, desc ->
                                viewModel.report("recipe", recipeId = recipeId.toLongOrNull(), reason = reason, description = desc) {
                                    Toast.makeText(context, "Жалоба отправлена", Toast.LENGTH_SHORT).show()
                                    showReportDialog = false
                                }
                            }
                        )
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
            when (val s = state) {
                is RecipeDetailState.Loading -> {
                    item {
                        CircularProgressIndicator(
                            color = UmamiOrange,
                            modifier = Modifier.padding(20.dp).fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }
                }

                is RecipeDetailState.Error -> {
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
                            Text(
                                "Не удалось загрузить рецепт",
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFontFamily,
                                color = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Проверьте подключение к интернету",
                                color = Color.Gray,
                                fontFamily = InterFontFamily
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.loadRecipe(recipeId, currentUserId) },
                                colors = ButtonDefaults.buttonColors(containerColor = UmamiOrange)
                            ) {
                                Text("Попробовать снова", fontFamily = InterFontFamily)
                            }
                        }
                    }
                }

                is RecipeDetailState.Success -> {
                    val recipe = s.recipe
                    val comments = s.comments

                    item {
                        if (!recipe.imageUrl.isNullOrBlank()) {
                            val image = normalizeImageUrl(recipe.imageUrl)
                            coil.compose.AsyncImage(
                                model = image,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color.LightGray)
                                    .clickable { showFullScreenImage = image },
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

                        if (showFullScreenImage != null) {
                            FullScreenImageViewer(imageUrl = showFullScreenImage!!, onDismiss = { showFullScreenImage = null })
                        }

                        Text(recipe.title, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp)

                        if (!recipe.description.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(recipe.description, fontFamily = InterFontFamily, color = Color.Gray, fontSize = 14.sp)
                        }

                        if (currentUserId != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            PersonalNoteSection(
                                initialNote = recipe.personalNote ?: "",
                                onSave = { viewModel.savePersonalNote(recipeId, it) }
                            )
                        }

                        if (recipe.User != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val isFollowing = recipe.User.isFollowing ?: false
                            
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFF9F9F9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth()
                                        .clickable { 
                                            val uid = recipe.User.id
                                            if (!uid.isNullOrBlank()) {
                                                navController.navigate("user_detail/$uid")
                                            }
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!recipe.User.avatarUrl.isNullOrBlank()) {
                                        coil.compose.AsyncImage(
                                            model = normalizeImageUrl(recipe.User.avatarUrl),
                                            contentDescription = "Аватар",
                                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.LightGray),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.size(40.dp).clip(CircleShape).background(UmamiOrange.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(recipe.User.username.firstOrNull()?.uppercase() ?: "?", fontWeight = FontWeight.Bold, color = UmamiOrange)
                                        }
                                    }
 
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            recipe.User.name ?: recipe.User.username,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = InterFontFamily,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text("@${recipe.User.username}", color = Color.Gray, fontFamily = InterFontFamily, fontSize = 12.sp)
                                    }
 
                                    if (currentUserId != null && recipe.User.id != currentUserId) {
                                        Button(
                                            onClick = {
                                                if (isBlocked) {
                                                    Toast.makeText(context, "Действие недоступно: аккаунт заблокирован", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    viewModel.toggleFollow(recipe.User.id, isFollowing)
                                                }
                                            },
                                            shape = RoundedCornerShape(20.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isFollowing) com.example.diplom.ui.theme.UmamiGreen.copy(alpha = 0.1f) else UmamiOrange,
                                                contentColor = if (isFollowing) com.example.diplom.ui.theme.UmamiGreen else Color.White
                                            ),
                                            border = if (isFollowing) androidx.compose.foundation.BorderStroke(1.dp, com.example.diplom.ui.theme.UmamiGreen.copy(alpha = 0.3f)) else null,
                                            modifier = Modifier.width(112.dp).height(36.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isFollowing) 0.dp else 2.dp)
                                        ) {
                                            Text(
                                                if (isFollowing) "Отписаться" else "Подписаться",
                                                fontSize = 12.sp,
                                                fontFamily = InterFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DetailStat("${recipe.cookingTime ?: 0} мин", "готовка")
                            DetailStat("${recipe.portion ?: "—"} порц.", "выход")
                            DetailStat("${recipe.calorific ?: "—"}", "ккал")
                        }

                        if (currentUserId != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            val isCooked = recipe.isCooked == true
                            Button(
                                onClick = { 
                                    if (isBlocked) {
                                        Toast.makeText(context, "Действие недоступно: аккаунт заблокирован", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.markAsCooked(recipeId)
                                        val toastMsg = if (isCooked) "Удалено из приготовленных" else "Отмечено как приготовленное!"
                                        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCooked) Color(0xFF4CAF50) else Color(0xFFE8F5E9),
                                    contentColor = if (isCooked) Color.White else Color(0xFF2E7D32)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle, 
                                    contentDescription = null, 
                                    tint = if (isCooked) Color.White else Color(0xFF2E7D32)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Приготовлено", fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                            }
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
                                                    val unit = ingredient.pivot?.unit ?: ""
                                                    val note = ingredient.pivot?.note ?: ""
                                                    val detail = listOfNotNull(
                                                        if (qty.isNotBlank()) "$qty $unit".trim() else null,
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
                                                    Text("Шаг ${step.stepNumber}", fontWeight = FontWeight.Bold, color = UmamiOrange, fontFamily = InterFontFamily, fontSize = 14.sp)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(step.description, fontFamily = InterFontFamily, fontSize = 14.sp)
                                                    if (!step.imageUrl.isNullOrEmpty()) {
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        coil.compose.AsyncImage(
                                                            model = normalizeImageUrl(step.imageUrl),
                                                            contentDescription = "Шаг ${step.stepNumber}",
                                                            modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp)),
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
                                var replyingTo by remember { mutableStateOf<Comment?>(null) }
                                
                                Column {
                                    Text("Отзывы (${comments.size})", fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    comments.filter { it.author?.isBlocked != true }.forEach { comment ->
                                        CommentItem(
                                            comment = comment,
                                            currentUserId = currentUserId,
                                            onLikeClick = { commentId ->
                                                if (isBlocked) {
                                                    Toast.makeText(context, "Действие недоступно: аккаунт заблокирован", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    viewModel.toggleCommentLike(commentId)
                                                }
                                            },
                                            onReplyClick = { replyComment ->
                                                if (isBlocked) {
                                                    Toast.makeText(context, "Действие недоступно: аккаунт заблокирован", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    replyingTo = replyComment
                                                }
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    if (currentUserId.isNullOrBlank()) {
                                        Text("Войдите, чтобы писать комментарии", color = Color.Gray, fontFamily = InterFontFamily)
                                    } else {
                                        var newComment by remember { mutableStateOf("") }
                                        var rating by remember { mutableIntStateOf(0) }
                                        var sweet by remember { mutableIntStateOf(0) }
                                        var sour by remember { mutableIntStateOf(0) }
                                        var salty by remember { mutableIntStateOf(0) }
                                        var spicy by remember { mutableIntStateOf(0) }
                                        var umami by remember { mutableIntStateOf(0) }

                                        if (replyingTo == null) {
                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                                                shape = RoundedCornerShape(16.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(16.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text(
                                                        text = "Оцените рецепт",
                                                        fontFamily = InterFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp,
                                                        color = Color(0xFF2D3748)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = if (rating == 0) "Нажмите на звезду, чтобы поставить оценку (необязательно)" else "Ваша оценка: $rating из 5",
                                                        fontFamily = InterFontFamily,
                                                        fontSize = 12.sp,
                                                        color = if (rating == 0) Color.Gray else UmamiOrange,
                                                        fontWeight = if (rating == 0) FontWeight.Normal else FontWeight.Medium
                                                    )
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    Row(
                                                        horizontalArrangement = Arrangement.Center,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        for (i in 1..5) {
                                                            val isFilled = i <= rating
                                                            Icon(
                                                                imageVector = Icons.Default.Star,
                                                                contentDescription = "Оценка $i",
                                                                tint = if (isFilled) Color(0xFFFFB000) else Color(0xFFE2E8F0),
                                                                modifier = Modifier
                                                                    .size(36.dp)
                                                                    .clickable {
                                                                        rating = if (rating == i) 0 else i
                                                                    }
                                                                    .padding(horizontal = 2.dp)
                                                            )
                                                        }
                                                    }
                                                    if (rating > 0) {
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        TextButton(
                                                            onClick = { rating = 0 },
                                                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red.copy(alpha = 0.7f))
                                                        ) {
                                                            Text("Сбросить оценку", fontSize = 11.sp, fontFamily = InterFontFamily)
                                                        }
                                                    }
                                                }
                                            }

                                            Text(
                                                text = "Вкусовой профиль (необязательно)",
                                                fontFamily = InterFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF2D3748),
                                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                            )

                                            TasteRatingSelector(
                                                emoji = "🍭",
                                                label = "Сладкий",
                                                value = sweet,
                                                onValueChange = { sweet = it },
                                                activeColor = Color(0xFFEC4899)
                                            )
                                            TasteRatingSelector(
                                                emoji = "🍋",
                                                label = "Кислый",
                                                value = sour,
                                                onValueChange = { sour = it },
                                                activeColor = Color(0xFFEAB308)
                                            )
                                            TasteRatingSelector(
                                                emoji = "🧂",
                                                label = "Соленый",
                                                value = salty,
                                                onValueChange = { salty = it },
                                                activeColor = Color(0xFF3B82F6)
                                            )
                                            TasteRatingSelector(
                                                emoji = "🌶",
                                                label = "Острый",
                                                value = spicy,
                                                onValueChange = { spicy = it },
                                                activeColor = Color(0xFFEF4444)
                                            )
                                            TasteRatingSelector(
                                                emoji = "🥩",
                                                label = "Умами",
                                                value = umami,
                                                onValueChange = { umami = it },
                                                activeColor = UmamiOrange
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                        }

                                        if (replyingTo != null) {
                                            Surface(
                                                color = UmamiOrange.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        "Ответ пользователю ${replyingTo!!.author?.username}",
                                                        fontSize = 12.sp,
                                                        color = UmamiOrange,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(20.dp)) {
                                                        Icon(Icons.Default.Close, contentDescription = "Отмена", tint = UmamiOrange)
                                                    }
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = newComment,
                                            onValueChange = { newComment = it },
                                            placeholder = { Text(if (replyingTo != null) "Ваш ответ..." else "Написать отзыв...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = if (replyingTo != null) RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp) else RoundedCornerShape(24.dp),
                                            trailingIcon = {
                                                IconButton(onClick = {
                                                    if (isBlocked) {
                                                        Toast.makeText(context, "Действие недоступно: аккаунт заблокирован", Toast.LENGTH_SHORT).show()
                                                    } else if (newComment.isNotBlank()) {
                                                        viewModel.postComment(
                                                            recipeId, newComment, 
                                                            if (replyingTo == null && rating > 0) rating else null, 
                                                            if (replyingTo == null && sweet > 0) sweet else null, 
                                                            if (replyingTo == null && sour > 0) sour else null, 
                                                            if (replyingTo == null && salty > 0) salty else null, 
                                                            if (replyingTo == null && spicy > 0) spicy else null, 
                                                            if (replyingTo == null && umami > 0) umami else null,
                                                            parentCommentId = replyingTo?.id
                                                        )
                                                        newComment = ""
                                                        rating = 0
                                                        sweet = 0
                                                        sour = 0
                                                        salty = 0
                                                        spicy = 0
                                                        umami = 0
                                                        replyingTo = null
                                                    }
                                                }) {
                                                    Icon(Icons.Default.Send, contentDescription = "Отправить", tint = UmamiOrange)
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
}

@Composable
fun CommentItem(
    comment: Comment,
    currentUserId: String?,
    onLikeClick: (String) -> Unit,
    onReplyClick: (Comment) -> Unit,
    depth: Int = 0
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    Column(modifier = Modifier.padding(start = (depth * 20).dp)) {
        Surface(
            color = if (depth > 0) Color(0xFFF0F0F0) else Color(0xFFF9F9F9),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            comment.author?.name ?: comment.author?.username ?: "Пользователь",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = InterFontFamily
                        )
                        
                        if (comment.rating != null && comment.rating > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                for (i in 1..5) {
                                    val isFilled = i <= comment.rating
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (isFilled) Color(0xFFFFB000) else Color(0xFFE2E8F0),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Like button
                    val cLiked = comment.isLiked ?: false
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { 
                            if (currentUserId != null) onLikeClick(comment.id)
                            else Toast.makeText(context, "Войдите, чтобы ставить лайки", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("${comment.likeCount ?: 0}", fontSize = 12.sp, color = if (cLiked) UmamiOrange else Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            if (cLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (cLiked) UmamiOrange else Color.Gray
                        )
                    }
                }
                
                val hasTastes = (comment.tasteSweet ?: 0) > 0 ||
                                (comment.tasteSour ?: 0) > 0 ||
                                (comment.tasteSalty ?: 0) > 0 ||
                                (comment.tasteSpicy ?: 0) > 0 ||
                                (comment.tasteUmami ?: 0) > 0
                                
                if (hasTastes) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if ((comment.tasteSweet ?: 0) > 0) {
                            TasteBadge("🍭", "Сладкий", comment.tasteSweet!!, Color(0xFFEC4899))
                        }
                        if ((comment.tasteSour ?: 0) > 0) {
                            TasteBadge("🍋", "Кислый", comment.tasteSour!!, Color(0xFFEAB308))
                        }
                        if ((comment.tasteSalty ?: 0) > 0) {
                            TasteBadge("🧂", "Соленый", comment.tasteSalty!!, Color(0xFF3B82F6))
                        }
                        if ((comment.tasteSpicy ?: 0) > 0) {
                            TasteBadge("🌶", "Острый", comment.tasteSpicy!!, Color(0xFFEF4444))
                        }
                        if ((comment.tasteUmami ?: 0) > 0) {
                            TasteBadge("🥩", "Умами", comment.tasteUmami!!, UmamiOrange)
                        }
                    }
                }
                
                Text(comment.content, fontSize = 14.sp, fontFamily = InterFontFamily, modifier = Modifier.padding(top = 6.dp))
                
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        "Ответить",
                        fontSize = 12.sp,
                        color = UmamiOrange,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { 
                            if (currentUserId != null) onReplyClick(comment)
                            else Toast.makeText(context, "Войдите, чтобы отвечать", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
        
        // Render replies
        comment.replies?.filter { it.author?.isBlocked != true }?.forEach { reply ->
            CommentItem(
                comment = reply,
                currentUserId = currentUserId,
                onLikeClick = onLikeClick, 
                onReplyClick = onReplyClick,
                depth = depth + 1
            )
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
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(value, color = UmamiOrange, fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = InterFontFamily)
            Text(label, color = Color.Gray, fontSize = 12.sp, fontFamily = InterFontFamily)
        }
    }
}

@Composable
fun NutritionRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontFamily = InterFontFamily, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
@Composable
fun PersonalNoteSection(initialNote: String, onSave: (String) -> Unit) {
    var note by remember(initialNote) { mutableStateOf(initialNote) }
    var isEditing by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFFFFF9C4), // Light yellow for note
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.StickyNote2, contentDescription = null, tint = Color(0xFFFBC02D))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Личная заметка", fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = InterFontFamily)
                }
                
                if (isEditing) {
                    TextButton(onClick = { 
                        onSave(note)
                        isEditing = false 
                    }) {
                        Text("Сохранить", color = Color(0xFFFBC02D), fontWeight = FontWeight.Bold)
                    }
                } else {
                    IconButton(onClick = { isEditing = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = Color.Gray)
                    }
                }
            }
            
            if (isEditing) {
                TextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color(0xFFFBC02D)
                    ),
                    placeholder = { Text("Добавьте что-то от себя...", fontSize = 13.sp) }
                )
            } else {
                Text(
                    text = if (note.isBlank()) "Нажмите, чтобы добавить заметку..." else note,
                    fontSize = 13.sp,
                    color = if (note.isBlank()) Color.Gray else Color.Black,
                    fontFamily = InterFontFamily,
                    modifier = Modifier.clickable { isEditing = true }
                )
            }
        }
    }
}

@Composable
fun Icon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color) {
    Icon(imageVector, contentDescription, modifier = Modifier.size(size), tint = tint)
}
@Composable
fun LikesSection(likes: List<RecipeLike>, navController: NavController) {
    if (likes.isEmpty()) return
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val displayLikes = likes.take(5)
        Box(modifier = Modifier.padding(end = 8.dp)) {
            displayLikes.forEachIndexed { index, like ->
                val user = like.user
                Surface(
                    modifier = Modifier.padding(start = (index * 20).dp).size(28.dp),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                    color = Color.LightGray
                ) {
                    if (!user?.avatarUrl.isNullOrBlank()) {
                        coil.compose.AsyncImage(
                            model = normalizeImageUrl(user!!.avatarUrl),
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.background(UmamiOrange.copy(alpha = 0.2f))) {
                            Text(user?.username?.firstOrNull()?.uppercase() ?: "?", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = UmamiOrange)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.width(if (displayLikes.isNotEmpty()) ( (displayLikes.size - 1) * 20 + 8).dp else 0.dp))

        val text = if (likes.size > 1) {
            "Нравится ${likes.size} пользователям"
        } else {
            "Нравится ${likes[0].user?.username ?: "1 пользователю"}"
        }
        
        Text(
            text = text,
            fontSize = 12.sp,
            fontFamily = InterFontFamily,
            color = Color.Gray,
            modifier = Modifier.clickable { 
                // Можно открыть список пользователей
            }
        )
    }
}

@Composable
fun TasteBadge(emoji: String, label: String, value: Int, color: Color) {
    Surface(
        color = color.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(end = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$label: $value/5",
                fontSize = 11.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
fun TasteRatingSelector(
    emoji: String,
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    activeColor: Color
) {
    val levels = listOf("Слабый", "Умеренный", "Выраженный", "Интенсивный", "Экстремальный")
    
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    fontFamily = InterFontFamily,
                    color = Color(0xFF2D3748)
                )
            }
            Text(
                text = if (value == 0) "Не выбран" else "${levels[value - 1]} ($value/5)",
                fontSize = 12.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                color = if (value == 0) Color.Gray else activeColor
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 1..5) {
                val isSelected = i == value
                val bgColor = if (isSelected) activeColor.copy(alpha = 0.15f) else Color(0xFFF7FAFC)
                val borderColor = if (isSelected) activeColor else Color(0xFFE2E8F0)
                val textColor = if (isSelected) activeColor else Color(0xFF4A5568)
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColor)
                        .clickable {
                            onValueChange(if (value == i) 0 else i)
                        }
                        .border(1.dp, borderColor, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$i",
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = textColor,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }
    }
}
