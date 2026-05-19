package com.example.diplom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.diplom.data.AdminState
import com.example.diplom.data.AdminViewModel
import com.example.diplom.data.MenuOfTheWeekItem
import com.example.diplom.data.Recipe
import com.example.diplom.data.normalizeImageUrl
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiOrange
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMenuScreen(navController: NavController, viewModel: AdminViewModel = viewModel()) {
    val menuState by viewModel.menuOfWeek.collectAsState()
    val recipesState by viewModel.recipes.collectAsState()
    
    var selectedDay by remember { mutableStateOf(1) } // 1 = Monday, ..., 7 = Sunday
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val daysOfWeek = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    val daysOfWeekFull = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")

    LaunchedEffect(Unit) {
        viewModel.loadMenuOfWeek()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Составление меню недели", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    searchQuery = ""
                    viewModel.searchRecipes("") // Clear previous search
                    showAddDialog = true 
                },
                containerColor = UmamiOrange,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить рецепт")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Horizontal Day Selector
            TabRow(
                selectedTabIndex = selectedDay - 1,
                containerColor = Color.White,
                contentColor = UmamiOrange,
                modifier = Modifier.fillMaxWidth()
            ) {
                daysOfWeek.forEachIndexed { index, day ->
                    Tab(
                        selected = selectedDay == index + 1,
                        onClick = { selectedDay = index + 1 },
                        text = { Text(day, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    )
                }
            }

            Text(
                text = daysOfWeekFull[selectedDay - 1],
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            when (val state = menuState) {
                is AdminState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = UmamiOrange)
                    }
                }
                is AdminState.Success -> {
                    val dayItems = state.data.filter { it.day_of_week == selectedDay }
                    
                    if (dayItems.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.RestaurantMenu, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "В меню этого дня пока нет рецептов",
                                    color = Color.Gray,
                                    fontSize = 16.sp,
                                    fontFamily = InterFontFamily
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(dayItems, key = { it.id }) { item ->
                                item.Recipe?.let { recipe ->
                                    AdminMenuRecipeCard(
                                        recipe = recipe,
                                        onRemove = {
                                            viewModel.removeFromMenuOfWeek(item.id) { success, error ->
                                                if (success) {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Рецепт удален из меню")
                                                    }
                                                } else {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(error ?: "Ошибка при удалении")
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                is AdminState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = Color.Red, fontFamily = InterFontFamily)
                    }
                }
                else -> {}
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { 
                Text(
                    "Добавить рецепт: ${daysOfWeekFull[selectedDay - 1]}",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ) 
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { 
                            searchQuery = it 
                            viewModel.searchRecipes(it)
                        },
                        placeholder = { Text("Поиск рецептов...", fontFamily = InterFontFamily) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    
                    if (recipesState.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                            Text("Рецепты не найдены", color = Color.Gray, fontFamily = InterFontFamily)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(recipesState) { recipe ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addToMenuOfWeek(selectedDay, recipe.id) { success, error ->
                                                if (success) {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Рецепт успешно добавлен в меню")
                                                    }
                                                } else {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(error ?: "Ошибка при добавлении в меню")
                                                    }
                                                }
                                            }
                                            showAddDialog = false
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFF5F5F5))
                                    ) {
                                        if (recipe.imageUrl != null) {
                                            AsyncImage(
                                                model = normalizeImageUrl(recipe.imageUrl),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(Icons.Default.Restaurant, contentDescription = null, tint = Color.Gray, modifier = Modifier.align(Alignment.Center))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = recipe.title ?: "Без названия",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Закрыть", fontFamily = InterFontFamily)
                }
            }
        )
    }
}

@Composable
fun AdminMenuRecipeCard(recipe: Recipe, onRemove: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5))
            ) {
                if (recipe.imageUrl != null) {
                    AsyncImage(
                        model = normalizeImageUrl(recipe.imageUrl),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Restaurant, contentDescription = null, tint = Color.Gray, modifier = Modifier.align(Alignment.Center))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipe.title ?: "Без названия",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Автор: ${recipe.User?.name ?: recipe.User?.username ?: "Аноним"}",
                    fontFamily = InterFontFamily,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.Red)
            }
        }
    }
}
