package com.android.instagramportfolio.extension

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.android.instagramportfolio.model.ResultSlide
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

// result slide의 정보를 이용해 내부 pdf 파일의 위치를 알리는 File 객체를 리턴
fun ResultSlide.getFileOfPdf(context: Context): File {
    val cw = ContextWrapper(context)
    var dir = cw.getDir("slides", Context.MODE_PRIVATE)
    dir = File(dir, "id_$id")
    dir = File(dir, "0.pdf")
    return dir
}

// result slide의 정보를 이용해 내부 이미지 파일들을 bitmap 객체로 리턴
fun ResultSlide.getFileAsImages(context: Context): MutableList<Bitmap> {
    val bitmaps = arrayListOf<Bitmap>()

    for (index in 0 until size) {
        val bitmap = getBitmap(context, "slides", "id_$id", "$index", format)
        bitmaps.add(bitmap)
    }

    return bitmaps
}

// 내부 저장소에서 이미지 하나 불러오기
fun getBitmap(
    context: Context,
    directory: String,
    innerDirectory: String,
    name: String,
    extension: String,
): Bitmap {
    val cw = ContextWrapper(context)
    var cacheDir = cw.getDir(directory, Context.MODE_PRIVATE)
    cacheDir = File(cacheDir, innerDirectory)
    val file = File(cacheDir, "${name}.${extension}")

    if (!file.exists()) {
        throw FileNotFoundException("${directory}/${name}.${extension} is not existing.")
    }

    val inputStream = FileInputStream(file)
    val bitmap = BitmapFactory.decodeStream(inputStream)
    inputStream.close()

    return bitmap
}