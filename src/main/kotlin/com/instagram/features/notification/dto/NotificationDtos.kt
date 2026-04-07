package com.instagram.features.notification.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceTokenRequest(
    val fcmToken: String
)
