package com.connor.asyncdownload.model.room

import androidx.room.*
import com.connor.asyncdownload.model.data.DownloadData
import kotlinx.coroutines.flow.Flow

@Dao
interface DownDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDown(data: DownloadData): Long

    @Update
    suspend fun updateDowns(vararg users: DownloadData)

    @Query("select * from down_data")
    fun loadDown(): Flow<List<DownloadData>>
}