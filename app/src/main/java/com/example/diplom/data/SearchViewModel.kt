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
    val selectedCategoryId = mutableStateOf<String?>(null)

    private val recipeService = ApiClient.recipeService
    private val userService = ApiClient.userService
    private var searchJob: Job? = null

    init {
        performSearch()
    }

    fun toggleCategory(id: String) {
        selectedCategoryId.value = if (selectedCategoryId.value == id) null else id
        performSearch()
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.isNotBlank()) {
                delay(500) // Debounce only when typing text
            }
            performSearch()
        }
    }

    fun performSearch() {
        viewModelScope.launch {
            _searchState.value = SearchResultState.Loading
            try {
                if (activeTab.value == 0) {
                    // Search recipes
                    val recipes = recipeService.getRecipes(
                        categoryId = selectedCategoryId.value,
                        search = searchQuery.value.takeIf { it.isNotBlank() }
                    )
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
        performSearch()
    }
}
