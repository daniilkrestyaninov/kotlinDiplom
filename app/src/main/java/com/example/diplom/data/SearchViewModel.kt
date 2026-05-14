package com.example.diplom.data

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class SearchResultState {
    object Idle : SearchResultState()
    object Loading : SearchResultState()
    data class Success(val recipes: List<Recipe>, val users: List<User>) : SearchResultState()
    data class Error(val message: String) : SearchResultState()
}

class SearchViewModel : ViewModel() {
    private val _searchState = mutableStateOf<SearchResultState>(SearchResultState.Idle)
    val searchState: State<SearchResultState> = _searchState

    var searchQuery = mutableStateOf("")
    var activeTab = mutableStateOf(0) // 0 - Recipes, 1 - Users

    private val recipeService = ApiClient.recipeService
    private val userService = ApiClient.userService
    private var searchJob: Job? = null

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchState.value = SearchResultState.Idle
            return
        }

        searchJob = viewModelScope.launch {
            delay(500) // Debounce
            performSearch()
        }
    }

    fun performSearch() {
        if (searchQuery.value.isBlank()) return

        viewModelScope.launch {
            _searchState.value = SearchResultState.Loading
            try {
                if (activeTab.value == 0) {
                    // Search recipes
                    val recipes = recipeService.getRecipes(search = searchQuery.value)
                    _searchState.value = SearchResultState.Success(recipes, emptyList())
                } else {
                    // Search users
                    val users = userService.searchUsers(searchQuery.value)
                    _searchState.value = SearchResultState.Success(emptyList(), users)
                }
            } catch (e: Exception) {
                _searchState.value = SearchResultState.Error("Ошибка поиска: ${e.message}")
            }
        }
    }

    fun setTab(index: Int) {
        activeTab.value = index
        if (searchQuery.value.isNotBlank()) {
            performSearch()
        }
    }
}
