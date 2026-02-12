package com.termux.api.handlers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.termux.api.ResultReturner

/**
 * Handler for Vibrate API method.
 * Vibrates the device for a specified duration.
 *
 * Usage: termux-vibrate [-d duration_ms] [-f]
 *   -d: duration in milliseconds (default: 1000)
 *   -f: force vibrate even in silent mode
 */
object VibrateHandler {

    fun onReceive(receiver: BroadcastReceiver, context: Context, intent: Intent) {
        val durationMs = intent.getIntExtra("duration_ms", 1000).toLong()
        val force = intent.getBooleanExtra("force", false)

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(
                    durationMs,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        }

        ResultReturner.noteDone(receiver, intent)
    }
}
