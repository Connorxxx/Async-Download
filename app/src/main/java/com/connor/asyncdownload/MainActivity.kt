package com.connor.asyncdownload

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.connor.asyncdownload.databinding.ActivityMainBinding
import com.connor.asyncdownload.model.data.*
import com.connor.asyncdownload.type.DownloadType
import com.connor.asyncdownload.type.UiState
import com.connor.asyncdownload.ui.adapter.DlAdapter
import com.connor.asyncdownload.utils.*
import com.connor.asyncdownload.viewmodls.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
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
        createNotificationChannel()
        with(binding) {
            rvDl.layoutManager = LinearLayoutManager(this@MainActivity)
            rvDl.adapter = dlAdapter
            fab.setOnLongClickListener {
                viewModel.addData(dlAdapter.currentList)
                true
            }
            fab.setOnClickListener {
               // if (dlAdapter.currentList.none { it.state != State.Finished }) return@setOnClickListener
                dlAdapter.currentList.filterNot { it.state == State.Finished }.forEach { link ->
                    link.state.logCat()
//                    if (!viewModel.fabClick) {
//                        if (link.state != State.Downloading)
//                            viewModel.download(link) { type -> dlAdapter.progressState.emit(type) }
//                    } else sendPause(link.ktorDownload)
                }
                viewModel.setUi(UiState.FabClick(!viewModel.fabClick))
            }
        }
        with(dlAdapter) {
            finished {
                viewModel.setUi(UiState.FabClick(it))
            }
            setFileClicked { link ->
                when (link.state) {
                    State.Pause, State.Default, State.Canceled, State.Failed -> {
                        viewModel.download(link) { type -> progressState.emit(type) }
                    }
                    State.Finished -> {
                        val uri = Uri.parse(link.uriString)
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, contentResolver.getType(uri))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            startActivity(this)
                        }
                    }
                    State.Downloading -> sendPause(link.ktorDownload)
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.loadDownData.collect {
                        dlAdapter.submitList(it.sortedBy { d -> d.id })
                    }
                }
                launch {
                    viewModel.uiState.collect {
                        when(it) {
                            is UiState.FabClick -> {
                                viewModel.fabClick = it.boolean
                                if (!it.boolean) binding.fab.load(R.drawable.circle_down)
                                else binding.fab.load(R.drawable.pause_circle)
                            }
                        }

                    }
                }
            }
        }
    }

    //requestNotify.launch(postNotify)
    private val requestNotify = registerForActivityResult(
        RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            "todo".logCat()
        }
    }

    private fun sendPause(link: KtorDownload) {
        lifecycleScope.launch {
            dlAdapter.progressState.emit(DownloadType.Pause(link))
        }
    }

    private fun createNotificationChannel() {
        val name = getString(R.string.channel_name)
        val descriptionText = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            setSound(null, null)
            enableVibration(false)
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "download"
    }
}