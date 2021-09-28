package com.xx.qqbot.message

import com.xx.qqbot.model.ImageB
import com.xx.qqbot.utils.Constant
import com.xx.qqbot.utils.FileUtil
import com.xx.qqbot.utils.XXLog
import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions
import java.io.File
import javax.imageio.ImageIO
import javax.swing.ImageIcon




open class ImageManager constructor(rootPath: String, groupId: Long){

    var enable = true
    var groupId:Long = groupId;
    var recommendType = 1;  //0默认 1最新

    private var rootPath:String = rootPath;
    private val allFiles: MutableList<ImageB> = mutableListOf()

    val unsentFiles: MutableList<ImageB> = mutableListOf()
    val sentFiles: MutableList<ImageB> = mutableListOf()

    private val configurationPath:String

    init {
        configurationPath = "${groupId}_configuration.txt";
        loadSentImages()
        loadImages()
    }

    //----------------------------------------------------------------------------------------------
    fun changePath(rootPath: String) {
        this.rootPath = rootPath
        loadImages()
    }
    fun getRotatingPicture(imageB: ImageB):String {
        var resImage = rootPath+"临时图片/ls_"+System.currentTimeMillis()+".jpg";
        val imageIcon = ImageIcon(imageB.filePath)
        try {
            //rotate(角度),正数：顺时针负数：逆时针
            Thumbnails.of(imageB.filePath)
                    .size((imageIcon.iconWidth/1f).toInt(), (imageIcon.iconHeight/1f).toInt())
                    .sourceRegion(Positions.CENTER, (imageIcon.iconWidth/1.1f).toInt(), (imageIcon.iconHeight/1.1f).toInt())
                    .rotate(180.0)
                    .watermark(Positions.BOTTOM_RIGHT, ImageIO.read(File("D:\\Sundry\\ACG图片\\给PCR群友看的\\临时图片/水印.png")),0.8f)
                    .toFile(resImage);
        }catch (e: javax.imageio.IIOException){
            XXLog.e("getRotatingPicture:$e")
        }


        return resImage
    }
    fun resetSentFile() {
        if (File(configurationPath).exists()){
            File(configurationPath).delete()
        }
        sentFiles.clear()
        loadSentImages()
        loadImages()
    }
    fun addFailFile(imageB: ImageB) {
        for (value in allFiles) {
            if (value.filePath.equals(imageB.filePath)){
                value.errorNum++
                break
            }
        }
    }
    fun printFailFile() {
        println("==========================================发不出去图片==========================================")
        for (value in allFiles) {
            if(value.errorNum>0){
                println("${value.filePath},次数${value.errorNum}")
            }
        }
        println("==============================================================================================")
    }
    fun addSentFile(fileName:ImageB): Boolean {
        unsentFiles.remove(fileName)
        sentFiles.add(fileName)
        if (sentFiles.size>=allFiles.size || unsentFiles.size<=0){
            resetSentFile()
            return true
        }else{
            //把已发送的写入文件
            if (!File(configurationPath).exists()){
                File(configurationPath).createNewFile()
            }
            File(configurationPath).appendText(fileName.filePath+"\r\n")
        }

        return false
    }
    fun loadSentImages() {
        if (!File(configurationPath).exists()){
            return
        }
        //读取已发送的图片
        val sentFileContent = File(configurationPath).readText()
        var replacedSentFileContent=FileUtil.replaceSeparator(sentFileContent)
        val sentFilesList = replacedSentFileContent!!.split("\r\n")
        sentFiles.clear()
        for (fileName in sentFilesList) {
            var imageB = ImageB();
            imageB.setPath(fileName)
            sentFiles.add(imageB)
        }

        if (sentFiles.size>0){
            val value = sentFiles.get(sentFiles.size-1)
            if (value.filePath.isEmpty()){
                sentFiles.removeAt(sentFiles.size-1)
            }
        }

    }
    fun loadImages() {
        XXLog.d("涩图库路径:$rootPath")
        allFiles.clear()
        val fileTree: FileTreeWalk = File(rootPath).walk()
        fileTree.maxDepth(1) //需遍历的目录层次为1，即无须检查子目录
                .filter { it.isFile } //只挑选文件，不处理文件夹
                .filter { it.extension in listOf("jpg", "png", "gif","jpeg") }//选择扩展名为txt或者mp4的文件
                .forEach {
                    var imageB = ImageB()
                    imageB.setPath(rootPath+it.name)
                    allFiles.add(imageB)
                }//循环 处理符合条件的文件
        sortBylastModified(allFiles)
        //去掉已发送的图片
        unsentFiles.clear()
        unsentFiles.addAll(allFiles)
        val mIterator = unsentFiles.iterator()
        while (mIterator.hasNext()) {
            val next = mIterator.next()
                for (value in sentFiles) {
                    if (value.filePath.equals(next.filePath)) {
                        mIterator.remove()
                        break
                    }
                }
        }

        if (XXLog.debug){
            for (value in sentFiles) {
                println("已发图片:${value.filePath}")
            }
            for (value in unsentFiles) {
                println("未发图片:${value.filePath}")
            }
        }

        sortBylastModified(unsentFiles)
    }

    fun getRandomImage(loadFile: Boolean): ImageB? {
        if (!enable){
            return null
        }
        if (loadFile){
            loadImages()
        }
        val randomIndex = (0..(unsentFiles.size-1)).random()
        XXLog.d("发送图片路径:${unsentFiles[randomIndex].filePath}")
        return unsentFiles[randomIndex];
    }
    fun getRandomImage(): ImageB? {
        return getRandomImage(false)
    }
    fun getRecommendImage(): ImageB? {

        if(recommendType==1){
            return getNewImage();
        }else{
            var maxIndex = allFiles.size/5
            if (maxIndex < 200 && allFiles.size>=200) {
                maxIndex = 200
            }
            val randomIndex = (0..(maxIndex)).random()
            XXLog.d("发送推荐图片路径:${allFiles[randomIndex].filePath}")
            return allFiles[randomIndex];
        }
    }

    fun getImage(path : String): String {
        return rootPath+FileUtil.replaceSeparator(path)
    }
    fun getNewImage(): ImageB? {
        if (unsentFiles.size<=0){
            return null
        }
        return unsentFiles[0];
    }
    fun sortUnsentFiles(){
        sortBylastModified(unsentFiles)
    }
    private fun sortBylastModified(imageList : MutableList<ImageB>) {

        imageList.forEachIndexed {index, value ->run{
            imageList.forEachIndexed { index, value ->
                run{
                    if(((index+1)<imageList.size) && (imageList[index+1].lastModified>=imageList[index].lastModified)){
                        imageList.swap(index,index+1)
                    }
                }
            }
        }}
    }
    private fun MutableList<ImageB>.swap(index1: Int, index2: Int) {
        val tmp = this[index1] // 'this' corresponds to the list
        this[index1] = this[index2]
        this[index2] = tmp
    }

}
//--------------------------------------------------------------------------------------------------
fun main(args: Array<String>) {

    //在该目录下走一圈，得到文件目录树结构
    System.out.println("--------------------------------------------开始运行--------------------------------------------");

    var test = ImageManager("D:\\Sundry\\ACG图片\\给PCR群友看的\\", Constant.groupList[2])
//    test.loadImages()
//    test.fileNames.forEach(::println)
//    for (i in 1..10){
//        println(test.getRandomImage());
//    }

    var imageB = test.getRandomImage()
    if (imageB != null) {
        test.getRotatingPicture(imageB)
    }
}