package com.termux.api

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.termux.api.handlers.*
import com.termux.shared.logger.Logger

/**
 * BroadcastReceiver that handles API requests from the termux-api C binary.
 *
 * The C binary (at $PREFIX/bin/termux-api) creates Unix domain sockets and sends
 * a broadcast intent to this receiver with extras:
 * - api_method: The API method to invoke (e.g., "BatteryStatus", "Clipboard")
 * - socket_input: Address of the input socket (stdin from terminal)
 * - socket_output: Address of the output socket (stdout to terminal)
 *
 * This receiver dispatches to the appropriate handler which connects to the
 * sockets and writes the result.
 */
class TermuxApiReceiver : BroadcastReceiver() {

    companion object {
        private const val LOG_TAG = "TermuxApiReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Logger.logDebug(LOG_TAG, "Intent received")

        try {
            doWork(context, intent)
        } catch (t: Throwable) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Error in TermuxApiReceiver", t)
            ResultReturner.noteDone(this, intent)
        }
    }

    private fun doWork(context: Context, intent: Intent) {
        val apiMethod = intent.getStringExtra("api_method")
        if (apiMethod == null) {
            Logger.logError(LOG_TAG, "Missing 'api_method' extra")
            return
        }

        Logger.logDebug(LOG_TAG, "API method: $apiMethod")

        when (apiMethod) {
            "BatteryStatus" ->
                BatteryStatusHandler.onReceive(this, context, intent)

            "Clipboard" ->
                ClipboardHandler.onReceive(this, context, intent)

            "Toast" ->
                ToastHandler.onReceive(this, context, intent)

            "Vibrate" ->
                VibrateHandler.onReceive(this, context, intent)

            "Volume" ->
                VolumeHandler.onReceive(this, context, intent)

            else -> {
                Logger.logWarn(LOG_TAG, "API method '$apiMethod' is not yet implemented")
                ResultReturner.returnData(this, intent, ResultReturner.ResultWriter { out ->
                    out.println("{\"error\": \"API method '$apiMethod' is not yet implemented in this app. Use 'termuxctl device' for available built-in APIs.\"}")
                })
            }
        }
    }
}
