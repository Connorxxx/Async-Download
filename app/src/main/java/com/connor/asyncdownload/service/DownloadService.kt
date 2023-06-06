package com.connor.asyncdownload.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.connor.asyncdownload.MainActivity
import com.connor.asyncdownload.R
import com.connor.asyncdownload.model.Repository
import com.connor.asyncdownload.model.data.Link
import com.connor.asyncdownload.type.DownloadType
import com.connor.asyncdownload.utils.logCat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService @Inject constructor() : LifecycleService() {

    @Inject lateinit var repository: Repository

    var job: Job? = null
        private set

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            "package_server", "Package Service",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, "package_server")
            .setContentTitle("Running...")
            .setContentText("please waiting until finish")
            .setSmallIcon(R.drawable.baseline_ac_unit_24)
            .setContentIntent(pi)
            .build()
        startForeground(1, notification)
    }

    fun download(link: Link, block: suspend (DownloadType<Link>) -> Unit) {
        job = lifecycleScope.launch {
            link.url.logCat()
            repository.downloadFile(link).collect { block(it) }
        }
    }

}