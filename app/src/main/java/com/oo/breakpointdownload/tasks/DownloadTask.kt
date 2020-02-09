package com.oo.breakpointdownload.tasks

import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
* create by 朱晓龙 2020/2/9 12:46 PM
 * 下载任务 执行单元
 * 支持断点续传
*/
class DownloadTask(val url:String,val path:String,val offset:Long,val callback:Callback):Runnable {
    val TAG = "DownloadTask"
    open interface Callback{
        fun started()
        fun success()
        fun failure()
        fun process(process:Int)
    }




    override fun run() {
        val openConnection = URL(url).openConnection() as HttpsURLConnection
        openConnection.requestMethod="GET"
        openConnection.readTimeout=10*1000
        openConnection.connectTimeout=15*1000
        openConnection.addRequestProperty("Range","bytes=$offset-")

        if (openConnection.responseCode == HttpURLConnection.HTTP_OK||openConnection.responseCode == HttpURLConnection.HTTP_PARTIAL) {
            val totalSize = openConnection.contentLength

            val inputStream = openConnection.inputStream
            val time = System.currentTimeMillis()

            val localFile = File(path)
            if (!localFile.exists()) {
                localFile.createNewFile()
            }
            val randomWriteFile = RandomAccessFile(localFile,"rwd")
            randomWriteFile.seek(randomWriteFile.length())
            val byteArray = ByteArray(1024*1024*100)
            var l = 0
            var content = 0
            while (inputStream.read(byteArray).also { l = it } != -1) {
                content +=l
                randomWriteFile.write(byteArray, 0, l)
                callback.process(content*100/totalSize)
            }
            Log.i(TAG,"download run takeTime ${System.currentTimeMillis()-time}")
            randomWriteFile.close()
            callback.success()
        }else{
            // TODO: 2020/2/9 异常处理
            Log.i(TAG,"${openConnection.responseMessage}")
            callback.failure()
        }
    }
}