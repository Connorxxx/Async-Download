package com.connor.asyncdownload.receiver

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.connor.asyncdownload.type.Id
import com.connor.asyncdownload.utils.logCat
import com.connor.asyncdownload.utils.post
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CancelReceiver : BroadcastReceiver() {
    override fun onReceive(coBroadcastReceiverntext: Context, intent: Intent) {
        val i = intent.getIntExtra(Notification.EXTRA_NOTIFICATION_ID, -1)
        i.logCat()
        CoroutineScope(Dispatchers.Main.immediate).launch {
            post(Id(i))
            cancel()
        }
    }
}