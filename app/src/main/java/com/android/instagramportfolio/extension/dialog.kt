package com.android.instagramportfolio.extension

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.android.instagramportfolio.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.dialog.MaterialDialogs

fun Fragment.showSelectFormatDialog(onItemSelected: (String) -> Unit) {
    val dialog = Dialog(requireContext()).apply {
        setContentView(R.layout.dialog_select_format)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    val pngButton = dialog.findViewById<TextView>(R.id.button_png)
    pngButton.setOnClickListener {
        onItemSelected("png")
        dialog.dismiss()
    }
    val jpgButton = dialog.findViewById<TextView>(R.id.button_jpg)
    jpgButton.setOnClickListener {
        onItemSelected("jpg")
        dialog.dismiss()
    }
    val pdfButton = dialog.findViewById<TextView>(R.id.button_pdf)
    pdfButton.setOnClickListener {
        onItemSelected("pdf")
        dialog.dismiss()
    }

    dialog.show()
}
