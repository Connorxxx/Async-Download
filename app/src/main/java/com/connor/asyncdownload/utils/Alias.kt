package com.connor.asyncdownload.utils

import android.Manifest
import android.app.Activity
import androidx.core.app.ActivityCompat

typealias PermissionChecker = (activity: Activity, permission: String) -> Boolean

const val postNotify = Manifest.permission.POST_NOTIFICATIONS
const val writeStorage = Manifest.permission.WRITE_EXTERNAL_STORAGE

val shouldShowPermission: PermissionChecker = { activity, permission ->
    ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
}
