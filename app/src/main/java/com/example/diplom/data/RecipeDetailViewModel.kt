package com.example.diplom.data

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed class RecipeDetailState {
    object Loading : RecipeDetailState()
    data class Success(val recipe: Recipe, val comments: List<Comment>, val isFavorited: Boolean = false) : RecipeDetailState()
    data class Error(val message: String) : RecipeDetailState()
}

class RecipeDetailViewModel : ViewModel() {
    private val _state = mutableStateOf<RecipeDetailState>(RecipeDetailState.Loading)
    val state: State<RecipeDetailState> = _state
    
    private val recipeService = ApiClient.recipeService

    fun loadRecipe(id: String, currentUserId: String? = null) {
        val recipeIdLong = id.toLongOrNull()
        viewModelScope.launch {
            _state.value = RecipeDetailState.Loading
            try {
                val recipe = recipeService.getRecipeById(id)
                val comments = recipeService.getComments(id)
                var isFavorited = false
                
                // Проверяем, подписаны ли мы на автора
                if (currentUserId != null) {
                    try {
                        val following = ApiClient.userService.getFollowing(currentUserId)
                        recipe.User?.isFollowing = following.any { it.id == recipe.User?.id }
                    } catch (e: Exception) {
                        android.util.Log.e("RecipeDetailVM", "Failed to check following status", e)
                    }

                    try {
                        // Проверяем избранное
                        val favorites = ApiClient.userService.getFavorites()
                        if (recipeIdLong != null) {
                            isFavorited = favorites.any { it.recipe?.id == recipeIdLong }
                            android.util.Log.d("RecipeDetailVM", "Is favorited: $isFavorited (found in ${favorites.size} favorites)")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("RecipeDetailVM", "Failed to check favorite status", e)
                    }
                }
                
                _state.value = RecipeDetailState.Success(recipe, comments, isFavorited)
            } catch (e: Exception) {
                _state.value = RecipeDetailState.Error("Не удалось загрузить рецепт")
            }
        }
    }

    fun toggleFavorite(recipeId: String, currentIsFavorited: Boolean) {
        val currentState = _state.value
        if (currentState is RecipeDetailState.Success) {
            // Оптимистичное обновление
            _state.value = currentState.copy(isFavorited = !currentIsFavorited)
            
            viewModelScope.launch {
                try {
                    if (currentIsFavorited) {
                        ApiClient.userService.removeFavorite(recipeId)
                        android.util.Log.d("RecipeDetailVM", "Removed from favorites: $recipeId")
                    } else {
                        ApiClient.userService.addFavorite(recipeId)
                        android.util.Log.d("RecipeDetailVM", "Added to favorites: $recipeId")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RecipeDetailVM", "Toggle favorite failed", e)
                    // Откат при ошибке
                    _state.value = currentState
                }
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
        tasteUmami: Int?,
        parentCommentId: String? = null
    ) {
        viewModelScope.launch {
            try {
                recipeService.postComment(
                    recipeId,
                    CommentRequest(text, rating, tasteSweet, tasteSour, tasteSalty, tasteSpicy, tasteUmami, parentCommentId)
                )
                // Reload recipe and comments to get fresh data
                val currentState = _state.value
                if (currentState is RecipeDetailState.Success) {
                    val recipe = recipeService.getRecipeById(recipeId)
                    val comments = recipeService.getComments(recipeId)
                    _state.value = RecipeDetailState.Success(recipe, comments, currentState.isFavorited)
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
                currentLikes + RecipeLike(currentUserId, null)
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

    fun toggleCommentLike(commentId: String) {
        val currentState = _state.value
        if (currentState is RecipeDetailState.Success) {
            val updatedComments = currentState.comments.map { comment ->
                if (comment.id == commentId) {
                    val isLiked = comment.isLiked ?: false
                    val likeCount = comment.likeCount ?: 0
                    comment.copy(
                        isLiked = !isLiked,
                        likeCount = if (isLiked) (likeCount - 1).coerceAtLeast(0) else likeCount + 1
                    )
                } else comment
            }
            _state.value = currentState.copy(comments = updatedComments)

            viewModelScope.launch {
                try {
                    ApiClient.recipeService.toggleCommentLike(commentId)
                } catch (e: Exception) {
                    android.util.Log.e("RecipeDetailVM", "Comment like toggle failed", e)
                    _state.value = currentState
                }
            }
        }
    }

    fun report(type: String, recipeId: Long? = null, reportedUserId: Long? = null, reason: String, description: String? = null, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                ApiClient.reportService.createReport(
                    ReportRequest(type, reportedUserId, recipeId, reason, description)
                )
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("RecipeDetailVM", "Report failed", e)
            }
        }
    }

    fun savePersonalNote(recipeId: String, note: String) {
        viewModelScope.launch {
            try {
                recipeService.updatePersonalNote(recipeId, mapOf("note" to note))
                val currentState = _state.value
                if (currentState is RecipeDetailState.Success) {
                    _state.value = currentState.copy(recipe = currentState.recipe.copy(personalNote = note))
                }
            } catch (e: Exception) {
                android.util.Log.e("RecipeDetailVM", "Failed to save note", e)
            }
        }
    }

    fun markAsCooked(recipeId: String) {
        viewModelScope.launch {
            try {
                val response = recipeService.markCooked(recipeId)
                val currentState = _state.value
                if (currentState is RecipeDetailState.Success) {
                    val currentCount = currentState.recipe.cookedCount ?: 0
                    val updatedRecipe = currentState.recipe.copy(
                        isCooked = response.cooked,
                        cookedCount = if (response.cooked) currentCount + 1 else (currentCount - 1).coerceAtLeast(0)
                    )
                    _state.value = currentState.copy(recipe = updatedRecipe)
                }
            } catch (e: Exception) {
                android.util.Log.e("RecipeDetailVM", "Failed to mark cooked", e)
            }
        }
    }

    fun exportIngredients(recipeId: String, onExported: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val responseBody = recipeService.exportIngredients(recipeId)
                val text = responseBody.string()
                onExported(text)
            } catch (e: Exception) {
                android.util.Log.e("RecipeDetailViewModel", "Export failed", e)
                onError(e.localizedMessage ?: "Не удалось экспортировать ингредиенты")
            }
        }
    }

    fun loadRecipeLikes(recipeId: String, onLoaded: (List<UserBrief>) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val likes = recipeService.getRecipeLikes(recipeId)
                onLoaded(likes)
            } catch (e: Exception) {
                android.util.Log.e("RecipeDetailViewModel", "Load likes failed", e)
                onError(e.localizedMessage ?: "Не удалось загрузить список оценивших")
            }
        }
    }
}
