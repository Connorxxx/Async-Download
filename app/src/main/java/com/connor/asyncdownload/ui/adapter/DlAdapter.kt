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
import com.connor.asyncdownload.model.data.DownloadData
import com.connor.asyncdownload.model.data.State
import com.connor.asyncdownload.utils.getFileNameFromUrl
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DlAdapter @Inject constructor(
    @ApplicationContext private val ctx: Context
) : ListAdapter<DownloadData, DlAdapter.ViewHolder>(DlDiffCallback) {

    object DlDiffCallback : DiffUtil.ItemCallback<DownloadData>() {
        override fun areItemsTheSame(oldItem: DownloadData, newItem: DownloadData) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: DownloadData, newItem: DownloadData): Boolean {
            return oldItem.state == newItem.state
        }
    }

    private var fileListen: ((DownloadData) -> Unit)? = null
    private var deleteClick: ((DownloadData) -> Unit)? = null
    fun setFileClicked(datas: (DownloadData) -> Unit) {
        fileListen = datas
    }

    fun deleteDownload(list: (DownloadData) -> Unit) {
        deleteClick = list
    }

    inner class ViewHolder(private val binding: ItemDownloadBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val getBinding get() = binding
        var currentLink: DownloadData? = null

        init {
            with(binding) {
                imgDl.setOnClickListener {
                    currentLink?.let { data ->
                        fileListen?.let { it(data) }
                    }
                }
                tvFile.setOnLongClickListener {
                    currentLink?.let { data ->
                        deleteClick?.let { it(data) }
                    }
                    true
                }
            }
        }

        fun bind(data: DownloadData) {
            currentLink = data
            binding.updateUI(data)
        }

        private fun ItemDownloadBinding.updateUI(data: DownloadData) {
            imgDl.load(
                when (data.state) {
                    State.Finished -> R.drawable.check_circle
                    State.Downloading -> R.drawable.pause_circle
                    else -> R.drawable.circle_down
                }
            )
            tvFile.text = data.url.getFileNameFromUrl()
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
                tvSpeed.text = ""
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