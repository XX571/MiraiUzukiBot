package com.xx.qqbot.model

import com.xx.qqbot.MiraiXXBot
import com.xx.qqbot.utils.Util
import net.mamoe.mirai.message.data.Message
import java.io.File
import java.util.*

open class ImageB {

    var filePath = ""

    var fileName = ""

    var lastModified: Long = 0;

    var errorNum = 0

    var groupId: Long = 0;

    var fileSize: Long = 0;  //以字节计算

    var text: String = "";

    //--------------------------------------------------------------------------------------------------
    fun setPath(path: String) {
        filePath = path
        val file = File(filePath)
        lastModified = getLastModified(file)
        fileSize = Util.getFileSize(file)
        fileName = file.getName();

        //text
        val start = fileName.indexOf('[')
        val end = fileName.indexOf(']')
        if (start >= 0 && end > (start + 1)) {
            text = fileName.substring(start + 1, end)
        }
    }

    fun getLastModified(file: File): Long {

        val lastModified = file.lastModified()
//        System.out.println(path+" 修改时间:"+lastModified)
        return lastModified
    }

    fun getRarity(): String {
        if (fileName.startsWith("SSR")) {  //fileSize>6000*1024
            return "★★★SSR(传说稀有)★★★"
        } else if (fileName.startsWith("SR") || fileSize > 2370 * 1024) {
            return "★★SR(超稀有)★★"
        } else if (fileSize > 720 * 1024) {
            return "★R(稀有)★"
        }
        return "N(普通)"
    }
}