package com.connor.asyncdownload.usecase

import com.connor.asyncdownload.model.data.DownloadData
import com.connor.asyncdownload.model.repo.HttpRepository
import com.connor.asyncdownload.type.DownloadType
import javax.inject.Inject

class DownloadFileUseCase @Inject constructor(private val httpRepository: HttpRepository) {

    suspend operator fun invoke(
        download: DownloadData,
        block: suspend (DownloadType<DownloadData>) -> Unit
    ) = httpRepository.downloadFile(download).collect(block)
}