package com.connor.asyncdownload.utils

import android.Manifest
import com.connor.asyncdownload.type.DownloadType

const val postNotify = Manifest.permission.POST_NOTIFICATIONS
const val writeStorage = Manifest.permission.WRITE_EXTERNAL_STORAGE

typealias Started<T> = DownloadType.Started<T>
typealias Pause<T> = DownloadType.Pause<T>
typealias Waiting<T> = DownloadType.Waiting<T>
typealias Progress<T> = DownloadType.Progress<T>
typealias Speed<T> = DownloadType.Speed<T>
typealias Finished<T> = DownloadType.Finished<T>
typealias Failed<T> = DownloadType.Failed<T>
typealias Canceled = DownloadType.Canceled

