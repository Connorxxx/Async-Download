package com.connor.asyncdownload

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.connor.asyncdownload.databinding.ActivityMainBinding
import com.connor.asyncdownload.model.data.Link
import com.connor.asyncdownload.model.data.State
import com.connor.asyncdownload.type.DownloadType
import com.connor.asyncdownload.ui.adapter.DlAdapter
import com.connor.asyncdownload.utils.logCat
import com.connor.asyncdownload.viewmodls.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
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
                viewModel.linkList.filterNot {
                    it.state == State.Finished
                }.forEach { link ->
                    if (link.isPause) fab.load(R.drawable.circle_down)
                    else fab.load(R.drawable.pause_circle)
                    if (!link.isPause) {
                        viewModel.download(link) {
                            dlAdapter.progressState.emit(it)
                        }
                    } else link.ktorDownload.job?.let { sendCancel(it, link) }
                }
            }
        }
        with(dlAdapter) {
            submitList(viewModel.linkList)
            setFileClicked { link ->
                link.isPause.logCat()
                if (!link.isPause) {
                    viewModel.download(link) { type -> progressState.emit(type) }
                } else link.ktorDownload.job?.let { job -> sendCancel(job, link) }
            }
        }
    }

    private fun sendCancel(
        job: Job,
        link: Link
    ): DisposableHandle {
        job.cancel()
        return job.invokeOnCompletion {
            lifecycleScope.launch {
                dlAdapter.progressState.emit(DownloadType.Canceled(link.ktorDownload))
            }.cancel()
        }
    }

    private fun createNotificationChannel() {

        val name = getString(R.string.channel_name)
        val descriptionText = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
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