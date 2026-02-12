package com.termux.api

import android.content.BroadcastReceiver
import android.content.Intent
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.util.JsonWriter
import com.termux.shared.logger.Logger
import java.io.InputStream
import java.io.PrintWriter

/**
 * Handles connecting to Unix domain sockets created by the termux-api C binary
 * and writing results back. This is the Kotlin equivalent of the official
 * termux-api ResultReturner.java.
 *
 * The C binary creates two abstract namespace Unix domain sockets:
 * - socket_output: we connect and write our response (JSON) here
 * - socket_input: we connect and read stdin data from here (for APIs that accept input)
 */
object ResultReturner {

    private const val LOG_TAG = "ResultReturner"
    private const val SOCKET_OUTPUT_EXTRA = "socket_output"
    private const val SOCKET_INPUT_EXTRA = "socket_input"

    /** Functional interface for writing text results */
    fun interface ResultWriter {
        fun writeResult(out: PrintWriter)
    }

    /** For APIs that need to read stdin input */
    abstract class WithInput : ResultWriter {
        lateinit var inputStream: InputStream
    }

    /** For APIs that write JSON output */
    abstract class ResultJsonWriter : ResultWriter {
        override fun writeResult(out: PrintWriter) {
            val writer = JsonWriter(out)
            writer.setIndent("  ")
            writeJson(writer)
            out.println()
        }

        abstract fun writeJson(out: JsonWriter)
    }

    /** Convenience for APIs that read string input and write text output */
    abstract class WithStringInput : WithInput() {
        var inputString: String = ""
            private set

        override fun writeResult(out: PrintWriter) {
            // Read all input first
            val buffer = ByteArray(1024)
            val baos = java.io.ByteArrayOutputStream()
            var len: Int
            while (inputStream.read(buffer).also { len = it } > 0) {
                baos.write(buffer, 0, len)
            }
            inputString = baos.toString(Charsets.UTF_8.name()).trim()
            writeResultWithInput(out)
        }

        abstract fun writeResultWithInput(out: PrintWriter)
    }

    /**
     * Get a [LocalSocketAddress] for a socket address string.
     * Supports both abstract namespace and filesystem sockets.
     */
    private fun getSocketAddress(socketAddress: String): LocalSocketAddress {
        return if (socketAddress.startsWith("/")) {
            LocalSocketAddress(socketAddress, LocalSocketAddress.Namespace.FILESYSTEM)
        } else {
            LocalSocketAddress(socketAddress, LocalSocketAddress.Namespace.ABSTRACT)
        }
    }

    /**
     * Signal completion to the C binary without writing any data.
     */
    fun noteDone(receiver: BroadcastReceiver, intent: Intent) {
        returnData(receiver, intent, null)
    }

    /**
     * Connect to the Unix sockets and write the result.
     * Runs in a new thread to avoid blocking the BroadcastReceiver.
     */
    fun returnData(receiver: BroadcastReceiver, intent: Intent, resultWriter: ResultWriter?) {
        val asyncResult = receiver.goAsync()

        Thread {
            var writer: PrintWriter? = null
            var outputSocket: LocalSocket? = null
            try {
                outputSocket = LocalSocket()
                val outputAddr = intent.getStringExtra(SOCKET_OUTPUT_EXTRA)
                    ?: throw java.io.IOException("Missing '$SOCKET_OUTPUT_EXTRA' extra")

                Logger.logDebug(LOG_TAG, "Connecting to output socket: $outputAddr")
                outputSocket.connect(getSocketAddress(outputAddr))
                writer = PrintWriter(outputSocket.getOutputStream())

                if (resultWriter != null) {
                    if (resultWriter is WithInput) {
                        val inputSocket = LocalSocket()
                        try {
                            val inputAddr = intent.getStringExtra(SOCKET_INPUT_EXTRA)
                                ?: throw java.io.IOException("Missing '$SOCKET_INPUT_EXTRA' extra")
                            inputSocket.connect(getSocketAddress(inputAddr))
                            resultWriter.inputStream = inputSocket.getInputStream()
                            resultWriter.writeResult(writer)
                        } finally {
                            try { inputSocket.close() } catch (_: Exception) {}
                        }
                    } else {
                        resultWriter.writeResult(writer)
                    }
                }

                if (receiver.isOrderedBroadcast) {
                    asyncResult.resultCode = 0
                }
            } catch (t: Throwable) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Error returning API result", t)
                if (receiver.isOrderedBroadcast) {
                    asyncResult.resultCode = 1
                }
            } finally {
                try { writer?.close() } catch (_: Exception) {}
                try { outputSocket?.close() } catch (_: Exception) {}
                try { asyncResult.finish() } catch (_: Exception) {}
            }
        }.start()
    }
}
