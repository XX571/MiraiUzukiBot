package com.xx.qqbot.utils

import java.io.File


open class Util  {

    companion object {

        open fun isEmpty(str: String?): Boolean {
            return str == null || str.length == 0
        }

        open fun getFileSize(file : File): Long {
            if (!file.exists() || !file.isFile) {
                XXLog.e("getFileSize:文件不存在:${file.name}")
                return -1
            }
            return file.length()
        }
    }


}