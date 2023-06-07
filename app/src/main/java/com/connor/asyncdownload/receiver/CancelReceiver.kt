package com.connor.asyncdownload.receiver

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.connor.asyncdownload.utils.logCat
import com.connor.asyncdownload.utils.showToast

class CancelReceiver : BroadcastReceiver() {
    override fun onReceive(coBroadcastReceiverntext: Context, intent: Intent) {
        val i = intent.getIntExtra(Notification.EXTRA_NOTIFICATION_ID, -1)
        "i: $i".logCat()
        "i: $i".showToast()
//        CoroutineScope(Dispatchers.Main).launch {
//            post(Id(i))
//        }
    }
}