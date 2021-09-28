package com.xx.qqbot.utils

import com.xx.qqbot.MiraiXXBot
import net.mamoe.mirai.message.data.Message

open class XXLog  {

    companion object {
        var debug = false

        public fun d(str : String) {
            if (debug){
                println(str)
            }
        }

        public fun e(str : String) {
            if (debug){
                println("报错了:$str")
            }
        }
    }


}