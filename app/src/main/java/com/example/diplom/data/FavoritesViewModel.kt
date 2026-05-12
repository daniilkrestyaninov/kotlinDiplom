package com.example.diplom.data

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed class FavoritesState {
    object Loading : FavoritesState()
    data class Success(val favorites: List<FavoriteItem>) : FavoritesState()
    data class Error(val message: String) : FavoritesState()
}

class FavoritesViewModel : ViewModel() {
    private val _state = mutableStateOf<FavoritesState>(FavoritesState.Loading)
    val state: State<FavoritesState> = _state

    fun loadFavorites() {
        viewModelScope.launch {
            _state.value = FavoritesState.Loading
            try {
                val favs = ApiClient.userService.getFavorites()
                android.util.Log.d("FavoritesVM", "Loaded ${favs.size} favorites")
                favs.forEach { fav ->
                    android.util.Log.d("FavoritesVM", "Fav ID: ${fav.id}, Recipe ID: ${fav.recipeId}, Recipe null?: ${fav.recipe == null}")
                    if (fav.recipe != null) {
                        android.util.Log.d("FavoritesVM", "  Recipe Title: ${fav.recipe.title}")
                    }
                }
                _state.value = FavoritesState.Success(favs)
            } catch (e: Exception) {
                android.util.Log.e("FavoritesVM", "Error loading favorites", e)
                _state.value = FavoritesState.Error("Не удалось загрузить избранное")
            }
        }
    }

    fun removeFavorite(recipeId: String) {
        val recipeIdLong = recipeId.toLongOrNull() ?: return
        val currentState = _state.value
        if (currentState is FavoritesState.Success) {
            // Optimistic update
            val updatedList = currentState.favorites.filter { it.recipe?.id != recipeIdLong }
            _state.value = FavoritesState.Success(updatedList)

            viewModelScope.launch {
                try {
                    ApiClient.userService.removeFavorite(recipeId)
                } catch (e: Exception) {
                    android.util.Log.e("FavoritesVM", "Error removing favorite", e)
                    // Reload to sync with server
                    loadFavorites()
                }
            }
        }
    }
}
