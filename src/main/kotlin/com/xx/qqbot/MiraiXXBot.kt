package com.xx.qqbot

import com.xx.qqbot.message.ImageManager
import com.xx.qqbot.message.ReplyManager
import com.xx.qqbot.model.ImageB
import com.xx.qqbot.utils.Constant
import com.xx.qqbot.utils.Util
import com.xx.qqbot.utils.XXLog
import net.mamoe.mirai.Bot
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MemberJoinEvent
import net.mamoe.mirai.event.events.MemberLeaveEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.utils.BotConfiguration
import net.mamoe.mirai.utils.ExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.io.File
import java.time.LocalDateTime


class MiraiXXBot {

    private var failImageB: ImageB = ImageB()
    private var run = true

    @Volatile
    private var runSendImage = 0    // 1正在发图片
    private var good_morning = false    //早安

    var qqId: Long = 0L//Bot的QQ号，需为Long类型，在结尾处添加大写L
    var password: String = ""//Bot的密码
    var imagPath = "图片/"
    var atMeTextPath = "special\\reply.txt"
    var commonTexPath = "special\\common.txt"
    lateinit var miraiBot: Bot;

    //复读功能
    val rereadMax = 6;
    var rereadCount = 0
    var rereadContent: MessageChain? = null

    val regexImage = Regex("不够[涩瑟色]|[涩瑟色]图|来一?[点份张].*[涩瑟色]|再来[点份张]|看过了|铜")

    var imageManagers: MutableList<ImageManager> = mutableListOf()
    var replyManagers: MutableList<ReplyManager> = mutableListOf()

    constructor (qqId: Long, password: String) {
        // 初始化...
        this.qqId = qqId;
        this.password = password;

    }

    //----------------------------------------------------------------------------------------------
    private fun getReplyManager(groupId: Long): ReplyManager {
        for (value in replyManagers) {
            if (value.groupId == groupId) {
                return value;
            }
        }
        return ReplyManager(atMeTextPath, commonTexPath, Constant.groupList[0]);
    }

    private fun getImageUtil(groupId: Long): ImageManager {
        for (value in imageManagers) {
            if (value.groupId == groupId) {
                return value;
            }
        }
        return ImageManager(imagPath, Constant.groupList[0]);
    }

    fun close() {
        miraiBot.close();
        println("--------------------------------------------退出--------------------------------------------");
    }

    private fun haskeys(message: MessageChain, vararg keys: String): Boolean {
        var res = false
        if (keys.size == 1) {
            val regexImage = Regex(keys[0])
            res = regexImage.containsMatchIn(message.content);
        } else {
            for (str in keys) {
                if (message.content.contains(str)) {
                    res = true
                    break
                }
            }
        }

        return res;
    }

    private suspend fun needImage(key: String, groupId: Long): Boolean {
        if (runSendImage > 0) {
            if ((runSendImage - 1) % 3 == 0) {
                miraiBot.getGroup(groupId)?.sendMessage("你们冲的太快啦,请稍后再冲")
            }
            runSendImage++
            return false
        }
        return regexImage.containsMatchIn(key);
    }

    private fun isRelatedToSelf(self: Bot, message: MessageChain): Boolean {
        return message.any {
            when (it) {
                is At -> it.target == self.id
                is QuoteReply -> it.source.fromId == self.id
                else -> false
            }
        }
    }

    private suspend fun sendImageFail(message: GroupMessageEvent, image: ImageB) {

        if (image.errorNum > 0) {
            var num = image.errorNum + 1
            message.group.sendMessage("涩图太涩,发不出去嘞(这张图已经${num}次没发出去了)")
        } else {
            message.group.sendMessage("涩图太涩,发不出去嘞......")
        }

        failImageB = image;
        failImageB.groupId = message.group.id

        getImageUtil(message.group.id).addFailFile(image)
    }

    private suspend fun sendScheduleImage(groupId: Long, current: LocalDateTime) {
        var imageUtil = getImageUtil(groupId)
        val image = imageUtil.getRecommendImage()
        if (image != null) {
//            try {
//                miraiBot.getGroup(groupId)?.sendImage(File(image.filePath))
//                imageUtil.addSentFile(image)
//            } catch (e: java.lang.Exception) {
//                image.lastModified = 0;
//                imageUtil.sortUnsentFiles()
//                XXLog.e("发图报错:$e:${image.filePath}")
//            }
            sendImageWithText(miraiBot.getGroup(groupId), image, imageUtil)
        }
    }

    //发送消息链
    private suspend fun sendImageWithText(subject: Group?, image: ImageB, imageManager: ImageManager) {
        if (image != null && subject!=null) {
            try {
                var externalResource: ExternalResource = File(image.filePath).toExternalResource()
                var uploadImage = subject.uploadImage(externalResource)
                var chain: MessageChain
                if (image.text.isNullOrEmpty()) {
                    chain = buildMessageChain {
                        +uploadImage
                    }
                } else {
                    chain = buildMessageChain {
                        +uploadImage
                        +image.text
                    }
                }
                subject.sendMessage(chain)
                imageManager.addSentFile(image)
                externalResource.close()
            } catch (e: java.lang.IllegalStateException) {
                image.lastModified = 0;
                imageManager.sortUnsentFiles()
                XXLog.e("sendImageWithText报错:$e:${image.filePath}")
            }
        }
    }

    private suspend fun sendImageWithRarity(subject: Group, imageManager: ImageManager, event: GroupMessageEvent) {
        val image = imageManager.getRandomImage(false);

        if (image != null) {
            try {
//                subject.sendImage(File(image.filePath))

                var externalResource: ExternalResource = File(image.filePath).toExternalResource()
                var uploadImage = subject.uploadImage(externalResource)
                var plainText = PlainText(image.text + "\r\n稀有度: ${image.getRarity()}")
                var chain: MessageChain
                if (image.getRarity().contains("SSR")) {
                    chain = buildMessageChain {
                        add(At(event.sender)) // `+` 和 `add` 作用相同
                        +PlainText(" 恭喜您,中奖了!")
                        +uploadImage
                        +plainText
                    }
                } else {
                    chain = buildMessageChain {
                        +uploadImage
                        +plainText
                    }
                }

                subject.sendMessage(chain)
                imageManager.addSentFile(image)
                externalResource.close()
            } catch (e: java.lang.IllegalStateException) {
                sendImageFail(event, image)
                XXLog.e("sendImageWithRarity报错:$e:${image.filePath}")
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    suspend fun start() {
        for (groupId in Constant.groupList) {
            var imageManager: ImageManager
            imageManager = ImageManager(imagPath, groupId)

            imageManagers.add(imageManager)
            var replyManager = ReplyManager(atMeTextPath, commonTexPath, groupId)
            replyManagers.add(replyManager)
        }
        //新建Bot并登录
        // 使用自定义配置
        miraiBot = BotFactory.newBot(qqId, password) {
            fileBasedDeviceInfo()
            protocol = BotConfiguration.MiraiProtocol.ANDROID_PAD // 切换协议
            cacheDir = File("cache") // 最终为 workingDir 目录中的 cache
            heartbeatStrategy = BotConfiguration.HeartbeatStrategy.STAT_HB

        }.alsoLogin()

//        miraiBot.getFriend(571529359L)?.sendMessage("Hello, XX!")

        miraiBot.eventChannel.subscribeAlways<GroupMessageEvent> { event ->

            //黑名单
            if (Constant.blackList.contains(sender.id)) {
                return@subscribeAlways
            }

            //获取每个群独立的能力

            var imageUtil = getImageUtil(event.group.id)
            var replyManager = getReplyManager(event.group.id)

            if (!good_morning) {
                val message = event.message
                //good_morning
                val current = LocalDateTime.now()
                if (current.hour in 6..10) {
                    good_morning = true
                    event.group.sendImage(File("special/早安.jpg"))
                    XXLog.d("发送早安图片")
                }
            }

            // 处理@自己消息
            if (isRelatedToSelf(event.bot, message)) {

//                XXLog.debug("@自己:$message")
                if (haskeys(message, "你好", "你好!")) {
                    subject.sendMessage(message.quote() + "你好!") // 引用收到的消息并回复 "Hi!", 也可以添加图片等更多元素.
                } else if (haskeys(message, "涩图状态", "色图状态")) {
                    subject.sendMessage("已发图片:" + imageUtil.sentFiles.size + ",剩余图片:" + imageUtil.unsentFiles.size)
                } else if (haskeys(message, "插嘴频率增加")) {
                    replyManager.interruptFrequency -= 5;
                    if (replyManager.interruptFrequency < 1) {
                        replyManager.interruptFrequency = 1;
                    }
                    subject.sendMessage("当前插嘴频率为1/" + (replyManager.interruptFrequency + 1))
                } else if (haskeys(message, "插嘴频率减少")) {
                    replyManager.interruptFrequency += 5;
                    subject.sendMessage("当前插嘴频率为1/" + (replyManager.interruptFrequency + 1))
                } else if (message.content.contains("插嘴频率")) {
                    val index = message.content.indexOf("插嘴频率") + "插嘴频率".length
                    var numStr = message.content.substring(index, message.content.length)
                    numStr = numStr.trim()
                    XXLog.d("插嘴频率:$numStr")
                    val num = numStr.toInt()
                    replyManager.interruptFrequency = num;
                    if (replyManager.interruptFrequency < 0) {
                        replyManager.interruptFrequency = 0;
                    }
                    subject.sendMessage("当前插嘴频率为1/" + (replyManager.interruptFrequency + 1))
                } else if (message.content.contains("推荐模式") && event.group.id == Constant.groupList[0]) {
                    val typeStr = "推荐模式"
                    val index = message.content.indexOf(typeStr) + typeStr.length
                    var numStr = message.content.substring(index, message.content.length)
                    numStr = numStr.trim()
                    val num = numStr.toInt()
                    imageUtil.recommendType = num;
                    if (imageUtil.recommendType < 0) {
                        imageUtil.recommendType = 0;
                    }
                    if (imageUtil.recommendType >= Constant.recommendTypeList.size) {
                        imageUtil.recommendType = Constant.recommendTypeList.size - 1;
                    }
                    subject.sendMessage("当前${typeStr}为" + Constant.recommendTypeList[imageUtil.recommendType])
                } else if (sender.id == 571529359L) {     //指定成员指令
                    XXLog.d("指定成员指令:" + sender.id)
                    if (haskeys(message, "关机")) {
                        subject.sendMessage("关机中...")
                        run = false
                        close()
                    } else if (haskeys(message, "刷新图库")) {
                        imageUtil.loadImages()
                        subject.sendMessage("图库已刷新")
                    } else if (haskeys(message, "刷新文本")) {
                        replyManager.loadTexts()
                        subject.sendMessage("文本已刷新")
                    } else if (haskeys(message, "关闭涩图")) {
                        imageUtil.enable = false
                        subject.sendMessage("涩图功能已关闭")
                    } else if (haskeys(message, "打开涩图")) {
                        imageUtil.enable = true
                        subject.sendMessage("涩图功能已开启")
                    } else if (haskeys(message, "重置涩图")) {
                        imageUtil.resetSentFile()
                        subject.sendMessage("已发图片已清空")
                        subject.sendMessage("已发图片:" + imageUtil.sentFiles.size + ",剩余图片:" + imageUtil.unsentFiles.size)
                    } else if (haskeys(message, "失败图片|失败")) {
                        imageUtil.printFailFile()
                    } else {
                        //---------------------通用发消息代码-------------------------------
                        val replyData = replyManager.getRandomReplyAtMe(message.content);
                        if (Util.isEmpty(replyData)) {
                        } else {
                            if (replyData.startsWith("repeat://")) {
                                subject.sendMessage(event.message)
                            } else if (replyData.startsWith("image://")) {
                                var path = replyData.substring(8)
                                subject.sendImage(File(replyManager.getEmotionImage(path)))
                            } else {   // 普通文本消息
                                subject.sendMessage(message.quote() + replyData)
                            }
                        }
                    }

                } else {        //一般@自己消息

                    //---------------------通用发消息代码-------------------------------
                    val replyData = replyManager.getRandomReplyAtMe(message.content);
                    if (Util.isEmpty(replyData)) {
                    } else {

                        if (replyData.startsWith("repeat://")) {
                            subject.sendMessage(event.message)
                        } else if (replyData.startsWith("image://")) {
                            var path = replyData.substring(8)
                            subject.sendImage(File(replyManager.getEmotionImage(path)))
                        } else {   // 普通文本消息
                            subject.sendMessage(message.quote() + replyData)
                        }
                    }
                    //----------------------------------------------------
                }

            } else {

                if (haskeys(message, "剩余涩图|剩余色图")) {   // 一般关键字消息
                    subject.sendMessage("已发图片:" + imageUtil.sentFiles.size + ",剩余图片:" + imageUtil.unsentFiles.size)
                } else if (haskeys(message, "新[涩瑟色]图") && runSendImage <= 0) {   //新涩图
                    runSendImage = 1
                    val image = imageUtil.getNewImage()
                    if (image != null) {
//                        try {
//                            subject.sendImage(File(image.filePath))
//                            imageUtil.addSentFile(image)
//                        } catch (e: java.lang.IllegalStateException) {
//                            image.lastModified = 0;
//                            imageUtil.sortUnsentFiles()
//                            sendImageFail(event, image)
//                            XXLog.e("发图报错:$e:${image.filePath}")
//                        }
                        sendImageWithText(event.group, image, imageUtil)
                    }
                    runSendImage = 0
                } else if (haskeys(message, "批量[涩瑟色]图|[涩瑟色]图十连")) {     //批量涩图
                    runSendImage = 1
                    if (imageUtil.unsentFiles.size <= 0) {
                        subject.sendMessage("---------------涩图发完了,现在从头开始发---------------")
                        imageUtil.resetSentFile()
                    }
                    for (index in 1..10) {
                        sendImageWithRarity(subject, imageUtil, event)
                    }
                    runSendImage = 0
                } else if (needImage(event.message.content, event.group.id)) {     //涩图
                    runSendImage = 1
                    if (imageUtil.unsentFiles.size <= 0) {
                        subject.sendMessage("---------------涩图发完了,现在从头开始发---------------")
                        imageUtil.resetSentFile()
                    }
                    sendImageWithRarity(subject, imageUtil, event)

                    runSendImage = 0
                } else if (haskeys(message, "失败图片|失败")) {
                    imageUtil.printFailFile()
                } else if (haskeys(message, "开启R18|开启r18|R18开启|r18开启") && event.group.id == Constant.groupList[0]) {
                    imageUtil.changePath("D:\\Sundry\\ACG图片\\给PCR群友看的\\R18\\")
//                    subject.sendMessage("---------------暂时开启不了R18模式---------------")
                } else if (haskeys(message, "关闭R18|关闭r18|R18关闭|r18关闭") && event.group.id == Constant.groupList[0]) {
                    imageUtil.changePath(imagPath)
                    subject.sendMessage("---------------关闭R18模式---------------")
                } else if (!event.message.content.isNullOrEmpty()) {    // 一般其它消息

                    var replyData = replyManager.getRandomReply(event.message.content)
                    if (!Util.isEmpty(replyData)) {
                        //---------------------通用发消息代码,不知道怎么封装-------------------------------
                        if (replyData.startsWith("repeat://")) {
                            subject.sendMessage(event.message)
                        } else if (replyData.startsWith("image://")) {
                            var path = replyData.substring(8)
                            subject.sendImage(File(replyManager.getEmotionImage(path)))
                        } else {   // 普通文本消息
                            subject.sendMessage(replyData)
                        }
                        //----------------------------------------------------
                    } else {
                        //复读功能
                        XXLog.d("复读功能:${rereadContent},$rereadCount,${event.message}")
                        if (rereadContent == null) {
                            rereadContent = event.message
                        } else if (rereadCount <= 0) {
                            rereadContent = event.message
                            rereadCount++
                        } else if (rereadContent != null && rereadContent!!.contentEquals(event.message.content, true)
                            && !event.message.toString().contains("mirai:image:")
                        ) {
                            rereadCount++
                            if (rereadCount > 2 && rereadCount < rereadMax) {
                                val randoms = (0..(rereadMax - rereadCount)).random()
                                if (randoms == 0) {
                                    subject.sendMessage(event.message)
                                    rereadCount = 0
                                }
                            } else if (rereadCount > rereadMax) {
                                subject.sendMessage(event.message)
                                rereadCount = 0
                            }
                        } else {
                            rereadCount = 0;
                        }
                    }

                }

            }
        }
        //成员已经加入群
        miraiBot.eventChannel.subscribeAlways<MemberJoinEvent> {
            onMemberJoinEvent(it)
        }
        miraiBot.eventChannel.subscribeAlways<MemberLeaveEvent> {
            onMemberLeaveEvent(it)
        }

        var interval = 999L;
        while (run) {
            val current = LocalDateTime.now()
            if (good_morning) {
                if (current.hour == 5 && good_morning) {
                    good_morning = false;
                }
            }
            //定时涩图
            if (current.hour > 8 || current.hour < 3) {
                if (current.minute == 0 && current.second == 0) {
                    sendScheduleImage(Constant.groupList[0], current)
                }
            }
            if (runSendImage > 0) {
                Thread.sleep(interval)
                runSendImage--;
            }

            //发送失败图片
            if (failImageB.filePath.isNotEmpty()) {
                try {
                    XXLog.d("尝试发送发不出图片:${failImageB!!.filePath}")
                    miraiBot.getGroup(failImageB.groupId)
                        ?.sendImage(File(getImageUtil(failImageB.groupId).getRotatingPicture(failImageB!!)))
                    getImageUtil(failImageB.groupId).addSentFile(failImageB!!)
                    miraiBot.getGroup(failImageB.groupId)?.sendMessage("发出去了...")
                } catch (e: java.lang.IllegalStateException) {
                    XXLog.e("发图又报错了:$e:${failImageB!!.filePath}")
                }
                failImageB = ImageB()
            }

            Thread.sleep(interval)
        }

//        miraiBot.join() // 等待 Bot 离线, 避免主线程退出
    }

    suspend fun onMemberJoinEvent(event: MemberJoinEvent) {
        miraiBot.getGroup(event.group.id)
            ?.sendMessage("------------${event.member.nameCardOrNick}(${event.member.id})进群了------------")
//        MasterLogger.sendLog(event.bot, "------------${event.member.nameCardOrNick}(${event.member.id})进群了------------")
    }

    suspend fun onMemberLeaveEvent(event: MemberLeaveEvent) {
        miraiBot.getGroup(event.group.id)?.sendMessage("${event.member.nameCardOrNick}(${event.member.id})跑路了......")
//        MasterLogger.sendLog(event.bot, "${event.member.nameCardOrNick}(${event.member.id})跑路了......")
    }
}
//--------------------------------------------------------------------------------------------------
