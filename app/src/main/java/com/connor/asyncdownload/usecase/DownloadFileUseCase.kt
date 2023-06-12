package com.connor.asyncdownload.usecase

import com.connor.asyncdownload.model.data.KtorDownload
import com.connor.asyncdownload.model.repo.HttpRepository
import com.connor.asyncdownload.type.DownloadType
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class DownloadFileUseCase @Inject constructor(private val httpRepository: HttpRepository) {

    suspend operator  fun <T: KtorDownload> invoke(
        download: T,
        block: suspend (DownloadType<T>) -> Unit
    ) = httpRepository.downloadFile(download).collect(block)
}