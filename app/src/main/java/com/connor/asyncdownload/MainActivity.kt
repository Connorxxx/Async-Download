package com.connor.asyncdownload

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.connor.asyncdownload.databinding.ActivityMainBinding
import com.connor.asyncdownload.type.DownloadType
import com.connor.asyncdownload.ui.adapter.DlAdapter
import com.connor.asyncdownload.viewmodls.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<MainViewModel>()

    @Inject lateinit var dlAdapter: DlAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        with(binding) {
            rvDl.layoutManager = LinearLayoutManager(this@MainActivity)
            rvDl.adapter = dlAdapter
        }
        viewModel.initLink()
        dlAdapter.submitList(ArrayList(viewModel.linkList))
        dlAdapter.setClickListener { b, data ->
            b.tvName.setOnClickListener {

            }
            lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.download(data).collect {
                        when (it) {
                            is DownloadType.Started -> { }
                            is DownloadType.Progress -> {
                                if (it.name == data.name) data.progress = it.value
                                b.progressBar.progress = data.progress.toInt()
                            }
                            is DownloadType.Failed -> {

                            }
                            is DownloadType.Finished -> {

                            }
                            is DownloadType.AsyncDownload -> {
                                it.body.await()
                            }
                        }
                    }
                }
            }

        }
    }
}