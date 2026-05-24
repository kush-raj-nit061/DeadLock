package com.ingray.deadlock.domain.repository

import com.ingray.deadlock.data.local.entity.VaultedNotificationEntity
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    suspend fun vaultNotification(packageName: String, title: String?, content: String?)
    fun observeVaultedNotifications(): Flow<List<VaultedNotificationEntity>>
    suspend fun releaseAllNotifications()
    suspend fun clearOldVault()
}
