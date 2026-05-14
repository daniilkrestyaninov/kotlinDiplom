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
}
