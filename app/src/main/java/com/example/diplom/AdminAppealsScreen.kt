package com.example.diplom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.diplom.data.AdminState
import com.example.diplom.data.AdminViewModel
import com.example.diplom.data.AppealItem
import com.example.diplom.data.normalizeImageUrl
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAppealsScreen(navController: NavController, viewModel: AdminViewModel = viewModel()) {
    val appealsState by viewModel.appeals.collectAsState()
    var selectedAppeal by remember { mutableStateOf<AppealItem?>(null) }
    var showReviewDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadAppeals()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Апелляции", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = appealsState) {
            is AdminState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = UmamiOrange)
                }
            }
            is AdminState.Success -> {
                if (state.data.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Нет активных апелляций", color = Color.Gray, fontFamily = InterFontFamily)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                        items(state.data) { appeal ->
                            AppealItemCard(appeal = appeal) {
                                selectedAppeal = appeal
                                showReviewDialog = true
                            }
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

    if (showReviewDialog && selectedAppeal != null) {
        ReviewAppealDialog(
            appeal = selectedAppeal!!,
            onDismiss = { showReviewDialog = false },
            onAction = { status, notes ->
                viewModel.updateAppealStatus(selectedAppeal!!.id, status, notes)
                showReviewDialog = false
            }
        )
    }
}

@Composable
fun AppealItemCard(appeal: AppealItem, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFF5F5F5))) {
                    if (appeal.User?.avatar_url != null) {
                        AsyncImage(
                            model = normalizeImageUrl(appeal.User.avatar_url),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.align(Alignment.Center))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(appeal.User?.username ?: "Anonymous", fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    Text(appeal.created_at.take(10), color = Color.Gray, fontSize = 12.sp, fontFamily = InterFontFamily)
                }
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    color = when (appeal.status) {
                        "pending" -> UmamiOrange.copy(alpha = 0.1f)
                        "reviewed" -> Color.Blue.copy(alpha = 0.1f)
                        "resolved" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                        else -> Color.Gray.copy(alpha = 0.1f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = appeal.status.uppercase(),
                        color = when (appeal.status) {
                            "pending" -> UmamiOrange
                            "reviewed" -> Color.Blue
                            "resolved" -> Color(0xFF4CAF50)
                            else -> Color.DarkGray
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = appeal.message,
                fontFamily = InterFontFamily,
                fontSize = 14.sp,
                color = Color.DarkGray,
                maxLines = 3
            )
        }
    }
}

@Composable
fun ReviewAppealDialog(
    appeal: AppealItem,
    onDismiss: () -> Unit,
    onAction: (status: String, notes: String) -> Unit
) {
    var notes by remember { mutableStateOf(appeal.admin_notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Рассмотрение апелляции", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Сообщение пользователя:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Surface(
                    color = Color(0xFFF9F9F9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(appeal.message, modifier = Modifier.padding(12.dp), fontSize = 14.sp, fontFamily = InterFontFamily)
                }
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Заметки администратора") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAction("resolved", notes) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Одобрить и разблокировать")
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction("reviewed", notes) }) {
                Text("Отклонить (пометить как просмотрено)", color = Color.Red)
            }
        }
    )
}
