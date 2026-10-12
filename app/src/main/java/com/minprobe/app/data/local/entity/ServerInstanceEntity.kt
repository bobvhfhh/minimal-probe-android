package com.minprobe.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 面板实例（密码 / session 均为加密后存储） */
@Entity(tableName = "server_instances")
data class ServerInstanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val baseUrl: String,
    val encryptedPassword: String,
    val encryptedSessionCookie: String?,
    val authType: String,
    val isActive: Boolean,
    val createdAt: Long,
    val siteName: String,
    val historyDays: Int
)
