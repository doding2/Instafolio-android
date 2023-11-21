package com.instafolio.instagramportfolio.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "ResultSlideTable")
data class ResultSlide(
    @PrimaryKey(autoGenerate = true)
    var id: Long,
    var format: String,
    var size: Int,
    var thumbnail: Bitmap,
): Serializable

fun getEmptyResultSlides(): List<ResultSlide> {
    val list = mutableListOf<ResultSlide>()

    val config = Bitmap.Config.RGB_565
    val emptyBitmap = Bitmap.createBitmap(1, 1, config)
    val canvas = Canvas(emptyBitmap)
    canvas.drawColor(Color.WHITE)

    for (i in 0..47) {
        list.add(ResultSlide(i.toLong(), "empty", 0, emptyBitmap))
    }

    return list
}