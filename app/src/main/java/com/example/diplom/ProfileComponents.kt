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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.diplom.data.*
import com.example.diplom.ui.theme.*
import com.example.diplom.ui.navigation.Routes
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import com.example.diplom.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ModeComment

@Composable
fun ProfileStat(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black, fontFamily = InterFontFamily)
        Text(label, color = Color.Gray, fontSize = 12.sp, fontFamily = InterFontFamily)
    }
}

@Composable
fun ProfileMenuItem(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, fontSize = 16.sp, fontFamily = InterFontFamily)
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
    }
}

@Composable
fun UserListDialog(title: String, users: List<User>, currentUserId: String?, onDismiss: () -> Unit, onFollowToggle: (String, Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$title (${users.size})", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.heightIn(max = 400.dp)) {
                items(users) { u ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(UmamiOrange.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                            if (u.avatarUrl != null) AsyncImage(model = normalizeImageUrl(u.avatarUrl), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else Icon(Icons.Default.Person, contentDescription = null, tint = UmamiOrange)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(u.name ?: u.username, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("@${u.username}", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть", color = UmamiOrange) } }
    )
}

@Composable
fun RecipePostCard(
    recipe: Recipe, 
    navController: NavController,
    currentUserId: String? = null,
    isFavorited: Boolean = false,
    onLikeClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    onFollowClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {}
) {
    val onClick = { navController.navigate(Routes.recipeDetail(recipe.id.toString())) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var comments by remember { mutableStateOf<List<Comment>?>(null) }
    
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(recipe.id) {
        try {
            comments = ApiClient.recipeService.getComments(recipe.id.toString())
        } catch (e: Exception) {
            // ignore
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // User Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { recipe.User?.id?.let { navController.navigate("user_detail/$it") } }
                ) {
                    AsyncImage(
                        model = normalizeImageUrl(recipe.User?.avatarUrl) ?: R.drawable.ic_avatar,
                        contentDescription = "Пользователь",
                        modifier = Modifier
                            .size(45.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            recipe.User?.name ?: "Anonymous",
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("@${recipe.User?.username ?: "unknown"}", fontSize = 12.sp, color = Color.Gray, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (currentUserId != null && recipe.User?.id != currentUserId) {
                    var showReportMenu by remember { mutableStateOf(false) }
                    var showReportDialog by remember { mutableStateOf(false) }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (recipe.User?.isFollowing != true) {
                            Button(
                                onClick = { onFollowClick() },
                                colors = ButtonDefaults.buttonColors(containerColor = UmamiGreen),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                modifier = Modifier.width(112.dp).height(36.dp)
                            ) {
                                Text("Подписаться", fontSize = 12.sp, fontFamily = InterFontFamily, maxLines = 1)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        
                        IconButton(onClick = { showReportMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Меню", tint = Color.Gray)
                            DropdownMenu(expanded = showReportMenu, onDismissRequest = { showReportMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Пожаловаться", fontFamily = InterFontFamily) },
                                    onClick = { 
                                        showReportMenu = false
                                        showReportDialog = true 
                                    }
                                )
                            }
                        }
                    }

                    if (showReportDialog) {
                        ReportDialog(
                            onDismiss = { showReportDialog = false },
                            onSubmit = { reason, desc ->
                                scope.launch {
                                    try {
                                        ApiClient.reportService.createReport(
                                            ReportRequest(type = "recipe", recipeId = recipe.id, reason = reason, description = desc)
                                        )
                                        android.widget.Toast.makeText(context, "Жалоба отправлена", android.widget.Toast.LENGTH_SHORT).show()
                                        showReportDialog = false
                                    } catch (e: Exception) {
                                        android.util.Log.e("UmamiScreen", "Report failed", e)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = normalizeImageUrl(recipe.imageUrl) ?: R.drawable.img_pasta,
                    contentDescription = recipe.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Bookmark
                Surface(
                    color = Color.White.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd)
                        .size(36.dp)
                ) {
                    IconButton(onClick = { onFavoriteClick() }) {
                        Icon(
                            if (isFavorited) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (isFavorited) UmamiOrange else Color.Gray
                        )
                    }
                }

                // Badges
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (recipe.isGenerated == true) {
                        BadgeItem(icon = Icons.Default.Bolt, text = "ИИ")
                    }
                    if (recipe.isPrivate == true) {
                        BadgeItem(icon = Icons.Default.Lock, text = "Приватно")
                    }
                    BadgeItem(icon = Icons.Default.AccessTime, text = "${recipe.cookingTime ?: 0} мин")
                    BadgeItem(icon = Icons.Default.KeyboardArrowDown, text = recipe.difficulty ?: "Легко")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title & Description
            Text(
                recipe.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 26.sp,
                fontFamily = InterFontFamily
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                recipe.description ?: "Нет описания",
                fontSize = 14.sp,
                color = Color.Gray,
                fontFamily = InterFontFamily
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stats
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onLikeClick() }) {
                    val currentLikes = recipe.likesCount ?: recipe.likes?.size ?: 0
                    val currentIsLiked = recipe.isLiked == true

                    Icon(
                        if (currentIsLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        modifier = Modifier.size(24.dp),
                        tint = if (currentIsLiked) UmamiOrange else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(currentLikes.toString(), fontWeight = FontWeight.Bold, color = Color.DarkGray, fontFamily = InterFontFamily)
                }
                    val currentCommentsCount = comments?.size ?: recipe.commentsCount ?: 0
                StatItem(
                    icon = Icons.Outlined.ModeComment, 
                    count = currentCommentsCount.toString(),
                    modifier = Modifier.clickable { onCommentClick() }
                )
                
                if (recipe.rating != null && recipe.rating!! > 0) {
                    StatItem(
                        icon = Icons.Default.Star, 
                        count = String.format("%.1f", recipe.rating),
                        tint = UmamiOrange
                    )
                }

                if (recipe.viewsCount != null) {
                    StatItem(
                        icon = Icons.Default.Visibility, 
                        count = recipe.viewsCount.toString()
                    )
                }
            }

            if (!comments.isNullOrEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFF0F0F0))
                CommentPreview(comment = comments!!.first())
            }
        }
    }
}

@Composable
fun BadgeItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(
        color = Color.White.copy(alpha = 0.9f),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 2.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
        }
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    count: String, 
    modifier: Modifier = Modifier,
    tint: Color = Color.Gray
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = tint)
        Spacer(modifier = Modifier.width(6.dp))
        Text(count, fontWeight = FontWeight.Bold, color = Color.DarkGray, fontFamily = InterFontFamily)
    }
}

@Composable
fun CommentPreview(comment: Comment) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            AsyncImage(
                model = normalizeImageUrl(comment.author?.avatarUrl) ?: R.drawable.ic_avatar,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = comment.author?.name ?: comment.author?.username ?: "Пользователь",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                fontFamily = InterFontFamily
            )
            Spacer(modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = comment.content,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 42.dp),
            fontFamily = InterFontFamily,
            color = Color.DarkGray
        )
    }
}
