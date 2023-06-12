package com.hifnawy.bootanimationplayer

import net.lingala.zip4j.ZipFile
import processing.core.PApplet
import processing.core.PImage
import java.io.File
import java.text.DecimalFormat


class Sketch
constructor() : PApplet() {
    private lateinit var zipFile: File

    internal enum class TextAlignment {
        LEFT, RIGHT, CENTER
    }

    var animationWidth = 0
    var animationHeight = 0
    var animationFrameRate = 0
    var animationFrameTime = 0
    var animationXOffset = 0f
    var animationYOffset = 0f
    var descWidth = 0
    var descHeight = 0

    var partIndex = 0
    var partImageIndex = 0
    var images: ArrayList<ArrayList<PImage>> = ArrayList()

    constructor(zipFile: File) : this() {
        this.zipFile = zipFile
    }

    override fun settings() {
        val unzipFile = ZipFile(zipFile.path)
        val animationData = File(zipFile.absolutePath.replace(".${zipFile.extension}", ""))

        if (!animationData.exists()) {
            animationData.mkdir()
            unzipFile.extractAll(animationData.absolutePath)
        }

        val descFile = File("${animationData.absolutePath}/desc.txt")
        val lines = loadStrings(descFile)

        val parameters = lines[0].split(" ".toRegex()).toTypedArray()

        descWidth = parameters[0].toInt()
        descHeight = parameters[1].toInt()
        animationFrameRate = parameters[2].toInt()

        animationWidth = 800
        animationHeight = animationWidth * descHeight / descWidth

        animationFrameTime = 1000 / animationFrameRate

        println("width, height: ", parameters[0].toInt(), parameters[1].toInt())
        println("animation width, animation height: ", animationWidth, animationHeight)

        size(displayWidth, displayHeight)
    }

    override fun setup() {
        val animationData = File(zipFile.absolutePath.replace(".${zipFile.extension}", ""))

        animationData.listFiles()!!.forEach { file ->
            if (file.isDirectory) {
                println("loading " + file.path + "/*.png")
                val partImages: ArrayList<PImage> = ArrayList()
                file.listFiles()!!.forEach { partFile ->
                    if (partFile.extension == "png") {
                        val scaleX = descWidth * 1.0f / animationWidth
                        val scaleY = descHeight * 1.0f / animationHeight

                        val partImage = loadImage(partFile.path)
                        partImage.resize(
                            (partImage.width / scaleX).toInt(), (partImage.height / scaleY).toInt()
                        )

                        partImages.add(partImage)
                    }
                }
                images.add(partImages)
                println("DONE! Processed " + partImages.size.toString() + " images!")
            }
        }
        println()
        println("animation frame rate: $animationFrameRate")
        println("animation frame time: $animationFrameTime")

        animationXOffset = (displayWidth / 2 - images[0][0].width / 2).toFloat()
        animationYOffset = (displayHeight / 2 - images[0][0].height / 2).toFloat()

        frameRate(animationFrameRate.toFloat())
    }

    override fun draw() {
        background(0)

        val image = images[partIndex][partImageIndex]
        image(image, animationXOffset, animationYOffset)

        partImageIndex = ++partImageIndex % images.get(partIndex).size;
        partIndex = if (partImageIndex == 0) ++partIndex % images.size else partIndex;

        if ((partIndex == 0) && (partImageIndex == 0)) noLoop()

        fill(0f, 255f, 0f)
        textSize(64f)

        text("FPS: " + DecimalFormat("#0.00").format(frameRate), 20f, 70f, TextAlignment.RIGHT)
        text("Part: $partIndex", 20f, 140f, TextAlignment.RIGHT)
        text("Image: $partImageIndex", 20f, 210f, TextAlignment.RIGHT)
    }

    override fun mousePressed() {
        partIndex = 0
        partImageIndex = 0

        loop()
    }

    private fun text(
        text: String,
        @Suppress("SameParameterValue") x: Float,
        y: Float,
        @Suppress("SameParameterValue") alignment: TextAlignment
    ) {
        val textWidth = textWidth(text)
        when (alignment) {
            TextAlignment.LEFT -> text(text, x, y)
            TextAlignment.RIGHT -> text(text, displayWidth - textWidth - x, y)
            TextAlignment.CENTER -> text(text, x + displayWidth / 2 - textWidth / 2 - x, y)
        }
    }
}