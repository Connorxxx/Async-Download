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
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.connor.asyncdownload.databinding.ActivityMainBinding
import com.connor.asyncdownload.databinding.ItemDownloadBinding
import com.connor.asyncdownload.model.data.*
import com.connor.asyncdownload.receiver.CancelReceiver
import com.connor.asyncdownload.type.Cancel
import com.connor.asyncdownload.type.DownloadType
import com.connor.asyncdownload.type.P
import com.connor.asyncdownload.type.UiEvent
import com.connor.asyncdownload.ui.adapter.DlAdapter
import com.connor.asyncdownload.utils.*
import com.connor.asyncdownload.viewmodls.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
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

    private val requestNotify = registerForActivityResult(RequestPermission()) {
        if (!it) "todo".logCat()
    }

    private val requestWriteStorage = registerForActivityResult(RequestPermission()) {
        if (!it) "No Permission".showToast()
    }

    private var i = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
//        supportActionBar?.apply {
//            setDisplayHomeAsUpEnabled(true)
//            setHomeButtonEnabled(true)
//        }
        createNotificationChannel(CHANNEL_ID)
        initBuilder()
        if (TargetApi.T) requestNotify.launch(postNotify)
        if (!TargetApi.Q) requestWriteStorage.launch(writeStorage)
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
                addOnScrollListener(object : OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        if (dy > 0) binding.fab.hide() else binding.fab.show()
                    }
                })
            }
            fab.setOnLongClickListener {
                viewModel.addData(dlAdapter.currentList)
                true
            }
            fab.setOnClickListener {
                val url = "http://192.168.3.193:8080/$i.apk" //TODO
                i++
                if (!dlAdapter.currentList.none { it.url == url }) {
                    "Task already exists".showToast()
                    return@setOnClickListener
                }
                viewModel.insertDown(DownloadData(url))
                lifecycleScope.launch { startDownByUrl(url) }

            }
            btnDownAll.setOnClickListener {
                if (dlAdapter.currentList.none { it.state != State.Finished }) return@setOnClickListener
                dlAdapter.currentList.filterNot { it.state == State.Finished }.forEach { data ->
                    if (!viewModel.doAllClick) {
                        if (data.state != State.Downloading)
                            viewModel.setUi(UiEvent.StartDownload(data))
                    } else {
                        if (data.state == State.Downloading)
                            sendPause(data)
                    }
                }
                viewModel.setUi(UiEvent.DoAllClick(!viewModel.doAllClick))
            }
        }
    }

    private suspend fun startDownByUrl(url: String) {
        delay(25)
        dlAdapter.currentList.find { it.url == url }?.let {
            viewModel.setUi(UiEvent.StartDownload(it))
        } ?: startDownByUrl(url)
    }

    private fun initAdapter() {
        with(dlAdapter) {
            viewModel.loadDownData {
                dlAdapter.submitList(it)
            }
            setFileClicked { data ->
                when (data.state) {
                    State.Pause, State.Default, State.Canceled, State.Failed -> {
                        viewModel.setUi(UiEvent.StartDownload(data))
                    }
                    State.Finished -> {
                        val uri = Uri.parse(data.uriString)
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, contentResolver.getType(uri))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            startActivity(this)
                        }
                    }
                    State.Downloading -> {
                        sendPause(data)
                        setFabState(data)
                    }
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
                            is UiEvent.DoAllClick -> {
                                viewModel.doAllClick = it.boolean
                                if (!it.boolean) binding.btnDownAll.text =
                                    getString(R.string.download_all)
                                else binding.btnDownAll.text = getString(R.string.cancel_all)
                            }
                            is UiEvent.Download -> {
                                getHolder(it.data.url)?.also { holder ->
                                    holder.currentLink?.also { data ->
                                        setVH(it.type, data, holder.getBinding)
                                    }
                                }
                            }
                            is UiEvent.StartDownload -> {
                                viewModel.download(it.data, ::sendNotify, ::setFinished)
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

    private fun setVH(
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
                    "Start: ${data.url}".logCat()
                    data.copy().apply {
                        state = State.Downloading
                        fileName = it.name
                        updateDowns()
                    }
                    viewModel.setUi(UiEvent.DoAllClick(true))
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
                        downBytes = it.m.downBytes
                        updateDowns()
                    }
                    it.throwable.localizedMessage?.showToast()
                }
                is DownloadType.Pause -> {
                    viewModel.jobs.find { it.id == data.id }?.job?.cancel()
                    NotificationManagerCompat.from(this@MainActivity).cancel(data.id)
                    data.copy().apply {
                        state = State.Pause
                        downBytes = it.m.downBytes
                        updateDowns()
                    }
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
                if (it) viewModel.setUi(UiEvent.DoAllClick(false))
            }
    }

    private fun getHolder(url: String) =
        dlAdapter.currentList.let { list ->
            list.indexOf(list.find { it.url == url })
                .takeIf { it >= 0 }?.let { binding.rvDl.getHolderFromPosition(it) }
        }

    private fun setFinished(
        data: DownloadData,
        file: File
    ) {
        data.uriString = file.copyToDownload(this@MainActivity)
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
                downBytes = 0
                uiState.apply { p = "0"; size = ""; total = "" }
                updateDowns()
            }
            viewModel.setUi(UiEvent.Download(data, DownloadType.Canceled))
        }
    }

    private fun sendPause(link: DownloadData) {
        viewModel.setUi(UiEvent.Download(link, DownloadType.Pause(link)))
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
            setContentTitle(data.url.getFileNameFromUrl())
        }
        NotificationManagerCompat.from(this).notify(data.id, builder.build())
    }

    @SuppressLint("MissingPermission")
    fun finishedNotify(data: DownloadData, progress: Int, id: Int) {
        builder.apply {
            clearActions()
            setProgress(100, progress, false)
            setContentTitle("Downloaded: ${data.url.getFileNameFromUrl()}")
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