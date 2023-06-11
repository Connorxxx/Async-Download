package com.connor.asyncdownload

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.connor.asyncdownload.databinding.ActivityMainBinding
import com.connor.asyncdownload.databinding.ItemDownloadBinding
import com.connor.asyncdownload.model.data.*
import com.connor.asyncdownload.receiver.CancelReceiver
import com.connor.asyncdownload.type.Cancel
import com.connor.asyncdownload.type.DownloadType
import com.connor.asyncdownload.type.P
import com.connor.asyncdownload.type.UiState
import com.connor.asyncdownload.ui.adapter.DlAdapter
import com.connor.asyncdownload.utils.*
import com.connor.asyncdownload.viewmodls.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<MainViewModel>()

    @Inject
    lateinit var dlAdapter: DlAdapter

    private lateinit var builder: NotificationCompat.Builder

    private val requestNotify = registerForActivityResult(
        RequestPermission()
    ) { isGranted ->
        if (!isGranted) "todo".logCat()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        createNotificationChannel(CHANNEL_ID)
        initBuilder()
        if (TargetApi.T) requestNotify.launch(postNotify)
        initUI()
        initAdapter()
        initScope()
    }

    override fun onDestroy() {
        super.onDestroy()
        NotificationManagerCompat.from(this).cancelAll()
    }

    private fun initUI() {
        with(binding) {
            rvDl.apply {
                layoutManager = LinearLayoutManager(this@MainActivity)
                adapter = dlAdapter
                itemAnimator = null
            }
            fab.setOnLongClickListener {
                viewModel.addData(dlAdapter.currentList)
                true
            }
            fab.setOnClickListener {
                if (dlAdapter.currentList.none { it.state != State.Finished }) return@setOnClickListener
                dlAdapter.currentList.filterNot { it.state == State.Finished }.forEach { link ->
                    if (!viewModel.fabClick) {
                        if (link.state != State.Downloading)
                            viewModel.download(link, ::sendNotify, ::setFinished)
                    } else {
                        if (link.state == State.Downloading)
                            sendPause(link)
                    }
                }
                viewModel.setUi(UiState.FabClick(!viewModel.fabClick))
            }
        }
    }

    private fun initAdapter() {
        with(dlAdapter) {
            viewModel.loadDownData {
                dlAdapter.submitList(it)
            }
            finished {
                viewModel.setUi(UiState.FabClick(it))
            }
            setFileClicked { link ->
                when (link.state) {
                    State.Pause, State.Default, State.Canceled, State.Failed -> {
                        viewModel.download(link, ::sendNotify, ::setFinished)
                    }
                    State.Finished -> {
                        val uri = Uri.parse(link.uriString)
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, contentResolver.getType(uri))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            startActivity(this)
                        }
                    }
                    State.Downloading -> sendPause(link)
                }
            }
        }
    }

    private fun initScope() {
        lifecycleScope.launch {
            subscribe<Cancel> {
                setCancel(it.id)
            }
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect {
                        when (it) {
                            is UiState.FabClick -> {
                                viewModel.fabClick = it.boolean
                                if (!it.boolean) binding.fab.load(R.drawable.circle_down)
                                else binding.fab.load(R.drawable.pause_circle)
                            }
                            is UiState.Download -> {
                                getHolder(it.link.ktorDownload.url)?.also { holder ->
                                    holder.currentLink?.also { data ->
                                        setVH(it.type, data, holder.getBinding)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initBuilder() {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
    }

    private suspend fun setVH(
        it: DownloadType<DownloadData>,
        data: DownloadData,
        binding: ItemDownloadBinding
    ) {
        with(binding) {
            when (it) {
                is DownloadType.Waiting -> {
                    tvProgress.text = getString(R.string.wating)
                }
                is DownloadType.Started -> {
                    data.copy().apply {
                        state = State.Downloading
                        fileName = it.name
                        updateDowns()
                    }
                    viewModel.setUi(UiState.FabClick(true))
                }
                is DownloadType.Progress -> {
                    data.uiState.apply {
                        p = it.value.p
                        size = it.value.size + " / "
                        total = it.value.total
                        tvProgress.text = getString(R.string.progress_value, p)
                        tvSize.text =
                            getString(R.string.download_size, size, total)
                        val anima = progressBar.setAmin(p.toInt(), 450) //animator.v =
                        viewModel.animas.addID(data.id, Animator(data.id, anima))
                    }
                }
                is DownloadType.Speed -> {
                    binding.tvSpeed.text = it.value
                }
                is DownloadType.Failed -> {
                    data.copy().apply {
                        state = State.Failed
                        ktorDownload.downBytes = it.m.ktorDownload.downBytes
                        updateDowns()
                    }
                    it.throwable.localizedMessage?.showToast()
                }
                is DownloadType.Pause -> {
                    viewModel.jobs.find { it.id == data.id }?.job?.cancel()
                    NotificationManagerCompat.from(this@MainActivity).cancel(data.id)
                    data.copy().apply {
                        state = State.Pause
                        ktorDownload.downBytes = it.m.ktorDownload.downBytes
                        updateDowns()
                    }
                    setFabState(data)
                }
                is DownloadType.Canceled -> {
                    setFabState(data)
                }
                is DownloadType.Finished -> {
                    setFabState(data)
                }
            }
        }
    }

    private fun setFabState(data: DownloadData) {
        dlAdapter.currentList
            .filterNot { it.id == data.id }
            .none { it.state == State.Downloading }.also {
                if (it) viewModel.setUi(UiState.FabClick(false))
            }
    }

    private fun getHolder(url: String) =
        dlAdapter.currentList.let { list ->
            list.indexOf(list.find { it.ktorDownload.url == url })
                .takeIf { it >= 0 }?.let { binding.rvDl.getHolderFromPosition(it) }
        }

    private fun setFinished(
        data: DownloadData,
        file: File
    ) {
        @SuppressLint("NewApi")
        if (TargetApi.Q) data.uriString = file.copyToDownload(this@MainActivity)
        data.copy().apply {
            state = State.Finished
            uiState.apply {
                p = "100"
                size = ""
                total = file.length().formatSize()
            }
            if (data.uriString.isNotEmpty()) file.delete()
            updateDowns()
        }
        finishedNotify(data, 100, data.id)
    }

    private fun setCancel(id: Int) {
        dlAdapter.currentList.find { it.id == id }?.let { data ->
            viewModel.jobs.find { it.id == id }?.job?.cancel()
            viewModel.animas.find { it.id == id }?.anima?.cancel()
            data.copy().apply {
                state = State.Canceled
                ktorDownload.downBytes = 0
                uiState.apply { p = "0"; size = ""; total = "" }
                updateDowns()
            }
            viewModel.setUi(UiState.Download(data, DownloadType.Canceled))
        }
    }

    private fun sendPause(link: DownloadData) {
        "sendPause ${link.id}".logCat()
        viewModel.setUi(UiState.Download(link, DownloadType.Pause(link)))
    }

    @SuppressLint("NewApi", "MissingPermission")
    private fun sendNotify(data: DownloadData, p: P) {
        val cancelIntent = Intent(this, CancelReceiver::class.java).apply {
            putExtra(Notification.EXTRA_NOTIFICATION_ID, data.id)
        }
        val flag = if (TargetApi.S) PendingIntent.FLAG_MUTABLE else 0
        val cancel = PendingIntent.getBroadcast(this, data.id, cancelIntent, flag)
        builder.apply {
            clearActions()
            setProgress(100, p.p.toInt(), false)
            addAction(R.drawable.ic_cancel, "取消", cancel)
            setContentTitle(data.ktorDownload.url.getFileNameFromUrl())
        }
        NotificationManagerCompat.from(this).notify(data.id, builder.build())
    }

    @SuppressLint("MissingPermission")
    fun finishedNotify(data: DownloadData, progress: Int, id: Int) {
        builder.apply {
            clearActions()
            setProgress(100, progress, false)
            setContentTitle("Downloaded: ${data.ktorDownload.url.getFileNameFromUrl()}")
            setOngoing(false)
        }
        NotificationManagerCompat.from(this).notify(id, builder.build())
    }

    private fun DownloadData.updateDowns() {
        viewModel.updateDowns(this)
    }

    companion object {
        const val CHANNEL_ID = "download"
    }
}