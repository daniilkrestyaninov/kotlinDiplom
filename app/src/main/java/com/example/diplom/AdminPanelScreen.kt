package com.example.diplom

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
                AdminMenuCard(
                    title = "Жалобы",
                    icon = Icons.Default.Report,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { navController.navigate("admin_reports") }
                )
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
