package com.android.instagramportfolio.extension

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import org.apache.commons.io.IOUtils
import java.io.*


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
fun Context.dpToPx(dp: Int): Int {
    return (dp * resources.displayMetrics.density).toInt()
}

fun Context.pxToDp(px: Int): Int {
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
fun getFilePathFromURI(fragment: Fragment, contentUri: Uri?): String? {
    val fileName = getFileName(contentUri)
    if (!TextUtils.isEmpty(fileName)) {
        val copyFile = File(fragment.requireContext().filesDir.toString() + File.separator + fileName)
        copy(fragment, contentUri, copyFile)
        return copyFile.absolutePath
    }
    return null
}
fun getFileName(uri: Uri?): String? {
    if (uri == null) return null
    var fileName: String? = null
    val path = uri.path
    val cut = path!!.lastIndexOf('/')
    if (cut != -1) {
        fileName = path.substring(cut + 1)
    }
    return fileName
}
fun copy(fragment: Fragment, srcUri: Uri?, dstFile: File?, permissionGranted: Boolean = false) {
    try {
        val inputStream = fragment.requireContext().contentResolver.openInputStream(srcUri!!) ?: return
        val outputStream: OutputStream = FileOutputStream(dstFile)
        IOUtils.copy(inputStream, outputStream)
        inputStream.close()
        outputStream.close()
    } catch (e: IOException) {
        // manage storage 퍼미션이 허용됨
        try {
            val inputStream = File(srcUri?.path).inputStream()
            val outputStream: OutputStream = FileOutputStream(dstFile)
            IOUtils.copy(inputStream, outputStream)
            inputStream.close()
            outputStream.close()
        }
        // manage storage 퍼미션이 허용 안됨
        catch (e: Exception) {
            throw NoManageStoragePermissionException("퍼미션이 승인 안 됨")
        }
    }
}



// pdf to image 변환
fun Fragment.pdfToBitmaps(pdfFile: File): ArrayList<Bitmap> {
    val bitmaps: ArrayList<Bitmap> = ArrayList()
    try {
        val renderer =
            PdfRenderer(ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY))
        var bitmap: Bitmap
        val pageCount = renderer.pageCount
        for (i in 0 until pageCount) {
            val page = renderer.openPage(i)

            var width: Int
            var height: Int

            // 너비 또는 높이 둘 중 큰 것을 1080으로 고정
            val criteria = 1080.0
            if (page.width > page.height) {
                val scale = page.width / criteria
                height = (page.height / scale).toInt()
                width = criteria.toInt()
            } else {
                val scale = page.height / criteria
                width = (page.width / scale).toInt()
                height = criteria.toInt()
            }

            // 위에서 고정에 실패하면 크기 조절
            var divider = 432
            while (width <= 0 || height <= 0) {
                width = resources.displayMetrics.densityDpi / divider * page.width
                height = resources.displayMetrics.densityDpi / divider * page.height
                divider /= 2
            }
            Log.d("extension", "pdf image $i  width: $width, height: $height")

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

// 인자로 전달된 uri로부터 비트맵을 생성하는 함수
fun Fragment.imageToBitmap(uri: Uri): Bitmap {
    val bitmap: Bitmap = if (Build.VERSION.SDK_INT < 28) {
        MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
    }
    else {
        return try {
            val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        }
        catch (e: Exception) {
            try {
                // 퍼미션이 승인됨
                val file = File(uri.path)
                val bmOptions = BitmapFactory.Options()
                bmOptions.inPreferredConfig = Bitmap.Config.ARGB_8888
                val bitmap = BitmapFactory.decodeStream(FileInputStream(file), null, bmOptions)
                return bitmap!!
            }
            catch (e: Exception) {
                // 퍼미션이 승인 안됨
                throw NoReadStoragePermissionException("퍼미션이 승인 안 됨")
            }
        }
    }

    return bitmap
}


