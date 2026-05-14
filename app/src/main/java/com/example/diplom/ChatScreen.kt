package com.example.diplom

import androidx.compose.foundation.BorderStroke
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
import com.example.diplom.data.local.*
import androidx.compose.runtime.collectAsState

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
    val db = remember { UmamiDatabase.getDatabase(navController.context) }
    val dao = db.dao()
    val localMessages by dao.getAllMessages().collectAsState(initial = emptyList())
    
    var messageText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    var showPreviewSuggestion by remember { mutableStateOf<AiRecipeSuggestion?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = UmamiOrange.copy(alpha = 0.1f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Face, // Better chef icon representation
                                contentDescription = "Chef",
                                tint = UmamiOrange,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Микро-шеф", fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = InterFontFamily)
                            Text(if (isLoading) "Печатает..." else "В сети", color = if (isLoading) UmamiOrange else Color.Gray, fontSize = 12.sp, fontFamily = InterFontFamily)
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
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Спросите шефа...", color = Color.Gray, fontSize = 14.sp) },
                        shape = RoundedCornerShape(28.dp),
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFEEEEEE),
                            focusedBorderColor = UmamiOrange,
                            focusedContainerColor = Color(0xFFFDFDFD),
                            unfocusedContainerColor = Color(0xFFFDFDFD)
                        ),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    FloatingActionButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                val currentText = messageText
                                messageText = ""
                                isLoading = true
                                
                                scope.launch {
                                    // Save User message
                                    dao.insertMessage(LocalChatMessage(content = currentText, isUser = true))
                                    
                                    isLoading = true
                                    try {
                                        val products = currentText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                        val response = ApiClient.chatService.generateRecipe(AiGenerateRequest(products))
                                        response.suggestion?.let { sug ->
                                            val json = Gson().toJson(sug)
                                            dao.insertMessage(LocalChatMessage(content = sug.title, isUser = false, isRecipeSuggestion = true, recipeJson = json))
                                        } ?: run {
                                            dao.insertMessage(LocalChatMessage(content = "Хмм, из этого сложно что-то придумать. Может, добавим еще ингредиентов?", isUser = false))
                                        }
                                    } catch (e: Exception) {
                                        dao.insertMessage(LocalChatMessage(content = "Шеф отошел на минутку. Попробуйте позже!", isUser = false))
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = UmamiOrange,
                        contentColor = Color.White,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Generate")
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
                .background(Color(0xFFF5F5F5)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (localMessages.isEmpty() && !isLoading) {
                item { MessageBubble("Привет! Я твой личный шеф-повар. Напиши список продуктов, и я придумаю что-нибудь вкусное!", false) }
            }
            
            items(localMessages) { msg ->
                if (msg.isRecipeSuggestion && msg.recipeJson != null) {
                    val sug = Gson().fromJson(msg.recipeJson, AiRecipeSuggestion::class.java)
                    AiRecipeCard(
                        sug, 
                        onFullView = { showPreviewSuggestion = sug },
                        onSave = {
                            AiDraft.suggestion = sug
                            navController.navigate(Routes.ADD_RECIPE)
                        }
                    )
                } else {
                    MessageBubble(msg.content, msg.isUser)
                }
            }
        }
    }

    if (showPreviewSuggestion != null) {
        RecipePreviewDialog(
            suggestion = showPreviewSuggestion!!,
            onDismiss = { showPreviewSuggestion = null },
            onSave = {
                AiDraft.suggestion = showPreviewSuggestion
                showPreviewSuggestion = null
                navController.navigate(Routes.ADD_RECIPE)
            }
        )
    }
}

@Composable
fun MessageBubble(text: String, isUser: Boolean) {
    val backgroundColor = if (isUser) UmamiOrange else Color.White
    val textColor = if (isUser) Color.White else Color.Black
    val align = if (isUser) Alignment.End else Alignment.Start
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Surface(
            shape = shape,
            color = backgroundColor,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = text,
                color = textColor,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                fontFamily = InterFontFamily,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun AiRecipeCard(suggestion: AiRecipeSuggestion, onFullView: () -> Unit, onSave: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, UmamiOrange.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = UmamiOrange.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Text("👨‍🍳", modifier = Modifier.wrapContentSize(), fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        suggestion.title,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        fontFamily = InterFontFamily
                    )
                    Text("Рецепт от ИИ", color = UmamiOrange, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                suggestion.description,
                color = Color.Gray,
                fontSize = 14.sp,
                fontFamily = InterFontFamily,
                maxLines = 2
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onFullView,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, UmamiOrange)
                ) {
                    Text("Подробнее", color = UmamiOrange)
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = UmamiOrange)
                ) {
                    Text("Сохранить")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipePreviewDialog(suggestion: AiRecipeSuggestion, onDismiss: () -> Unit, onSave: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxSize()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                TopAppBar(
                    title = { Text("Предпросмотр рецепта", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                    }
                )
                
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(suggestion.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(suggestion.description, color = Color.Gray, fontSize = 15.sp)
                    }
                    
                    item {
                        Text("Ингредиенты", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        suggestion.ingredients.forEach { ing ->
                            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text("• ", fontWeight = FontWeight.Bold)
                                Text("${ing.name}: ${ing.quantity} ${ing.unit}", modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    
                    item {
                        Text("Шаги приготовления", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    
                    items(suggestion.steps) { step ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
                        ) {
                            Row(modifier = Modifier.padding(12.dp)) {
                                Surface(
                                    shape = CircleShape,
                                    color = UmamiOrange,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text("${step.step_number}", color = Color.White, modifier = Modifier.wrapContentSize(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(step.description, fontSize = 14.sp)
                            }
                        }
                    }
                }
                
                Surface(shadowElevation = 8.dp) {
                    Button(
                        onClick = onSave,
                        modifier = Modifier.fillMaxWidth().padding(20.dp).height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = UmamiOrange)
                    ) {
                        Text("Использовать этот рецепт", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

