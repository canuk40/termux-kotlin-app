package com.termux.api.handlers

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.termux.api.ResultReturner
import java.io.PrintWriter

/**
 * Handler for Clipboard API method.
 * Handles both clipboard get and set operations.
 *
 * Get: termux-clipboard-get → outputs current clipboard contents
 * Set: termux-clipboard-set → reads stdin and sets clipboard
 */
object ClipboardHandler {

    fun onReceive(receiver: BroadcastReceiver, context: Context, intent: Intent) {
        val isSet = intent.getBooleanExtra("api_set", false)

        if (isSet) {
            handleClipboardSet(receiver, context, intent)
        } else {
            handleClipboardGet(receiver, context, intent)
        }
    }

    private fun handleClipboardGet(receiver: BroadcastReceiver, context: Context, intent: Intent) {
        ResultReturner.returnData(receiver, intent, ResultReturner.ResultWriter { out ->
            // ClipboardManager must be accessed on main thread
            val result = java.util.concurrent.CountDownLatch(1)
            var clipText: String? = null

            Handler(Looper.getMainLooper()).post {
                try {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = clipboard.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        clipText = clip.getItemAt(0).coerceToText(context)?.toString()
                    }
                } finally {
                    result.countDown()
                }
            }

            result.await()
            out.print(clipText ?: "")
        })
    }

    private fun handleClipboardSet(receiver: BroadcastReceiver, context: Context, intent: Intent) {
        ResultReturner.returnData(receiver, intent, object : ResultReturner.WithStringInput() {
            override fun writeResultWithInput(out: PrintWriter) {
                val result = java.util.concurrent.CountDownLatch(1)

                Handler(Looper.getMainLooper()).post {
                    try {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("termux-api", inputString))
                    } finally {
                        result.countDown()
                    }
                }

                result.await()
            }
        })
    }
}
