package com.example.diplom

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import com.example.diplom.data.VerificationRequest
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationScreen(navController: NavController, viewModel: AdminViewModel = viewModel()) {
    val state by viewModel.verifications.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadVerifications()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Заявки на верификацию", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is AdminState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = UmamiOrange)
                }
                is AdminState.Success -> {
                    val pending = s.data.filter { it.status == "pending" }
                    if (pending.isEmpty()) {
                        Text("Нет новых заявок", modifier = Modifier.align(Alignment.Center), color = Color.Gray, fontFamily = InterFontFamily)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(pending) { request ->
                                VerificationItem(
                                    request = request,
                                    onAccept = { viewModel.updateVerificationStatus(request.id, "approved") },
                                    onReject = { viewModel.updateVerificationStatus(request.id, "rejected") }
                                )
                            }
                        }
                    }
                }
                is AdminState.Error -> {
                    Text("Ошибка: ${s.message}", modifier = Modifier.align(Alignment.Center), color = Color.Red, fontFamily = InterFontFamily)
                }
                else -> {}
            }
        }
    }
}

@Composable
fun VerificationItem(request: VerificationRequest, onAccept: () -> Unit, onReject: () -> Unit) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Пользователь: ${request.User?.username ?: "ID ${request.user_id}"}", fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = InterFontFamily)
            Text("ФИО: ${request.full_name}", color = Color.DarkGray, fontSize = 14.sp, fontFamily = InterFontFamily)
            if (!request.info.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Инфо: ${request.info}", fontSize = 13.sp, color = Color.Gray, fontFamily = InterFontFamily)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Одобрить", fontSize = 12.sp)
                }
                Button(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Отклонить", fontSize = 12.sp)
                }
            }
        }
    }
}
