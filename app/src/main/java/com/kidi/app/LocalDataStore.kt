package com.kidi.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object LocalDataStore {
    private const val PREFS_NAME = "kidi_prefs"
    
    fun setLockEndTime(ctx: Context, endTimeMs: Long) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong("lock_end_ms", endTimeMs).apply()
    }
    
    fun getLockEndTime(ctx: Context): Long =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong("lock_end_ms", 0L)
    
    fun isLocked(ctx: Context): Boolean {
        val end = getLockEndTime(ctx)
        return end > System.currentTimeMillis()
    }
    
    fun unlock(ctx: Context) {
        setLockEndTime(ctx, 0L)
    }
    
    fun lockForMinutes(ctx: Context, minutes: Int) {
        val clamped = minutes.coerceIn(5, 1440)
        val endTime = System.currentTimeMillis() + clamped * 60 * 1000L
        setLockEndTime(ctx, endTime)
    }
    
    fun getRemainingMinutes(ctx: Context): Int {
        val end = getLockEndTime(ctx)
        if (end == 0L) return 0
        val msLeft = end - System.currentTimeMillis()
        return (msLeft / 60000).toInt().coerceAtLeast(0)
    }
    
    fun setSchoolSchedule(ctx: Context, schedule: SchoolSchedule) {
        val json = JSONObject().apply {
            put("enabled", schedule.enabled)
            put("lockScreen", schedule.lockScreen)
            put("dndMode", schedule.dndMode)
            put("schoolStartHour", schedule.schoolStartHour)
            put("schoolEndHour", schedule.schoolEndHour)
            put("afternoonStartHour", schedule.afternoonStartHour)
            put("afternoonEndHour", schedule.afternoonEndHour)
        }.toString()
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString("school_schedule", json).apply()
    }
    
    fun getSchoolSchedule(ctx: Context): SchoolSchedule {
        val jsonStr = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("school_schedule", null) ?: return SchoolSchedule()
        return try {
            val json = JSONObject(jsonStr)
            SchoolSchedule(
                enabled = json.optBoolean("enabled", false),
                lockScreen = json.optBoolean("lockScreen", true),
                dndMode = json.optBoolean("dndMode", true),
                schoolStartHour = json.optInt("schoolStartHour", 8),
                schoolEndHour = json.optInt("schoolEndHour", 12),
                afternoonStartHour = json.optInt("afternoonStartHour", 14),
                afternoonEndHour = json.optInt("afternoonEndHour", 17)
            )
        } catch (e: Exception) {
            SchoolSchedule()
        }
    }
    
    fun isSchoolTime(ctx: Context): Boolean {
        val schedule = getSchoolSchedule(ctx)
        if (!schedule.enabled) return false
        
        val calendar = java.util.Calendar.getInstance()
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        
        if (dayOfWeek < java.util.Calendar.MONDAY || dayOfWeek > java.util.Calendar.FRIDAY) {
            return false
        }
        
        val morning = hour >= schedule.schoolStartHour && hour < schedule.schoolEndHour
        val afternoon = hour >= schedule.afternoonStartHour && hour < schedule.afternoonEndHour
        return morning || afternoon
    }
    
    fun saveTimeRequest(ctx: Context, request: TimeRequest) {
        val requests = getTimeRequests(ctx).toMutableList()
        requests.add(request)
        saveTimeRequests(ctx, requests)
    }
    
    fun updateRequestStatus(ctx: Context, requestId: String, status: String) {
        val requests = getTimeRequests(ctx).map {
            if (it.id == requestId) it.copy(status = status) else it
        }
        saveTimeRequests(ctx, requests)
    }
    
    private fun saveTimeRequests(ctx: Context, requests: List<TimeRequest>) {
        val jsonArr = JSONArray()
        requests.forEach { req ->
            jsonArr.put(JSONObject().apply {
                put("id", req.id)
                put("childName", req.childName)
                put("message", req.message)
                put("durationMin", req.durationMin)
                put("time", req.time)
                put("status", req.status)
            })
        }
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString("time_requests", jsonArr.toString()).apply()
    }
    
    fun getTimeRequests(ctx: Context): List<TimeRequest> {
        val jsonStr = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("time_requests", null) ?: return emptyList()
        return try {
            val jsonArr = JSONArray(jsonStr)
            List(jsonArr.length()) { i ->
                val json = jsonArr.getJSONObject(i)
                TimeRequest(
                    id = json.optString("id"),
                    childName = json.optString("childName"),
                    message = json.optString("message"),
                    durationMin = json.optInt("durationMin"),
                    time = json.optString("time"),
                    status = json.optString("status", "pending")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun setAppMode(ctx: Context, mode: AppMode) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString("app_mode", mode.name).apply()
    }
    
    fun getAppMode(ctx: Context): AppMode {
        val name = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("app_mode", AppMode.PARENT.name)
        return AppMode.valueOf(name!!)
    }
    
    fun saveChild(ctx: Context, child: ChildDevice) {
        val children = getChildren(ctx).toMutableList()
        val idx = children.indexOfFirst { it.id == child.id }
        if (idx >= 0) children[idx] = child else children.add(child)
        saveChildren(ctx, children)
    }
    
    private fun saveChildren(ctx: Context, children: List<ChildDevice>) {
        val jsonArr = JSONArray()
        children.forEach { c ->
            jsonArr.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("avatar", c.avatar)
                put("isOnline", c.isOnline)
                put("remainingTimeMin", c.remainingTimeMin)
                put("totalTimeDayMin", c.totalTimeDayMin)
            })
        }
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString("children", jsonArr.toString()).apply()
    }
    
    fun getChildren(ctx: Context): List<ChildDevice> {
        val jsonStr = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("children", null)
        if (jsonStr == null) {
            return listOf(
                ChildDevice("1", "Léo", "L", true, 210, 300),
                ChildDevice("2", "Emma", "E", true, 180, 240)
            )
        }
        return try {
            val jsonArr = JSONArray(jsonStr)
            List(jsonArr.length()) { i ->
                val json = jsonArr.getJSONObject(i)
                ChildDevice(
                    id = json.optString("id"),
                    name = json.optString("name"),
                    avatar = json.optString("avatar"),
                    isOnline = json.optBoolean("isOnline", true),
                    remainingTimeMin = json.optInt("remainingTimeMin"),
                    totalTimeDayMin = json.optInt("totalTimeDayMin")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
