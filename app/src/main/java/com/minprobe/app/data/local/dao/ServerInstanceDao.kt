package com.minprobe.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.minprobe.app.data.local.entity.ServerInstanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerInstanceDao {
    @Query("SELECT * FROM server_instances ORDER BY createdAt ASC")
    suspend fun getAll(): List<ServerInstanceEntity>

    @Query("SELECT * FROM server_instances ORDER BY createdAt ASC")
    fun getAllFlow(): Flow<List<ServerInstanceEntity>>

    @Query("SELECT * FROM server_instances WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): ServerInstanceEntity?

    @Query("SELECT * FROM server_instances WHERE isActive = 1 LIMIT 1")
    fun getActiveFlow(): Flow<ServerInstanceEntity?>

    @Query("SELECT * FROM server_instances WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ServerInstanceEntity?

    @Insert
    suspend fun insert(entity: ServerInstanceEntity): Long

    @Update
    suspend fun update(entity: ServerInstanceEntity)

    @Delete
    suspend fun delete(entity: ServerInstanceEntity)

    @Query("UPDATE server_instances SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE server_instances SET isActive = 1 WHERE id = :id")
    suspend fun activateById(id: Long)

    @Query("UPDATE server_instances SET encryptedSessionCookie = :cookie WHERE id = :id")
    suspend fun updateSessionCookie(id: Long, cookie: String?)

    @Query("UPDATE server_instances SET siteName = :siteName, historyDays = :historyDays WHERE id = :id")
    suspend fun updateMeta(id: Long, siteName: String, historyDays: Int)
}
