package com.example.diplom

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiOrange
import com.example.diplom.ui.theme.UmamiCream

@Composable
fun UmamiProfileScreen(
    navController: NavController,
    isLoggedIn: Boolean,
    onLoginClick: () -> Unit,
    user: com.example.diplom.data.User?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(UmamiCream),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            // Profile Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (!isLoggedIn) onLoginClick()
                    },
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 2.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoggedIn && user?.avatarUrl != null) {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.LightGray)
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(60.dp).clip(CircleShape).background(Color(0xFFE0E0E0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Avatar",
                                modifier = Modifier.size(40.dp),
                                tint = Color.Gray
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Личный профиль",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            fontFamily = InterFontFamily
                        )
                        Text(
                            text = if (isLoggedIn) (user?.name ?: user?.username ?: "Пользователь") else "Войти",
                            color = if (isLoggedIn) Color.Black else UmamiOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            fontFamily = InterFontFamily
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Go",
                        tint = Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            // Menu Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 2.dp,
                color = Color.White
            ) {
                Column {
                    ProfileMenuItem("Избранное") { if (!isLoggedIn) onLoginClick() else navController.navigate("favorites") }
                    HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 20.dp))
                    ProfileMenuItem("Комментарии") { if (!isLoggedIn) onLoginClick() }
                    HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 20.dp))
                    ProfileMenuItem("Подписки") { if (!isLoggedIn) onLoginClick() }
                    HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 20.dp))
                    ProfileMenuItem("Подписчики") { if (!isLoggedIn) onLoginClick() }
                    HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 20.dp))
                    ProfileMenuItem("Список покупок") { if (!isLoggedIn) onLoginClick() }
                    HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 20.dp))
                    ProfileMenuItem("Настройки") { if (!isLoggedIn) onLoginClick() }
                }
            }
        }
    }
}

@Composable
fun ProfileMenuItem(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, fontSize = 16.sp, fontFamily = InterFontFamily)
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.Gray
        )
    }
}
