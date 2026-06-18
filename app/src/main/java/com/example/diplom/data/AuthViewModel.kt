package com.example.diplom.data

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.diplom.data.local.UmamiDatabase
import com.example.diplom.data.local.LocalUserAccount
import com.google.gson.Gson

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class VerificationRequired(val email: String, val password: String) : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application, private val tokenManager: TokenManager) : AndroidViewModel(application) {
    private val _state = mutableStateOf<AuthState>(AuthState.Idle)
    val state: State<AuthState> = _state

    private val service = ApiClient.authService
    private val userService = ApiClient.userService
    private val db = UmamiDatabase.getDatabase(application)
    private val dao = db.dao()
    private val gson = Gson()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            // First load from cached user account if available
            try {
                val cached = dao.getUserAccount()
                if (cached != null) {
                    val user = gson.fromJson(cached.userJson, User::class.java)
                    _state.value = AuthState.Success(user)
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Failed to load cached account", e)
            }

            tokenManager.getToken.collect { token ->
                if (!token.isNullOrEmpty()) {
                    refreshProfile()
                } else {
                    _state.value = AuthState.Idle
                }
            }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            try {
                val user = userService.getMyProfile()
                _state.value = AuthState.Success(user)
                // Cache user profile
                dao.saveUserAccount(LocalUserAccount(user.id, gson.toJson(user)))
            } catch (_: Exception) {
                // If we are already in Success (from cache), keep it and don't overwrite with error
                if (_state.value !is AuthState.Success) {
                    _state.value = AuthState.Idle
                }
            }
        }
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val res = service.login(AuthRequest(email, pass))
                tokenManager.saveToken(res.token)
                _state.value = AuthState.Success(res.user)
                // Cache user profile
                dao.saveUserAccount(LocalUserAccount(res.user.id, gson.toJson(res.user)))
            } catch (e: Exception) {
                val msg = e.message ?: ""
                if (msg.contains("403")) {
                    _state.value = AuthState.VerificationRequired(email, pass)
                } else {
                    val displayMsg = when {
                        msg.contains("404") -> "Пользователь не найден"
                        msg.contains("401") -> "Неверный пароль"
                        else -> "Ошибка входа. Проверьте данные или подключение."
                    }
                    _state.value = AuthState.Error(displayMsg)
                }
            }
        }
    }

    fun register(username: String, name: String, email: String, pass: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                service.register(RegisterRequest(username, name, email, pass))
                _state.value = AuthState.VerificationRequired(email, pass)
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Register failed", e)
                _state.value = AuthState.Error("Ошибка регистрации. Возможно, email или логин уже заняты.")
            }
        }
    }

    fun resendCode(email: String) {
        viewModelScope.launch {
            try {
                service.resendCode(mapOf("email" to email))
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Resend failed", e)
                _state.value = AuthState.Error("Ошибка переотправки кода.")
            }
        }
    }

    fun verifyEmail(email: String, code: String, passwordForAutoLogin: String? = null) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                service.verifyEmail(VerifyEmailRequest(email, code))
                if (!passwordForAutoLogin.isNullOrBlank()) {
                    val res = service.login(AuthRequest(email, passwordForAutoLogin))
                    tokenManager.saveToken(res.token)
                    _state.value = AuthState.Success(res.user)
                    // Cache user profile
                    dao.saveUserAccount(LocalUserAccount(res.user.id, gson.toJson(res.user)))
                } else {
                    _state.value = AuthState.Idle
                }
            } catch (e: Exception) {
                _state.value = AuthState.Error("Ошибка подтверждения email. Проверьте код.")
            }
        }
    }

    fun requestPasswordRecovery(email: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                service.passwordRecovery(mapOf("email" to email))
                _state.value = AuthState.Idle
                onSuccess()
            } catch (e: Exception) {
                _state.value = AuthState.Error("Ошибка восстановления пароля. Проверьте email.")
            }
        }
    }

    fun resetPassword(email: String, code: String, newPass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                service.resetPassword(mapOf(
                    "email" to email,
                    "code" to code,
                    "new_password" to newPass
                ))
                _state.value = AuthState.Idle
                onSuccess()
            } catch (e: Exception) {
                _state.value = AuthState.Error("Ошибка смены пароля. Проверьте код.")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenManager.deleteToken()
            // Clear caches
            try {
                dao.clearUserAccount()
                dao.clearFavoritesCache()
                dao.clearMyRecipesCache()
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Failed to clear caches on logout", e)
            }
            _state.value = AuthState.Idle
        }
    }

    fun deleteAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                userService.deleteMyProfile()
                tokenManager.deleteToken()
                // Clear caches
                try {
                    dao.clearUserAccount()
                    dao.clearFavoritesCache()
                    dao.clearMyRecipesCache()
                } catch (e: Exception) {
                    android.util.Log.e("AuthViewModel", "Failed to clear caches on deletion", e)
                }
                _state.value = AuthState.Idle
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Account deletion failed", e)
                onError(e.localizedMessage ?: "Неизвестная ошибка")
            }
        }
    }

    companion object {
        fun provideFactory(application: Application, tokenManager: TokenManager): androidx.lifecycle.ViewModelProvider.Factory =
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(application, tokenManager) as T
                }
            }
    }
}
