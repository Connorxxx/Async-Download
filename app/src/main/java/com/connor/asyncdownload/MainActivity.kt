package com.connor.asyncdownload

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.connor.asyncdownload.databinding.ActivityMainBinding
import com.connor.asyncdownload.model.data.Link
import com.connor.asyncdownload.type.DownloadAll
import com.connor.asyncdownload.ui.adapter.DlAdapter
import com.connor.asyncdownload.utils.logCat
import com.connor.asyncdownload.utils.post
import com.connor.asyncdownload.utils.subscribe
import com.connor.asyncdownload.viewmodls.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<MainViewModel>()

    @Inject
    lateinit var dlAdapter: DlAdapter

    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        with(binding) {
            rvDl.layoutManager = LinearLayoutManager(this@MainActivity)
            rvDl.adapter = dlAdapter
        }
        viewModel.initLink()
        with(dlAdapter) {
            submitList(ArrayList(viewModel.linkList))
            setNameClicked {
                job = lifecycleScope.launch {
                    viewModel.download(it).collect {
                        progressState.emit(it)
                    }
                }
            }
            setFileClicked {
                job?.cancel()
            }
        }

        binding.fab.setOnClickListener {
            lifecycleScope.launch {
                viewModel.linkList.forEach { link ->
                    launch {
                        viewModel.download(link).collect {
                            dlAdapter.progressState.emit(it)
                        }
                    }
                }
            }

        }
    }
}