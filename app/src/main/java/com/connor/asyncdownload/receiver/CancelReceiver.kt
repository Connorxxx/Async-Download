package com.connor.asyncdownload.receiver

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.connor.asyncdownload.type.Cancel
import com.connor.asyncdownload.utils.post
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CancelReceiver : BroadcastReceiver() {
    override fun onReceive(coBroadcastReceiverntext: Context, intent: Intent) {
        val i = intent.getIntExtra(Notification.EXTRA_NOTIFICATION_ID, -1)
        CoroutineScope(Dispatchers.Default).launch {
            post(Cancel(i)).join()
            cancel()
        }
        NotificationManagerCompat.from(coBroadcastReceiverntext).cancel(i)
    }
}