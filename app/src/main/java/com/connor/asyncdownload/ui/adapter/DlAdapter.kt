package com.connor.asyncdownload.ui.adapter

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Notification.EXTRA_NOTIFICATION_ID
import android.app.PendingIntent.*
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.connor.asyncdownload.MainActivity
import com.connor.asyncdownload.R
import com.connor.asyncdownload.databinding.ItemDownloadBinding
import com.connor.asyncdownload.model.Repository
import com.connor.asyncdownload.model.data.*
import com.connor.asyncdownload.receiver.CancelReceiver
import com.connor.asyncdownload.type.DownloadType
import com.connor.asyncdownload.utils.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DlAdapter @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val repository: Repository
) : ListAdapter<DownloadData, DlAdapter.ViewHolder>(DlDiffCallback) {

    object DlDiffCallback : DiffUtil.ItemCallback<DownloadData>() {
        override fun areItemsTheSame(oldItem: DownloadData, newItem: DownloadData) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: DownloadData, newItem: DownloadData) =
            true//oldItem == newItem
    }

    //private val job = Job()
    val scope = CoroutineScope(Dispatchers.Main)
    val progressState = MutableSharedFlow<DownloadType<KtorDownload>>()

    private val intent = Intent(ctx, MainActivity::class.java)
    private val pendingIntent = getActivity(ctx, 0, intent, FLAG_IMMUTABLE)

    private val builder = NotificationCompat.Builder(ctx, MainActivity.CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_download)
        .setContentIntent(pendingIntent)
        .setOnlyAlertOnce(true)
        .setOngoing(true)


    private var fileListen: ((DownloadData) -> Unit)? = null
    private var finishedListen: ((Boolean) -> Unit)? = null
    fun setFileClicked(datas: (DownloadData) -> Unit) {
        fileListen = datas
    }

    fun finished(block: (Boolean) -> Unit) {
        finishedListen = block
    }

    inner class ViewHolder(private val binding: ItemDownloadBinding) :
        RecyclerView.ViewHolder(binding.root) {

        var currentLink: DownloadData? = null
        private var animator: ObjectAnimator? = null

        init {
            with(binding) {
                imgDl.setOnClickListener {
                    currentLink?.let { data ->
                        fileListen?.let { it(data) }
                    }
                }
                scope.launch {
                    progressState.collect {
                        currentLink?.let { data ->
                            when (it) {
                                is DownloadType.Waiting -> {
                                    if (!isSame(it.m, data)) return@collect
                                    tvProgress.text = ctx.getString(R.string.wating)
                                }
                                is DownloadType.Started -> {
                                    if (!isSame(it.m, data)) return@collect
                                    data.state = State.Downloading
                                    data.fileName = it.name
                                    imgDl.load(R.drawable.pause_circle)
                                }
                                is DownloadType.Progress -> {
                                    if (!isSame(it.m, data)) return@collect
                                    data.uiState.apply {
                                        p = it.value.p
                                        size = it.value.size + " / "
                                        total = it.value.total
                                        tvProgress.text = ctx.getString(R.string.progress_value, p)
                                        tvSize.text =
                                            ctx.getString(R.string.download_size, size, total)
                                        sendNotify(data, p.toInt())
                                        animator = progressBar.setAmin(p.toInt(), 450)
                                    }
                                }
                                is DownloadType.Speed -> {
                                    if (!isSame(it.m, data)) return@collect
                                    binding.tvSpeed.text = it.value
                                }
                                is DownloadType.Failed -> {
                                    if (!isSame(it.m, data)) return@collect
                                    data.state = State.Failed
                                    data.ktorDownload.downBytes = it.m.downBytes
                                    repository.updateDowns(data)
                                    updateUI(data)
                                    it.throwable.localizedMessage?.showToast()
                                }
                                is DownloadType.Pause -> {
                                    if (!isSame(it.m, data)) return@collect
                                    currentList.find { it.id == data.id }.also {
                                        currentList.indexOf(it).logCat()
                                    }
                                    data.state = State.Pause
                                    data.ktorDownload.job?.cancel()
                                    data.ktorDownload.downBytes = it.m.downBytes
                                    repository.updateDowns(data)
                                    updateUI(data)
                                }
                                is DownloadType.Canceled -> {
                                    if (!isSame(it.m, data)) return@collect
                                    data.state = State.Canceled
                                    data.ktorDownload.downBytes = 0
                                    data.uiState.apply { p = "0"; size = ""; total = "" }
                                    animator?.cancel()
                                    repository.updateDowns(data)
                                    updateUI(data)
                                    tvSpeed.text = ""
//                                    tvSize.text = ""
//                                    progressBar.progress = 0
                                }
                                is DownloadType.Finished -> {
                                    if (!isSame(it.m, data)) return@collect
                                    data.state = State.Finished
                                    data.uiState.apply {
                                        p = "100"
                                        size = ""
                                        total = it.file.length().formatSize()
                                    }
                                    @SuppressLint("NewApi")
                                    if (TargetApi.Q) data.uriString = it.file.copyToDownload(ctx)
                                    it.file.delete()
                                    animator?.cancel()
                                    repository.updateDowns(data)
                                    tvSpeed.text = ""
                                    updateUI(data)
                                    finishedNotify(data, 100, data.id)
                                    delay(200)
                                    currentList.none { it.state != State.Finished }.also { b ->
                                        finishedListen?.let { it(!b) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val getBinding get() = binding

        private fun isSame(
            sendData: KtorDownload,
            data: DownloadData
        ) = sendData.url == data.ktorDownload.url

        fun bind(data: DownloadData) {
            currentLink = data
            with(binding) {
                updateUI(data)
            }
        }

        private fun ItemDownloadBinding.updateUI(data: DownloadData) {
            imgDl.load(
                when (data.state) {
                    State.Finished -> R.drawable.check_circle
                    State.Downloading -> R.drawable.pause_circle
                    else -> R.drawable.circle_down
                }
            )
            tvFile.text = data.ktorDownload.url.getFileNameFromUrl()
            data.uiState.apply {
                progressBar.progress = p.toInt()
                tvProgress.text = when (data.state) {
                    State.Finished -> ctx.getString(R.string.done)
                    State.Failed -> ctx.getString(R.string.dl_failed)
                    else -> if (p != "0") ctx.getString(R.string.progress_value, p) else ""
                }
                tvSize.text = if (total.isNotEmpty()) ctx.getString(
                    R.string.download_size,
                    size,
                    total
                ) else ""
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDownloadBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val repo = getItem(position)
        holder.bind(repo)
    }

    suspend fun sendCancel(id: Int) {
        currentList.find { it.id == id }?.let {
            it.ktorDownload.job?.cancel()
            progressState.emit(DownloadType.Canceled(it.ktorDownload))
        }
    }

    @SuppressLint("NewApi", "MissingPermission")
    private fun sendNotify(data: DownloadData, progress: Int) {
        val cancelIntent = Intent(ctx, CancelReceiver::class.java).apply {
            putExtra(EXTRA_NOTIFICATION_ID, data.id)
        }
        val flag = if (TargetApi.S) FLAG_MUTABLE else 0
        val cancel = getBroadcast(ctx, data.id, cancelIntent, flag)
        builder.clearActions()
        builder.setProgress(100, progress, false)
        builder.addAction(R.drawable.ic_cancel, "取消", cancel)
        builder.setContentTitle(data.ktorDownload.url.getFileNameFromUrl())
        with(NotificationManagerCompat.from(ctx)) {
            notify(data.id, builder.build())
        }
    }


    @SuppressLint("MissingPermission")
    fun finishedNotify(data: DownloadData, progress: Int, id: Int) {
        builder.apply {
            clearActions()
            setProgress(100, progress, false)
            setContentTitle("Downloaded: ${data.ktorDownload.url.getFileNameFromUrl()}")
            setOngoing(false)
        }

        with(NotificationManagerCompat.from(ctx)) {
            notify(id, builder.build())
        }
    }
}