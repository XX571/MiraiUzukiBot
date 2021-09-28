package com.xx.qqbot.message

import com.google.gson.GsonBuilder
import com.google.gson.internal.Streams
import com.xx.qqbot.model.ReplyRootB
import com.xx.qqbot.model.TextsB
import com.xx.qqbot.utils.FileUtil
import com.xx.qqbot.utils.Util
import com.xx.qqbot.utils.XXLog
import java.io.*
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths


open class ReplyManager constructor(textFilePath: String,commonTexPath: String,groupId: Long) {

    var groupId:Long = groupId;

    private var textFilePath: String = textFilePath;
    private var commonTexPath: String = commonTexPath;
    var textList: MutableList<TextsB> = mutableListOf()
    var commonTextList: MutableList<TextsB> = mutableListOf()
    var interruptFrequency = 999;

    private val gson = GsonBuilder()
            .setLenient()
            .setPrettyPrinting()
            .create()

    init {
        loadAtMe(textFilePath)
        loadCommon(commonTexPath)
    }

    //--------------------------------------------------------------------------------------------------
    private fun getRandomReply(recvText: String, textList: MutableList<TextsB>,isAtme:Boolean): String {
        var res = ""

        var hasReply = false
        for (i in 1..(textList.size - 1)) {
            val textsB = textList[i];
            val randoms = (0..(textsB.replys.size - 1)).random()
            if (!Util.isEmpty(textsB.contentRegex)) {      //根据正则表达式判断
                val regex = Regex(textsB.contentRegex)
                if (regex.containsMatchIn(recvText)) {
                    res = textsB.replys[randoms]
                    hasReply = true
                    break
                }
            }
            if (textsB.content.equals(recvText)) {
                res = textsB.replys[randoms]
                hasReply = true
                break
            }
        }



        if (!hasReply) {
            if(isAtme){     //@自己的消息
                val textsB = textList[0];
                val randoms = (0..(textsB.replys.size - 1)).random()
                res = textsB.replys[randoms]
            }else{      //普通消息.有一定几率插嘴
                val interrupt = (0..interruptFrequency).random()
                if(interrupt==1){
                    val textsB = textList[0];
                    val randoms = (0..(textsB.replys.size - 1)).random()
                    res = textsB.replys[randoms]
                }
                XXLog.d("插嘴功能:${interruptFrequency},$interrupt,${res}")
            }
        }

        if (!Util.isEmpty(res)){
            XXLog.d("回复内容:$res")
        }

        return res;
    }

    fun getRandomReply(recvText: String): String {
        return getRandomReply(recvText, commonTextList,false)
    }

    fun getRandomReplyAtMe(recvText: String): String {
        return getRandomReply(recvText, textList,true)
    }

    fun loadTexts() {
        loadAtMe(textFilePath)
        loadCommon(commonTexPath)
    }
    fun getEmotionImage(path : String): String {
        return "./"+ FileUtil.replaceSeparator(path)
    }
    //--------------------------------------------------------------------------------------------------
    private fun loadAtMe(path:String) {

        val stream = Files.newInputStream(Paths.get(path))
        stream.buffered().reader().use {
//            println("读取文件:"+it.readText())
            var replyRootB: ReplyRootB = gson.fromJson(it.readText(), ReplyRootB::class.javaObjectType)
            textList = replyRootB.texts

//            for (i in 0..(textList.size-1)){
//                val textsB = textList[i];
//                XXLog.d("textsB.content:"+textsB.content)
//            }
        }
    }
    private fun loadCommon(path:String) {

        val stream = Files.newInputStream(Paths.get(path))
        stream.buffered().reader().use {
//            println("读取文件:"+it.readText())
            var replyRootB: ReplyRootB = gson.fromJson(it.readText(), ReplyRootB::class.javaObjectType)
            commonTextList = replyRootB.texts

//            for (i in 0..(textList.size-1)){
//                val textsB = textList[i];
//                XXLog.d("textsB.content:"+textsB.content)
//            }
        }
    }
}