package com.android.instagramportfolio.extension

import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.fragment.app.Fragment
import java.io.File
import java.io.IOException
import java.io.InputStream


/** status bar와
 * navigation bar를
 * 투명하게 만든 후
 * 높이 조정을 위해 필요 */
// status bar의 높이 구하는 확장함수
fun Fragment.getStatusBarHeight(): Int {
    val resourceId = this.resources.getIdentifier("status_bar_height", "dimen", "android")

    return if (resourceId > 0) {
        this.resources.getDimensionPixelSize(resourceId)
    } else {
        0
    }
}

// navigation bar의 높이 구하는 확장함수
fun Fragment.getNaviBarHeight(): Int {
    val resourceId: Int = this.resources.getIdentifier("navigation_bar_height", "dimen", "android")
    return if (resourceId > 0) {
        this.resources.getDimensionPixelSize(resourceId)
    } else {
        0
    }
}

// dp와 px간의 변환
fun Fragment.dpToPx(dp: Int): Int {
    return (dp * resources.displayMetrics.density).toInt()
}

fun Fragment.pxToDp(px: Int): Int {
    return (px / resources.displayMetrics.density).toInt()
}




// content uri로부터 파일 이름 알아내는 함수
fun Fragment.getFileName(uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor: Cursor? = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.let {
            if (it.moveToFirst()) {
                result = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result!!.lastIndexOf('/')
        if (cut != -1) {
            result = result!!.substring(cut + 1)
        }
    }
    return result
}

// 파일 이름 String을 가지고 확장자 알아내기
val String.extension: String get() = this.substringAfterLast(".")

// 주어진 uri의 파일이 이미지인지 아닌지 체크
fun Fragment.checkIsImage(uri: Uri): Boolean {
    val contentResolver = requireContext().contentResolver
    val type = contentResolver.getType(uri)
    if (type != null) {
        return type.startsWith("image/")
    } else {
        // try to decode as image (bounds only)
        var inputStream: InputStream? = null
        try {
            inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeStream(inputStream, null, options)
                return options.outWidth > 0 && options.outHeight > 0
            }
        } catch (e: IOException) {
            // ignore
        } finally {
            inputStream?.close()
        }
    }
    // default outcome if image not confirmed
    return false
}

// content uri로부터 파일 진짜 경로 가져오기
fun Fragment.getRealPathFromUri(contentUri: Uri): String? {
    var cursor: Cursor? = null
    return try {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        cursor = requireContext().contentResolver.query(contentUri, proj, null, null, null)!!
        val columnIndex: Int = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        cursor.getString(columnIndex)
    } finally {
        cursor?.close()
    }
}

// pdf to image 변환
fun Fragment.pdfToBitmap(pdfFile: File): ArrayList<Bitmap> {
    val bitmaps: ArrayList<Bitmap> = ArrayList()
    try {
        val renderer =
            PdfRenderer(ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY))
        var bitmap: Bitmap
        val pageCount = renderer.pageCount
        for (i in 0 until pageCount) {
            val page = renderer.openPage(i)
            val width: Int = resources.displayMetrics.densityDpi / 72 * page.width
            val height: Int = resources.displayMetrics.densityDpi / 72 * page.height
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmaps.add(bitmap)

            // close the page
            page.close()
        }

        // close the renderer
        renderer.close()
    } catch (ex: Exception) {
        ex.printStackTrace()
    }

    return bitmaps
}