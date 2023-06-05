package com.connor.asyncdownload.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.connor.asyncdownload.databinding.ItemDownloadBinding
import com.connor.asyncdownload.model.data.Link
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class DlAdapter @Inject constructor() : ListAdapter<Link, DlAdapter.ViewHolder>(DlDiffCallback) {

    object DlDiffCallback : DiffUtil.ItemCallback<Link>() {
        override fun areItemsTheSame(oldItem: Link, newItem: Link): Boolean {
            return oldItem.url == newItem.url
        }

        override fun areContentsTheSame(oldItem: Link, newItem: Link): Boolean {
            return oldItem == newItem
        }
    }

    private var listener: ((ItemDownloadBinding, Link) -> Unit?)? = null

    fun setClickListener(listener: (ItemDownloadBinding, Link) -> Unit) {
        this.listener = listener
    }

    inner class ViewHolder(private val binding: ItemDownloadBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(data: Link) {
            binding.tvName.text = data.name
            binding.progressBar.progress = data.progress.toInt()
            listener?.let { it(binding, data) }
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