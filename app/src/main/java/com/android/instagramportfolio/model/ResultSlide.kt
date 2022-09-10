package com.android.instagramportfolio.model

import android.graphics.Bitmap
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
