package com.epam.filedownloader

import android.content.*
import android.graphics.Bitmap
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment

class DownloadFragment : Fragment() {

    private lateinit var downloadService: DownloadService

    private var isBound = false
    private var bitmap: Bitmap? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            isBound = true
            downloadService = (service as DownloadService.ServiceBinder).serviceInstance
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fragment = inflater.inflate(R.layout.download_fragment, container, false)

        val downloadButton = fragment.findViewById<Button>(R.id.downloadButton)
        downloadButton.setOnClickListener {
            if (isBound && !downloadService.isDownloading) {
                val editText = fragment.findViewById<EditText>(R.id.urlEditText)
                initDownloadButton(editText.text.toString())
                Toast.makeText(context, getString(R.string.starting_downloading), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, getString(R.string.fail_to_download), Toast.LENGTH_SHORT).show()
            }
        }

        return fragment
    }

    private fun initDownloadButton(uri: String) {
        Intent(context, DownloadService::class.java).apply {
            putExtra(FILE_URI, uri)
            action = DOWNLOAD_ACTION
        }.also {
            context?.startService(it)
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(context, DownloadService::class.java).also {
            context?.bindService(it, serviceConnection, Context.BIND_AUTO_CREATE)
        }
        isBound = true
    }

    override fun onStop() {
        super.onStop()
        if (!isBound) return
        context?.unbindService(serviceConnection)
        isBound = false
    }

    inner class LocalBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == DownloadService.DOWNLOADED) {
                bitmap = downloadService.bitmap
            }
        }
    }

    companion object {
        private const val FILE_URI = "FILE_URI"
        const val DOWNLOAD_ACTION = "com.epam.filedownloader.DOWNLOAD_ACTION"
    }
}