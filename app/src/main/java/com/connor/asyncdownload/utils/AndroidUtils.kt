package com.connor.asyncdownload.utils

import android.animation.ObjectAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.webkit.MimeTypeMap
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.connor.asyncdownload.App
import com.connor.asyncdownload.BuildConfig
import com.connor.asyncdownload.R
import com.connor.asyncdownload.ui.adapter.DlAdapter
import kotlinx.coroutines.*
import java.io.File

inline fun <reified T : ViewBinding> Fragment.viewBinding() =
    ViewBindingDelegate(T::class.java, this)

fun Any.logCat(tab: String = "ASYNC_DOWNLOAD_LOG") {
    if (!BuildConfig.DEBUG) return
    if (this is String) Log.d(tab, this) else Log.d(tab, this.toString())
}

fun String.showToast() {
    Toast.makeText(App.app, this, Toast.LENGTH_SHORT).show()
}

fun AppCompatActivity.createNotificationChannel(channelID: String) {
    val name = getString(R.string.channel_name)
    val descriptionText = getString(R.string.channel_description)
    val importance = NotificationManager.IMPORTANCE_DEFAULT
    val channel = NotificationChannel(channelID, name, importance).apply {
        setSound(null, null)
        enableVibration(false)
        description = descriptionText
    }
    val notificationManager: NotificationManager =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)
}

fun ProgressBar.setAmin(value: Int, d: Long): ObjectAnimator =
    ObjectAnimator.ofInt(this, "progress", this.progress, value).apply {
        duration = d
        interpolator = LinearInterpolator()
        start()
    }


fun View.debounceClick(time: Long = 500L, listen: (View) -> Unit) {
    var job: Job? = null
    this.setOnClickListener {
        job?.cancel()
        job = CoroutineScope(Dispatchers.Main).launch {
            delay(time)
            listen(it)
        }
    }
}

fun RecyclerView.getHolderFromPosition(position: Int) =
    findViewHolderForAdapterPosition(position) as? DlAdapter.ViewHolder


@RequiresApi(Build.VERSION_CODES.Q)
fun File.copyToDownload(ctx: Context): String {
    var uriString = ""
    val v = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, getMimeType())
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }
    this.inputStream().buffered().use { bis ->
        ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v)?.also { uri ->
            uriString = uri.toString()
            ctx.contentResolver.openOutputStream(uri).use {
                it?.buffered()?.let { bos -> bis.copyTo(bos) }
            }
        }
    }
    return uriString
}

private fun File.getMimeType() = MimeTypeMap.getSingleton()
    .getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(this.name))
