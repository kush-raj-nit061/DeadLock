package com.ingray.deadlock.data.repository

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.ingray.deadlock.data.local.dao.VaultedNotificationDao
import com.ingray.deadlock.data.local.entity.VaultedNotificationEntity
import com.ingray.deadlock.domain.repository.NotificationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultedNotificationDao: VaultedNotificationDao
) : NotificationRepository {

    override suspend fun vaultNotification(packageName: String, title: String?, content: String?) {
        vaultedNotificationDao.insert(
            VaultedNotificationEntity(
                packageName = packageName,
                title = title,
                content = content
            )
        )
    }

    override fun observeVaultedNotifications(): Flow<List<VaultedNotificationEntity>> =
        vaultedNotificationDao.observePendingNotifications()

    override suspend fun releaseAllNotifications() {
        val pending = vaultedNotificationDao.getPendingNotifications()
        if (pending.isEmpty()) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        pending.forEach { vault ->
            val appName = try {
                val pm = context.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(vault.packageName, 0)).toString()
            } catch (e: Exception) {
                vault.packageName.substringAfterLast('.')
            }

            val notification = NotificationCompat.Builder(context, "deadlock_enforcement")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Missed: $appName")
                .setContentText(vault.title?.let { "$it: ${vault.content}" } ?: vault.content)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            
            manager.notify(vault.id.toInt(), notification)
        }

        vaultedNotificationDao.markAllAsReleased()
    }

    override suspend fun clearOldVault() {
        vaultedNotificationDao.clearReleased()
    }
}
