package com.connor.asyncdownload.receiver

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.connor.asyncdownload.ui.adapter.DlAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CancelReceiver : BroadcastReceiver() {

    @Inject lateinit var dlAdapter: DlAdapter
    override fun onReceive(coBroadcastReceiverntext: Context, intent: Intent) {
        val i = intent.getIntExtra(Notification.EXTRA_NOTIFICATION_ID, -1)
        dlAdapter.apply {
            scope.launch {
                sendCancel(i)
            }
        }
        NotificationManagerCompat.from(coBroadcastReceiverntext).cancel(i)
    }
}