package com.example.diplom.data

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.diplom.data.local.UmamiDatabase
import com.example.diplom.data.local.CachedFavoriteRecipe
import com.google.gson.Gson

sealed class FavoritesState {
    object Loading : FavoritesState()
    data class Success(val favorites: List<Recipe>) : FavoritesState()
    data class Error(val message: String) : FavoritesState()
}

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {
    private val db = UmamiDatabase.getDatabase(application)
    private val dao = db.dao()
    private val gson = Gson()

    private val _state = mutableStateOf<FavoritesState>(FavoritesState.Loading)
    val state: State<FavoritesState> = _state

    fun loadFavorites() {
        viewModelScope.launch {
            // First load from cached database
            var cachedList = emptyList<Recipe>()
            try {
                val cached = dao.getCachedFavorites()
                if (cached.isNotEmpty()) {
                    cachedList = cached.map {
                        gson.fromJson(it.recipeJson, Recipe::class.java)
                    }
                    _state.value = FavoritesState.Success(cachedList)
                } else if (_state.value !is FavoritesState.Success) {
                    _state.value = FavoritesState.Loading
                }
            } catch (e: Exception) {
                android.util.Log.e("FavoritesVM", "Failed to load cached favorites", e)
            }

            try {
                val favs = ApiClient.userService.getFavorites()
                android.util.Log.d("FavoritesVM", "Loaded ${favs.size} favorites")
                _state.value = FavoritesState.Success(favs)

                // Update cache
                try {
                    dao.clearFavoritesCache()
                    dao.insertFavorites(favs.map { r ->
                        CachedFavoriteRecipe(
                            id = r.id,
                            recipeJson = gson.toJson(r)
                        )
                    })
                } catch (e: Exception) {
                    android.util.Log.e("FavoritesVM", "Failed to save favorites to cache", e)
                }
            } catch (e: Exception) {
                android.util.Log.e("FavoritesVM", "Error loading favorites from network", e)
                if (_state.value !is FavoritesState.Success) {
                    _state.value = FavoritesState.Error("Не удалось загрузить избранное")
                }
            }
        }
    }

    fun removeFavorite(recipeId: String) {
        val recipeIdLong = recipeId.toLongOrNull() ?: return
        val currentState = _state.value
        if (currentState is FavoritesState.Success) {
            // Optimistic update in UI
            val updatedList = currentState.favorites.filter { it.id != recipeIdLong }
            _state.value = FavoritesState.Success(updatedList)

            viewModelScope.launch {
                try {
                    // Remove from database cache
                    dao.deleteFavoriteById(recipeIdLong)
                    // Remove from server
                    ApiClient.userService.removeFavorite(recipeId)
                } catch (e: Exception) {
                    android.util.Log.e("FavoritesVM", "Error removing favorite", e)
                    // Reload to sync with server/cache
                    loadFavorites()
                }
            }
        }
    }
}
