package com.hifnawy.bootanimationplayer

import android.content.Context
import com.hifnawy.bootanimationplayer.settingsDataStorage.SettingsDataStore
import processing.core.PApplet
import processing.core.PImage
import java.io.File
import java.text.DecimalFormat


class Sketch(
    context: Context,
    var animationFolder: File
) : PApplet() {
    private var resolutionScale: Float = 0f
    private var animationWidth = 0
    private var animationHeight = 0
    private var animationFrameRate = 0
    private var animationFrameTime = 0
    private var animationXOffset = 0f
    private var animationYOffset = 0f
    private var descWidth = 0
    private var descHeight = 0

    private var partIndex = 0
    private var partImageIndex = 0
    private var images: ArrayList<ArrayList<PImage>> = ArrayList()

    private var settings = SettingsDataStore(context)

    init {
        resolutionScale = settings.resolutionScale
        animationFrameRate = settings.fps

        resolutionScale = 1.0f
    }

    override fun settings() {
        val descFile = File("${this.animationFolder}/desc.txt")

        if (descFile.exists()) {
            val lines = loadStrings(descFile)
            val parameters = lines[0].split(" ")

            descWidth = parameters[0].toInt()
            descHeight = parameters[1].toInt()

            if (animationFrameRate == 0) {
                animationFrameRate = parameters[2].toInt()
            }

            animationWidth =
                if (resolutionScale > 0) (descWidth * resolutionScale).toInt() else descWidth
            animationHeight = animationWidth * descHeight / descWidth

            animationFrameTime = 1000 / animationFrameRate

            println("width: $descWidth, height: $descHeight")
            println("animation width: $animationWidth, animation height: $animationHeight")

            size(displayWidth, displayHeight)
        } else {
            println("${descFile.path} doesn't exist!")
            noLoop()
        }
    }

    override fun setup() {
        animationFolder.listFiles()!!.sorted().forEach { file ->
            if (file.isDirectory) {
                println("loading ${file.path}/*.png")
                val partImages: ArrayList<PImage> = ArrayList()

                file.listFiles()!!.sorted().forEach { partFile ->
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
                println("DONE! Processed ${partImages.size} images!")
            }
        }

        println("\nanimation frame rate: $animationFrameRate")
        println("animation frame time: $animationFrameTime")

        animationXOffset = (displayWidth / 2 - images[0][0].width / 2).toFloat()
        animationYOffset = (displayHeight / 2 - images[0][0].height / 2).toFloat()

        animationFolder.deleteRecursively()
        frameRate(animationFrameRate.toFloat())
        // frameRate(5f)
    }

    override fun draw() {
        background(0)

        val image = images[partIndex][partImageIndex]
        image(image, animationXOffset, animationYOffset)

        partImageIndex = ++partImageIndex % images[partIndex].size
        partIndex = if (partImageIndex == 0) ++partIndex % images.size else partIndex

        if ((partIndex == 0) && (partImageIndex == 0)) noLoop()

        textSize(64f)
        text(
            "FPS: ${DecimalFormat("#0.00").format(frameRate)}",
            20f,
            70f,
            TextAlignment.RIGHT,
            if ((animationFrameRate - frameRate) >= 30) TextType.WARN else TextType.INFO
        )
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
        @Suppress("SameParameterValue") alignment: TextAlignment,
        type: TextType = TextType.INFO
    ) {
        when (type) {
            TextType.INFO -> fill(0f, 255f, 0f)
            TextType.DEBUG -> fill(0f, 255f, 255f)
            TextType.WARN -> fill(255f, 255f, 0f)
            TextType.ERROR -> fill(255f, 0f, 0f)
        }
        val textWidth = textWidth(text)
        when (alignment) {
            TextAlignment.LEFT -> text(text, x, y)
            TextAlignment.RIGHT -> text(text, displayWidth - textWidth - x, y)
            TextAlignment.CENTER -> text(text, x + displayWidth / 2 - textWidth / 2 - x, y)
        }
    }

    internal enum class TextAlignment {
        LEFT, RIGHT, CENTER
    }

    internal enum class TextType {
        INFO,
        DEBUG,
        WARN,
        ERROR
    }
}