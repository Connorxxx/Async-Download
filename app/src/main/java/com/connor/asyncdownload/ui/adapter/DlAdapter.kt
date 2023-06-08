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
import com.connor.asyncdownload.model.data.DownloadData
import com.connor.asyncdownload.model.data.KtorDownload
import com.connor.asyncdownload.model.data.State
import com.connor.asyncdownload.receiver.CancelReceiver
import com.connor.asyncdownload.type.DownloadType
import com.connor.asyncdownload.utils.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DlAdapter @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val repository: Repository
) : ListAdapter<DownloadData, DlAdapter.ViewHolder>(DlDiffCallback) {

    object DlDiffCallback : DiffUtil.ItemCallback<DownloadData>() {
        override fun areItemsTheSame(oldItem: DownloadData, newItem: DownloadData): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DownloadData, newItem: DownloadData): Boolean {
            return oldItem == newItem
        }
    }

    private val job = Job()

    val scope = CoroutineScope(Dispatchers.Main + job)

    private val intent = Intent(ctx, MainActivity::class.java)
    private val pendingIntent = getActivity(ctx, 0, intent, FLAG_IMMUTABLE)

    private val builder = NotificationCompat.Builder(ctx, MainActivity.CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_download)
        .setContentIntent(pendingIntent)
        .setOnlyAlertOnce(true)
        .setOngoing(true)

    val progressState = MutableSharedFlow<DownloadType<KtorDownload>>()

    private var fileListen: ((DownloadData) -> Unit)? = null

    fun setFileClicked(datas: (DownloadData) -> Unit) {
        fileListen = datas
    }

    inner class ViewHolder(private val binding: ItemDownloadBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var currentLink: DownloadData? = null
        private var animator: ObjectAnimator? = null

        init {
            binding.imgDl.setOnClickListener {
                currentLink?.let { data ->
                    fileListen?.let { it(data) }
                }
            }
        }

        @SuppressLint("MissingPermission")
        fun bind(data: DownloadData) {
            currentLink = data
            with(binding) {
                imgDl.load(
                    when(data.state) {
                        State.Finished -> R.drawable.check_circle
                        State.Downloading -> R.drawable.pause_circle
                        else -> R.drawable.circle_down
                    }
                )
                tvFile.text = data.ktorDownload.url.getFileNameFromUrl()
                data.uiState.apply {
                    progressBar.progress = p.toInt()
                    tvProgress.text = if (p != "0" && p != "100") ctx.getString(R.string.progress_value, p)
                        else if (p == "100") ctx.getString(R.string.done) else ""
                    tvSize.text = if (total.isNotEmpty()) ctx.getString(R.string.download_size, size, total) else ""
                }
                scope.launch {
                    progressState.collect {
                        when (it) {
                            is DownloadType.Waiting -> {
                                if (it.m.url == data.ktorDownload.url) {
                                    tvProgress.text = ctx.getString(R.string.wating)
                                }
                            }
                            is DownloadType.Started -> {
                                if (it.m.url == data.ktorDownload.url) {
                                    //"Started: ${data.id}".logCat()
                                    data.state = State.Downloading
                                    imgDl.load(R.drawable.pause_circle)
                                }
                            }
                            is DownloadType.FileExists -> {
                                if (it.m.url == data.ktorDownload.url) {
                                    imgDl.load(R.drawable.circle_down)
                                }
                            }
                            is DownloadType.Progress -> {
                                if (it.m.url == data.ktorDownload.url) {
                                    data.uiState.apply {
                                        p = it.value.p
                                        size = it.value.size + " / "
                                        total = it.value.total
                                        tvProgress.text =
                                            ctx.getString(R.string.progress_value, p)
                                        tvSize.text =
                                            ctx.getString(R.string.download_size, size, total)
                                        sendNotify(data, p.toInt())
                                        animator = progressBar.setAmin(p.toInt(), 450)
                                    }
                                }
                            }
                            is DownloadType.Speed -> {
                                if (it.m.url == data.ktorDownload.url) {
                                    binding.tvSpeed.text = it.value
                                }
                            }
                            is DownloadType.Failed -> {
                                if (it.m.url == data.ktorDownload.url) {
                                    data.state = State.Failed
                                    // data.isPause = false
                                    imgDl.load(R.drawable.circle_down)
                                    tvProgress.text = ctx.getString(R.string.dl_failed)
                                    it.throwable.localizedMessage?.showToast()
                                }
                            }
                            is DownloadType.Pause -> {
                                if (it.m.url == data.ktorDownload.url) {
                                    data.state = State.Pause
                                   // imgDl.load(R.drawable.circle_down)
                                    repository.updateDowns(data)
                                }
                            }
                            is DownloadType.Canceled -> {
                                if (it.m.url == data.ktorDownload.url) {
                                    data.state = State.Canceled
                                  //  imgDl.load(R.drawable.circle_down)
                                    data.ktorDownload.downBytes = 0
                                    data.uiState.apply {
                                        p = "0"
                                        size = ""
                                        total = ""
                                    }
                                    repository.updateDowns(data)
//                                    tvSpeed.text = ""
//                                    tvSize.text = ""
//                                    tvProgress.text = ""
//                                    animator?.cancel()
//                                    progressBar.progress = 0
                                }
                            }
                            is DownloadType.Finished -> {
                                if (it.m.url == data.ktorDownload.url) {
                                    data.state = State.Finished
                                  //  imgDl.load(R.drawable.check_circle)
                                    data.uiState.apply {
                                        p = "100"
                                        size = ""
                                        total = it.file.length().formatSize()
                                    }
                                    //animator?.cancel()
                                    repository.updateDowns(data)
                                    //progressBar.progress = 100
                                    finishedNotify(data, 100, data.id)
                                }
                            }
                        }
                    }
                }
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

    suspend fun sendCancel(job: Job, link: DownloadData) {
        job.cancel()
        progressState.emit(DownloadType.Canceled(link.ktorDownload))
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