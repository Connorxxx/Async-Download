package com.connor.asyncdownload.ui.adapter

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PendingIntent.*
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.connor.asyncdownload.MainActivity
import com.connor.asyncdownload.R
import com.connor.asyncdownload.databinding.ItemDownloadBinding
import com.connor.asyncdownload.model.data.KtorDownload
import com.connor.asyncdownload.model.data.Link
import com.connor.asyncdownload.model.data.State
import com.connor.asyncdownload.receiver.CancelReceiver
import com.connor.asyncdownload.receiver.PauseReceiver
import com.connor.asyncdownload.type.DownloadType
import com.connor.asyncdownload.utils.formatSize
import com.connor.asyncdownload.utils.logCat
import com.connor.asyncdownload.utils.setAmin
import com.connor.asyncdownload.utils.showToast
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ActivityScoped
class DlAdapter @Inject constructor(
    @ActivityContext private val context: Context,
    private val scope: CoroutineScope
) : ListAdapter<Link, DlAdapter.ViewHolder>(DlDiffCallback) {

    object DlDiffCallback : DiffUtil.ItemCallback<Link>() {
        override fun areItemsTheSame(oldItem: Link, newItem: Link): Boolean {
            return oldItem.ktorDownload.uuid == newItem.ktorDownload.uuid
        }

        override fun areContentsTheSame(oldItem: Link, newItem: Link): Boolean {
            return oldItem == newItem
        }
    }

    private val pauseIntent = Intent(context, PauseReceiver::class.java)
    private val pendingPauseIntent = getBroadcast(context, 0, pauseIntent, FLAG_IMMUTABLE)

    private val cancelIntent = Intent(context, CancelReceiver::class.java)
    private val pendingCancelIntent = getBroadcast(context, 0, cancelIntent, FLAG_IMMUTABLE)

    val builder =  NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_download)
        .setContentTitle("Downloading...")
        .setProgress(100, 0, false)
        .addAction(R.drawable.pause_circle, "暂停", pendingPauseIntent)
        .addAction(R.drawable.ic_cancel, "取消", pendingCancelIntent)
        .setOngoing(true)

    val progressState = MutableStateFlow<DownloadType<KtorDownload>>(DownloadType.Default)

    private var fileListen: ((Link) -> Unit)? = null

    fun setFileClicked(datas: (Link) -> Unit) {
        fileListen = datas
    }

    inner class ViewHolder(private val binding: ItemDownloadBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var currentLink: Link? = null
        private var animator: ObjectAnimator? = null

        init {
            binding.imgDl.setOnClickListener {
                currentLink?.let { data ->
                    if (data.state == State.Finished) return@setOnClickListener
                    fileListen?.let { it(data) }
                }
            }
        }

        @SuppressLint("MissingPermission")
        fun bind(data: Link) {
            currentLink = data
            with(binding) {
                tvFile.text = data.name
                scope.launch {
                    progressState.collect {
                        when (it) {
                            is DownloadType.Default -> {}
                            is DownloadType.Waiting -> {
                                if (it.m.uuid == data.ktorDownload.uuid) {
                                    tvProgress.text = context.getString(R.string.wating)
                                }
                            }
                            is DownloadType.Started -> {
                                if (it.m.uuid == data.ktorDownload.uuid) {
                                    with(NotificationManagerCompat.from(context)) {
                                        notify(data.id, builder.build())
                                    }
                                    data.state = State.Downloading
                                    data.isPause = true
                                    imgDl.load(R.drawable.pause_circle)
                                }
                            }
                            is DownloadType.FileExists -> {
                                if (it.m.uuid == data.ktorDownload.uuid) {
                                    "File exists".showToast()
                                    imgDl.load(R.drawable.circle_down)
                                }
                            }
                            is DownloadType.Progress -> {
                                if (it.m.uuid == data.ktorDownload.uuid) {
                                    tvProgress.text =
                                        context.getString(R.string.progress_value, it.value)
                                    updateProgress(it.value.toInt(), data.id)
                                    animator = progressBar.setAmin(it.value.toInt(), 450)
                                }
                            }
                            is DownloadType.Size -> {
                                if (it.m.uuid == data.ktorDownload.uuid) {
                                    tvSize.text =
                                        context.getString(R.string.download_size, it.size, it.total)
                                }
                            }
                            is DownloadType.Speed -> {
                                if (it.m.uuid == data.ktorDownload.uuid) {
                                    binding.tvSpeed.text = it.value
                                }
                            }
                            is DownloadType.Failed -> {
                                if (it.m.uuid == data.ktorDownload.uuid) {
                                    data.state = State.Failed
                                    imgDl.load(R.drawable.circle_down)
                                    tvProgress.text = context.getString(R.string.dl_failed)
                                    it.throwable.localizedMessage?.showToast()
                                }
                            }
                            is DownloadType.Canceled -> {
                                if (it.m.uuid == data.ktorDownload.uuid) {
                                    data.isPause = false
                                    imgDl.load(R.drawable.circle_down)
                                }
                            }
                            is DownloadType.Finished -> {
                                if (it.m.uuid == data.ktorDownload.uuid) {
                                    data.state = State.Finished
                                    data.name = it.file.name
                                    tvProgress.text = context.getString(R.string.done)
                                    imgDl.load(R.drawable.check_circle)
                                    tvSize.text = it.file.length().formatSize()
                                    tvSpeed.text = ""
                                    animator?.cancel()
                                    progressBar.progress = 100
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


    @SuppressLint("MissingPermission")
    fun updateProgress(progress: Int, id: Int) {
        builder.setProgress(100, progress, false)
        with(NotificationManagerCompat.from(context)) {
            notify(id, builder.build())
        }
    }
}