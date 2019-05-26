package com.epam.filedownloader

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.net.URL

class DownloadService : Service() {

    private lateinit var connectivityManager: ConnectivityManager

    private var imageUrl = ""

    private var _bitmap: Bitmap? = null
    val bitmap: Bitmap? get() = _bitmap

    private var _isDownloading = false
    val isDownloading: Boolean get() = _isDownloading

    private val networkRequest =
        NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()

    private val connectivityCallBack = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network?) {
            super.onAvailable(network)
            Log.d(TAG, "IN CONNECTION CALLBACK")
            if (isDownloading) {
                startDownload(imageUrl)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerNetworkCallback(networkRequest, connectivityCallBack)
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(connectivityCallBack)
    }

    override fun onBind(intent: Intent?): IBinder? = ServiceBinder()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == DownloadFragment.DOWNLOAD_ACTION && isConnected()) {
            createNotificationChannel(
                NOTIFICATION_CHANNEL,
                getString(R.string.download_service),
                getString(R.string.download_service)
            )
            startForeground(NOTIFICATION_ID, createNotification(NOTIFICATION_CHANNEL))
            imageUrl = intent.getStringExtra(FILE_URI).also {
                startDownload(it)
            }
        }
        if (!isConnected()) {
            sendIntentToBroadcast(false)
        }
        return START_STICKY
    }

    private fun startDownload(uri: String) {
        Thread {
            _isDownloading = true
            _bitmap = BitmapFactory.decodeStream(URL(uri).openConnection().getInputStream())
            if (bitmap != null) {
                sendIntentToBroadcast(true)
                _isDownloading = false
                stopForeground(true)
            }
            sendIntentToBroadcast(false)
        }.start()
    }

    private fun sendIntentToBroadcast(succeed: Boolean) {
        Intent(DOWNLOADED).putExtra(SUCCESS_DOWNLOAD, succeed).also {
            sendBroadcast(it)
        }
    }

    private fun createNotificationChannel(
        channelId: String,
        name: String,
        content: String
    ): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return ""
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(
                channelId,
                name,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = content
            }
        )
        return channelId
    }

    private fun createNotification(channelId: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_file_download_black_24dp)
            .setContentTitle("Downloading...")
            .setContentText("Downloading in progress...")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun isConnected(): Boolean = connectivityManager.activeNetworkInfo?.isConnected == true

    inner class ServiceBinder : Binder() {
        val serviceInstance = this@DownloadService
    }

    companion object {
        private const val TAG = "DownloadService"
        private const val NOTIFICATION_CHANNEL = "NOTIFICATION_CHANNEL"
        private const val NOTIFICATION_ID = 1
        private const val FILE_URI = "FILE_URI"
        const val DOWNLOADED = "DOWNLOADED"
        const val SUCCESS_DOWNLOAD = "SUCCESS_DOWNLOAD"
    }
}