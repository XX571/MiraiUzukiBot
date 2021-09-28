package com.xx.qqbot.utils

import java.io.File
import java.io.FileInputStream

import java.io.FileOutputStream




class FileUtil {


    //保存文本文件
    fun saveText(path: String?, txt: String) {
        try {
            val fos = FileOutputStream(path)
            fos.write(txt.toByteArray())
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //读取文本文件
    fun openText(path: String?): String? {
        var readStr: String? = ""
        try {
            val fis = FileInputStream(path)
            val b = ByteArray(fis.available())
            fis.read(b)
            readStr = String(b)
            fis.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return readStr
    }
    companion object {
        //Windows和Linux路径分隔符通用书写方法
        fun replaceSeparator(path: String): String? {
            var pathNew = path
            if (path.contains("\\")){
                pathNew = path.replace("\\", File.separator)
            }else if (path.contains("/")){
                pathNew = path.replace("/", File.separator)
            }
            return pathNew
        }

    }

}