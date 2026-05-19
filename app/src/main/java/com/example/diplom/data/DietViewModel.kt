package com.example.diplom.data

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class DietState {
    object Loading : DietState()
    data class Success(
        val myPlans: List<DietPlan> = emptyList(),
        val publicPlans: List<DietPlan> = emptyList(),
        val currentDetailPlan: DietPlan? = null
    ) : DietState()
    data class Error(val message: String) : DietState()
}

class DietViewModel : ViewModel() {
    private val _state = mutableStateOf<DietState>(DietState.Loading)
    val state: State<DietState> = _state

    private val dietService = ApiClient.dietService

    fun loadPlans(searchQuery: String? = null) {
        viewModelScope.launch {
            _state.value = DietState.Loading
            try {
                val myPlans = dietService.getMyDietPlans()
                val publicPlans = dietService.getPublicDietPlans(searchQuery)
                _state.value = DietState.Success(myPlans, publicPlans)
            } catch (e: Exception) {
                android.util.Log.e("DietViewModel", "Failed to load plans", e)
                _state.value = DietState.Error("Не удалось загрузить планы питания")
            }
        }
    }

    fun loadPlanDetail(id: String) {
        viewModelScope.launch {
            val currentState = _state.value
            val previousMy = if (currentState is DietState.Success) currentState.myPlans else emptyList()
            val previousPub = if (currentState is DietState.Success) currentState.publicPlans else emptyList()
            _state.value = DietState.Loading
            try {
                val plan = dietService.getDietPlanById(id)
                _state.value = DietState.Success(previousMy, previousPub, plan)
            } catch (e: Exception) {
                android.util.Log.e("DietViewModel", "Failed to load plan detail", e)
                _state.value = DietState.Error("Не удалось открыть план питания")
            }
        }
    }

    fun createPlan(title: String, description: String?, isPrivate: Boolean, recipes: List<DietPlanRecipeRequest>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                dietService.createDietPlan(CreateDietPlanRequest(title, description, isPrivate, recipes))
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("DietViewModel", "Failed to create plan", e)
            }
        }
    }

    fun updatePlan(id: String, title: String, description: String?, isPrivate: Boolean, recipes: List<DietPlanRecipeRequest>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                dietService.updateDietPlan(id, mapOf(
                    "title" to title,
                    "description" to (description ?: ""),
                    "is_private" to isPrivate,
                    "recipes" to recipes
                ))
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("DietViewModel", "Failed to update plan", e)
            }
        }
    }

    fun deletePlan(id: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                dietService.deleteDietPlan(id)
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("DietViewModel", "Failed to delete plan", e)
            }
        }
    }
}
