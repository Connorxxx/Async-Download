package com.connor.asyncdownload.model.repo

import com.connor.asyncdownload.model.data.DownloadData
import com.connor.asyncdownload.model.room.DownDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepository @Inject constructor(
    private val downDao: DownDao
) {
    val loadDownData = downDao.loadDown().flowOn(Dispatchers.IO)

    suspend fun insertDown(data: DownloadData) = withContext(Dispatchers.IO) {
        downDao.insertDown(data)
    }

    suspend fun updateDowns(vararg data: DownloadData) = withContext(Dispatchers.IO) {
        downDao.updateDowns(*data)
    }

    suspend fun deleteDowns(vararg data: DownloadData) = withContext(Dispatchers.IO) {
        downDao.deleteDowns(*data)
    }
}