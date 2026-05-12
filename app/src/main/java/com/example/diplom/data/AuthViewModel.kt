package com.example.diplom.data

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class VerificationRequired(val email: String, val password: String) : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val tokenManager: TokenManager) : ViewModel() {
    private val _state = mutableStateOf<AuthState>(AuthState.Idle)
    val state: State<AuthState> = _state

    private val service = ApiClient.authService
    private val userService = ApiClient.userService

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
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
            } catch (_: Exception) {
                // If it fails during checkAuthStatus, we might want to logout
                // but if it's a manual refresh, we just ignore
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
            } catch (e: Exception) {
                _state.value = AuthState.Error("Ошибка входа. Проверьте данные или подключение.")
            }
        }
    }

    fun register(username: String, name: String, email: String, pass: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                service.register(RegisterRequest(username, name, email, pass))
                _state.value = AuthState.VerificationRequired(email = email, password = pass)
            } catch (e: Exception) {
                _state.value = AuthState.Error("Ошибка регистрации. Попробуйте позже.")
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
                } else {
                    _state.value = AuthState.Idle
                }
            } catch (e: Exception) {
                _state.value = AuthState.Error("Ошибка подтверждения email. Проверьте код.")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenManager.deleteToken()
            _state.value = AuthState.Idle
        }
    }

    companion object {
        fun provideFactory(tokenManager: TokenManager): androidx.lifecycle.ViewModelProvider.Factory =
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(tokenManager) as T
                }
            }
    }
}
