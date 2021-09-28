package com.xx.qqbot

import com.xx.qqbot.utils.XXLog


class MainClass constructor(firstName: String) {

    var name: String = firstName
    var url: String = ""
    var city: String = ""
    var no: Int = 100

        set(value) {
            if (value < 10) {       // 如果传入的值小于 10 返回该值
                field = value
            } else {
                field = -1         // 如果传入的值大于等于 10 返回 -1
            }
        }

    companion object {


    }


}
interface MyInterface {
    fun bar()    // 未实现
    fun foo() {  //已实现
        // 可选的方法体
        println("已实现")
    }
}
//--------------------------------------------------------------------------------------------------------
fun sum(a: Int, b: Int): Int {   // Int 参数，返回值 Int
    return a + b
}

fun getStringLength(obj: Any): Int? {
    if (obj is String) {
        // 做过类型判断以后，obj会被系统自动转换为String类型
        return obj.length
    }

    //在这里还有一种方法，与Java中instanceof不同，使用!is
    // if (obj !is String){
    //   // XXX
    // }

    // 这里的obj仍然是Any类型的引用
    return null
}
//--------------------------------------------------------------------------------------------------------
suspend fun main(args: Array<String>){

    var mainClass = MainClass("开始运行")
    System.out.println("name:"+ mainClass.name);
    XXLog.debug=true

    val bot = MiraiXXBot(账户,"pwd")

    bot.imagPath = "D:\\Sundry\\ACG图片\\给PCR群友看的\\"
//    bot.imagPath = "/Users/xx/Documents/图片/QQBotIMage/"
    bot.atMeTextPath = "reply.txt"
    bot.commonTexPath = "common.txt"
    bot.start();

}
