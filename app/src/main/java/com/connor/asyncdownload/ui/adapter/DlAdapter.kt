package com.connor.asyncdownload.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.connor.asyncdownload.databinding.ItemDownloadBinding
import com.connor.asyncdownload.model.data.Link
import com.connor.asyncdownload.type.DownloadType
import com.connor.asyncdownload.utils.logCat
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

@ActivityScoped
class DlAdapter @Inject constructor(
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

    val progressState = MutableStateFlow<DownloadType<Link>>(DownloadType.Waiting)

    private var l: ((Link) -> Unit)? = null

    fun setNameClicked(datas: (Link) -> Unit) {
        this.l = datas
    }

    inner class ViewHolder(private val binding: ItemDownloadBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var currentLink: Link? = null

        init {
            binding.tvName.setOnClickListener {
                currentLink?.let { data ->
                    l?.let { it(data) }
                }
            }
        }

        fun bind(data: Link) {
            currentLink = data
            with(binding) {
                tvName.text = data.name
                progressBar.progress = data.progress.toInt()
                scope.launch {
                    progressState.collect {
                        when (it) {
                            is DownloadType.Waiting -> {

                            }
                            is DownloadType.Started -> {
                                if (it.m.uuid == data.uuid) {
                                    data.isEnable = false
                                    tvName.isEnabled = data.isEnable
                                    it.m.url.logCat()
                                }
                            }
                            is DownloadType.Progress -> {
                                if (it.m.uuid == data.uuid) {
                                    data.progress = it.value
                                    data.name = it.value
                                    tvName.text = data.name
                                    progressBar.progress = data.progress.toInt()
                                }
                            }
                            is DownloadType.Failed -> {
                                if (it.m.uuid == data.uuid) {
                                    data.isEnable = true
                                    data.name = "error"
                                    it.throwable.localizedMessage?.logCat()
                                    tvName.isEnabled = data.isEnable
                                    tvName.text = data.name
                                }
                            }
                            is DownloadType.Finished -> {
                                if (it.m.uuid == data.uuid) {
                                    data.isEnable = true
                                    data.name = it.file.name
                                    tvName.isEnabled = data.isEnable
                                    tvName.text = data.name
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