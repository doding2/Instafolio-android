package com.android.instagramportfolio.extension

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.android.instagramportfolio.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.dialog.MaterialDialogs

fun Context.showSelectFormatDialog() {
    val dialog = Dialog(this).apply {
        setContentView(R.layout.dialog_select_format)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
    dialog.show()
}
