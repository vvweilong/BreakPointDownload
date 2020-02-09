package com.oo.breakpointdownload

import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.oo.breakpointdownload.tasks.DownloadTask
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

/**
* create by 朱晓龙 2020/2/9 12:31 PM
 * 文件下载管理器 支持端点续传
 * 下载的文件分为  app 内资源文件 某些页面的 pdf 资源 网页资源 图片资源 单纯图片展示可用 glide 进行加载 所以这部分功能 暂缓
 * apk 安装包文件
 * 公共资源文件  如 需要保存到相册的图片  以及文档等
 *
 * 需要一个文档来记录已下载的产品，当下载完成 删除记录
*/
object FileDownloadManager {
    val TAG = "FileDownloadManager"
    //确立工作线程池
    val workThreadPool = Executors.newFixedThreadPool(5)
    //串行下载时的任务队列
    val downloadQueue = LinkedBlockingQueue<DownloadTask>()
    //串行时 检查下载任务队列的线程
    val handlerThread = HandlerThread("sequenceCheckThread")
    val sequenceHandler :Handler
    /**
     * 下载状态
     * */
    enum class DownloadState{
        DOWNLOADING,//下载中
        FINISHED//完成下载
    }

    private val stateList = HashMap<String,DownloadState>()
    private val sequenceCheckRunnable = Runnable {
        while (true){

        }
    }

    init {
        handlerThread.start()
        sequenceHandler= Handler(handlerThread.looper)
//        sequenceHandler.post { sequenceCheckRunnable }
    }

    private fun saveDownloadState(path: String,state:DownloadState){
        stateList.put(path,state)
    }

    /**
     * 串行下载
     * */
    fun downloadSequence(context: Context,url:String,listener:DownLoadListener){
        val buildDowntask = buildDowntask(context, url, listener)
        downloadQueue.put(buildDowntask)
    }

    private fun buildDowntask(context: Context, url: String, listener: DownLoadListener):DownloadTask {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val fileName = makeFileName(url)
        val targetPath = "${dir?.path}/${fileName}"

        val offset = Math.max(checkTargetFile(targetPath), 0)

        val downloadTask =
            DownloadTask(url, targetPath, offset, -1, object : DownloadTask.Callback {
                override fun started() {
                    saveDownloadState(fileName, DownloadState.DOWNLOADING)
                }

                override fun success() {
                    Log.i(TAG, "download success")
                    saveDownloadState(fileName, DownloadState.FINISHED)
                    listener.downloadFinished(targetPath)
                }

                override fun failure() {
                    Log.i(TAG, "download failure")
                }

                override fun process(process: Int) {
                    Log.i(TAG, "download process $process")
                    listener.process(process)
                }
            })
        return downloadTask
    }

    /**
     * 大文件下载
     * 需要开启多个 连接进行分段下载
     * */
    fun downloadBigFile(context: Context,url:String,listener: DownLoadListener){

    }


    fun download(context: Context,url:String,listener:DownLoadListener){
        val buildDowntask = buildDowntask(context, url, listener)
        workThreadPool.submit(buildDowntask)
    }
    private fun makeFileName(url:String):String{
        val split = url.split("/")
        return split.last()
    }


    /**
     * 检查是否有目标文件了 如果有返回文件长度
     * @param path 目标存放位置
     * @return 返回已有文件的长度   ，没有文件的时候返回-1
     * */
    private fun checkTargetFile(path:String):Long{
        val file = File(path)
        if (file.exists()) {
            return file.length()
        }
        return -1
    }
}