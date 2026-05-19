package com.example.diplom

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.diplom.data.*
import com.example.diplom.ui.theme.InterFontFamily
import com.example.diplom.ui.theme.UmamiOrange
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UmamiParseRecipeScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val toolsService = ApiClient.toolsService
    val recipeService = ApiClient.recipeService

    var url by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var parsedRecipe by remember { mutableStateOf<ParsedRecipe?>(null) }

    // Editable state
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(true) }
    val ingredients = remember { mutableStateListOf<ParsedIngredient>() }
    val steps = remember { mutableStateListOf<ParsedStep>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Импорт рецепта", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, color = UmamiOrange) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Text(
                    "Введите ссылку на рецепт с сайтов russianfood.com или food.ru",
                    fontFamily = InterFontFamily,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://...") },
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = {
                        if (url.isNotBlank()) {
                            IconButton(onClick = { url = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (url.isBlank()) return@Button
                        isLoading = true
                        scope.launch {
                            try {
                                val response = toolsService.parseRecipe(ParseRequest(url))
                                val result = response.parsed
                                
                                if (result == null) {
                                    Toast.makeText(context, "Сервер не вернул данные рецепта", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                
                                parsedRecipe = result
                                
                                // Update basic info
                                title = result.title ?: ""
                                description = result.description ?: ""
                                
                                // Update lists
                                ingredients.clear()
                                result.ingredients?.forEach { ing ->
                                    ingredients.add(ParsedIngredient(
                                        name = ing.name ?: "",
                                        quantity = ing.quantity ?: "",
                                        unit = ing.unit ?: ""
                                    ))
                                }
                                
                                steps.clear()
                                result.steps?.forEach { step ->
                                    steps.add(ParsedStep(
                                        stepNumber = step.stepNumber ?: (steps.size + 1),
                                        description = step.description ?: ""
                                    ))
                                }
                                
                                val ingCount = result.ingredients?.size ?: 0
                                val stepsCount = result.steps?.size ?: 0
                                Toast.makeText(context, "Загружено: $ingCount ингред., $stepsCount шагов", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                android.util.Log.e("ParseRecipe", "Parsing failed", e)
                                Toast.makeText(context, "Ошибка парсинга. Проверьте ссылку и подключение.", Toast.LENGTH_LONG).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = UmamiOrange),
                    enabled = !isLoading && url.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Загрузить данные", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (parsedRecipe != null) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Редактирование", fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = InterFontFamily)
                }

                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Название") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Описание") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        minLines = 3
                    )
                }

                if (ingredients.isEmpty()) {
                    item {
                        Text("Ингредиенты не найдены или список пуст", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }

                itemsIndexed(ingredients, key = { index, _ -> "ing_$index" }) { index, ing ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF5F5F5),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = ing.name ?: "",
                                    onValueChange = { ingredients[index] = ing.copy(name = it) },
                                    label = { Text("Название", fontSize = 10.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedContainerColor = Color.White,
                                        focusedContainerColor = Color.White
                                    )
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedTextField(
                                        value = ing.quantity ?: "",
                                        onValueChange = { ingredients[index] = ing.copy(quantity = it) },
                                        label = { Text("Кол-во", fontSize = 10.sp) },
                                        modifier = Modifier.weight(1f),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedContainerColor = Color.White,
                                            focusedContainerColor = Color.White
                                        )
                                    )
                                    OutlinedTextField(
                                        value = ing.unit ?: "",
                                        onValueChange = { ingredients[index] = ing.copy(unit = it) },
                                        label = { Text("Ед.", fontSize = 10.sp) },
                                        modifier = Modifier.weight(1f),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedContainerColor = Color.White,
                                            focusedContainerColor = Color.White
                                        )
                                    )
                                }
                            }
                            IconButton(onClick = { ingredients.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray)
                            }
                        }
                    }
                }

                item {
                    TextButton(onClick = { ingredients.add(ParsedIngredient("", "", "")) }) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = UmamiOrange)
                        Text("Добавить ингредиент", color = UmamiOrange)
                    }
                }

                if (steps.isEmpty()) {
                    item {
                        Text("Шаги не найдены или список пуст", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }

                itemsIndexed(steps, key = { index, _ -> "step_$index" }) { index, step ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF5F5F5),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Шаг ${index + 1}", fontWeight = FontWeight.Bold, color = UmamiOrange)
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(onClick = { steps.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray)
                                }
                            }
                            OutlinedTextField(
                                value = step.description ?: "",
                                onValueChange = { steps[index] = step.copy(description = it) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = Color.White,
                                    focusedContainerColor = Color.White
                                )
                            )
                        }
                    }
                }

                item {
                    TextButton(onClick = { steps.add(ParsedStep(steps.size + 1, "")) }) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = UmamiOrange)
                        Text("Добавить шаг", color = UmamiOrange)
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Сделать приватным (обязательно для импорта)", fontFamily = InterFontFamily, modifier = Modifier.weight(1f), fontSize = 12.sp, color = Color.Gray)
                        Switch(checked = true, onCheckedChange = { }, enabled = false, colors = SwitchDefaults.colors(checkedThumbColor = UmamiOrange, checkedTrackColor = UmamiOrange.copy(alpha = 0.3f)))
                    }
                }

                item {
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                Toast.makeText(context, "Введите название", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isLoading = true
                            scope.launch {
                                try {
                                    val request = CreateRecipeRequest(
                                        title = title,
                                        description = description,
                                        difficulty = "1", // Default
                                        imageUrl = null,
                                        cookingTime = 30, // Default or parsed if available
                                        portion = 1,
                                        calorific = null,
                                        isPrivate = true,
                                        isParsed = true,
                                        ingredients = ingredients.map { 
                                            IngredientInput(name = it.name ?: "", quantity = it.quantity ?: "", unit = it.unit ?: "")
                                        },
                                        steps = steps.filter { it.description?.isNotBlank() == true }.mapIndexed { i, s -> 
                                            StepInput(stepNumber = i + 1, description = s.description ?: "", imageUrl = null)
                                        },
                                        categories = emptyList()
                                    )
                                    recipeService.createRecipe(request)
                                    Toast.makeText(context, "Рецепт сохранен!", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                } catch (e: Exception) {
                                    android.util.Log.e("ParseRecipe", "Save failed", e)
                                    Toast.makeText(context, "Ошибка сохранения. Проверьте подключение.", Toast.LENGTH_LONG).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Сохранить в мои рецепты", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
