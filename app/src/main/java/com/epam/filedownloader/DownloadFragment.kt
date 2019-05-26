package com.epam.filedownloader

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment

class DownloadFragment : Fragment() {

    private lateinit var downloadService: DownloadService
    private val downloadedReceiver = LocalBroadcastReceiver()

    private var isBound = false

    private lateinit var imageView: ImageView

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            isBound = true
            downloadService = (service as DownloadService.ServiceBinder).serviceInstance
        }
    }

    override fun onResume() {
        super.onResume()
        context?.registerReceiver(downloadedReceiver, IntentFilter(DownloadService.DOWNLOADED))
    }

    override fun onPause() {
        super.onPause()
        context?.unregisterReceiver(downloadedReceiver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fragment = inflater.inflate(R.layout.download_fragment, container, false)

        imageView = fragment.findViewById(R.id.downloadedImage)

        val downloadButton = fragment.findViewById<Button>(R.id.downloadButton)
        downloadButton.setOnClickListener {
            initDownloadButton(fragment.findViewById<EditText>(R.id.urlEditText).text.toString())
        }
        return fragment
    }

    private fun initDownloadButton(uri: String) {
        if (isBound && !downloadService.isDownloading) {
            if (URLUtil.isValidUrl(uri)) {
                Intent(context, DownloadService::class.java).apply {
                    putExtra(FILE_URI, uri)
                    action = DOWNLOAD_ACTION
                }.also {
                    context?.startService(it)
                }
                Toast.makeText(context, getString(R.string.starting_downloading), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, getString(R.string.invalid_uri), Toast.LENGTH_SHORT).show()
            }
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
                val success = intent.getBooleanExtra(DownloadService.SUCCESS_DOWNLOAD, false)
                if (success) {
                    imageView.setImageBitmap(downloadService.bitmap)
                } else {
                    Toast.makeText(context, "An error occurred during downloading", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    companion object {
        private const val TAG = "DOWNLOAD FRAGMENT"
        private const val FILE_URI = "FILE_URI"
        const val DOWNLOAD_ACTION = "com.epam.filedownloader.DOWNLOAD_ACTION"

        fun newInstance() = DownloadFragment()
    }
}