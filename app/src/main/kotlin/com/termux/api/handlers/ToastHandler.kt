package com.termux.api.handlers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.termux.api.ResultReturner

/**
 * Handler for Toast API method.
 * Shows an Android toast notification with text from stdin or args.
 *
 * Usage: echo "hello" | termux-toast [-g gravity] [-s] [-c color] [-b background]
 * Flags:
 *   -s: use short duration instead of long
 *   -g: gravity (top, middle, bottom — default: middle)
 *   -c: text color (e.g., red, blue, white)
 *   -b: background color (e.g., gray, black)
 */
object ToastHandler {

    fun onReceive(receiver: BroadcastReceiver, context: Context, intent: Intent) {
        val shortDuration = intent.getBooleanExtra("short", false)

        ResultReturner.returnData(receiver, intent, object : ResultReturner.WithStringInput() {
            override fun writeResultWithInput(out: java.io.PrintWriter) {
                val toastText = inputString.ifEmpty { "(empty)" }
                val duration = if (shortDuration) Toast.LENGTH_SHORT else Toast.LENGTH_LONG

                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, toastText, duration).show()
                }
            }
        })
    }
}
