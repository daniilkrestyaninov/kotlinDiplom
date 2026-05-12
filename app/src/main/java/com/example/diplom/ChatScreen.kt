package com.example.diplom

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.diplom.data.*
import com.example.diplom.ui.theme.*
import com.example.diplom.ui.navigation.Routes
import kotlinx.coroutines.launch

import com.google.gson.Gson
import java.net.URLEncoder

sealed class ChatMessage {
    data class Text(val content: String, val isUser: Boolean) : ChatMessage()
    data class RecipeSuggestion(val suggestion: AiRecipeSuggestion) : ChatMessage()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UmamiChatScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var messageText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>(
        ChatMessage.Text("Привет, из чего будем готовить сегодня?", false)
    ) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_avatar), // Use a chef icon
                            contentDescription = "Chef",
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(UmamiOrange.copy(alpha=0.2f))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Чат с микро-шефом", fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = InterFontFamily)
                            Text(if (isLoading) "Печатает..." else "В сети", color = if (isLoading) UmamiOrange else Color.Gray, fontSize = 12.sp, fontFamily = InterFontFamily)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 2.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Сообщение...", color = Color.Gray, fontSize = 14.sp) },
                        shape = RoundedCornerShape(24.dp),
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFF0F0F0),
                            focusedBorderColor = UmamiOrange,
                            focusedContainerColor = Color(0xFFF9F9F9),
                            unfocusedContainerColor = Color(0xFFF9F9F9)
                        ),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                val currentText = messageText
                                messages.add(ChatMessage.Text(currentText, true))
                                messageText = ""
                                isLoading = true
                                
                                scope.launch {
                                    try {
                                        // Parse products from string
                                        val products = currentText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                        val response = ApiClient.chatService.generateRecipe(AiGenerateRequest(products))
                                        response.suggestion?.let {
                                            messages.add(ChatMessage.RecipeSuggestion(it))
                                        } ?: run {
                                            messages.add(ChatMessage.Text("Извините, я не смог придумать рецепт из этих продуктов.", false))
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("Chat", "AI Request failed", e)
                                        messages.add(ChatMessage.Text("Не удалось связаться с шефом. Проверьте подключение к интернету и попробуйте ещё раз.", false))
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(UmamiOrange, CircleShape),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(UmamiCream.copy(alpha = 0.5f)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { msg ->
                when (msg) {
                    is ChatMessage.Text -> MessageBubble(msg.content, msg.isUser)
                    is ChatMessage.RecipeSuggestion -> AiRecipeCard(msg.suggestion, navController)
                }
            }
        }
    }
}

@Composable
fun MessageBubble(text: String, isUser: Boolean) {
    val backgroundColor = if (isUser) Color.White else Color(0xFF86947D)
    val textColor = if (isUser) Color.Black else Color.White
    val align = if (isUser) Alignment.End else Alignment.Start
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Surface(
            shape = shape,
            color = backgroundColor,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 300.dp)
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

@Composable
fun AiRecipeCard(suggestion: AiRecipeSuggestion, navController: NavController) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF86947D)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🍗", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    suggestion.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    fontFamily = InterFontFamily
                )
            }
            
            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                    Text(" 40 мин • ", color = Color.White, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                    Text(" Легко", color = Color.White, fontSize = 12.sp)
                }
            }
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
            
            Text(
                "Из ваших продуктов:",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                suggestion.ingredients.joinToString(", ") { it.name },
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            
            suggestion.steps.take(3).forEach { step ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("${step.step_number}. ", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(step.description, color = Color.White, fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    AiDraft.suggestion = suggestion
                    navController.navigate(Routes.ADD_RECIPE)
                },
                colors = ButtonDefaults.buttonColors(containerColor = UmamiCream),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().height(40.dp)
            ) {
                Text("🔖 Сохранить рецепт", color = UmamiOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

