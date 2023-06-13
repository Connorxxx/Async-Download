package com.connor.asyncdownload

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_MUTABLE
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
import com.connor.asyncdownload.model.data.*
import com.connor.asyncdownload.receiver.CancelReceiver
import com.connor.asyncdownload.type.*
import com.connor.asyncdownload.ui.adapter.DlAdapter
import com.connor.asyncdownload.utils.*
import com.connor.asyncdownload.viewmodls.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
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
                when (dlAdapter.currentList.none { it.url == url }) {
                    true -> {
                        viewModel.insertDown(DownloadData(url))
                        lifecycleScope.startDownByUrl(url)
                    }
                    false -> "Task already exists".showToast()
                }
            }
            btnDownAll.setOnClickListener {
                dlAdapter.currentList.filterNot { it.state == State.Finished }
                    .takeIf { it.isNotEmpty() }?.run {
                        forEach { data ->
                            when (viewModel.doAllClick) {
                                false -> data.takeIf { it.state != State.Downloading }
                                    ?.let { viewModel.setUi(UiEvent.StartDownload(it)) }
                                true -> data.takeIf { it.state == State.Downloading }
                                    ?.let { viewModel.setUi(UiEvent.Download(data, Pause(data))) }
                            }
                        }
                        viewModel.setUi(UiEvent.DoAllClick(!viewModel.doAllClick))
                    }
            }
        }
    }

    private fun CoroutineScope.startDownByUrl(url: String) {
        launch {
            delay(25)
            dlAdapter.currentList.find { it.url == url }?.let {
                viewModel.setUi(UiEvent.StartDownload(it))
            } ?: startDownByUrl(url)
        }
    }

    private fun initAdapter() {
        with(dlAdapter) {
            viewModel.loadDownData {
                dlAdapter.submitList(it)
            }
            setFileClicked { data ->
                when (data.state) {
                    State.Pause, State.Default, State.Canceled, State.Failed ->
                        viewModel.setUi(UiEvent.StartDownload(data))
                    State.Finished -> openUriByView(Uri.parse(data.uriString))
                    State.Downloading -> {
                        viewModel.setUi(UiEvent.Download(data, Pause(data)))
                        setFabState(data)
                    }
                }
            }
            deleteDownload {
                viewModel.deleteDowns(it)
            }
        }
    }

    private fun initScope() {
        lifecycleScope.launch {
            subscribe<Cancel> {
                setCancel(it.id)
            }
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                collectUiState()
            }
        }
    }

    private fun CoroutineScope.collectUiState() {
        launch {
            viewModel.uiState.collect {
                when (it) {
                    is UiEvent.DoAllClick -> {
                        viewModel.doAllClick = it.boolean
                        binding.btnDownAll.text = when (it.boolean) {
                            true -> getString(R.string.cancel_all)
                            false -> getString(R.string.download_all)
                        }
                    }
                    is UiEvent.Download -> getHolder(it.data.url)?.setHolderUI(it.type)
                    is UiEvent.StartDownload -> viewModel.download(it.data, ::sendNotify, ::setFinished)
                }
            }
        }
    }

    private fun initBuilder() {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            FLAG_IMMUTABLE
        )
        builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
    }

    private fun DlAdapter.ViewHolder.setHolderUI(it: DownloadType<DownloadData>) {
        val data = currentLink ?: return
        getBinding.apply {
            when (it) {
                is Waiting -> tvProgress.text = getString(R.string.wating)
                is Started -> {
                    data.copy().apply {
                        state = State.Downloading
                        fileName = it.name
                        updateDowns()
                    }
                    viewModel.setUi(UiEvent.DoAllClick(true))
                }
                is Progress -> {
                    data.uiState.apply {
                        p = it.value.p
                        size = it.value.size + " / "
                        total = it.value.total
                        tvProgress.text = getString(R.string.progress_value, p)
                        tvSize.text =
                            getString(R.string.download_size, size, total)
                        val anima = progressBar.setAmin(p.toInt(), 450)
                        viewModel.animas.addID(data.id, Animator(data.id, anima))
                    }
                }
                is Speed -> tvSpeed.text = it.value
                is Failed -> {
                    data.copy().apply {
                        state = State.Failed
                        downBytes = it.m.downBytes
                        updateDowns()
                    }
                    it.throwable.localizedMessage?.showToast()
                }
                is Pause -> {
                    viewModel.jobs.find { it.id == data.id }?.job?.cancel()
                    NotificationManagerCompat.from(this@MainActivity).cancel(data.id)
                    data.copy().apply {
                        state = State.Pause
                        downBytes = it.m.downBytes
                        updateDowns()
                    }
                }
                is Canceled -> setFabState(data)
                is Finished -> setFabState(data)
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
        viewModel.jobs.find { it.id == id }?.job?.cancel()
        viewModel.animas.find { it.id == id }?.anima?.cancel()
        dlAdapter.currentList.find { it.id == id }?.let { data ->
            data.copy().apply {
                state = State.Canceled
                downBytes = 0
                uiState.apply { p = "0"; size = ""; total = "" }
                updateDowns()
            }
            viewModel.setUi(UiEvent.Download(data, Canceled))
        }
    }

    @SuppressLint("NewApi", "MissingPermission")
    private fun sendNotify(data: DownloadData, p: P) {
        val cancelIntent = Intent(this, CancelReceiver::class.java).apply {
            putExtra(Notification.EXTRA_NOTIFICATION_ID, data.id)
        }
        val flag = if (TargetApi.S) FLAG_MUTABLE else FLAG_IMMUTABLE
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