package com.minprobe.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.minprobe.app.data.local.dao.ServerInstanceDao
import com.minprobe.app.data.local.entity.ServerInstanceEntity

@Database(entities = [ServerInstanceEntity::class], version = 1, exportSchema = false)
abstract class MiniProbeDatabase : RoomDatabase() {
    abstract fun serverInstanceDao(): ServerInstanceDao
}
