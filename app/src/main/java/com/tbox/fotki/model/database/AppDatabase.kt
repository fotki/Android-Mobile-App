package com.tbox.fotki.model.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pro.produktydb.HistoryDao

@Database(entities = [FilesEntity::class], version = 5)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}