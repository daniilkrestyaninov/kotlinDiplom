package com.example.diplom

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.diplom.data.*
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietPlanDetailScreen(
    navController: NavController,
    planId: String,
    currentUserId: String? = null,
    viewModel: DietViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state
    var selectedDay by remember { mutableStateOf(1) } // 1: Mon, 7: Sun
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(planId) {
        viewModel.loadPlanDetail(planId)
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить план питания?", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
            text = { Text("Это действие нельзя отменить.", fontFamily = InterFontFamily) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlan(planId) {
                        Toast.makeText(context, "План питания удалён", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                    showDeleteConfirm = false
                }) {
                    Text("Удалить", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Отмена", color = Color.Gray)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            val currentPlan = (state as? DietState.Success)?.currentDetailPlan
            val isAuthor = currentPlan != null && currentUserId != null && currentPlan.userId.toString() == currentUserId
            
            TopAppBar(
                title = { Text(currentPlan?.title ?: "План питания", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (isAuthor) {
                        IconButton(onClick = { navController.navigate("diet_plan_editor?planId=$planId") }) {
                            Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = UmamiOrange)
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.Red)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFFAFAFA)
    ) { paddingValues ->
        when (val s = state) {
            is DietState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = UmamiOrange)
                }
            }
            is DietState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text(s.message, color = Color.Red, fontFamily = InterFontFamily)
                }
            }
            is DietState.Success -> {
                val plan = s.currentDetailPlan
                if (plan == null) {
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        Text("План питания не найден", fontFamily = InterFontFamily)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        // Description Header Card
                        if (!plan.description.isNullOrBlank()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                shadowElevation = 1.dp
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Описание рационов",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = UmamiOrange,
                                        fontFamily = InterFontFamily
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = plan.description,
                                        fontSize = 14.sp,
                                        color = Color.DarkGray,
                                        fontFamily = InterFontFamily
                                    )
                                }
                            }
                        }

                        // Day of Week Picker (1-7)
                        val days = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                        ScrollableTabRow(
                            selectedTabIndex = selectedDay - 1,
                            containerColor = Color.White,
                            contentColor = UmamiOrange,
                            edgePadding = 16.dp,
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
                                    text = { Text(day, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                                )
                            }
                        }

                        // Scheduled recipes for the selected day
                        val dayRecipes = plan.dayRecipes?.filter { it.dayOfWeek == selectedDay }?.sortedBy { it.mealOrder } ?: emptyList()

                        if (dayRecipes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.RestaurantMenu,
                                        contentDescription = null,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "В этот день меню пустое.",
                                        fontFamily = InterFontFamily,
                                        color = Color.Gray
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(dayRecipes, key = { it.id ?: 0 }) { item ->
                                    val mealLabel = when (item.mealOrder) {
                                        1 -> "🌅 Завтрак"
                                        2 -> "☀️ Обед"
                                        3 -> "🍎 Полдник"
                                        4 -> "🌙 Ужин"
                                        else -> "🍿 Перекус"
                                    }
                                    
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = mealLabel,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = Color.Gray,
                                            fontFamily = InterFontFamily,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                        
                                        val recipe = item.Recipe
                                        if (recipe != null) {
                                            ScheduledRecipeCard(recipe = recipe, onClick = {
                                                navController.navigate("recipe_detail/${recipe.id}")
                                            })
                                        } else {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                color = Color(0xFFF5F5F5),
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Text(
                                                    "Рецепт удалён или недоступен",
                                                    modifier = Modifier.padding(16.dp),
                                                    color = Color.Gray,
                                                    fontSize = 13.sp,
                                                    fontFamily = InterFontFamily
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

@Composable
fun ScheduledRecipeCard(recipe: Recipe, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageToShow = recipe.imageUrl
            if (!imageToShow.isNullOrEmpty()) {
                AsyncImage(
                    model = normalizeImageUrl(imageToShow),
                    contentDescription = recipe.title,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(UmamiOrange.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🍴", fontSize = 24.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipe.title,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black,
                    maxLines = 1
                )
                
                if (recipe.cookingTime != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⏱ ${recipe.cookingTime} мин",
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
