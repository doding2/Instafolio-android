package com.instafolioo.instagramportfolio.extension

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.instafolioo.instagramportfolio.model.ResultSlide
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.ObjectInputStream

// document 파일을 열때마다 생기는 알 수 없는 놈들 삭제하기
fun Context.optimizeDocumentCache() {
    for (file in fileList()) {
        if (file.startsWith("document:")) {
            deleteFile(file)
            Log.d("extension", "strange file removed: $file, ${file.extension}")
        }
    }
}

// 캐싱된 이미지 삭제하기
fun ResultSlide.deleteCache(context: Context) {
    val cw = ContextWrapper(context)
    var cacheDir = cw.getDir("slides", Context.MODE_PRIVATE)
    cacheDir = File(cacheDir, "id_$id")
    cacheDir.deleteRecursively()

    context.optimizeDocumentCache()
}

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

    val cw = ContextWrapper(context)
    var cacheDir = cw.getDir("slides", Context.MODE_PRIVATE)
    cacheDir = File(cacheDir, "id_$id")

    for (file in cacheDir.listFiles()) {
        // 이미지 파일이 아니면 패스
        if (file.extension == "dat")
            continue

        val inputStream = FileInputStream(file)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        bitmaps.add(bitmap)
        inputStream.close()
    }

    return bitmaps
}

// 내부저장소에서 인스타 사이즈 상태 가져오기
fun ResultSlide.getInstarSizeState(context: Context): Boolean {
    val cw = ContextWrapper(context)
    var cacheDir = cw.getDir("slides", Context.MODE_PRIVATE)
    cacheDir = File(cacheDir, "id_$id")

    val instarSizeState = File(cacheDir, "instar_size_state.dat")

    val input = ObjectInputStream(FileInputStream(instarSizeState))
    val isInstarSize = input.readBoolean()
    input.close()

    return isInstarSize
}

// 내부저장소에서 바인딩 상태 가져오기
fun ResultSlide.getBindingIndicesState(context: Context): List<Pair<Int, Int>> {
    val cw = ContextWrapper(context)
    var cacheDir = cw.getDir("slides", Context.MODE_PRIVATE)
    cacheDir = File(cacheDir, "id_$id")

    val bindingState = File(cacheDir, "binding_state.dat")

    val input = ObjectInputStream(FileInputStream(bindingState))
    val bindingIndices = input.readObject() as List<Pair<Int, Int>>
    input.close()

    return bindingIndices
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