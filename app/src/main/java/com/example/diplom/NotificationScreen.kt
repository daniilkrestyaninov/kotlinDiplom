package com.example.diplom

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.diplom.data.Notification
import com.example.diplom.data.NotificationType
import com.example.diplom.data.NotificationViewModel
import com.example.diplom.data.normalizeImageUrl
import com.example.diplom.ui.theme.InterFontFamily
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    navController: NavController,
    viewModel: NotificationViewModel = viewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val canLoadMore by viewModel.canLoadMore.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
    }

    // Infinite scroll
    val listState = rememberLazyListState()
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && canLoadMore) {
            viewModel.loadMore()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Уведомления", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (notifications.any { !it.isRead }) {
                        IconButton(onClick = { viewModel.markAllRead() }) {
                            Icon(Icons.Default.DoneAll, contentDescription = "Прочитать все", tint = Color(0xFFFF6B6B))
                        }
                    }
                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Очистить", tint = Color.Gray)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading && notifications.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFFFF6B6B))
            } else if (notifications.isEmpty()) {
                EmptyNotifications(modifier = Modifier.align(Alignment.Center))
            } else {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(notifications, key = { it.id }) { notification ->
                            NotificationItem(
                                notification = notification,
                                onClick = {
                                    viewModel.markAsRead(notification.id)
                                    when (notification.type) {
                                        NotificationType.FOLLOW -> {
                                            navController.navigate("user_detail/${notification.actorId}")
                                        }
                                        NotificationType.LIKE, NotificationType.COMMENT, NotificationType.REPLY, NotificationType.NEW_POST -> {
                                            notification.recipeId?.let {
                                                navController.navigate("recipe_detail/$it")
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        // Loading indicator at the bottom for infinite scroll
                        if (canLoadMore) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFFFF6B6B)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Очистить уведомления", fontFamily = InterFontFamily) },
            text = { Text("Удалить все уведомления? Это действие нельзя отменить.", fontFamily = InterFontFamily) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAll()
                    showDeleteDialog = false
                }) {
                    Text("Удалить", color = Color(0xFFFF6B6B), fontFamily = InterFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена", fontFamily = InterFontFamily)
                }
            }
        )
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit
) {
    val bgColor = if (notification.isRead) Color.White else Color(0xFFFFF0F0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isRead) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Actor Avatar with Badge Icon
            Box {
                AsyncImage(
                    model = notification.actor?.avatarUrl?.let { normalizeImageUrl(it) } ?: R.drawable.ic_avatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                val icon = when (notification.type) {
                    NotificationType.LIKE -> Icons.Default.Favorite
                    NotificationType.FOLLOW -> Icons.Default.PersonAdd
                    NotificationType.NEW_POST -> Icons.Default.NewReleases
                    NotificationType.COMMENT -> Icons.Default.Comment
                    NotificationType.REPLY -> Icons.Default.Reply
                }
                val iconColor = when (notification.type) {
                    NotificationType.LIKE -> Color(0xFFFF6B6B)
                    NotificationType.FOLLOW -> Color(0xFF4DABF7)
                    NotificationType.NEW_POST -> Color(0xFF51CF66)
                    NotificationType.COMMENT -> Color(0xFFFCC419)
                    NotificationType.REPLY -> Color(0xFF94D82D)
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .size(20.dp)
                        .background(iconColor, CircleShape)
                        .padding(3.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.fillMaxSize())
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                val actorName = notification.actor?.name?.takeIf { it.isNotBlank() }
                    ?: notification.actor?.username
                    ?: "Пользователь"

                val message = when (notification.type) {
                    NotificationType.LIKE -> "оценил(а) ваш рецепт"
                    NotificationType.FOLLOW -> "подписался(-ась) на вас"
                    NotificationType.NEW_POST -> "опубликовал(а) новый рецепт"
                    NotificationType.COMMENT -> "прокомментировал(а) ваш рецепт"
                    NotificationType.REPLY -> "ответил(а) на ваш комментарий"
                }

                Text(
                    text = "$actorName $message",
                    fontFamily = InterFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = if (!notification.isRead) FontWeight.Medium else FontWeight.Normal,
                    color = Color.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Показываем название рецепта отдельной строкой
                notification.recipe?.title?.let { title ->
                    Text(
                        text = title,
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = Color(0xFFFF6B6B),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Text(
                    text = formatNotificationTime(notification.createdAt),
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Recipe thumbnail for relevant types
            val recipeImage = notification.recipe?.imageUrl
            if (recipeImage != null && notification.type in listOf(
                NotificationType.LIKE, NotificationType.NEW_POST, NotificationType.COMMENT
            )) {
                Spacer(modifier = Modifier.width(8.dp))
                AsyncImage(
                    model = normalizeImageUrl(recipeImage),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else if (!notification.isRead) {
                // Unread dot for notifications without a thumbnail
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFFFF6B6B), CircleShape)
                )
            }
        }
    }
}

@Composable
fun EmptyNotifications(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.NotificationsNone,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFDEE2E6)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Уведомлений пока нет",
            fontFamily = InterFontFamily,
            color = Color.Gray,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Здесь появятся лайки, подписки\nи комментарии к вашим рецептам",
            fontFamily = InterFontFamily,
            color = Color(0xFFADB5BD),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

fun formatNotificationTime(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return ""
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val date = sdf.parse(dateStr)
        val now = Date()
        val diff = now.time - (date?.time ?: 0L)

        when {
            diff < 60_000 -> "Только что"
            diff < 3_600_000 -> "${diff / 60_000} мин. назад"
            diff < 86_400_000 -> "${diff / 3_600_000} ч. назад"
            diff < 604_800_000 -> "${diff / 86_400_000} дн. назад"
            else -> SimpleDateFormat("dd MMM", Locale("ru")).format(date!!)
        }
    } catch (e: Exception) {
        ""
    }
}
