package com.example.diplom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.diplom.data.AuditLog
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAuditLogsScreen(navController: NavController, viewModel: AdminViewModel = viewModel()) {
    val logsState by viewModel.auditLogs.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAuditLogs()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Логи действий", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = logsState) {
            is AdminState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = UmamiOrange)
                }
            }
            is AdminState.Success -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
                    items(state.data) { log ->
                        AuditLogItem(log = log)
                        Spacer(modifier = Modifier.height(8.dp))
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
fun AuditLogItem(log: AuditLog) {
    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = log.action,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = InterFontFamily,
                    color = UmamiOrange
                )
                Text(
                    text = log.created_at?.take(10) ?: "---", // Safe date
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontFamily = InterFontFamily
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Админ ID: ${log.admin_id}", fontSize = 13.sp, fontFamily = InterFontFamily)
            if (log.entity != null) {
                Text("Объект: ${log.entity} (ID: ${log.entity_id})", fontSize = 13.sp, fontFamily = InterFontFamily)
            }
            if (log.details != null) {
                Text("Детали: ${log.details}", fontSize = 12.sp, color = Color.Gray, fontFamily = InterFontFamily)
            }
        }
    }
}
