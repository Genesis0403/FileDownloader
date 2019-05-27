package com.epam.filedownloader

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Main Activity which contains [DownloadFragment].
 *
 * @author Vlad Korotkevich
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.mainActivity, DownloadFragment.newInstance())
                .commit()
        }
    }
}
