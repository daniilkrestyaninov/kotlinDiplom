package com.example.diplom

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.diplom.data.AdminViewModel
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiOrange
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(navController: NavController, viewModel: AdminViewModel = viewModel()) {
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastBody by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val statsState by viewModel.stats.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadStats()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Панель управления", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Общая статистика", fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            }

            item {
                when (val s = statsState) {
                    is com.example.diplom.data.AdminState.Loading -> {
                        CircularProgressIndicator(color = UmamiOrange, modifier = Modifier.padding(16.dp))
                    }
                    is com.example.diplom.data.AdminState.Success -> {
                        val stats = s.data
                        PlatformStatsChart(stats = stats, onNavigate = { route -> navController.navigate(route) })
                    }
                    else -> {
                        Text("Не удалось загрузить статистику", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }

            item {
                Text("Модерация", fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminMenuCard(
                        title = "Верификация",
                        icon = Icons.Default.VerifiedUser,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("admin_verifications") }
                    )
                    AdminMenuCard(
                        title = "Пользователи",
                        icon = Icons.Default.People,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("admin_users") }
                    )
                }
            }
            
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminMenuCard(
                        title = "Жалобы",
                        icon = Icons.Default.Report,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("admin_reports") }
                    )
                    AdminMenuCard(
                        title = "Апелляции",
                        icon = Icons.Default.Gavel,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("admin_appeals") }
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminMenuCard(
                        title = "Справочники",
                        icon = Icons.Default.FolderOpen,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("admin_metadata") }
                    )
                    AdminMenuCard(
                        title = "Меню недели",
                        icon = Icons.Default.RestaurantMenu,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("admin_menu") }
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminMenuCard(
                        title = "Рецепты",
                        icon = Icons.Default.Restaurant,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("admin_recipes") }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Push-рассылка", fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            }

            item {
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = broadcastTitle,
                            onValueChange = { broadcastTitle = it },
                            label = { Text("Заголовок") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = broadcastBody,
                            onValueChange = { broadcastBody = it },
                            label = { Text("Текст сообщения") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 3
                        )
                        Button(
                            onClick = {
                                if (broadcastTitle.isNotBlank() && broadcastBody.isNotBlank()) {
                                    viewModel.broadcastNotification(broadcastTitle, broadcastBody) {
                                        broadcastTitle = ""
                                        broadcastBody = ""
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Рассылка успешно отправлена")
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = UmamiOrange),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Отправить всем пользователям")
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Логи действий", fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            }
            
            item {
                AdminMenuCard(
                    title = "Просмотр аудита",
                    icon = Icons.Default.History,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { navController.navigate("admin_audit_logs") }
                )
            }
        }
    }
}

@Composable
fun AdminMenuCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = UmamiOrange, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = InterFontFamily, color = Color.DarkGray)
        }
    }
}

@Composable
fun DashboardMetricCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        modifier = Modifier.width(130.dp).height(80.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    title,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Text(
                value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                color = Color.DarkGray,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PlatformStatsChart(stats: Map<String, Any>, onNavigate: (String) -> Unit) {
    val users = (stats["users"] ?: 0).toString().toDoubleOrNull()?.toInt() ?: 0
    val recipes = (stats["recipes"] ?: 0).toString().toDoubleOrNull()?.toInt() ?: 0
    val comments = (stats["comments"] ?: 0).toString().toDoubleOrNull()?.toInt() ?: 0
    val reports = (stats["reports"] ?: 0).toString().toDoubleOrNull()?.toInt() ?: 0
    val appeals = (stats["appeals"] ?: 0).toString().toDoubleOrNull()?.toInt() ?: 0
    val verifications = (stats["verifications"] ?: 0).toString().toDoubleOrNull()?.toInt() ?: 0

    // Chart items mapping
    data class ChartItem(
        val name: String, 
        val count: Int, 
        val color: Color, 
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val route: String?,
        val details: String,
        val actionText: String?
    )
    val items = listOf(
        ChartItem(
            "Пользователи", 
            users, 
            Color(0xFF2196F3), 
            Icons.Default.People, 
            "admin_users", 
            "Зарегистрированные кулинары на платформе. Нажмите для управления аккаунтами, изменения ролей и блокировок.",
            "Управлять пользователями"
        ),
        ChartItem(
            "Рецепты", 
            recipes, 
            Color(0xFF4CAF50), 
            Icons.Default.Book, 
            "admin_recipes", 
            "Опубликованные кулинарные рецепты. Нажмите для просмотра каталога, скрытия рецептов или изменения статуса модерации.",
            "Модерация рецептов"
        ),
        ChartItem(
            "Отзывы", 
            comments, 
            Color(0xFF9C27B0), 
            Icons.Default.Comment, 
            null, 
            "Комментарии и оценки под рецептами. Пользователи активно обмениваются кулинарным опытом и ставят оценки!",
            null
        ),
        ChartItem(
            "Жалобы", 
            reports, 
            Color(0xFFF44336), 
            Icons.Default.Report, 
            "admin_reports", 
            "Жалобы от пользователей на рецепты и профили. Требуют оперативного разбора и принятия мер модератором.",
            "Открыть жалобы"
        ),
        ChartItem(
            "Апелляции", 
            appeals, 
            Color(0xFFFF9800), 
            Icons.Default.Gavel, 
            "admin_appeals", 
            "Апелляции на блокировку от пользователей. Требуют беспристрастной перепроверки модератором.",
            "Разбор апелляций"
        ),
        ChartItem(
            "Верификации", 
            verifications, 
            Color(0xFF009688), 
            Icons.Default.VerifiedUser, 
            "admin_verifications", 
            "Заявки на получение статуса верифицированного повара-профессионала. Нажмите для проверки дипломов.",
            "Рассмотреть заявки"
        )
    )

    val maxVal = items.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        "Активность платформы",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color.Black,
                        fontFamily = InterFontFamily
                    )
                    Text(
                        "Нажмите на столбец для интерактивных действий",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontFamily = InterFontFamily
                    )
                }
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = null,
                    tint = UmamiOrange,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Graph Area (height 200.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                // Background dashed gridlines
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(5) { i ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFF5F5F5))
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    items.forEachIndexed { index, item ->
                        val isSelected = selectedIndex == index
                        val anySelected = selectedIndex != null
                        
                        // Fading out non-selected bars
                        val alpha = if (isSelected) 1f else if (anySelected) 0.25f else 0.85f
                        
                        val ratio = item.count.toFloat() / maxVal.toFloat()
                        val targetBarHeightFactor = ratio.coerceAtLeast(0.06f)

                        // Smooth Height Animation!
                        val barHeightFactor by animateFloatAsState(
                            targetValue = targetBarHeightFactor,
                            animationSpec = tween(durationMillis = 800)
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedIndex = if (isSelected) null else index
                                }
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            // Value text
                            Text(
                                text = item.count.toString(),
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                fontSize = if (isSelected) 14.sp else 12.sp,
                                color = item.color.copy(alpha = alpha),
                                fontFamily = InterFontFamily
                            )
                            
                            Spacer(modifier = Modifier.height(6.dp))

                            // Colored Bar with dynamic gradient & glow
                            Box(
                                modifier = Modifier
                                    .width(if (isSelected) 24.dp else 18.dp)
                                    .fillMaxHeight(barHeightFactor * 0.72f) // Scale it slightly to fit numbers nicely
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = if (isSelected) {
                                                listOf(item.color, item.color.copy(alpha = 0.7f))
                                            } else {
                                                listOf(item.color.copy(alpha = alpha), item.color.copy(alpha = alpha * 0.4f))
                                            }
                                        ),
                                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) item.color else Color.Transparent,
                                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                    )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Icon
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = if (isSelected) item.color else Color.Gray.copy(alpha = alpha),
                                modifier = Modifier.size(if (isSelected) 20.dp else 16.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(2.dp))

                            // Short Label
                            val shortLabel = when (item.name) {
                                "Пользователи" -> "Польз."
                                "Рецепты" -> "Рецепты"
                                "Отзывы" -> "Отзывы"
                                "Жалобы" -> "Жалобы"
                                "Апелляции" -> "Апел."
                                "Верификации" -> "Вериф."
                                else -> item.name
                            }
                            Text(
                                text = shortLabel,
                                fontSize = 9.sp,
                                color = if (isSelected) item.color else Color.Gray.copy(alpha = alpha),
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                fontFamily = InterFontFamily,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive details section
            androidx.compose.animation.AnimatedVisibility(
                visible = selectedIndex != null,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
            ) {
                if (selectedIndex != null) {
                    val item = items[selectedIndex!!]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = item.color.copy(alpha = 0.05f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, item.color.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(item.color.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = item.color,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.Black,
                                        fontFamily = InterFontFamily
                                    )
                                    Text(
                                        text = "Показатель: ${item.count}",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = item.color,
                                        fontFamily = InterFontFamily
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Text(
                                text = item.details,
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                fontFamily = InterFontFamily,
                                lineHeight = 16.sp
                            )
                            
                            if (item.route != null && item.actionText != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { onNavigate(item.route) },
                                    colors = ButtonDefaults.buttonColors(containerColor = item.color),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.actionText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        fontFamily = InterFontFamily
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Hint displayed when nothing is selected
            androidx.compose.animation.AnimatedVisibility(
                visible = selectedIndex == null,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(Color(0xFFF9F9F9), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Выберите столбец для подробностей и быстрых действий",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5))
            Spacer(modifier = Modifier.height(16.dp))

            // Platform Insight Indicators
            Text(
                "Аналитические инсайты",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.Black,
                fontFamily = InterFontFamily,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Ratio: Recipes per User
                val recipesPerUser = if (users > 0) String.format("%.2f", recipes.toDouble() / users) else "0.00"
                InsightRow(
                    label = "Индекс наполнения контентом",
                    value = "$recipesPerUser рец./польз.",
                    desc = "Количество рецептов на одного зарегистрированного пользователя.",
                    color = Color(0xFF4CAF50)
                )

                // Ratio: Report percentage
                val reportRatio = if (recipes > 0) ((reports.toDouble() / recipes) * 100).toInt() else 0
                InsightRow(
                    label = "Уровень конфликтности контента",
                    value = "$reportRatio%",
                    desc = "Соотношение поданных жалоб к общему числу рецептов на платформе.",
                    color = Color(0xFFF44336)
                )

                // Moderation Load
                val pendingModeration = reports + appeals
                InsightRow(
                    label = "Нагрузка на модерацию",
                    value = "$pendingModeration инц.",
                    desc = "Суммарное количество жалоб и апелляций, требующих внимания.",
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
fun InsightRow(label: String, value: String, desc: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9F9F9), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.Black,
                fontFamily = InterFontFamily
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = Color.Gray,
                fontFamily = InterFontFamily,
                lineHeight = 13.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = color,
            fontFamily = InterFontFamily
        )
    }
}
