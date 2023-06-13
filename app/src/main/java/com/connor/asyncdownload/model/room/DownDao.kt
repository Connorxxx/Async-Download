package com.connor.asyncdownload.model.room

import androidx.room.*
import com.connor.asyncdownload.model.data.DownloadData
import kotlinx.coroutines.flow.Flow

@Dao
interface DownDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDown(data: DownloadData): Long

    @Delete
    suspend fun deleteDowns(vararg data: DownloadData): Int

    @Update
    suspend fun updateDowns(vararg data: DownloadData): Int

    @Query("select * from down_data")
    fun loadDown(): Flow<List<DownloadData>>
}