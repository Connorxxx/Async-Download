package com.connor.asyncdownload.model.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.connor.asyncdownload.model.data.DownloadData

@Database(version = 1, entities = [DownloadData::class])
abstract class DownDataBase : RoomDatabase() {
    abstract fun downDao(): DownDao
}