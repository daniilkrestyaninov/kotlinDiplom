package com.example.diplom.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationViewModel(private val service: NotificationService = ApiClient.notificationService) : ViewModel() {
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private var currentPage = 1
    private var totalPages = 1

    private val _canLoadMore = MutableStateFlow(true)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore.asStateFlow()

    /**
     * Лёгкий запрос — только число непрочитанных, без загрузки всего списка.
     * Вызывается из TopBar / NavGraph при каждом резюме.
     */
    fun refreshUnreadCount() {
        viewModelScope.launch {
            try {
                val response = service.getUnreadCount()
                _unreadCount.value = response.count
            } catch (e: Exception) {
                android.util.Log.e("NotificationVM", "Failed to get unread count", e)
            }
        }
    }

    /**
     * Полная загрузка первой страницы уведомлений.
     */
    fun loadNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                currentPage = 1
                val response = service.getNotifications(page = 1)
                _notifications.value = response.notifications
                totalPages = response.pagination.totalPages
                _canLoadMore.value = currentPage < totalPages
                _unreadCount.value = response.notifications.count { !it.isRead }
            } catch (e: Exception) {
                android.util.Log.e("NotificationVM", "Failed to load", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Pull-to-refresh — обновить список с самого начала.
     */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                currentPage = 1
                val response = service.getNotifications(page = 1)
                _notifications.value = response.notifications
                totalPages = response.pagination.totalPages
                _canLoadMore.value = currentPage < totalPages
                _unreadCount.value = response.notifications.count { !it.isRead }
            } catch (e: Exception) {
                android.util.Log.e("NotificationVM", "Failed to refresh", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Подгрузить следующую страницу (бесконечная прокрутка).
     */
    fun loadMore() {
        if (currentPage >= totalPages || _isLoading.value) return
        viewModelScope.launch {
            try {
                currentPage++
                val response = service.getNotifications(page = currentPage)
                _notifications.value = _notifications.value + response.notifications
                totalPages = response.pagination.totalPages
                _canLoadMore.value = currentPage < totalPages
            } catch (e: Exception) {
                currentPage-- // откатываем, чтобы можно было повторить
                android.util.Log.e("NotificationVM", "Failed to load more", e)
            }
        }
    }

    fun markAsRead(id: Int) {
        viewModelScope.launch {
            try {
                service.markAsRead(id)
                _notifications.value = _notifications.value.map {
                    if (it.id == id) it.copy(isRead = true) else it
                }
                _unreadCount.value = _notifications.value.count { !it.isRead }
            } catch (e: Exception) {
                android.util.Log.e("NotificationVM", "Failed to mark read", e)
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            try {
                service.markAllAsRead()
                _notifications.value = _notifications.value.map { it.copy(isRead = true) }
                _unreadCount.value = 0
            } catch (e: Exception) {
                android.util.Log.e("NotificationVM", "Failed to mark all read", e)
            }
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            try {
                service.deleteAll()
                _notifications.value = emptyList()
                _unreadCount.value = 0
                currentPage = 1
                totalPages = 1
                _canLoadMore.value = false
            } catch (e: Exception) {
                android.util.Log.e("NotificationVM", "Failed to delete all", e)
            }
        }
    }

    /**
     * Отправка FCM токена на сервер для регистрации устройства.
     */
    fun updateFcmToken(token: String) {
        viewModelScope.launch {
            try {
                service.registerDevice(DeviceTokenRequest(token))
                android.util.Log.d("NotificationVM", "FCM токен успешно зарегистрирован")
            } catch (e: Exception) {
                android.util.Log.e("NotificationVM", "Ошибка регистрации FCM токена", e)
            }
        }
    }
}
