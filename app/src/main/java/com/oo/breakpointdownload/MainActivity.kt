package com.oo.breakpointdownload

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity(), FileDownloadManager.DownLoadListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val readPermission = ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )==PackageManager.PERMISSION_GRANTED
        val writePermission = ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )==PackageManager.PERMISSION_GRANTED

        if(!readPermission||!writePermission){

            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE
                ,android.Manifest.permission.WRITE_EXTERNAL_STORAGE),0)
        }
    }


    fun download(v:View?){
        val arrayOf = arrayOf(
            "https://www.bookben.net/down/all/34042.txt"
//            "https://www.bookben.net/down/all/1339.txt",
//            "https://www.bookben.net/down/all/34754.txt",
//            "https://www.bookben.net/down/all/34933.txt",
//            "https://www.bookben.net/down/all/2257.txt"
        )

        for (url in arrayOf) {
            FileDownloadManager.download(this,url,this)
        }
    }

    override fun downloadFinished(path: String, process: Int) {
        Toast.makeText(this, "$path", Toast.LENGTH_SHORT).show()
    }

    override fun process(process: Int) {
        findViewById<TextView>(R.id.result_tv).text = "$process"
    }
}
