package com.example.diplom.data

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

sealed class RecipeState {
    object Loading : RecipeState()
    data class Success(val recipes: List<Recipe>) : RecipeState()
    data class Error(val message: String) : RecipeState()
}

class RecipeViewModel : ViewModel() {
    private val _state = mutableStateOf<RecipeState>(RecipeState.Loading)
    val state: State<RecipeState> = _state

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

    private val service = ApiClient.recipeService

    init {
        initialFetch()
    }

    private fun initialFetch() {
        viewModelScope.launch {
            try {
                // Загружаем все метаданные один раз
                val categoriesDef = async { service.getCategories() }
                val kitchensDef = async { service.getKitchens() }
                val cookingDef = async { service.getCookingTypes() }
                val celebrationsDef = async { service.getCelebrations() }

                _categories.value = categoriesDef.await()
                _kitchens.value = kitchensDef.await()
                _cookingTypes.value = cookingDef.await()
                _celebrations.value = celebrationsDef.await()
                
                // После метаданных грузим рецепты
                fetchRecipes()
            } catch (e: Exception) {
                _state.value = RecipeState.Error("Metadata: ${e.message}")
            }
        }
    }

    fun fetchRecipes() {
        viewModelScope.launch {
            _state.value = RecipeState.Loading
            try {
                val recipes = service.getRecipes(
                    categoryId = selectedCategoryId,
                    kitchenId = selectedKitchenId,
                    cookingId = selectedCookingId,
                    celebrationId = selectedCelebrationId
                )
                _state.value = RecipeState.Success(recipes)
            } catch (e: Exception) {
                _state.value = RecipeState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun toggleLike(recipeId: String, isCurrentlyLiked: Boolean, currentUserId: String? = null) {
        val currentState = _state.value
        if (currentState is RecipeState.Success) {
            val updatedRecipes = currentState.recipes.map { recipe ->
                if (recipe.id == recipeId) {
                    val currentLikes = recipe.likes?.toMutableList() ?: mutableListOf()
                    val newLikes = if (isCurrentlyLiked) {
                        currentLikes.filter { it.userId != currentUserId }
                    } else {
                        if (currentUserId != null) currentLikes + RecipeLike(currentUserId) else currentLikes
                    }
                    recipe.copy(
                        isLiked = !isCurrentlyLiked,
                        likes = newLikes,
                        likesCount = newLikes.size
                    )
                } else {
                    recipe
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
        fetchRecipes()
    }
    
    fun toggleKitchen(id: String) {
        selectedKitchenId = if (selectedKitchenId == id) null else id
        fetchRecipes()
    }

    fun toggleCookingType(id: String) {
        selectedCookingId = if (selectedCookingId == id) null else id
        fetchRecipes()
    }

    fun toggleCelebration(id: String) {
        selectedCelebrationId = if (selectedCelebrationId == id) null else id
        fetchRecipes()
    }
}
