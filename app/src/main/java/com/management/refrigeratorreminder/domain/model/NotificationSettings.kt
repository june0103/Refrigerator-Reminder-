package com.management.refrigeratorreminder.domain.model

data class NotificationSettings(
    val enabled: Boolean = false,
    val leadDays: Int = 3,
    val hour: Int = 9,
    val minute: Int = 0,
)
