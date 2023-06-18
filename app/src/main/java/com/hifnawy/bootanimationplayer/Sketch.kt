package com.hifnawy.bootanimationplayer

import android.app.Dialog
import android.view.LayoutInflater
import android.view.Window
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.hifnawy.bootanimationplayer.databinding.LoadingDialogBinding
import com.hifnawy.bootanimationplayer.settingsDataStorage.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import processing.core.PApplet
import processing.core.PImage
import java.io.File
import java.text.DecimalFormat


class Sketch(
    private val fragment: Fragment, private val file: File
) : PApplet() {
    private val loadingDialog: Dialog
    private val loadingDialogBinding: LoadingDialogBinding

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
    private var settings = SettingsDataStore(fragment.requireContext())

    init {
        resolutionScale = settings.resolutionScale
        animationFrameRate = settings.fps

        resolutionScale = 1.0f

        loadingDialogBinding = LoadingDialogBinding.inflate(
            LayoutInflater.from(fragment.requireContext()), null, false
        )
        loadingDialog = Dialog(fragment.requireContext())
    }

    override fun settings() {
        // fullScreen(P2D)
        size(displayWidth, displayHeight)
    }

    override fun setup() {
        noLoop()

        loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        loadingDialog.setContentView(loadingDialogBinding.root)
        loadingDialog.setCancelable(false)
        loadingDialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout((displayWidth * 0.7f).toInt(), (displayHeight * 0.2f).toInt())
        }
        loadingDialogBinding.message.text = fragment.getString(R.string.loading_file, file.path)

        val extractedFile = File(file.path.replace(".zip", ""))

        fragment.lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                loadingDialog.show()
            }

            withContext(Dispatchers.IO) {
                if (extractedFile.exists()) {
                    extractedFile.deleteRecursively()
                }

                withContext(Dispatchers.Main) {
                    loadingDialogBinding.message.text =
                        fragment.getString(R.string.extracting_file, file.name)
                }

                ZipFile(file).extractAll(extractedFile.path)

                withContext(Dispatchers.Main) {
                    loadingDialogBinding.message.text =
                        fragment.getString(R.string.reading_file, "${extractedFile.name}/desc.txt")
                }

                val descFile = File("${extractedFile.path}/desc.txt")

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
                } else {
                    println("${descFile.path} doesn't exist!")
                    noLoop()
                    loadingDialog.dismiss()
                }

                extractedFile.listFiles()!!.sorted().forEach { file ->
                    if (file.isDirectory) {
                        println("loading ${file.path}/*.png")
                        val partImages: ArrayList<PImage> = ArrayList()

                        file.listFiles()!!.sorted().forEach { partFile ->
                            if (partFile.extension == "png") {
                                withContext(Dispatchers.Main) {
                                    loadingDialogBinding.message.text = fragment.getString(
                                        R.string.loading_image,
                                        "${file.name}/${partFile.name}"
                                    )
                                }

                                val scaleX = descWidth * 1.0f / animationWidth
                                val scaleY = descHeight * 1.0f / animationHeight

                                val partImage = loadImage(partFile.path)
                                partImage.resize(
                                    (partImage.width / scaleX).toInt(),
                                    (partImage.height / scaleY).toInt()
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

                extractedFile.listFiles()!!.apply {
                    for (index in size - 1 downTo 0) {
                        val file = get(index)

                        if (file.isDirectory) {
                            file.listFiles()!!.apply {
                                for (folderIndex in size - 1 downTo 0) {
                                    val folderFile = get(folderIndex)
                                    withContext(Dispatchers.Main) {
                                        loadingDialogBinding.message.text =
                                            fragment.getString(
                                                R.string.deleting_file,
                                                "${file.name}/${folderFile.name}"
                                            )
                                    }
                                    folderFile.delete()
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                loadingDialogBinding.message.text = fragment.getString(
                                    R.string.deleting_file,
                                    file.name
                                )
                            }

                            file.delete()
                        }

                    }
                }

                withContext(Dispatchers.Main) {
                    loadingDialogBinding.message.text = fragment.getString(
                        R.string.deleting_file,
                        extractedFile.name
                    )
                }
                extractedFile.deleteRecursively()
                frameRate(animationFrameRate.toFloat())
                // frameRate(5f)
            }

            withContext(Dispatchers.Main) {
                loadingDialog.dismiss()
                loop()
            }
        }
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
        INFO, DEBUG, WARN, ERROR
    }
}