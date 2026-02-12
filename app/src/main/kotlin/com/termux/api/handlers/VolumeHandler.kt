package com.termux.api.handlers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.JsonWriter
import com.termux.api.ResultReturner

/**
 * Handler for Volume API method.
 * Gets or sets device volume for various audio streams.
 *
 * Get: termux-volume → outputs current volume info as JSON
 * Set: termux-volume <stream> <volume> → sets volume for a stream
 */
object VolumeHandler {

    private val STREAM_NAMES = mapOf(
        "call" to AudioManager.STREAM_VOICE_CALL,
        "system" to AudioManager.STREAM_SYSTEM,
        "ring" to AudioManager.STREAM_RING,
        "music" to AudioManager.STREAM_MUSIC,
        "alarm" to AudioManager.STREAM_ALARM,
        "notification" to AudioManager.STREAM_NOTIFICATION
    )

    fun onReceive(receiver: BroadcastReceiver, context: Context, intent: Intent) {
        val streamName = intent.getStringExtra("stream")
        val volume = intent.getIntExtra("volume", -1)

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (streamName != null && volume >= 0) {
            handleSetVolume(receiver, audioManager, intent, streamName, volume)
        } else {
            handleGetVolume(receiver, audioManager, intent)
        }
    }

    private fun handleGetVolume(
        receiver: BroadcastReceiver,
        audioManager: AudioManager,
        intent: Intent
    ) {
        ResultReturner.returnData(receiver, intent, object : ResultReturner.ResultJsonWriter() {
            override fun writeJson(out: JsonWriter) {
                out.beginArray()
                for ((name, stream) in STREAM_NAMES) {
                    out.beginObject()
                    out.name("stream").value(name)
                    out.name("volume").value(audioManager.getStreamVolume(stream))
                    out.name("max_volume").value(audioManager.getStreamMaxVolume(stream))
                    out.endObject()
                }
                out.endArray()
            }
        })
    }

    private fun handleSetVolume(
        receiver: BroadcastReceiver,
        audioManager: AudioManager,
        intent: Intent,
        streamName: String,
        volume: Int
    ) {
        val stream = STREAM_NAMES[streamName.lowercase()]
        if (stream != null) {
            val maxVolume = audioManager.getStreamMaxVolume(stream)
            val clampedVolume = volume.coerceIn(0, maxVolume)
            audioManager.setStreamVolume(stream, clampedVolume, 0)
        }
        ResultReturner.noteDone(receiver, intent)
    }
}
