package com.kidi.app

data class ChildDevice(
    val id: String,
    val name: String,
    val avatar: String,
    val isOnline: Boolean = true,
    var remainingTimeMin: Int = 0,
    val totalTimeDayMin: Int = 0
)

data class TimeRequest(
    val id: String = System.currentTimeMillis().toString(),
    val childName: String,
    val message: String,
    val durationMin: Int,
    val time: String,
    var status: String = "pending"
)

data class QuickMessage(
    val id: String,
    val text: String
)

data class SchoolSchedule(
    val enabled: Boolean = false,
    val lockScreen: Boolean = true,
    val dndMode: Boolean = true,
    val schoolStartHour: Int = 8,
    val schoolEndHour: Int = 12,
    val afternoonStartHour: Int = 14,
    val afternoonEndHour: Int = 17
)

enum class AppMode {
    PARENT, CHILD
}
