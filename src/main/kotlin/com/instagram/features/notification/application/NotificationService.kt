package com.instagram.features.notification.application

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.instagram.features.notification.data.DeviceTokenRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.UUID

class NotificationService(
    private val deviceTokenRepository: DeviceTokenRepository,
    private val firebaseEnabled: Boolean
) {
    private val logger = LoggerFactory.getLogger(NotificationService::class.java)
    private val scope = CoroutineScope(Dispatchers.IO)

    suspend fun registerDeviceToken(userId: UUID, token: String) {
        deviceTokenRepository.registerToken(userId, token)
        logger.info("Registered FCM token for user \$userId")
    }

    fun sendPushNotification(userId: UUID, title: String, body: String, data: Map<String, String> = emptyMap()) {
        if (!firebaseEnabled) {
            logger.info("Mock push notification to \$userId: \$title - \$body")
            return
        }

        scope.launch {
            try {
                val tokens = deviceTokenRepository.getTokensForUser(userId)
                
                tokens.forEach { token ->
                    val notification = Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build()

                    val message = Message.builder()
                        .setToken(token)
                        .setNotification(notification)
                        .putAllData(data)
                        .build()

                    try {
                        FirebaseMessaging.getInstance().send(message)
                    } catch (e: Exception) {
                        logger.error("Failed to send push notification to token \$token", e)
                        // If token is invalid or not registered anymore, we should remove it
                        deviceTokenRepository.removeToken(token)
                    }
                }
            } catch (e: Exception) {
                logger.error("Error sending push notification to user \$userId", e)
            }
        }
    }
}
