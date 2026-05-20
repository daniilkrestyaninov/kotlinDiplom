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

    var selectedRequest by remember { mutableStateOf<VerificationRequest?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var isApproveAction by remember { mutableStateOf(true) }

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
                                    onAccept = {
                                        selectedRequest = request
                                        isApproveAction = true
                                        showDialog = true
                                    },
                                    onReject = {
                                        selectedRequest = request
                                        isApproveAction = false
                                        showDialog = true
                                    }
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

    if (showDialog && selectedRequest != null) {
        ReviewVerificationDialog(
            request = selectedRequest!!,
            isApprove = isApproveAction,
            onDismiss = { showDialog = false },
            onAction = { notes ->
                val status = if (isApproveAction) "approved" else "rejected"
                viewModel.updateVerificationStatus(selectedRequest!!.id, status, notes)
                showDialog = false
            }
        )
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
            Text(
                "Пользователь: ${request.User?.username ?: "ID ${request.user_id}"}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontFamily = InterFontFamily,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                "ФИО: ${request.full_name}",
                color = Color.DarkGray,
                fontSize = 14.sp,
                fontFamily = InterFontFamily,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
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

@Composable
fun ReviewVerificationDialog(
    request: VerificationRequest,
    isApprove: Boolean,
    onDismiss: () -> Unit,
    onAction: (adminNotes: String) -> Unit
) {
    var notes by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = if (isApprove) "Одобрение верификации" else "Отклонение верификации", 
                fontFamily = InterFontFamily, 
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("ФИО заявителя: ${request.full_name}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                if (!request.info.isNullOrBlank()) {
                    Text("Информация от заявителя:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Gray)
                    Surface(
                        color = Color(0xFFF9F9F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(request.info, modifier = Modifier.padding(12.dp), fontSize = 13.sp, fontFamily = InterFontFamily)
                    }
                }
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (isApprove) "Заметки администратора (необязательно)" else "Причина отклонения") },
                    placeholder = { 
                        Text(
                            if (isApprove) "Например, проверен диплом шеф-повара" 
                            else "Например, нечитаемый диплом или недостаточно опыта"
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAction(notes) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isApprove) Color(0xFF4CAF50) else Color(0xFFE57373)
                ),
                enabled = isApprove || notes.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isApprove) "Одобрить" else "Отклонить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
