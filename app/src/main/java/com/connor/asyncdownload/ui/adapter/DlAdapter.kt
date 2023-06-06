package com.connor.asyncdownload.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.connor.asyncdownload.R
import com.connor.asyncdownload.databinding.ItemDownloadBinding
import com.connor.asyncdownload.model.data.Link
import com.connor.asyncdownload.type.DownloadType
import com.connor.asyncdownload.utils.logCat
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

@ActivityScoped
class DlAdapter @Inject constructor(
    @ActivityContext private val context: Context,
    private val scope: CoroutineScope
) : ListAdapter<Link, DlAdapter.ViewHolder>(DlDiffCallback) {

    object DlDiffCallback : DiffUtil.ItemCallback<Link>() {
        override fun areItemsTheSame(oldItem: Link, newItem: Link): Boolean {
            return oldItem.url == newItem.url
        }

        override fun areContentsTheSame(oldItem: Link, newItem: Link): Boolean {
            return oldItem == newItem
        }
    }

    val progressState = MutableStateFlow<DownloadType<Link>>(DownloadType.Default)

    private var l: ((Link) -> Unit)? = null
    private var fileListen: ((Link) -> Unit)? = null

    fun setNameClicked(datas: (Link) -> Unit) {
        this.l = datas
    }

    fun setFileClicked(datas: (Link) -> Unit) {
        fileListen = datas
    }

    inner class ViewHolder(private val binding: ItemDownloadBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var currentLink: Link? = null

        init {
            binding.tvFile.setOnClickListener {
                currentLink?.let { data ->
                    l?.let { it(data) }
                }
            }
            binding.imgDl.setOnClickListener {
                currentLink?.let { data ->
                    fileListen?.let { it(data) }
                }
            }
        }

        fun bind(data: Link) {
            currentLink = data
            with(binding) {
                tvFile.text = data.name
                progressBar.progress = data.progress.toInt()
                scope.launch {
                    progressState.collect {
                        when (it) {
                            is DownloadType.Default -> { }
                            is DownloadType.Waiting -> {
                                if (it.m.uuid == data.uuid) {
                                    tvProgress.text = context.getString(R.string.wating)
                                }
                            }
                            is DownloadType.Started -> {
                                if (it.m.uuid == data.uuid) {
                                    it.m.url.logCat()
                                    imgDl.load(R.drawable.pause_circle)
                                }
                            }
                            is DownloadType.Progress -> {
                                if (it.m.uuid == data.uuid) {
                                    data.progress = it.value
                                    tvProgress.text = context.getString(R.string.progress_value, data.progress)
                                    progressBar.progress = data.progress.toInt()
                                }
                            }
                            is DownloadType.Speed -> {
                                if (it.m.uuid == data.uuid) {
                                    data.speed = it.value
                                    binding.tvSpeed.text = data.speed
                                }
                            }
                            is DownloadType.Failed -> {
                                if (it.m.uuid == data.uuid) {
                                    data.name = "error"
                                    it.throwable.localizedMessage?.logCat()
                                }
                            }
                            is DownloadType.Canceled -> {
                                if (it.m.uuid == data.uuid) {
                                    imgDl.load(R.drawable.circle_down)
                                }
                                "CANCELED v".logCat()
                            }
                            is DownloadType.Finished -> {
                                if (it.m.uuid == data.uuid) {
                                    data.name = it.file.name
                                    binding.tvSpeed.text = ""
                                    tvProgress.text = context.getString(R.string.done)
                                    imgDl.load(R.drawable.check_circle)
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
}