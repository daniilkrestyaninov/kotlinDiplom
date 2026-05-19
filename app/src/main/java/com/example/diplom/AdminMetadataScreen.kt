package com.example.diplom

import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.diplom.data.*
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiOrange
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

enum class MetadataTab(val title: String) {
    CATEGORIES("Категории"),
    KITCHENS("Кухни"),
    COOKING_TYPES("Способы"),
    CELEBRATIONS("Праздники"),
    UNITS("Ед. изм."),
    INGREDIENTS("Ингредиенты")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMetadataScreen(
    navController: NavController,
    viewModel: AdminViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(MetadataTab.CATEGORIES) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog state
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<Any?>(null) }
    var itemToDelete by remember { mutableStateOf<Any?>(null) }

    // State flows
    val categoriesState by viewModel.categories.collectAsState()
    val kitchensState by viewModel.kitchens.collectAsState()
    val cookingTypesState by viewModel.cookingTypes.collectAsState()
    val celebrationsState by viewModel.celebrations.collectAsState()
    val unitsState by viewModel.units.collectAsState()
    val ingredientsState by viewModel.ingredients.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(selectedTab, searchQuery) {
        when (selectedTab) {
            MetadataTab.CATEGORIES -> viewModel.loadCategories()
            MetadataTab.KITCHENS -> viewModel.loadKitchens()
            MetadataTab.COOKING_TYPES -> viewModel.loadCookingTypes()
            MetadataTab.CELEBRATIONS -> viewModel.loadCelebrations()
            MetadataTab.UNITS -> viewModel.loadUnits()
            MetadataTab.INGREDIENTS -> {
                viewModel.loadIngredients(searchQuery.ifBlank { null })
                viewModel.loadUnits() // For dropdown list
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Управление справочниками", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = UmamiOrange,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 16.dp,
                divider = { HorizontalDivider(color = Color(0xFFF5F5F5)) },
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                        color = UmamiOrange
                    )
                }
            ) {
                MetadataTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = tab.title,
                                fontFamily = InterFontFamily,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == tab) UmamiOrange else Color.Gray
                            )
                        }
                    )
                }
            }

            if (selectedTab == MetadataTab.INGREDIENTS) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск ингредиентов...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Очистить")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                when (selectedTab) {
                    MetadataTab.CATEGORIES -> RenderGenericList(categoriesState, onEdit = { itemToEdit = it }, onDelete = { itemToDelete = it })
                    MetadataTab.KITCHENS -> RenderGenericList(kitchensState, onEdit = { itemToEdit = it }, onDelete = { itemToDelete = it })
                    MetadataTab.COOKING_TYPES -> RenderGenericList(cookingTypesState, onEdit = { itemToEdit = it }, onDelete = { itemToDelete = it })
                    MetadataTab.CELEBRATIONS -> RenderGenericList(celebrationsState, onEdit = { itemToEdit = it }, onDelete = { itemToDelete = it })
                    MetadataTab.UNITS -> RenderUnitsList(unitsState, onEdit = { itemToEdit = it }, onDelete = { itemToDelete = it })
                    MetadataTab.INGREDIENTS -> RenderIngredientsList(ingredientsState, onEdit = { itemToEdit = it }, onDelete = { itemToDelete = it })
                }
            }
        }
    }

    // CREATE DIALOGS
    if (showAddDialog) {
        MetadataDialog(
            tab = selectedTab,
            unitsState = unitsState,
            onDismiss = { showAddDialog = false },
            onConfirm = { map ->
                showAddDialog = false
                val successMsg = "Запись успешно создана"
                val errorMsg = "Ошибка создания записи"
                when (selectedTab) {
                    MetadataTab.CATEGORIES -> {
                        viewModel.createCategory(
                            name = map["name"] as String,
                            description = map["description"] as? String,
                            imageUrl = map["image_url"] as? String
                        ) { ok ->
                            scope.launch { snackbarHostState.showSnackbar(if (ok) successMsg else errorMsg) }
                        }
                    }
                    MetadataTab.KITCHENS -> {
                        viewModel.createKitchen(
                            name = map["name"] as String,
                            imageUrl = map["image_url"] as? String
                        ) { ok ->
                            scope.launch { snackbarHostState.showSnackbar(if (ok) successMsg else errorMsg) }
                        }
                    }
                    MetadataTab.COOKING_TYPES -> {
                        viewModel.createCookingType(
                            name = map["name"] as String,
                            imageUrl = map["image_url"] as? String
                        ) { ok ->
                            scope.launch { snackbarHostState.showSnackbar(if (ok) successMsg else errorMsg) }
                        }
                    }
                    MetadataTab.CELEBRATIONS -> {
                        viewModel.createCelebration(
                            name = map["name"] as String,
                            imageUrl = map["image_url"] as? String
                        ) { ok ->
                            scope.launch { snackbarHostState.showSnackbar(if (ok) successMsg else errorMsg) }
                        }
                    }
                    MetadataTab.UNITS -> {
                        viewModel.createUnit(
                            name = map["name"] as String,
                            shortName = map["short_name"] as String
                        ) { ok ->
                            scope.launch { snackbarHostState.showSnackbar(if (ok) successMsg else errorMsg) }
                        }
                    }
                    MetadataTab.INGREDIENTS -> {
                        viewModel.createIngredient(
                            name = map["name"] as String,
                            unitId = map["unit_id"] as? Long,
                            description = map["description"] as? String
                        ) { ok ->
                            scope.launch { snackbarHostState.showSnackbar(if (ok) successMsg else errorMsg) }
                        }
                    }
                }
            }
        )
    }

    // EDIT DIALOG
    itemToEdit?.let { item ->
        MetadataDialog(
            tab = selectedTab,
            unitsState = unitsState,
            item = item,
            onDismiss = { itemToEdit = null },
            onConfirm = { map ->
                itemToEdit = null
                val successMsg = "Запись успешно обновлена"
                val errorMsg = "Ошибка обновления записи"
                when (selectedTab) {
                    MetadataTab.CATEGORIES -> {
                        val c = item as Category
                        viewModel.updateCategory(
                            id = c.id,
                            name = map["name"] as String,
                            description = map["description"] as? String,
                            imageUrl = map["image_url"] as? String
                        ) { ok ->
                            scope.launch { snackbarHostState.showSnackbar(if (ok) successMsg else errorMsg) }
                        }
                    }
                    MetadataTab.KITCHENS -> {
                        val k = item as Category
                        viewModel.updateKitchen(
                            id = k.id,
                            name = map["name"] as String,
                            imageUrl = map["image_url"] as? String
                        ) { ok ->
                            scope.launch { snackbarHostState.showSnackbar(if (ok) successMsg else errorMsg) }
                        }
                    }
                    MetadataTab.COOKING_TYPES -> {
                        val ct = item as Category
                        viewModel.updateCookingType(
                            id = ct.id,
                            name = map["name"] as String,
                            imageUrl = map["image_url"] as? String
                        ) { ok ->
                            scope.launch { snackbarHostState.showSnackbar(if (ok) successMsg else errorMsg) }
                        }
                    }
                    MetadataTab.CELEBRATIONS -> {
                        val cel = item as Category
                        viewModel.updateCelebration(
                            id = cel.id,
                            name = map["name"] as String,
                            imageUrl = map["image_url"] as? String
                        ) { ok ->
                            scope.launch { snackbarHostState.showSnackbar(if (ok) successMsg else errorMsg) }
                        }
                    }
                    MetadataTab.UNITS -> {
                        val u = item as UnitModel
                        viewModel.updateUnit(
                            id = u.id,
                            name = map["name"] as String,
                            shortName = map["short_name"] as String
                        ) { ok ->
                            scope.launch { snackbarHostState.showSnackbar(if (ok) successMsg else errorMsg) }
                        }
                    }
                    MetadataTab.INGREDIENTS -> {
                        val ing = item as IngredientModel
                        viewModel.updateIngredient(
                            id = ing.id,
                            name = map["name"] as String,
                            unitId = map["unit_id"] as? Long,
                            description = map["description"] as? String
                        ) { ok ->
                            scope.launch { snackbarHostState.showSnackbar(if (ok) successMsg else errorMsg) }
                        }
                    }
                }
            }
        )
    }

    // DELETE CONFIRMATION
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Удаление записи", fontWeight = FontWeight.Bold, fontFamily = InterFontFamily) },
            text = {
                val name = when (item) {
                    is Category -> item.name
                    is UnitModel -> item.name
                    is IngredientModel -> item.name
                    else -> ""
                }
                Text("Вы уверены, что хотите удалить запись \"$name\"? Это действие необратимо и может затронуть связанные рецепты.", fontFamily = InterFontFamily)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        itemToDelete = null
                        val successMsg = "Запись успешно удалена"
                        val errorMsg = "Ошибка удаления записи"
                        when (selectedTab) {
                            MetadataTab.CATEGORIES -> {
                                viewModel.deleteCategory((item as Category).id) { ok ->
                                    scope.launch { snackbarHostState.showSnackbar(if (ok) successMsg else errorMsg) }
                                }
                            }
                            MetadataTab.KITCHENS -> {
                                viewModel.deleteKitchen((item as Category).id) { ok ->
                                    scope.launch { snackbarHostState.showSnackbar(if (ok) successMsg else errorMsg) }
                                }
                            }
                            MetadataTab.COOKING_TYPES -> {
                                viewModel.deleteCookingType((item as Category).id) { ok ->
                                    scope.launch { snackbarHostState.showSnackbar(if (ok) successMsg else errorMsg) }
                                }
                            }
                            MetadataTab.CELEBRATIONS -> {
                                viewModel.deleteCelebration((item as Category).id) { ok ->
                                    scope.launch { snackbarHostState.showSnackbar(if (ok) successMsg else errorMsg) }
                                }
                            }
                            MetadataTab.UNITS -> {
                                viewModel.deleteUnit((item as UnitModel).id) { ok ->
                                    scope.launch { snackbarHostState.showSnackbar(if (ok) successMsg else errorMsg) }
                                }
                            }
                            MetadataTab.INGREDIENTS -> {
                                viewModel.deleteIngredient((item as IngredientModel).id) { ok ->
                                    scope.launch { snackbarHostState.showSnackbar(if (ok) successMsg else errorMsg) }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Удалить", fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Отмена", fontFamily = InterFontFamily)
                }
            }
        )
    }
}

@Composable
fun RenderGenericList(
    state: AdminState<List<Category>>,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit
) {
    when (state) {
        is AdminState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = UmamiOrange) }
        is AdminState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Ошибка: ${state.message}", color = Color.Red, fontFamily = InterFontFamily) }
        is AdminState.Success -> {
            val list = state.data
            if (list.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Список пуст", color = Color.Gray, fontFamily = InterFontFamily) }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(list) { item ->
                        ElevatedCard(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!item.imageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = normalizeImageUrl(item.imageUrl),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, fontSize = 16.sp)
                                    if (!item.description.isNullOrBlank()) {
                                        Text(item.description, fontSize = 12.sp, color = Color.Gray, maxLines = 2, fontFamily = InterFontFamily)
                                    }
                                }
                                Row {
                                    IconButton(onClick = { onEdit(item) }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = Color.Gray)
                                    }
                                    IconButton(onClick = { onDelete(item) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.Red)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        else -> {}
    }
}

@Composable
fun RenderUnitsList(
    state: AdminState<List<UnitModel>>,
    onEdit: (UnitModel) -> Unit,
    onDelete: (UnitModel) -> Unit
) {
    when (state) {
        is AdminState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = UmamiOrange) }
        is AdminState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Ошибка: ${state.message}", color = Color.Red, fontFamily = InterFontFamily) }
        is AdminState.Success -> {
            val list = state.data
            if (list.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Список пуст", color = Color.Gray, fontFamily = InterFontFamily) }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(list) { item ->
                        ElevatedCard(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(45.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = UmamiOrange.copy(alpha = 0.1f)
                                    ) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(item.shortName, color = UmamiOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = InterFontFamily)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, fontSize = 16.sp)
                                    Text("Сокращение: ${item.shortName}", fontSize = 12.sp, color = Color.Gray, fontFamily = InterFontFamily)
                                }
                                Row {
                                    IconButton(onClick = { onEdit(item) }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = Color.Gray)
                                    }
                                    IconButton(onClick = { onDelete(item) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.Red)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        else -> {}
    }
}

@Composable
fun RenderIngredientsList(
    state: AdminState<List<IngredientModel>>,
    onEdit: (IngredientModel) -> Unit,
    onDelete: (IngredientModel) -> Unit
) {
    when (state) {
        is AdminState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = UmamiOrange) }
        is AdminState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Ошибка: ${state.message}", color = Color.Red, fontFamily = InterFontFamily) }
        is AdminState.Success -> {
            val list = state.data
            if (list.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Ингредиенты не найдены", color = Color.Gray, fontFamily = InterFontFamily) }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(list) { item ->
                        ElevatedCard(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, fontSize = 16.sp)
                                    val unitText = item.Unit?.let { "${it.name} (${it.shortName})" } ?: "Не указана"
                                    Text("Ед. измерения: $unitText", fontSize = 12.sp, color = Color.Gray, fontFamily = InterFontFamily)
                                    if (!item.description.isNullOrBlank()) {
                                        Text(item.description, fontSize = 12.sp, color = Color.Gray, maxLines = 1, fontFamily = InterFontFamily)
                                    }
                                }
                                Row {
                                    IconButton(onClick = { onEdit(item) }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = Color.Gray)
                                    }
                                    IconButton(onClick = { onDelete(item) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.Red)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        else -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataDialog(
    tab: MetadataTab,
    unitsState: AdminState<List<UnitModel>>,
    item: Any? = null,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any?>) -> Unit
) {
    val isEdit = item != null
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var shortName by remember { mutableStateOf("") }
    var selectedUnitId by remember { mutableStateOf<Long?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isUploading = true
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val bytes = inputStream.readBytes()
                        inputStream.close()

                        val requestFile = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                        val imagePart = MultipartBody.Part.createFormData("image", "category.jpg", requestFile)
                        val folderName = when (tab) {
                            MetadataTab.CATEGORIES -> "categories"
                            MetadataTab.KITCHENS -> "kitchens"
                            MetadataTab.COOKING_TYPES -> "cooking_types"
                            MetadataTab.CELEBRATIONS -> "celebrations"
                            else -> "meta"
                        }
                        val folderPart = folderName.toRequestBody("text/plain".toMediaTypeOrNull())

                        val uploadResult = ApiClient.recipeService.uploadImage(imagePart, folderPart)
                        imageUrl = uploadResult.url
                        android.widget.Toast.makeText(context, "Изображение успешно загружено", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MetadataDialog", "Image upload failed", e)
                    android.widget.Toast.makeText(context, "Ошибка загрузки изображения", android.widget.Toast.LENGTH_SHORT).show()
                } finally {
                    isUploading = false
                }
            }
        }
    }

    LaunchedEffect(item) {
        item?.let {
            when (it) {
                is Category -> {
                    name = it.name
                    description = it.description ?: ""
                    imageUrl = it.imageUrl ?: ""
                }
                is UnitModel -> {
                    name = it.name
                    shortName = it.shortName
                }
                is IngredientModel -> {
                    name = it.name
                    description = it.description ?: ""
                    selectedUnitId = it.unitId
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Редактирование" else "Добавление записи",
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (tab == MetadataTab.CATEGORIES || tab == MetadataTab.INGREDIENTS) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Описание (опционально)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }

                if (tab == MetadataTab.CATEGORIES || tab == MetadataTab.KITCHENS || tab == MetadataTab.COOKING_TYPES || tab == MetadataTab.CELEBRATIONS) {
                    Text("Изображение", fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = InterFontFamily)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF5F5F5))
                            .clickable(enabled = !isUploading) { imageLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploading) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = UmamiOrange)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Загрузка на сервер...", fontSize = 12.sp, color = Color.Gray, fontFamily = InterFontFamily)
                            }
                        } else if (imageUrl.isNotBlank()) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = normalizeImageUrl(imageUrl),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Camera overlay/change button
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .size(36.dp),
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.Black.copy(alpha = 0.6f)
                                ) {
                                    IconButton(
                                        onClick = { imageLauncher.launch("image/*") }
                                    ) {
                                        Icon(
                                            Icons.Default.CameraAlt,
                                            contentDescription = "Изменить фото",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                
                                // Delete overlay button
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(36.dp),
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.Black.copy(alpha = 0.6f)
                                ) {
                                    IconButton(
                                        onClick = { imageUrl = "" }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Удалить фото",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Выбрать фото",
                                    tint = UmamiOrange,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Выбрать фото с устройства", fontSize = 14.sp, color = UmamiOrange, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                            }
                        }
                    }
                }

                if (tab == MetadataTab.UNITS) {
                    OutlinedTextField(
                        value = shortName,
                        onValueChange = { shortName = it },
                        label = { Text("Сокращение") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (tab == MetadataTab.INGREDIENTS) {
                    Text("Единица измерения", fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = InterFontFamily)
                    
                    val units = (unitsState as? AdminState.Success)?.data ?: emptyList()
                    val selectedUnitName = units.find { it.id == selectedUnitId }?.let { "${it.name} (${it.shortName})" } ?: "Не выбрана"

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedUnitName, fontFamily = InterFontFamily)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Не выбрана", fontFamily = InterFontFamily) },
                                onClick = {
                                    selectedUnitId = null
                                    dropdownExpanded = false
                                }
                            )
                            units.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text("${unit.name} (${unit.shortName})", fontFamily = InterFontFamily) },
                                    onClick = {
                                        selectedUnitId = unit.id
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val result = mutableMapOf<String, Any?>("name" to name)
                        if (description.isNotBlank()) result["description"] = description
                        if (imageUrl.isNotBlank()) result["image_url"] = imageUrl
                        if (shortName.isNotBlank()) result["short_name"] = shortName
                        val unitId = selectedUnitId
                        if (unitId != null) result["unit_id"] = unitId
                        onConfirm(result)
                    }
                },
                enabled = !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = UmamiOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isEdit) "Сохранить" else "Создать", fontFamily = InterFontFamily)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isUploading) {
                Text("Отмена", fontFamily = InterFontFamily)
            }
        }
    )
}
