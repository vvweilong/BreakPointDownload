package com.oo.breakpointdownload

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import java.io.BufferedOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.net.URL
import java.net.URLDecoder
import java.util.*
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection
import kotlin.collections.ArrayList

/**
* create by 朱晓龙 2020/2/9 10:35 PM
 * 大文件下载器
 *
*/
object BigFileDownloader {

    const val KB_SIZE = 1024
    const val MB_SIZE = 1024* KB_SIZE
    const val GB_SIZE = 1024* MB_SIZE


    //确立工作线程池
    val workThreadPool = Executors.newFixedThreadPool(5)

    val uiHandler = Handler(Looper.getMainLooper())

    fun download(context: Context, urlStr:String, listener: DownLoadListener){
        workThreadPool.submit {
            val url = URL(urlStr)
            //获取文件名
            val fileName = makeFileName(urlStr)
            //获取文件长度
            val conn = url.openConnection() as HttpsURLConnection
            configConnection(conn)
            conn.connect()
            if (conn.responseCode == HttpsURLConnection.HTTP_OK) {
                var fileTotalSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    conn.contentLengthLong
                } else {
                    conn.contentLength.toLong()
                }
                conn.disconnect()
                //获取线程数量
                val threadNum = splitBigFile(fileTotalSize)
                val blockSize = fileTotalSize / threadNum

                val fileMap = ArrayList<String>()

                var startPos = 0L
                var endPos = blockSize
                for (index in 0 until  threadNum) {
                    val openConnection = url.openConnection() as HttpsURLConnection
                    configConnection(openConnection)
                    //设置请求文件的数据范围
                    if (index == threadNum) {
                        openConnection.setRequestProperty("Range", "bytes=$startPos-")
                    } else {
                        openConnection.setRequestProperty("Range", "bytes=$startPos-$endPos")
                    }
                    startPos = endPos + 1
                    endPos = startPos + blockSize
                    val splitFileName ="${index}_${fileName}"
                    download(context, openConnection, splitFileName,
                        object : SplitDownloadListener {
                            override fun finish(name: String) {
                                fileMap.add(splitFileName)
                                //检查
                                if (fileMap.size == threadNum) {
                                    uiHandler.post {
                                        compactFile(context,fileMap)
                                    }
                                }
                            }
                        })
                }

            } else {
                // TODO: 2020/2/9 异常
                conn.disconnect()
            }
        }
    }

    private fun mergeFileInto(f1:File?,f2:File):File{
        if (f1 == null) {
            return f2
        }
        val randomAccessFile = RandomAccessFile(f1, "rwd")
        val inputStream = f2.inputStream()
        randomAccessFile.seek(randomAccessFile.length())
        val bytes = ByteArray(1024*100)
        var l=0
        while (inputStream.read(bytes).also { l=it }!=-1){
            randomAccessFile.write(bytes,0,l)
        }
        randomAccessFile.close()
        inputStream.close()

        if (f2.exists()) {
            f2.delete()
        }

        return f1
    }

    private fun compactFile(context: Context,fileMap:ArrayList<String>) {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        Collections.sort(fileMap, comparator)
        for (i in fileMap.size-1 downTo 0){
            val f2 = File(dir,fileMap[i])

            val f1 =if (i>0){ File(dir,fileMap[i-1])}else{null}

            mergeFileInto(f1,f2)

        }
    }

    val comparator = MyComparator()
    class MyComparator : Comparator<String>{
        override fun compare(f1: String?, f2: String?): Int {
            val i1 = f1?.split("_")?.get(0)?.toInt()?:0
            val i2 = f2?.split("_")?.get(0)?.toInt()?:0

            return i1.compareTo(i2)
        }
    }

    private fun download(context: Context,conn:HttpsURLConnection,partName:String,listener:SplitDownloadListener){
        uiHandler.post {
            workThreadPool.submit {
                conn.connect()
                if (conn.responseCode == HttpsURLConnection.HTTP_OK || conn.responseCode == HttpsURLConnection.HTTP_PARTIAL) {
                    val readBytes = conn.inputStream.readBytes()
                    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(dir, partName)
                    if (file.exists()) {
                        file.delete()
                    }
                    file.createNewFile()

                    val bufferedOutputStream = BufferedOutputStream(file.outputStream())
                    bufferedOutputStream.write(readBytes)
                    bufferedOutputStream.flush()
                    bufferedOutputStream.close()
                    listener.finish(partName)
                }
            }
        }
    }


    /**
     * 对文件进行尺寸区分
     * @param totalSize 文件总大小
     * @return 返回分块数量
     * */
    private fun splitBigFile(totalSize:Long):Int{
        when{
            totalSize< KB_SIZE->{
                return 1
            }
            totalSize< MB_SIZE->{
                return 1
            }
            totalSize< GB_SIZE->{
                return 5
            }
            else->{
                return 10
            }
        }
    }


    private fun configConnection(conn:HttpsURLConnection){
        conn.readTimeout=10*1000
        conn.connectTimeout=15*1000
    }


    private fun makeFileName(url:String):String{

        val split = URLDecoder.decode(url).split("/")
        return split.last()
    }


    interface SplitDownloadListener{
        fun finish(name:String)
    }

}