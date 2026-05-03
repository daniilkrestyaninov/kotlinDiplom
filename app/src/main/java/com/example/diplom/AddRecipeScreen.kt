package com.example.diplom

import android.Manifest
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.diplom.data.*
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

data class StepData(
    var description: String = "",
    var imageUri: Uri? = null,
    var uploadedUrl: String? = null
)

data class SelectedIngredientUi(
    val id: Int,
    val name: String,
    val quantity: String = "",
    val note: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipeScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val service = ApiClient.recipeService

    // Form state
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var cookingTime by remember { mutableStateOf("") }
    var portion by remember { mutableStateOf("") }
    var calorific by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("1") }
    var isPrivate by remember { mutableStateOf(false) }

    // Image
    var mainImageUri by remember { mutableStateOf<Uri?>(null) }
    var mainImageUrl by remember { mutableStateOf<String?>(null) }

    // Steps
    val steps = remember { mutableStateListOf(StepData()) }

    // Metadata
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var kitchens by remember { mutableStateOf<List<Category>>(emptyList()) }
    var cookingTypes by remember { mutableStateOf<List<Category>>(emptyList()) }
    var celebrations by remember { mutableStateOf<List<Category>>(emptyList()) }
    var ingredients by remember { mutableStateOf<List<Ingredient>>(emptyList()) }

    var selectedCategoryIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedKitchenId by remember { mutableStateOf<String?>(null) }
    var selectedCookingId by remember { mutableStateOf<String?>(null) }
    var selectedCelebrationId by remember { mutableStateOf<String?>(null) }

    // Ingredient selection
    var ingredientSearch by remember { mutableStateOf("") }
    var selectedIngredients by remember { mutableStateOf<List<SelectedIngredientUi>>(emptyList()) }
    var showIngredientDialog by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }

    // For which step are we picking an image
    var activeStepIndex by remember { mutableIntStateOf(-1) }

    // Photo picker state
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var imagePickTarget by remember { mutableStateOf("main") } // "main" or "step_0", "step_1", etc.

    // Camera temp file
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            when {
                imagePickTarget == "main" -> mainImageUri = it
                imagePickTarget.startsWith("step_") -> {
                    val idx = imagePickTarget.removePrefix("step_").toIntOrNull()
                    if (idx != null && idx < steps.size) {
                        steps[idx] = steps[idx].copy(imageUri = it)
                    }
                }
            }
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            when {
                imagePickTarget == "main" -> mainImageUri = cameraImageUri
                imagePickTarget.startsWith("step_") -> {
                    val idx = imagePickTarget.removePrefix("step_").toIntOrNull()
                    if (idx != null && idx < steps.size) {
                        steps[idx] = steps[idx].copy(imageUri = cameraImageUri)
                    }
                }
            }
        }
    }

    // Camera permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createTempImageUri(context)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Нужно разрешение на камеру", Toast.LENGTH_SHORT).show()
        }
    }

    // Load metadata
    LaunchedEffect(Unit) {
        try {
            categories = service.getCategories()
            kitchens = service.getKitchens()
            cookingTypes = service.getCookingTypes()
            celebrations = service.getCelebrations()
            ingredients = service.getIngredients()
        } catch (e: Exception) {
            android.util.Log.e("AddRecipe", "Failed to load metadata", e)
        }
    }

    // Upload helper
    suspend fun uploadImage(uri: Uri, folder: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                val bytes = inputStream.readBytes()
                inputStream.close()

                val requestFile = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", "photo.jpg", requestFile)
                val folderPart = folder.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = service.uploadImage(imagePart, folderPart)
                response.url
            } catch (e: Exception) {
                android.util.Log.e("AddRecipe", "Upload failed", e)
                null
            }
        }
    }

    fun openImagePicker(target: String) {
        imagePickTarget = target
        showImageSourceDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Новый рецепт", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, color = UmamiOrange)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Main Image
            item {
                Text("Фото рецепта", fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF5F5F5))
                        .clickable { openImagePicker("main") },
                    contentAlignment = Alignment.Center
                ) {
                    if (mainImageUri != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = mainImageUri,
                                contentDescription = "Recipe photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Aspect Ratio Guide Overlay
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 2.dp.toPx()
                                val rectWidth = size.width
                                val rectHeight = size.height
                                
                                // Draw darkened corners to show the crop area
                                // The card uses a specific height, so we simulate that
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.3f),
                                    size = size
                                )
                                
                                // Clear the center area (visual guide)
                                drawRect(
                                    color = Color.Transparent,
                                    blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                                )
                            }
                            
                            // Border for the guide
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            )
                            
                            Surface(
                                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Black.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    "Зона видимости",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontFamily = InterFontFamily
                                )
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Нажмите, чтобы добавить фото", color = Color.Gray, fontFamily = InterFontFamily, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Title
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название рецепта *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }

            // Description
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    minLines = 3
                )
            }

            // Row: cooking time, portions, calories
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cookingTime,
                        onValueChange = { cookingTime = it.filter { c -> c.isDigit() } },
                        label = { Text("Время (мин)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = portion,
                        onValueChange = { portion = it.filter { c -> c.isDigit() } },
                        label = { Text("Порции") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = calorific,
                        onValueChange = { calorific = it.filter { c -> c.isDigit() } },
                        label = { Text("Ккал") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )
                }
            }

            // Difficulty
            item {
                Text("Сложность", fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val difficultyLabels = mapOf("1" to "Очень легко", "2" to "Легко", "3" to "Средне", "4" to "Сложно", "5" to "Очень сложно")
                    difficultyLabels.forEach { (value, label) ->
                        FilterChip(
                            selected = difficulty == value,
                            onClick = { difficulty = value },
                            label = { Text(label, fontFamily = InterFontFamily, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = UmamiOrange,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Categories
            item {
                Text("Категории", fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                if (categories.isEmpty()) {
                    Text("Загрузка...", color = Color.Gray, fontFamily = InterFontFamily, fontSize = 12.sp)
                } else {
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = cat.id in selectedCategoryIds,
                                onClick = {
                                    selectedCategoryIds = if (cat.id in selectedCategoryIds)
                                        selectedCategoryIds - cat.id
                                    else
                                        selectedCategoryIds + cat.id
                                },
                                label = { Text(cat.name, fontFamily = InterFontFamily, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = UmamiOrange,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Kitchen, Cooking type, Celebration dropdowns
            item {
                MetaDropdown(label = "Кухня", items = kitchens, selectedId = selectedKitchenId, onSelect = { selectedKitchenId = it })
            }
            item {
                MetaDropdown(label = "Способ готовки", items = cookingTypes, selectedId = selectedCookingId, onSelect = { selectedCookingId = it })
            }
            item {
                MetaDropdown(label = "Праздник", items = celebrations, selectedId = selectedCelebrationId, onSelect = { selectedCelebrationId = it })
            }

            // Ingredients
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ингредиенты", fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, fontSize = 16.sp)
                    TextButton(onClick = { showIngredientDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = UmamiOrange)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Добавить", color = UmamiOrange)
                    }
                }
            }

            if (selectedIngredients.isEmpty()) {
                item {
                    Text("Добавьте хотя бы один ингредиент", color = Color.Gray, fontFamily = InterFontFamily, fontSize = 12.sp)
                }
            } else {
                itemsIndexed(selectedIngredients) { index, ing ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF9F9F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(ing.name, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                IconButton(onClick = {
                                    selectedIngredients = selectedIngredients.toMutableList().also { it.removeAt(index) }
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Удалить", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = ing.quantity,
                                    onValueChange = { q ->
                                        selectedIngredients = selectedIngredients.toMutableList().also {
                                            it[index] = it[index].copy(quantity = q)
                                        }
                                    },
                                    label = { Text("Количество") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = ing.note,
                                    onValueChange = { n ->
                                        selectedIngredients = selectedIngredients.toMutableList().also {
                                            it[index] = it[index].copy(note = n)
                                        }
                                    },
                                    label = { Text("Примечание") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Steps
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Шаги приготовления", fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, fontSize = 16.sp)
                    IconButton(onClick = { steps.add(StepData()) }) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить шаг", tint = UmamiOrange)
                    }
                }
            }

            itemsIndexed(steps) { index, step ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF9F9F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Шаг ${index + 1}", fontWeight = FontWeight.Bold, color = UmamiOrange, fontFamily = InterFontFamily)
                            if (steps.size > 1) {
                                IconButton(onClick = { steps.removeAt(index) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Удалить", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = step.description,
                            onValueChange = { steps[index] = step.copy(description = it) },
                            placeholder = { Text("Описание шага...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 2
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Step image
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEEEEEE))
                                .clickable { openImagePicker("step_$index") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (step.imageUri != null) {
                                AsyncImage(
                                    model = step.imageUri,
                                    contentDescription = "Фото шага ${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Добавить фото шага", color = Color.Gray, fontSize = 12.sp, fontFamily = InterFontFamily)
                                }
                            }
                        }
                    }
                }
            }

            // Private toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Приватный рецепт", fontFamily = InterFontFamily)
                    Switch(
                        checked = isPrivate,
                        onCheckedChange = { isPrivate = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = UmamiOrange, checkedTrackColor = UmamiOrange.copy(alpha = 0.3f))
                    )
                }
            }

            // Submit button
            item {
                Button(
                    onClick = {
                        if (title.isBlank() || description.isBlank()) {
                            Toast.makeText(context, "Заполните название и описание", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (cookingTime.isBlank() || cookingTime.toIntOrNull() == null) {
                            Toast.makeText(context, "Укажите время готовки", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (portion.isBlank() || portion.toIntOrNull() == null) {
                            Toast.makeText(context, "Укажите количество порций", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (steps.none { it.description.isNotBlank() }) {
                            Toast.makeText(context, "Добавьте хотя бы один шаг", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (selectedIngredients.isEmpty()) {
                            Toast.makeText(context, "Добавьте хотя бы один ингредиент", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        scope.launch {
                            try {
                                // 1. Upload main image if present
                                var uploadedMainUrl: String? = null
                                if (mainImageUri != null) {
                                    isUploading = true
                                    uploadedMainUrl = uploadImage(mainImageUri!!, "recipes")
                                }

                                // 2. Upload step images
                                val stepInputs = mutableListOf<StepInput>()
                                steps.forEachIndexed { index, step ->
                                    if (step.description.isNotBlank()) {
                                        var stepImageUrl: String? = null
                                        if (step.imageUri != null) {
                                            stepImageUrl = uploadImage(step.imageUri!!, "steps")
                                        }
                                        stepInputs.add(
                                            StepInput(
                                                stepNumber = index + 1,
                                                description = step.description,
                                                imageUrl = stepImageUrl
                                            )
                                        )
                                    }
                                }
                                isUploading = false

                                // 3. Create recipe
                                val request = CreateRecipeRequest(
                                    title = title,
                                    description = description,
                                    difficulty = difficulty,
                                    imageUrl = uploadedMainUrl,
                                    cookingTime = cookingTime.toIntOrNull() ?: 0,
                                    portion = portion.toIntOrNull() ?: 1,
                                    calorific = calorific.toIntOrNull(),
                                    isPrivate = isPrivate,
                                    kitchenId = selectedKitchenId?.toIntOrNull(),
                                    celebrationId = selectedCelebrationId?.toIntOrNull(),
                                    cookingId = selectedCookingId?.toIntOrNull(),
                                    ingredients = selectedIngredients.map {
                                        IngredientInput(
                                            id = it.id,
                                            quantity = it.quantity.takeIf { q -> q.isNotBlank() },
                                            note = it.note.takeIf { n -> n.isNotBlank() }
                                        )
                                    },
                                    steps = stepInputs,
                                    categories = selectedCategoryIds.mapNotNull { it.toIntOrNull() }
                                )

                                service.createRecipe(request)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Рецепт создан!", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("AddRecipe", "Create failed", e)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            } finally {
                                isLoading = false
                                isUploading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = UmamiOrange),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isUploading) "Загрузка фото..." else "Создание...", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Опубликовать рецепт", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        // Image source dialog
        if (showIngredientDialog) {
            AlertDialog(
                onDismissRequest = { showIngredientDialog = false },
                title = { Text("Выбор ингредиентов", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = ingredientSearch,
                            onValueChange = { ingredientSearch = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Поиск ингредиента...") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val filtered = ingredients.filter {
                            it.name.contains(ingredientSearch, ignoreCase = true)
                        }.take(30)
                        LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                            items(filtered) { ing ->
                                val idInt = ing.id.toIntOrNull()
                                if (idInt != null) {
                                    val alreadyAdded = selectedIngredients.any { it.id == idInt }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (!alreadyAdded) {
                                                    selectedIngredients = selectedIngredients + SelectedIngredientUi(
                                                        id = idInt,
                                                        name = ing.name
                                                    )
                                                }
                                            }
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(ing.name, fontFamily = InterFontFamily)
                                        if (alreadyAdded) {
                                            Text("Добавлено", color = UmamiOrange, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showIngredientDialog = false }) {
                        Text("Готово", color = UmamiOrange)
                    }
                }
            )
        }

        // Image source dialog
        if (showImageSourceDialog) {
            AlertDialog(
                onDismissRequest = { showImageSourceDialog = false },
                title = { Text("Выберите источник", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        TextButton(
                            onClick = {
                                showImageSourceDialog = false
                                galleryLauncher.launch("image/*")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = UmamiOrange)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Галерея", fontFamily = InterFontFamily, fontSize = 16.sp, color = Color.Black)
                        }
                        TextButton(
                            onClick = {
                                showImageSourceDialog = false
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = UmamiOrange)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Камера", fontFamily = InterFontFamily, fontSize = 16.sp, color = Color.Black)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showImageSourceDialog = false }) {
                        Text("Отмена", color = Color.Gray)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetaDropdown(label: String, items: List<Category>, selectedId: String?, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = items.find { it.id == selectedId }?.name ?: label

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = if (selectedId != null) selectedName else "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Не выбрано", color = Color.Gray) },
                onClick = { onSelect(null); expanded = false }
            )
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.name) },
                    onClick = { onSelect(item.id); expanded = false }
                )
            }
        }
    }
}

private fun createTempImageUri(context: Context): Uri {
    val imagesDir = File(context.cacheDir, "images")
    imagesDir.mkdirs()
    val file = File(imagesDir, "camera_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}











