package com.connor.asyncdownload.utils

import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.connor.asyncdownload.App
import com.connor.asyncdownload.BuildConfig

inline fun <reified T : ViewBinding> Fragment.viewBinding() =
    ViewBindingDelegate(T::class.java, this)

fun String.showToast() {
    Toast.makeText(App.app, this, Toast.LENGTH_SHORT).show()
}

fun Any.logCat(tab: String = "ASYNC_DOWNLOAD_LOG") {
    if (!BuildConfig.DEBUG) return
    if (this is String) Log.d(tab, this) else Log.d(tab, this.toString())
}

fun String.getFileNameFromUrl(): String? {
    val regex = ".*/(.+)".toRegex()
    val matchResult = regex.find(this)
    return matchResult?.groupValues?.get(1)
}