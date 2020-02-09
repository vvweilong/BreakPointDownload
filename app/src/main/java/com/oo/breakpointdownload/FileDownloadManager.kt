package com.oo.breakpointdownload

import android.content.Context
import android.os.Environment
import android.util.Log
import com.oo.breakpointdownload.tasks.DownloadTask
import java.io.File
import java.util.concurrent.Executors

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
    /**
     * 下载状态
     * */
    enum class DownloadState{
        DOWNLOADING,//下载中
        FINISHED//完成下载
    }

    private val stateList = HashMap<String,DownloadState>()

    private fun saveDownloadState(path: String,state:DownloadState){
        stateList.put(path,state)
    }

    /**
     * 串行下载
     * */
    fun downloadSequence(context: Context,url:String,listener:DownLoadListener){

    }

    public fun download(context: Context,url:String,listener:DownLoadListener){
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val fileName = makeFileName(url)
        val targetPath = "${dir?.path}/${fileName}"

        val offset = Math.max(checkTargetFile(targetPath),0)

        val downloadTask = DownloadTask(url, targetPath, offset, object : DownloadTask.Callback {
            override fun started() {
                saveDownloadState(fileName, DownloadState.DOWNLOADING)
            }

            override fun success() {
                Log.i(TAG, "download success")
                saveDownloadState(fileName, DownloadState.FINISHED)
                listener.downloadFinished(targetPath, 0)
            }

            override fun failure() {
                Log.i(TAG, "download failure")
            }

            override fun process(process: Int) {
                Log.i(TAG, "download process $process")
                listener.process(process)
            }
        })
        workThreadPool.submit(downloadTask)
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

    open interface DownLoadListener{
        fun downloadFinished(path:String,process:Int)
        fun process(process:Int)
    }
}