package com.example.diplom.data

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed class RecipeDetailState {
    object Loading : RecipeDetailState()
    data class Success(val recipe: Recipe, val comments: List<Comment>) : RecipeDetailState()
    data class Error(val message: String) : RecipeDetailState()
}

class RecipeDetailViewModel : ViewModel() {
    private val _state = mutableStateOf<RecipeDetailState>(RecipeDetailState.Loading)
    val state: State<RecipeDetailState> = _state

    private val recipeService = ApiClient.recipeService

    fun loadRecipe(id: String) {
        viewModelScope.launch {
            _state.value = RecipeDetailState.Loading
            try {
                val recipe = recipeService.getRecipeById(id)
                val comments = recipeService.getComments(id)
                _state.value = RecipeDetailState.Success(recipe, comments)
            } catch (e: Exception) {
                _state.value = RecipeDetailState.Error(e.message ?: "Failed to load recipe")
            }
        }
    }

    fun postComment(recipeId: String, text: String, rating: Int?) {
        viewModelScope.launch {
            try {
                recipeService.postComment(recipeId, CommentRequest(text, rating))
                // Reload recipe and comments to get fresh data
                val currentState = _state.value
                if (currentState is RecipeDetailState.Success) {
                    val recipe = recipeService.getRecipeById(recipeId)
                    val comments = recipeService.getComments(recipeId)
                    _state.value = RecipeDetailState.Success(recipe, comments)
                }
            } catch (e: Exception) {
                android.util.Log.e("RecipeDetailViewModel", "Post comment failed", e)
            }
        }
    }
    
    fun toggleLike(recipeId: String, isCurrentlyLiked: Boolean, currentUserId: String?) {
        val currentState = _state.value
        if (currentState is RecipeDetailState.Success) {
            // Optimistic update: toggle the like state
            val currentLikes = currentState.recipe.likes?.toMutableList() ?: mutableListOf()
            val newLikes = if (isCurrentlyLiked) {
                currentLikes.filter { it.userId != currentUserId }
            } else {
                if (currentUserId != null) currentLikes + RecipeLike(currentUserId) else currentLikes
            }
            
            val updatedRecipe = currentState.recipe.copy(
                isLiked = !isCurrentlyLiked,
                likes = newLikes,
                likesCount = newLikes.size
            )
            _state.value = currentState.copy(recipe = updatedRecipe)
            
            viewModelScope.launch {
                try {
                    if (isCurrentlyLiked) {
                        recipeService.unlikeRecipe(recipeId)
                    } else {
                        recipeService.likeRecipe(recipeId)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RecipeDetailViewModel", "Toggle like failed", e)
                    _state.value = currentState
                }
            }
        }
    }
}
