package com.android.instagramportfolio.extension

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.android.instagramportfolio.model.PreviewSlide
import com.android.instagramportfolio.model.ResultSlide
import com.itextpdf.text.Document
import com.itextpdf.text.Image
import com.itextpdf.text.pdf.PdfWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.lang.Integer.max
import java.text.SimpleDateFormat
import java.util.*

// 슬라이드 이미지를 오리지널로 리턴
fun PreviewSlide.getAsOriginal(): Bitmap {
    return bitmap.run {
//        if ((width == 1080 && height <= 1080) || (height == 1080 && width <= 1080))  {
//            return bitmap
//        } else {
//            getResized(this)
//        }
        return bitmap
    }
}

// 슬라이드 이미지를 인스타 사이즈로 리턴
fun PreviewSlide.getAsInstarSize(): Bitmap {
    return setSlideInstarSize(bitmap)
}

// 서로 바인딩된 슬라이드 이미지 둘을 오리지널로 리턴
fun PreviewSlide.getAsOriginalBinding(): Bitmap {
    return getAsOriginal()
}

// 서로 바인딩된 슬라이드 이미지 둘을 인스타 사이즈로 리턴
fun PreviewSlide.getAsInstarSizeBinding(): Bitmap {
    val bindedBitmap = bindSlide(bitmap, bitmapSecond!!)
    return setSlideInstarSize(bindedBitmap)
}

fun saveBitmapsAsImage(
    context: Context,
    bitmaps: List<Bitmap>,
    directory: String,
    innerDirectory: String,
    extension: String,
    isSavingSlide: MutableLiveData<ResultSlide>
) {
    bitmaps.forEachIndexed { index, bitmap ->
        // 유저가 저장 도중에 나갈때
        isSavingSlide.value ?: return@forEachIndexed

        saveBitmap(context, bitmap, directory, innerDirectory, "$index", extension)
    }
}

// 인자로 받은 비트맵을 이미지로 저장
fun saveBitmap(
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

// 비트맵들을 pdf로 저장
fun saveBitmapsAsPdf(
    context: Context,
    bitmaps: List<Bitmap>,
    directory: String,
    innerDirectory: String,
    name: String
) {
    val cw = ContextWrapper(context)
    var cacheDir = cw.getDir(directory, Context.MODE_PRIVATE)
    cacheDir = File(cacheDir, innerDirectory)
    cacheDir.mkdirs()

    val pdfPath = File(cacheDir, "${name}.pdf")

    val document = Document()
    PdfWriter.getInstance(document, FileOutputStream(pdfPath))
    document.open()

    for (bitmap in bitmaps) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)

        val image = Image.getInstance(stream.toByteArray())

        document.pageSize = image
        document.newPage()

        image.setAbsolutePosition(0f, 0f)
        document.add(image)
        stream.close()
    }

    document.close()
}

// 외부 저장소 파일들 삭제
fun deleteExternalStorageDirectory(
    innerDirectory: String
) {
    val externalDirectory = getExternalStorageDir("인스타그램 포트폴리오", innerDirectory)
    if (externalDirectory.exists()) {
        externalDirectory.deleteRecursively()
    }
}

// 비트맵들을 pdf로 외부저장소에 저장
fun saveBitmapsAsPdfInExternalStorage(
    bitmaps: List<Bitmap>,
    innerDirectory: String? = null,
    name: String,
    isSavingSlide: MutableLiveData<ResultSlide>
) {
    val externalStorage = if (innerDirectory == null) {
        getExternalStorageDirWithoutInner("인스타그램 포트폴리오")
    } else {
        getExternalStorageDir("인스타그램 포트폴리오", innerDirectory)
    }

    val document = Document()
    var fileName = name
    var count = 1

    do {
        try {
            val pdfPath = File(externalStorage, "${fileName}.pdf")
            PdfWriter.getInstance(document, FileOutputStream(pdfPath))
            break
        } catch (e: FileNotFoundException) {
            fileName = "$name ($count)"
            count++
        }
    } while(true)

    document.open()

    for (bitmap in bitmaps) {
        // 유저가 저장 중간에 나갈때
        if (isSavingSlide.value == null) {
            document.close()
            return
        }

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)

        val image = Image.getInstance(stream.toByteArray())

        document.pageSize = image
        document.newPage()

        image.setAbsolutePosition(0f, 0f)
        document.add(image)
        stream.close()
    }

    document.close()
}

// 비트맵들을 이미지로 외부저장소에 저장
fun saveBitmapsAsImageInExternalStorage(
    bitmaps: List<Bitmap>,
    innerDirectory: String,
    extension: String,
    isSavingSlide: MutableLiveData<ResultSlide>
) {
    bitmaps.forEachIndexed { index, bitmap ->
        // 유저가 중간에 나갈때
        isSavingSlide.value ?: return@forEachIndexed
        
        saveBitmapInExternalStorage(bitmap, innerDirectory, "image $index", extension)
    }
}

// 비트맵들을 이미지로 외부저장소에 저장
fun saveBitmapInExternalStorage(
    bitmap: Bitmap,
    innerDirectory: String,
    name: String,
    extension: String
) {
    val externalStorage = getExternalStorageDir("인스타그램 포트폴리오", innerDirectory)

    var out: FileOutputStream
    var fileName = name
    var count = 1

    do {
        try {
            val imagePath = File(externalStorage, "${fileName}.${extension}")
            out = FileOutputStream(imagePath)
            break
        } catch (e: FileNotFoundException) {
            fileName = "$name ($count)"
            count++
        }
    } while(true)

    if (extension == "jpg") {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
    } else {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    out.close()
}

// 외부 저장소 주소를 리턴하는 함수
fun getExternalStorageDir(directory: String, innerDirectory: String): File {

    val dir: File = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        File(
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)}" +
                    "/$directory/$innerDirectory"
        )
    } else {
        File("${Environment.getExternalStorageDirectory()}/$directory/$innerDirectory")
    }

    if (!dir.exists()) {
        dir.mkdirs()
    }

    return dir
}

// inner directory 없는 버전
fun getExternalStorageDirWithoutInner(directory: String): File {
    val dir: File = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        File(
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)}" +
                    "/$directory"
        )
    } else {
        File("${Environment.getExternalStorageDirectory()}/$directory")
    }

    if (!dir.exists()) {
        dir.mkdirs()
    }

    return dir
}


// 이미지를 인스타 사이즈로 변환하여 리턴
fun setSlideInstarSize(
    bitmap: Bitmap
): Bitmap {
    // 비트맵 크기 1080으로 변환
    val resizedBitmap = if (bitmap.width != 1080 || bitmap.height != 1080) {
        getResized(bitmap)
    } else {
        bitmap
    }
    // 비트맵 타입을 hardware type에서 내가 수정할 수 있도록 변경
    val bitmapCopied = resizedBitmap.copy(Bitmap.Config.ARGB_8888, false)

    val backgroundBitmap = Bitmap.createBitmap(1080, 1080, Bitmap.Config.ARGB_8888)

    val canvas = Canvas(backgroundBitmap)
    canvas.drawColor(Color.WHITE)
    canvas.drawBitmap(
        bitmapCopied,
        (backgroundBitmap.width - bitmapCopied.width) / 2f,
        (backgroundBitmap.height - bitmapCopied.height) / 2f,
        null
    )

    return backgroundBitmap
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

    val backgroundBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    val canvas = Canvas(backgroundBitmap)
    canvas.drawBitmap(firstBitmapCopied, (width - firstBitmapCopied.width) / 2f, 0f, null)
    canvas.drawBitmap(secondBitmapCopied, (width - secondBitmapCopied.width) / 2f, firstBitmapCopied.height.toFloat(), null)

    return backgroundBitmap
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