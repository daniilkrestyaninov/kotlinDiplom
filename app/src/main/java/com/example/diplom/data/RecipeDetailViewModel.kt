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

    fun loadRecipe(id: String, currentUserId: String? = null) {
        viewModelScope.launch {
            _state.value = RecipeDetailState.Loading
            try {
                val recipe = recipeService.getRecipeById(id)
                val comments = recipeService.getComments(id)
                
                // Проверяем, подписаны ли мы на автора
                if (currentUserId != null) {
                    try {
                        val following = ApiClient.userService.getFollowing(currentUserId)
                        recipe.User?.isFollowing = following.any { it.id == recipe.User?.id }
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
                
                _state.value = RecipeDetailState.Success(recipe, comments)
            } catch (e: Exception) {
                _state.value = RecipeDetailState.Error(e.message ?: "Failed to load recipe")
            }
        }
    }

    fun postComment(
        recipeId: String,
        text: String,
        rating: Int?,
        tasteSweet: Int?,
        tasteSour: Int?,
        tasteSalty: Int?,
        tasteSpicy: Int?,
        tasteUmami: Int?
    ) {
        viewModelScope.launch {
            try {
                recipeService.postComment(
                    recipeId,
                    CommentRequest(text, rating, tasteSweet, tasteSour, tasteSalty, tasteSpicy, tasteUmami)
                )
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
        if (currentUserId.isNullOrBlank()) return
        val currentState = _state.value
        if (currentState is RecipeDetailState.Success) {
            // Optimistic update: toggle the like state
            val currentLikes = currentState.recipe.likes?.toMutableList() ?: mutableListOf()
            val newLikes = if (isCurrentlyLiked) {
                currentLikes.filter { it.userId != currentUserId }
            } else {
                currentLikes + RecipeLike(currentUserId)
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

    fun toggleFollow(authorId: String, isCurrentlyFollowing: Boolean) {
        val currentState = _state.value
        if (currentState is RecipeDetailState.Success) {
            val updatedUser = currentState.recipe.User?.copy(isFollowing = !isCurrentlyFollowing)
            val updatedRecipe = currentState.recipe.copy(User = updatedUser)
            _state.value = currentState.copy(recipe = updatedRecipe)

            viewModelScope.launch {
                try {
                    if (isCurrentlyFollowing) {
                        ApiClient.userService.unfollow(authorId)
                    } else {
                        ApiClient.userService.follow(authorId)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RecipeDetailViewModel", "Toggle follow failed", e)
                    _state.value = currentState
                }
            }
        }
    }
}
