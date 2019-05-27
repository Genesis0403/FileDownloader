package com.epam.filedownloader

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * Fragment which starts [DownloadService] and [LocalBroadcastReceiver]
 * to download an image from the Internet by its URL.
 * Also asks permission for writing into external storage.
 *
 * @author Vlad Korotkevich
 */

class DownloadFragment : Fragment() {

    private lateinit var downloadService: DownloadService
    private val downloadedReceiver = LocalBroadcastReceiver()

    private var isBound = false
    private var isPermissionGranted = false

    private lateinit var imageView: ImageView

    private var imageUri = Uri.EMPTY

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            isBound = true
            downloadService = (service as DownloadService.ServiceBinder).serviceInstance
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkStoragePermission()
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
        Log.d(TAG, isPermissionGranted.toString())
        if (isPermissionGranted && isBound && !downloadService.isDownloading) {
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

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        val uriString = savedInstanceState?.getString(FILE_URI) ?: return
        if (uriString != Uri.EMPTY.toString()) {
            imageUri = Uri.parse(uriString)
            imageView.setImageBitmap(MediaStore.Images.Media.getBitmap(context?.contentResolver, imageUri))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(FILE_URI, imageUri.toString())
    }

    /**
     * Receiver which get intent when image is downloaded.
     *
     * @author Vlad Korotkevich
     */
    inner class LocalBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == DownloadService.DOWNLOADED) {
                val success = intent.getBooleanExtra(DownloadService.SUCCESS_DOWNLOAD, false)
                if (success) {
                    imageUri = Uri.parse(intent.getStringExtra(DownloadService.FILE_URI))
                    imageView.setImageBitmap(MediaStore.Images.Media.getBitmap(context?.contentResolver, imageUri))
                } else {
                    Toast.makeText(context, "An error occurred during downloading", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (ContextCompat.checkSelfPermission(context as Activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                WRITE_EXTERNAL_STORAGE
            )
        }
        isPermissionGranted = true

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            WRITE_EXTERNAL_STORAGE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(context, "App requires location permission", Toast.LENGTH_SHORT).show()
                    return
                }
                isPermissionGranted = true
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private const val TAG = "DOWNLOAD FRAGMENT"
        private const val FILE_URI = "FILE_URI"
        const val DOWNLOAD_ACTION = "com.epam.filedownloader.DOWNLOAD_ACTION"
        private const val WRITE_EXTERNAL_STORAGE = 1

        fun newInstance() = DownloadFragment()
    }
}