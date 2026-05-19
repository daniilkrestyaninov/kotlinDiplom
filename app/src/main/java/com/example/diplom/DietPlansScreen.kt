package com.example.diplom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.diplom.data.*
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietPlansScreen(
    navController: NavController,
    currentUserId: String? = null,
    viewModel: DietViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val state by viewModel.state

    LaunchedEffect(searchQuery) {
        viewModel.loadPlans(searchQuery.ifBlank { null })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Планы питания", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("diet_plan_editor") },
                containerColor = UmamiOrange,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Создать план питания")
            }
        },
        containerColor = Color(0xFFFAFAFA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Switcher
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = UmamiOrange,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = UmamiOrange
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Мои планы", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                    selectedContentColor = UmamiOrange,
                    unselectedContentColor = Color.Gray
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Обзор рационов", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                    selectedContentColor = UmamiOrange,
                    unselectedContentColor = Color.Gray
                )
            }

            if (selectedTab == 1) {
                // Search Field for Browse Tab
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    placeholder = { Text("Поиск планов питания...", fontFamily = InterFontFamily) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = UmamiOrange,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    singleLine = true
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Pull to refresh view or simple lazy column list
            when (val s = state) {
                is DietState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = UmamiOrange)
                    }
                }
                is DietState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(s.message, color = Color.Red, fontFamily = InterFontFamily)
                    }
                }
                is DietState.Success -> {
                    val listToShow = if (selectedTab == 0) s.myPlans else s.publicPlans

                    if (listToShow.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (selectedTab == 0) "У вас пока нет планов питания.\nНажмите +, чтобы составить!" else "Публичные рационы не найдены.",
                                    fontFamily = InterFontFamily,
                                    color = Color.Gray,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(listToShow, key = { it.id ?: 0 }) { plan ->
                                DietPlanCard(plan = plan, onClick = {
                                    navController.navigate("diet_plan_detail/${plan.id}")
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DietPlanCard(plan: DietPlan, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.title,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                
                // Privacy Icon Indicator
                if (plan.isPrivate) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFECEFF1))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Приватный", fontSize = 10.sp, color = Color.Gray, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Публичный", fontSize = 10.sp, color = Color(0xFF2E7D32), fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (!plan.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = plan.description,
                    fontFamily = InterFontFamily,
                    color = Color.DarkGray,
                    fontSize = 13.sp,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Author credit
                val authorName = plan.Author?.name ?: plan.Author?.username ?: "Пользователь"
                Text(
                    text = "Автор: $authorName",
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                
                // Date or count indicator
                Text(
                    text = "🍽 Меню на неделю",
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = UmamiOrange,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
