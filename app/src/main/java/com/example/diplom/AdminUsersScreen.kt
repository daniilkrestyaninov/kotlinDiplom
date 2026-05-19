package com.example.diplom

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.diplom.data.AdminState
import com.example.diplom.data.AdminViewModel
import com.example.diplom.data.User
import com.example.diplom.data.normalizeImageUrl
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(navController: NavController, viewModel: AdminViewModel = viewModel()) {
    val usersState by viewModel.users.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Пользователи", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Поиск пользователей...", fontFamily = InterFontFamily) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            when (val state = usersState) {
                is AdminState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = UmamiOrange)
                    }
                }
                is AdminState.Success -> {
                    val filteredUsers = state.data.filter {
                        it.username.contains(searchQuery, ignoreCase = true) ||
                        (it.name?.contains(searchQuery, ignoreCase = true) == true)
                    }
                    
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                        items(filteredUsers) { user ->
                            AdminUserItem(user = user) {
                                selectedUser = user
                                showEditDialog = true
                            }
                            Spacer(modifier = Modifier.height(12.dp))
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

    if (showEditDialog && selectedUser != null) {
        AdminEditUserDialog(
            user = selectedUser!!,
            onDismiss = { showEditDialog = false },
            onSave = { name, bio, roleId ->
                viewModel.updateUser(selectedUser!!.id, name, bio, roleId)
                showEditDialog = false
            },
            onBlock = {
                viewModel.blockUser(selectedUser!!.id)
                showEditDialog = false
            },
            onUnblock = {
                viewModel.unblockUser(selectedUser!!.id)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun AdminUserItem(user: User, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFFF5F5F5))) {
                if (user.avatarUrl != null) {
                    AsyncImage(
                        model = normalizeImageUrl(user.avatarUrl),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.align(Alignment.Center))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.username, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                Text(user.name ?: "Без имени", color = Color.Gray, fontSize = 14.sp, fontFamily = InterFontFamily)
            }
            Surface(
                color = when (user.role?.lowercase()) {
                    "admin" -> Color.Red.copy(alpha = 0.1f)
                    "moderator" -> Color.Blue.copy(alpha = 0.1f)
                    else -> Color.Gray.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = user.role ?: "user",
                    color = when (user.role?.lowercase()) {
                        "admin" -> Color.Red
                        "moderator" -> Color.Blue
                        else -> Color.DarkGray
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

@Composable
fun AdminEditUserDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (name: String, bio: String, roleId: Int) -> Unit,
    onBlock: () -> Unit,
    onUnblock: () -> Unit
) {
    var name by remember { mutableStateOf(user.name ?: "") }
    var bio by remember { mutableStateOf(user.bio ?: "") }
    
    // Convert role string to ID: Admin = 1, User = 2, Moderator = 3 (Based on typical setup)
    var roleId by remember { 
        mutableStateOf(
            when (user.role?.lowercase()) {
                "admin" -> 1
                "moderator" -> 3
                else -> 2
            }
        ) 
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Управление пользователем", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя", fontFamily = InterFontFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("О себе", fontFamily = InterFontFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Text("Роль пользователя", fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = roleId == 2,
                        onClick = { roleId = 2 },
                        label = { Text("Пользователь") }
                    )
                    FilterChip(
                        selected = roleId == 3,
                        onClick = { roleId = 3 },
                        label = { Text("Модератор") }
                    )
                }
                Row {
                    FilterChip(
                        selected = roleId == 1,
                        onClick = { roleId = 1 },
                        label = { Text("Администратор") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, bio, roleId) },
                colors = ButtonDefaults.buttonColors(containerColor = UmamiOrange)
            ) {
                Text("Сохранить", fontFamily = InterFontFamily)
            }
        },
        dismissButton = {
            if (user.isBlocked == true) {
                TextButton(onClick = onUnblock) {
                    Text("Разблокировать", color = Color(0xFF4CAF50), fontFamily = InterFontFamily)
                }
            } else {
                TextButton(onClick = onBlock) {
                    Text("Заблокировать", color = Color.Red, fontFamily = InterFontFamily)
                }
            }
        }
    )
}
