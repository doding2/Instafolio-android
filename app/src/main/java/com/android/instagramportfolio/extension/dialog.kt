package com.android.instagramportfolio.extension

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.TextView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import com.android.instagramportfolio.R


fun Fragment.showSelectFormatDialog(onItemSelected: (String) -> Unit) {
    requireContext().showSelectFormatDialog(onItemSelected)
}

fun Fragment.showAlertDialog(message: String, onOk: () -> Unit = { }, onDismiss: () -> Unit = { }) {
    requireContext().showAlertDialog(message, onOk, onDismiss)
}

fun Fragment.showMessageDialog(title: String, message: String, onOk: () -> Unit = { }, onDismiss: () -> Unit = { }) {
    requireContext().showMessageDialog(title, message, onOk, onDismiss)
}

fun Fragment.showConfirmDialog(title: String, message: String, onOk: () -> Unit = { }, onCancel: () -> Unit = { }, onDismiss: () -> Unit = { }) {
    requireContext().showConfirmDialog(title, message, onOk, onCancel, onDismiss)
}


fun Context.showSelectFormatDialog(onItemSelected: (String) -> Unit) {
    val dialog = Dialog(this).apply {
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


fun Context.showAlertDialog(message: String, onOk: () -> Unit = { }, onDismiss: () -> Unit = { }) {
    val dialog = Dialog(this).apply {
        setContentView(R.layout.dialog_alert)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    val errorText = dialog.findViewById<TextView>(R.id.text_error)
    errorText.text = message

    val okButton = dialog.findViewById<TextView>(R.id.button_ok)
    okButton.setOnClickListener {
        onOk()
        dialog.dismiss()
    }

    dialog.setOnDismissListener {
        onDismiss()
    }

    dialog.show()
}

fun Context.showMessageDialog(title: String, message: String, onOk: () -> Unit = { }, onDismiss: () -> Unit = { }) {
    val dialog = Dialog(this).apply {
        setContentView(R.layout.dialog_message)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    val titleText = dialog.findViewById<TextView>(R.id.text_title)
    titleText.text = title

    val messageText = dialog.findViewById<TextView>(R.id.text_message)
    messageText.text = message

    val okButton = dialog.findViewById<TextView>(R.id.button_ok)
    okButton.setOnClickListener {
        onOk()
        dialog.dismiss()
    }

    dialog.setOnDismissListener {
        onDismiss()
    }

    dialog.show()
}

fun Context.showConfirmDialog(
    title: String,
    message: String,
    onOk: () -> Unit = { },
    onCancel: () -> Unit =  { },
    onDismiss: () -> Unit = { }
) {
    val dialog = Dialog(this).apply {
        setContentView(R.layout.dialog_confirm)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    val titleText = dialog.findViewById<TextView>(R.id.text_title)
    titleText.text = title

    val messageText = dialog.findViewById<TextView>(R.id.text_message)
    messageText.text = message

    val okButton = dialog.findViewById<TextView>(R.id.button_ok)
    okButton.setOnClickListener {
        onOk()
        dialog.dismiss()
    }

    val cancelButton = dialog.findViewById<TextView>(R.id.button_cancel)
    cancelButton.setOnClickListener {
        onCancel()
        dialog.dismiss()
    }

    dialog.setOnDismissListener {
        onDismiss()
    }

    dialog.show()
}