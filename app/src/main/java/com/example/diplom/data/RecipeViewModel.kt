package com.example.diplom.data

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed class RecipeState {
    object Loading : RecipeState()
    data class Success(val recipes: List<Recipe>) : RecipeState()
    data class Error(val message: String) : RecipeState()
}

class RecipeViewModel : ViewModel() {
    private val _state = mutableStateOf<RecipeState>(RecipeState.Loading)
    val state: State<RecipeState> = _state

    private val _isRefreshing = mutableStateOf(false)
    val isRefreshing: State<Boolean> = _isRefreshing

    // Metadata lists
    private val _categories = mutableStateOf<List<Category>>(emptyList())
    val categories: State<List<Category>> = _categories

    private val _kitchens = mutableStateOf<List<Category>>(emptyList())
    val kitchens: State<List<Category>> = _kitchens

    private val _cookingTypes = mutableStateOf<List<Category>>(emptyList())
    val cookingTypes: State<List<Category>> = _cookingTypes

    private val _celebrations = mutableStateOf<List<Category>>(emptyList())
    val celebrations: State<List<Category>> = _celebrations

    // Selected filters
    var selectedCategoryId by mutableStateOf<String?>(null)
    var selectedKitchenId by mutableStateOf<String?>(null)
    var selectedCookingId by mutableStateOf<String?>(null)
    var selectedCelebrationId by mutableStateOf<String?>(null)
    var searchQuery by mutableStateOf("")
    var currentUserId: String? = null

    private val service = ApiClient.recipeService
    private var fetchJob: kotlinx.coroutines.Job? = null

    init {
        initialFetch()
    }

    private fun initialFetch() {
        viewModelScope.launch {
            try {
                // Загружаем метаданные последовательно, каждый с защитой от ошибок
                _categories.value = try { service.getCategories() } catch (_: Exception) { emptyList() }
                _kitchens.value = try { service.getKitchens() } catch (_: Exception) { emptyList() }
                _cookingTypes.value = try { service.getCookingTypes() } catch (_: Exception) { emptyList() }
                _celebrations.value = try { service.getCelebrations() } catch (_: Exception) { emptyList() }
                
                // После метаданных грузим рецепты
                fetchRecipes()
            } catch (e: Exception) {
                android.util.Log.e("RecipeViewModel", "initialFetch failed", e)
                _state.value = RecipeState.Error("Не удалось загрузить данные")
            }
        }
    }

    fun fetchRecipes(currentUserId: String? = null, forceRefresh: Boolean = false) {
        if (!forceRefresh && _state.value is RecipeState.Success) return
        
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            // Если у нас уже есть данные, показываем индикатор обновления вместо полного экрана загрузки
            if (_state.value is RecipeState.Success) {
                _isRefreshing.value = true
            } else {
                _state.value = RecipeState.Loading
            }
            
            try {
                val noFilters = searchQuery.isBlank() && selectedCategoryId == null && 
                    selectedKitchenId == null && selectedCookingId == null && selectedCelebrationId == null
                    
                val recipes = if (noFilters) {
                    // Рекомендации требуют авторизацию, для гостей используем обычную ленту
                    if (currentUserId != null) {
                        try {
                            service.getRecommendations(page = 1, limit = 20)
                        } catch (_: Exception) {
                            service.getRecipes()
                        }
                    } else {
                        service.getRecipes()
                    }
                } else {
                    service.getRecipes(
                        categoryId = selectedCategoryId,
                        kitchenId = selectedKitchenId,
                        cookingId = selectedCookingId,
                        celebrationId = selectedCelebrationId,
                        search = searchQuery.takeIf { it.isNotBlank() }
                    )
                }
                
                // Fetch following and favorites status if logged in
                val uid = currentUserId
                if (uid != null) {
                    try {
                        val following = try { ApiClient.userService.getFollowing(uid) } catch (_: Exception) { emptyList() }
                        val favorites = try { ApiClient.userService.getFavorites() } catch (_: Exception) { emptyList() }
                        
                        val followingIds = following.map { it.id }.toSet()
                        val favoriteRecipeIds = favorites.map { it.recipe?.id }.filterNotNull().toSet()
                        
                        recipes.forEach { recipe ->
                            recipe.User?.isFollowing = followingIds.contains(recipe.User?.id)
                            recipe.isFavorited = favoriteRecipeIds.contains(recipe.id)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("RecipeViewModel", "Following/Favorites fetch failed", e)
                    }
                }
                
                _state.value = RecipeState.Success(recipes.toList())
            } catch (e: Exception) {
                android.util.Log.e("RecipeViewModel", "Fetch recipes failed", e)
                if (_state.value !is RecipeState.Success) {
                    _state.value = RecipeState.Error("Не удалось загрузить рецепты")
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun refresh() {
        fetchRecipes(currentUserId, forceRefresh = true)
    }

    fun retry() {
        initialFetch()
    }

    fun toggleLike(recipeId: String, isCurrentlyLiked: Boolean, currentUserId: String? = null) {
        if (currentUserId.isNullOrBlank()) return
        val currentState = _state.value
        if (currentState is RecipeState.Success) {
            val updatedRecipes = currentState.recipes.map {
                if (it.id.toString() == recipeId) {
                    val currentLikes = it.likes?.toMutableList() ?: mutableListOf()
                    val newLikes = if (isCurrentlyLiked) {
                        currentLikes.filter { it.userId != currentUserId }
                    } else {
                        currentLikes + RecipeLike(currentUserId)
                    }
                    it.copy(
                        isLiked = !isCurrentlyLiked,
                        likes = newLikes,
                        likesCount = newLikes.size
                    )
                } else {
                    it
                }
            }
            _state.value = RecipeState.Success(updatedRecipes)
            
            viewModelScope.launch {
                try {
                    if (isCurrentlyLiked) {
                        service.unlikeRecipe(recipeId)
                    } else {
                        service.likeRecipe(recipeId)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RecipeViewModel", "Like toggle failed", e)
                    _state.value = currentState
                }
            }
        }
    }

    fun toggleCategory(id: String) {
        selectedCategoryId = if (selectedCategoryId == id) null else id
        fetchRecipes(currentUserId, forceRefresh = true)
    }
    
    fun toggleKitchen(id: String) {
        selectedKitchenId = if (selectedKitchenId == id) null else id
        fetchRecipes(currentUserId, forceRefresh = true)
    }

    fun toggleCookingType(id: String) {
        selectedCookingId = if (selectedCookingId == id) null else id
        fetchRecipes(currentUserId, forceRefresh = true)
    }

    fun toggleCelebration(id: String) {
        selectedCelebrationId = if (selectedCelebrationId == id) null else id
        fetchRecipes(currentUserId, forceRefresh = true)
    }

    fun toggleFavorite(recipeId: Long, isCurrentlyFavorited: Boolean) {
        val currentState = _state.value
        if (currentState is RecipeState.Success) {
            val updatedRecipes = currentState.recipes.map {
                if (it.id == recipeId) {
                    it.copy(isFavorited = !isCurrentlyFavorited)
                } else {
                    it
                }
            }
            _state.value = RecipeState.Success(updatedRecipes)

            viewModelScope.launch {
                try {
                    if (isCurrentlyFavorited) {
                        ApiClient.userService.removeFavorite(recipeId.toString())
                    } else {
                        ApiClient.userService.addFavorite(recipeId.toString())
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RecipeViewModel", "Favorite toggle failed", e)
                    _state.value = currentState
                }
            }
        }
    }

    fun toggleFollow(authorId: String) {
        val currentState = _state.value
        if (currentState is RecipeState.Success) {
            // Создаем новый список с обновленными объектами
            val updatedRecipes = currentState.recipes.map { recipe ->
                if (recipe.User?.id == authorId) {
                    // Глубокое копирование: создаем новый Recipe и новый User
                    recipe.copy(User = recipe.User.copy(isFollowing = true))
                } else {
                    recipe
                }
            }
            // Явно устанавливаем новое состояние
            _state.value = RecipeState.Success(updatedRecipes.toList())

            viewModelScope.launch {
                try {
                    ApiClient.userService.follow(authorId)
                } catch (e: Exception) {
                    android.util.Log.e("RecipeViewModel", "Follow failed for author $authorId", e)
                }
            }
        }
    }
}
