package com.example.diplom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.diplom.data.AdminState
import com.example.diplom.data.AdminViewModel
import com.example.diplom.data.ReportItem
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiOrange
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsScreen(navController: NavController, viewModel: AdminViewModel = viewModel()) {
    val reportsState by viewModel.reports.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadReports()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Жалобы", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = reportsState) {
            is AdminState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = UmamiOrange)
                }
            }
            is AdminState.Success -> {
                if (state.data.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Нет активных жалоб", fontFamily = InterFontFamily, color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
                        items(state.data) { report ->
                            ReportCard(
                                report = report,
                                onNavigateToUser = { id -> if (id.isNotBlank()) navController.navigate("user_detail/$id") },
                                onNavigateToRecipe = { id -> if (id.isNotBlank()) navController.navigate("recipe_detail/$id") },
                                onManageUser = { navController.navigate("admin_users") },
                                onStatusUpdate = { newStatus ->
                                    viewModel.updateReportStatus(report.id, newStatus)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Статус жалобы обновлен")
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
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

@Composable
fun ReportCard(
    report: ReportItem,
    onNavigateToUser: (String) -> Unit,
    onNavigateToRecipe: (String) -> Unit,
    onManageUser: () -> Unit,
    onStatusUpdate: (String) -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = when (report.type) {
                        "recipe" -> Color(0xFFFFE0B2)
                        "user" -> Color(0xFFBBDEFB)
                        else -> Color(0xFFF5F5F5)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when (report.type) {
                            "recipe" -> "Рецепт"
                            "user" -> "Пользователь"
                            else -> report.type
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (report.status.lowercase()) {
                        "pending" -> "На рассмотрении"
                        "resolved" -> "Решено"
                        "dismissed" -> "Отклонено"
                        else -> report.status
                    }.uppercase(),
                    fontSize = 12.sp,
                    color = if (report.status == "pending") Color.Red else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Причина: ${report.reason}", fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            if (!report.description.isNullOrBlank()) {
                Text(report.description, fontSize = 14.sp, color = Color.Gray, fontFamily = InterFontFamily)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))
            Text("Отправил: ${report.Reporter?.username ?: "???"}", fontSize = 13.sp, fontFamily = InterFontFamily)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (report.type == "recipe" && report.ReportedRecipe != null) {
                OutlinedButton(
                    onClick = { onNavigateToRecipe(report.ReportedRecipe.id.toString()) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = UmamiOrange)
                ) {
                    Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ПЕРЕЙТИ К РЕЦЕПТУ: ${report.ReportedRecipe.title}", fontSize = 12.sp)
                }
            } else if (report.ReportedUser != null) {
                OutlinedButton(
                    onClick = { onNavigateToUser(report.ReportedUser.id.toString()) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = UmamiOrange)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ПЕРЕЙТИ В ПРОФИЛЬ: ${report.ReportedUser.username}", fontSize = 12.sp)
                }
            }
            
            if (report.status == "pending") {
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("Действия модератора:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onStatusUpdate("resolved") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ПРИНЯТЬ", fontWeight = FontWeight.Bold)
                            Text("и удалить объект", fontSize = 9.sp)
                        }
                    }
                    Button(
                        onClick = { onStatusUpdate("dismissed") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ОТКЛОНИТЬ", color = Color.Black, fontWeight = FontWeight.Bold)
                            Text("проигнорировать", fontSize = 9.sp, color = Color.Black)
                        }
                    }
                }
                
                if (report.type != "recipe" && report.ReportedUser != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onManageUser,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Перейти к управлению пользователем", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
