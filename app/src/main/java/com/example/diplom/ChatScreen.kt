package com.example.diplom

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiGreen
import com.example.diplom.ui.theme.UmamiOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UmamiChatScreen(navController: NavController) {
    var message by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_avatar), // replace with chef avatar
                            contentDescription = "Chef",
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(UmamiOrange.copy(alpha=0.2f))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Чат с микро-шефом", fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = InterFontFamily)
                            Text("В сети", color = Color.Gray, fontSize = 12.sp, fontFamily = InterFontFamily)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Сообщение...", color = Color.Gray) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFE5E5E5),
                        focusedBorderColor = UmamiOrange
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { /* TODO: Send message */ },
                    modifier = Modifier
                        .size(48.dp)
                        .background(UmamiOrange, CircleShape)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            reverseLayout = true
        ) {
            // Mock messages
            item {
                MessageBubble(text = "Привет, из чего будем готовить сегодня?", isUser = false)
            }
            item {
                MessageBubble(text = "Курица, сосиски, соус, кукуруза...", isUser = true)
            }
        }
    }
}

@Composable
fun MessageBubble(text: String, isUser: Boolean) {
    val backgroundColor = if (isUser) Color.White else UmamiGreen
    val textColor = if (isUser) Color.Black else Color.White
    val align = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = align
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = backgroundColor,
            shadowElevation = if (isUser) 2.dp else 0.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = text,
                color = textColor,
                modifier = Modifier.padding(12.dp),
                fontFamily = InterFontFamily,
                fontSize = 14.sp
            )
        }
    }
}
