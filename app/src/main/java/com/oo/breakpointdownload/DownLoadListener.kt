package com.oo.breakpointdownload

interface DownLoadListener {
    fun downloadFinished(path:String)
    fun process(process:Int)
}