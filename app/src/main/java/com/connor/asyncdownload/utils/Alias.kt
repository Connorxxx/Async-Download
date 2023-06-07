package com.connor.asyncdownload.utils

import android.Manifest
import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.app.ActivityCompat

typealias PermissionChecker = (activity: Activity, permission: String) -> Boolean

const val postNotify = Manifest.permission.POST_NOTIFICATIONS

val shouldShowPermission: PermissionChecker = { activity, permission ->
    ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
}
