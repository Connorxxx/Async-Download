package com.connor.asyncdownload

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.connor.asyncdownload.databinding.ActivityMainBinding
import com.connor.asyncdownload.model.data.DownloadData
import com.connor.asyncdownload.model.data.State
import com.connor.asyncdownload.type.DownloadType
import com.connor.asyncdownload.type.Id
import com.connor.asyncdownload.ui.adapter.DlAdapter
import com.connor.asyncdownload.utils.*
import com.connor.asyncdownload.viewmodls.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<MainViewModel>()

    @Inject
    lateinit var dlAdapter: DlAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        createNotificationChannel()
        with(binding) {
            rvDl.layoutManager = LinearLayoutManager(this@MainActivity)
            rvDl.adapter = dlAdapter
            fab.setOnClickListener {
                dlAdapter.currentList.filterNot { it.state == State.Finished }.forEach { link ->
                    if (!viewModel.fabState) {
                        if (link.state != State.Downloading)
                            viewModel.download(link) { type -> dlAdapter.progressState.emit(type) }
                    } else {
                        if (link.state == State.Downloading)
                            link.ktorDownload.job?.let { job -> sendPause(job, link) }
                    }
                }
                viewModel.fabState = !viewModel.fabState
                if (!viewModel.fabState) fab.load(R.drawable.circle_down)
                else fab.load(R.drawable.pause_circle)
            }
        }
        with(dlAdapter) {
            setFileClicked { link ->
                when (link.state) {
                    State.Pause, State.Default, State.Canceled, State.Failed -> {
                        viewModel.download(link) { type -> progressState.emit(type) }
                    }
                    State.Finished -> "finished".showToast()
                    State.Downloading -> link.ktorDownload.job?.let { job -> sendPause(job, link) }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.loadDownData.collect {
                        dlAdapter.submitList(it.sortedBy { d -> d.ktorDownload.url.getFileNameFromUrl() })
                    }
                }
            }
        }
    }

    //requestNotify.launch(postNotify)
    private val requestNotify = registerForActivityResult(
        RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            "todo".logCat()
        }
    }

    private fun sendPause(job: Job, link: DownloadData) {
        job.cancel()
        lifecycleScope.launch {
            dlAdapter.progressState.emit(DownloadType.Pause(link.ktorDownload))
        }
    }

    private fun createNotificationChannel() {
        val name = getString(R.string.channel_name)
        val descriptionText = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            setSound(null, null)
            enableVibration(false)
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "download"
    }
}