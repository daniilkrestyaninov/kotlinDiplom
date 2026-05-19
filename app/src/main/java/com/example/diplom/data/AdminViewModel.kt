package com.example.diplom.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AdminState<out T> {
    object Idle : AdminState<Nothing>()
    object Loading : AdminState<Nothing>()
    data class Success<T>(val data: T) : AdminState<T>()
    data class Error(val message: String) : AdminState<Nothing>()
}

class AdminViewModel(private val service: AdminService = ApiClient.adminService) : ViewModel() {

    private val _verifications = MutableStateFlow<AdminState<List<VerificationRequest>>>(AdminState.Idle)
    val verifications = _verifications.asStateFlow()

    private val _auditLogs = MutableStateFlow<AdminState<List<AuditLog>>>(AdminState.Idle)
    val auditLogs = _auditLogs.asStateFlow()

    fun loadVerifications() {
        viewModelScope.launch {
            _verifications.value = AdminState.Loading
            try {
                _verifications.value = AdminState.Success(service.getVerifications())
            } catch (e: Exception) {
                _verifications.value = AdminState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun updateVerificationStatus(id: Long, status: String) {
        viewModelScope.launch {
            try {
                service.updateVerification(id, mapOf("status" to status))
                loadVerifications() // Refresh list
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to update status", e)
            }
        }
    }

    fun loadAuditLogs() {
        viewModelScope.launch {
            _auditLogs.value = AdminState.Loading
            try {
                _auditLogs.value = AdminState.Success(service.getAuditLogs())
            } catch (e: Exception) {
                _auditLogs.value = AdminState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun broadcastNotification(title: String, body: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                service.broadcastNotification(mapOf("title" to title, "body" to body))
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Broadcast failed", e)
            }
        }
    }

    private val _users = MutableStateFlow<AdminState<List<User>>>(AdminState.Idle)
    val users = _users.asStateFlow()

    fun loadUsers() {
        viewModelScope.launch {
            _users.value = AdminState.Loading
            try {
                _users.value = AdminState.Success(service.getUsers())
            } catch (e: Exception) {
                _users.value = AdminState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun updateUser(id: String, name: String?, bio: String?, roleId: Int?) {
        viewModelScope.launch {
            try {
                service.updateUser(id, AdminUpdateUserRequest(name, bio, roleId))
                loadUsers() // Refresh list
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to update user", e)
            }
        }
    }

    fun blockUser(id: String) {
        viewModelScope.launch {
            try {
                service.blockUser(id)
                loadUsers() // Refresh list
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to block user", e)
            }
        }
    }

    fun unblockUser(id: String) {
        viewModelScope.launch {
            try {
                service.unblockUser(id)
                loadUsers() // Refresh list
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to unblock user", e)
            }
        }
    }

    private val _reports = MutableStateFlow<AdminState<List<ReportItem>>>(AdminState.Idle)
    val reports = _reports.asStateFlow()

    fun loadReports() {
        viewModelScope.launch {
            _reports.value = AdminState.Loading
            try {
                _reports.value = AdminState.Success(service.getReports())
            } catch (e: Exception) {
                _reports.value = AdminState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun updateReportStatus(id: Long, status: String) {
        viewModelScope.launch {
            try {
                service.updateReportStatus(id, mapOf("status" to status))
                loadReports()
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to update report", e)
            }
        }
    }

    private val _appeals = MutableStateFlow<AdminState<List<AppealItem>>>(AdminState.Idle)
    val appeals = _appeals.asStateFlow()

    fun loadAppeals() {
        viewModelScope.launch {
            _appeals.value = AdminState.Loading
            try {
                _appeals.value = AdminState.Success(service.getAppeals())
            } catch (e: Exception) {
                _appeals.value = AdminState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun updateAppealStatus(id: Long, status: String, adminNotes: String = "") {
        viewModelScope.launch {
            try {
                service.updateAppealStatus(id, mapOf("status" to status, "admin_notes" to adminNotes))
                loadAppeals()
                loadUsers() // In case it unblocked a user
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to update appeal", e)
            }
        }
    }

    // --- Metadata States ---
    private val _categories = MutableStateFlow<AdminState<List<Category>>>(AdminState.Idle)
    val categories = _categories.asStateFlow()

    private val _kitchens = MutableStateFlow<AdminState<List<Category>>>(AdminState.Idle)
    val kitchens = _kitchens.asStateFlow()

    private val _cookingTypes = MutableStateFlow<AdminState<List<Category>>>(AdminState.Idle)
    val cookingTypes = _cookingTypes.asStateFlow()

    private val _celebrations = MutableStateFlow<AdminState<List<Category>>>(AdminState.Idle)
    val celebrations = _celebrations.asStateFlow()

    private val _units = MutableStateFlow<AdminState<List<UnitModel>>>(AdminState.Idle)
    val units = _units.asStateFlow()

    private val _ingredients = MutableStateFlow<AdminState<List<IngredientModel>>>(AdminState.Idle)
    val ingredients = _ingredients.asStateFlow()

    // --- Loaders ---
    fun loadCategories() {
        viewModelScope.launch {
            _categories.value = AdminState.Loading
            try {
                _categories.value = AdminState.Success(ApiClient.recipeService.getCategories())
            } catch (e: Exception) {
                _categories.value = AdminState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadKitchens() {
        viewModelScope.launch {
            _kitchens.value = AdminState.Loading
            try {
                _kitchens.value = AdminState.Success(ApiClient.recipeService.getKitchens())
            } catch (e: Exception) {
                _kitchens.value = AdminState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadCookingTypes() {
        viewModelScope.launch {
            _cookingTypes.value = AdminState.Loading
            try {
                _cookingTypes.value = AdminState.Success(ApiClient.recipeService.getCookingTypes())
            } catch (e: Exception) {
                _cookingTypes.value = AdminState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadCelebrations() {
        viewModelScope.launch {
            _celebrations.value = AdminState.Loading
            try {
                _celebrations.value = AdminState.Success(ApiClient.recipeService.getCelebrations())
            } catch (e: Exception) {
                _celebrations.value = AdminState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadUnits() {
        viewModelScope.launch {
            _units.value = AdminState.Loading
            try {
                _units.value = AdminState.Success(service.getUnits())
            } catch (e: Exception) {
                _units.value = AdminState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadIngredients(search: String? = null) {
        viewModelScope.launch {
            _ingredients.value = AdminState.Loading
            try {
                _ingredients.value = AdminState.Success(service.getIngredients(search))
            } catch (e: Exception) {
                _ingredients.value = AdminState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // --- Categories CRUD ---
    fun createCategory(name: String, description: String?, imageUrl: String?, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val body = mutableMapOf("name" to name)
                if (description != null) body["description"] = description
                if (imageUrl != null) body["image_url"] = imageUrl
                service.createCategory(body)
                loadCategories()
                onComplete(true)
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to create category", e)
                onComplete(false)
            }
        }
    }

    fun updateCategory(id: String, name: String, description: String?, imageUrl: String?, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val body = mutableMapOf("name" to name)
                if (description != null) body["description"] = description
                if (imageUrl != null) body["image_url"] = imageUrl
                service.updateCategory(id, body)
                loadCategories()
                onComplete(true)
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to update category", e)
                onComplete(false)
            }
        }
    }

    fun deleteCategory(id: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                service.deleteCategory(id)
                loadCategories()
                onComplete(true)
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to delete category", e)
                onComplete(false)
            }
        }
    }

    // --- Kitchens CRUD ---
    fun createKitchen(name: String, imageUrl: String?, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val body = mutableMapOf("name" to name)
                if (imageUrl != null) body["image_url"] = imageUrl
                service.createKitchen(body)
                loadKitchens()
                onComplete(true)
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to create kitchen", e)
                onComplete(false)
            }
        }
    }

    fun updateKitchen(id: String, name: String, imageUrl: String?, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val body = mutableMapOf("name" to name)
                if (imageUrl != null) body["image_url"] = imageUrl
                service.updateKitchen(id, body)
                loadKitchens()
                onComplete(true)
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to update kitchen", e)
                onComplete(false)
            }
        }
    }

    fun deleteKitchen(id: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                service.deleteKitchen(id)
                loadKitchens()
                onComplete(true)
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to delete kitchen", e)
                onComplete(false)
            }
        }
    }

    // --- Cooking Types CRUD ---
    fun createCookingType(name: String, imageUrl: String?, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val body = mutableMapOf("name" to name)
                if (imageUrl != null) body["image_url"] = imageUrl
                service.createCookingType(body)
                loadCookingTypes()
                onComplete(true)
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to create cooking type", e)
                onComplete(false)
            }
        }
    }

    fun updateCookingType(id: String, name: String, imageUrl: String?, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val body = mutableMapOf("name" to name)
                if (imageUrl != null) body["image_url"] = imageUrl
                service.updateCookingType(id, body)
                loadCookingTypes()
                onComplete(true)
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to update cooking type", e)
                onComplete(false)
            }
        }
    }

    fun deleteCookingType(id: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                service.deleteCookingType(id)
                loadCookingTypes()
                onComplete(true)
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to delete cooking type", e)
                onComplete(false)
            }
        }
    }

    // --- Celebrations CRUD ---
    fun createCelebration(name: String, imageUrl: String?, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val body = mutableMapOf("name" to name)
                if (imageUrl != null) body["image_url"] = imageUrl
                service.createCelebration(body)
                loadCelebrations()
                onComplete(true)
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to create celebration", e)
                onComplete(false)
            }
        }
    }

    fun updateCelebration(id: String, name: String, imageUrl: String?, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val body = mutableMapOf("name" to name)
                if (imageUrl != null) body["image_url"] = imageUrl
                service.updateCelebration(id, body)
                loadCelebrations()
                onComplete(true)
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to update celebration", e)
                onComplete(false)
            }
        }
    }

    fun deleteCelebration(id: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                service.deleteCelebration(id)
                loadCelebrations()
                onComplete(true)
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to delete celebration", e)
                onComplete(false)
            }
        }
    }

    // --- Units CRUD ---
    fun createUnit(name: String, shortName: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                service.createUnit(mapOf("name" to name, "short_name" to shortName))
                loadUnits()
                onComplete(true)
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to create unit", e)
                onComplete(false)
            }
        }
    }

    fun updateUnit(id: Long, name: String, shortName: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                service.updateUnit(id, mapOf("name" to name, "short_name" to shortName))
                loadUnits()
                onComplete(true)
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to update unit", e)
                onComplete(false)
            }
        }
    }

    fun deleteUnit(id: Long, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                service.deleteUnit(id)
                loadUnits()
                onComplete(true)
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to delete unit", e)
                onComplete(false)
            }
        }
    }

    // --- Ingredients CRUD ---
    fun createIngredient(name: String, unitId: Long?, description: String?, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val body = mutableMapOf<String, Any>("name" to name)
                if (unitId != null) body["unit_id"] = unitId
                if (description != null) body["description"] = description
                service.createIngredient(body)
                loadIngredients()
                onComplete(true)
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to create ingredient", e)
                onComplete(false)
            }
        }
    }

    fun updateIngredient(id: String, name: String, unitId: Long?, description: String?, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val body = mutableMapOf<String, Any>("name" to name)
                if (unitId != null) body["unit_id"] = unitId
                if (description != null) body["description"] = description
                service.updateIngredient(id, body)
                loadIngredients()
                onComplete(true)
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to update ingredient", e)
                onComplete(false)
            }
        }
    }

    fun deleteIngredient(id: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                service.deleteIngredient(id)
                loadIngredients()
                onComplete(true)
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to delete ingredient", e)
                onComplete(false)
            }
        }
    }

    // --- Weekly Menu Management ---
    private val _menuOfWeek = MutableStateFlow<AdminState<List<MenuOfTheWeekItem>>>(AdminState.Idle)
    val menuOfWeek = _menuOfWeek.asStateFlow()

    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes = _recipes.asStateFlow()

    fun loadMenuOfWeek() {
        viewModelScope.launch {
            _menuOfWeek.value = AdminState.Loading
            try {
                _menuOfWeek.value = AdminState.Success(service.getMenuOfTheWeek())
            } catch (e: Exception) {
                _menuOfWeek.value = AdminState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun addToMenuOfWeek(dayOfWeek: Int, recipeId: Long, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                service.addToMenu(AddMenuRequest(dayOfWeek, recipeId))
                loadMenuOfWeek()
                onComplete(true, null)
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val message = try {
                    org.json.JSONObject(errorBody ?: "{}").getString("message")
                } catch (_: Exception) {
                    "Ошибка сервера: ${e.code()}"
                }
                android.util.Log.e("AdminVM", "Http error adding to menu: $message", e)
                onComplete(false, message)
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to add to menu", e)
                onComplete(false, e.localizedMessage ?: "Неизвестная ошибка сети")
            }
        }
    }

    fun removeFromMenuOfWeek(id: Long, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                service.removeFromMenu(id)
                loadMenuOfWeek()
                onComplete(true, null)
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val message = try {
                    org.json.JSONObject(errorBody ?: "{}").getString("message")
                } catch (_: Exception) {
                    "Ошибка сервера: ${e.code()}"
                }
                onComplete(false, message)
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to remove from menu", e)
                onComplete(false, e.localizedMessage ?: "Неизвестная ошибка сети")
            }
        }
    }

    fun searchRecipes(query: String) {
        viewModelScope.launch {
            try {
                _recipes.value = ApiClient.recipeService.getRecipes(search = query.takeIf { it.isNotBlank() })
            } catch (e: Exception) {
                android.util.Log.e("AdminVM", "Failed to search recipes", e)
            }
        }
    }
}
