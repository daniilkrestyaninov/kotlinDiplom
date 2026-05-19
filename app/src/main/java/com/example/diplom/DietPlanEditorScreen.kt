package com.example.diplom

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.diplom.data.*
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiOrange
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietPlanEditorScreen(
    navController: NavController,
    planId: String? = null,
    currentUserId: String? = null,
    viewModel: DietViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    
    // In-memory compiler state: List of scheduled recipes
    val compiledRecipes = remember { mutableStateListOf<DietPlanRecipeState>() }
    var selectedDay by remember { mutableStateOf(1) } // 1-7 (Mon-Sun)
    
    var showRecipeSearchDialog by remember { mutableStateOf(false) }

    // Prepopulate if editing an existing plan
    LaunchedEffect(planId) {
        if (!planId.isNullOrEmpty()) {
            viewModel.loadPlanDetail(planId)
        }
    }

    val state by viewModel.state
    LaunchedEffect(state) {
        val s = state
        if (s is DietState.Success && s.currentDetailPlan != null && planId != null) {
            val plan = s.currentDetailPlan
            title = plan.title
            description = plan.description ?: ""
            isPrivate = plan.isPrivate
            
            compiledRecipes.clear()
            plan.dayRecipes?.forEach { item ->
                compiledRecipes.add(
                    DietPlanRecipeState(
                        recipeId = item.recipeId,
                        recipeTitle = item.Recipe?.title ?: "Рецепт #${item.recipeId}",
                        dayOfWeek = item.dayOfWeek,
                        mealOrder = item.mealOrder
                    )
                )
            }
        }
    }

    if (showRecipeSearchDialog) {
        RecipeSearchAndSelectDialog(
            currentUserId = currentUserId,
            onDismiss = { showRecipeSearchDialog = false },
            onSelect = { recipe, mealOrder ->
                compiledRecipes.add(
                    DietPlanRecipeState(
                        recipeId = recipe.id,
                        recipeTitle = recipe.title,
                        dayOfWeek = selectedDay,
                        mealOrder = mealOrder
                    )
                )
                showRecipeSearchDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (planId == null) "Создать меню" else "Редактировать", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFFAFAFA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Horizontal scrollable setup for forms and schedule
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название плана питания", fontFamily = InterFontFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = UmamiOrange,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                // Description Field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание (диеты, цели, рекомендации)", fontFamily = InterFontFamily) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = UmamiOrange,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                // Privacy Switch Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Личное (Приватное) меню",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = InterFontFamily
                        )
                        Text(
                            text = "Если включено, только вы сможете видеть этот рацион.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontFamily = InterFontFamily
                        )
                    }
                    Switch(
                        checked = isPrivate,
                        onCheckedChange = { isPrivate = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = UmamiOrange
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFFEEEEEE))

                // Weekly calendar day tabs
                Text(
                    "Расписание по дням",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    fontFamily = InterFontFamily
                )

                val days = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                TabRow(
                    selectedTabIndex = selectedDay - 1,
                    containerColor = Color.White,
                    contentColor = UmamiOrange,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedDay - 1]),
                            color = UmamiOrange
                        )
                    }
                ) {
                    days.forEachIndexed { index, day ->
                        Tab(
                            selected = selectedDay == index + 1,
                            onClick = { selectedDay = index + 1 },
                            text = { Text(day, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        )
                    }
                }

                // Add button for current day
                Button(
                    onClick = { showRecipeSearchDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFECE9), contentColor = UmamiOrange),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Добавить блюдо в расписание", fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                }

                // Filtered compiler state for this day
                val itemsForDay = compiledRecipes.filter { it.dayOfWeek == selectedDay }.sortedBy { it.mealOrder }
                
                if (itemsForDay.isEmpty()) {
                    Text(
                        "Меню на этот день пустое. Добавьте завтрак, обед или ужин!",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontFamily = InterFontFamily,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    itemsForDay.forEach { item ->
                        val mealLabel = when (item.mealOrder) {
                            1 -> "🌅 Завтрак"
                            2 -> "☀️ Обед"
                            3 -> "🍎 Полдник"
                            4 -> "🌙 Ужин"
                            else -> "🍿 Перекус"
                        }
                        
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(mealLabel, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                    Text(item.recipeTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                }
                                IconButton(onClick = { compiledRecipes.remove(item) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Удалить", tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            // Save actions at bottom
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            Toast.makeText(context, "Укажите название меню", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        val mappedRecipes = compiledRecipes.map {
                            DietPlanRecipeRequest(
                                recipeId = it.recipeId,
                                dayOfWeek = it.dayOfWeek,
                                mealOrder = it.mealOrder
                            )
                        }

                        if (planId == null) {
                            viewModel.createPlan(title, description, isPrivate, mappedRecipes) {
                                Toast.makeText(context, "План питания создан успешно!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                        } else {
                            viewModel.updatePlan(planId, title, description, isPrivate, mappedRecipes) {
                                Toast.makeText(context, "План питания сохранён!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = UmamiOrange),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Сохранить и составить рацион", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                }
            }
        }
    }
}

// Dialog to search recipes and select meal slots
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeSearchAndSelectDialog(
    currentUserId: String? = null,
    onDismiss: () -> Unit,
    onSelect: (Recipe, Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(0) } // 0: Все, 1: Мои, 2: Избранное
    var searchResults by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var selectedRecipe by remember { mutableStateOf<Recipe?>(null) }
    var selectedMealOrder by remember { mutableStateOf(1) } // Default: breakfast

    LaunchedEffect(query, selectedFilter) {
        try {
            val rawRecipes = when (selectedFilter) {
                0 -> ApiClient.recipeService.getRecipes(search = query.ifBlank { null })
                1 -> {
                    if (!currentUserId.isNullOrBlank()) {
                        ApiClient.recipeService.getRecipes(userId = currentUserId, search = query.ifBlank { null })
                    } else {
                        emptyList()
                    }
                }
                2 -> {
                    val favorites = ApiClient.userService.getFavorites()
                    val favRecipes = favorites.mapNotNull { it.recipe }
                    if (query.isNotBlank()) {
                        favRecipes.filter { it.title.contains(query, ignoreCase = true) }
                    } else {
                        favRecipes
                    }
                }
                else -> emptyList()
            }
            searchResults = rawRecipes
        } catch (_: Exception) {}
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Добавить в расписание",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    fontFamily = InterFontFamily,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (selectedRecipe == null) {
                    // Step 1: Search recipes
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Поиск рецептов...", fontFamily = InterFontFamily) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Premium Filter Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        val filters = listOf(
                            0 to "Все",
                            1 to "Мои",
                            2 to "Избранное"
                        )
                        filters.forEach { (filterId, label) ->
                            val isSelected = selectedFilter == filterId
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { selectedFilter = filterId },
                                color = if (isSelected) UmamiOrange else Color(0xFFF5F5F5),
                                contentColor = if (isSelected) Color.White else Color.DarkGray
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Рецепты не найдены",
                                fontFamily = InterFontFamily,
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(280.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(searchResults, key = { it.id }) { recipe ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFFFAFAFA))
                                        .clickable { selectedRecipe = recipe }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Thumbnail Preview
                                    val imageToShow = recipe.imageUrl
                                    if (!imageToShow.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = normalizeImageUrl(imageToShow),
                                            contentDescription = recipe.title,
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color.LightGray),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(UmamiOrange.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("🍴", fontSize = 20.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = recipe.title,
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.Black,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            if (recipe.cookingTime != null) {
                                                Text(
                                                    text = "⏱ ${recipe.cookingTime} мин",
                                                    fontFamily = InterFontFamily,
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            val authorName = recipe.User?.name ?: recipe.User?.username
                                            if (!authorName.isNullOrBlank()) {
                                                Text(
                                                    text = "👤 $authorName",
                                                    fontFamily = InterFontFamily,
                                                    fontSize = 11.sp,
                                                    color = Color.Gray,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Step 2: Choose Meal slot
                    Text("Выбран рецепт: \"${selectedRecipe!!.title}\"", fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Выберите приём пищи:", fontSize = 12.sp, color = Color.Gray, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(8.dp))

                    val meals = listOf(
                        1 to "🌅 Завтрак",
                        2 to "☀️ Обед",
                        3 to "🍎 Полдник",
                        4 to "🌙 Ужин",
                        5 to "🍿 Перекус"
                    )

                    meals.forEach { (order, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMealOrder = order }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMealOrder == order,
                                onClick = { selectedMealOrder = order },
                                colors = RadioButtonDefaults.colors(selectedColor = UmamiOrange)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { selectedRecipe = null }) {
                            Text("Назад", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onSelect(selectedRecipe!!, selectedMealOrder) },
                            colors = ButtonDefaults.buttonColors(containerColor = UmamiOrange)
                        ) {
                            Text("Добавить", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

data class DietPlanRecipeState(
    val recipeId: Long,
    val recipeTitle: String,
    val dayOfWeek: Int,
    val mealOrder: Int
)
