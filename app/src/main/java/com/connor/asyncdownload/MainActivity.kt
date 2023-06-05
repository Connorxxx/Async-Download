package com.connor.asyncdownload

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.connor.asyncdownload.databinding.ActivityMainBinding
import com.connor.asyncdownload.databinding.ItemDownloadBinding
import com.connor.asyncdownload.model.data.Link
import com.connor.asyncdownload.type.DownloadAll
import com.connor.asyncdownload.type.DownloadType
import com.connor.asyncdownload.ui.adapter.DlAdapter
import com.connor.asyncdownload.utils.post
import com.connor.asyncdownload.utils.showToast
import com.connor.asyncdownload.utils.subscribe
import com.connor.asyncdownload.viewmodls.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<MainViewModel>()

    @Inject
    lateinit var dlAdapter: DlAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        with(binding) {
            rvDl.layoutManager = LinearLayoutManager(this@MainActivity)
            rvDl.adapter = dlAdapter
        }
        viewModel.initLink()
        dlAdapter.submitList(ArrayList(viewModel.linkList))
        dlAdapter.bindData(::handleAdapter)
        binding.fab.setOnClickListener {
            post(DownloadAll)
        }
    }

    private fun handleAdapter(itemBinding: ItemDownloadBinding, data: Link) {
        with(itemBinding) {
            lifecycleScope.launch {
                subscribe<DownloadAll> {
                    viewModel.download(data).collect {
                        downloadState(it)
                    }
                }
            }
            tvName.setOnClickListener {
                lifecycleScope.launch {
                    viewModel.download(data).collect {
                        downloadState(it)
                    }
                }
            }
        }
    }

    private fun ItemDownloadBinding.downloadState(it: DownloadType) {
        when (it) {
            is DownloadType.Started -> {
                tvName.isEnabled = false
            }
            is DownloadType.Progress -> {
                tvName.text = it.value
                progressBar.progress = it.value.toInt()
            }
            is DownloadType.Failed -> {

            }
            is DownloadType.Finished -> {
                tvName.isEnabled = true
                tvName.text = it.file.name
            }
        }
    }
}