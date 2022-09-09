package com.android.instagramportfolio.extension

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import com.android.instagramportfolio.model.PreviewSlide
import java.io.File
import java.io.FileOutputStream
import java.lang.Integer.max

// 슬라이드 이미지 하나를 오리지널로 저장
fun saveSlideAsOriginal(
    context: Context,
    previewSlide: PreviewSlide,
    directory: String,
    innerDirectory: String,
    name: String,
    extension: String
) {
    saveSlide(context, previewSlide.bitmap, directory, innerDirectory, name, extension)
}

// 슬라이드 이미지 하나를 인스타 사이즈로 저장
fun saveSlideAsInstarSize(
    context: Context,
    previewSlide: PreviewSlide,
    directory: String,
    innerDirectory: String,
    name: String,
    extension: String
) {
    // 인스타 사이즈로만 변환 하면 됨
}

// 서로 바인딩된 슬라이드 이미지 둘을 오리지널로 저장
fun saveSlideAsOriginalBinding(
    context: Context,
    previewSlide: PreviewSlide,
    directory: String,
    innerDirectory: String,
    name: String,
    extension: String
) {
    // 오리지널 바인딩은
    // 프리뷰에 보여지는 예시와 실제 결과가 같게 하기 위해
    // 그냥 미리 합쳐버림
    // 그래서 따로 할거 없음
    saveSlide(context, previewSlide.bitmap, directory, innerDirectory, name, extension)
}


// 서로 바인딩된 슬라이드 이미지 둘을 인스타 사이즈로 저장
fun saveSlideAsInstarSizeBinding(
    context: Context,
    previewSlide: PreviewSlide,
    directory: String,
    innerDirectory: String,
    name: String,
    extension: String
) {
    // 바인딩 한 다음에
    // 인스타 사이즈로 변환
}


// 인자로 받은 비트맵을 저장
fun saveSlide(
    context: Context,
    bitmap: Bitmap,
    directory: String,
    innerDirectory: String,
    name: String,
    extension: String
) {
    val cw = ContextWrapper(context)
    var cacheDir = cw.getDir(directory, Context.MODE_PRIVATE)
    cacheDir = File(cacheDir, innerDirectory)
    cacheDir.mkdirs()

    val imagePath = File(cacheDir, "${name}.${extension}")

    val out = FileOutputStream(imagePath)
    if (extension == "jpg") {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
    } else {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    out.close()
}

// 바인딩된 비트맵을 생성하여 리턴
fun bindSlide(
    firstBitmap: Bitmap,
    secondBitmap: Bitmap
): Bitmap {
    // 합치기 전 사이즈 조절(두 이미지의 높이가 같도록)
    val resizedFirst = if (firstBitmap.height != 1080) {
        getResizedForHeight(firstBitmap)
    } else {
        firstBitmap
    }
    val resizedSecond = if (secondBitmap.height != 1080) {
        getResizedForHeight(secondBitmap)
    } else {
        secondBitmap
    }

    // 비트맵 타입을 hardware type에서 내가 수정할 수 있도록 변경
    val firstBitmapCopied: Bitmap = resizedFirst.copy(Bitmap.Config.ARGB_8888, false)
    val secondBitmapCopied: Bitmap = resizedSecond.copy(Bitmap.Config.ARGB_8888, false)

    val width: Int = max(firstBitmapCopied.width, secondBitmapCopied.width)
    val height: Int = firstBitmapCopied.height + secondBitmapCopied.height

    val bindedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    val canvas = Canvas(bindedBitmap)
    canvas.drawBitmap(firstBitmapCopied, (width - firstBitmapCopied.width) / 2f, 0f, null)
    canvas.drawBitmap(secondBitmapCopied, (width - secondBitmapCopied.width) / 2f, firstBitmapCopied.height.toFloat(), null)

    return bindedBitmap
}

// 이미지를 인스타 사이즈로 변환하여 리턴
//fun setInstarSize(
//    context: Context,
//    bitmap: Bitmap
//): Bitmap {
//
//}

// 인자로 전달된 비트맵을 리사이징해서 반환하는 함수
fun getResized(
    bitmap: Bitmap,
    maxWidth: Int = 1080,
    maxHeight: Int = 1080
): Bitmap {
    var image = bitmap
    if (maxHeight > 0 && maxWidth > 0) {
        val width = image.width
        val height = image.height
        val ratioBitmap = width.toFloat() / height.toFloat()
        val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

        var finalWidth = maxWidth
        var finalHeight = maxHeight
        if (ratioMax > ratioBitmap) {
            finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
        } else {
            finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
        }
        image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true)
    }

    return image
}

// 이미지의 높이를 무조건 1080으로 맞추고
// 너비는 그에 비례하여 키움
fun getResizedForHeight(
    bitmap: Bitmap,
    maxHeight: Int = 1080,
): Bitmap {
    var image = bitmap
    if (maxHeight > 0) {
        val width = image.width
        val height = image.height
        val ratioBitmap = width.toFloat() / height.toFloat()

        val finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
        val finalHeight = maxHeight
        image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true)
    }

    return image
}