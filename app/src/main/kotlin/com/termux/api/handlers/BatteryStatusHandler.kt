package com.termux.api.handlers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.JsonWriter
import com.termux.api.ResultReturner

/**
 * Handler for BatteryStatus API method.
 * Output format matches official termux-api BatteryStatusAPI.java exactly.
 */
object BatteryStatusHandler {

    fun onReceive(receiver: BroadcastReceiver, context: Context, intent: Intent) {
        ResultReturner.returnData(receiver, intent, object : ResultReturner.ResultJsonWriter() {
            override fun writeJson(out: JsonWriter) {
                val batteryStatus = context.registerReceiver(
                    null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                )

                out.beginObject()

                if (batteryStatus == null) {
                    out.name("error").value("Could not get battery status")
                    out.endObject()
                    return
                }

                val health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
                val healthStr = when (health) {
                    BatteryManager.BATTERY_HEALTH_COLD -> "COLD"
                    BatteryManager.BATTERY_HEALTH_DEAD -> "DEAD"
                    BatteryManager.BATTERY_HEALTH_GOOD -> "GOOD"
                    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "OVERHEAT"
                    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "OVER_VOLTAGE"
                    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "UNSPECIFIED_FAILURE"
                    else -> "UNKNOWN"
                }

                val percentage = batteryStatus.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if (level >= 0 && scale > 0) (level * 100.0 / scale) else -1.0
                }

                val plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                val pluggedStr = when (plugged) {
                    0 -> "UNPLUGGED"
                    BatteryManager.BATTERY_PLUGGED_AC -> "PLUGGED_AC"
                    BatteryManager.BATTERY_PLUGGED_USB -> "PLUGGED_USB"
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "PLUGGED_WIRELESS"
                    else -> "UNPLUGGED"
                }

                val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val statusStr = when (status) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> "CHARGING"
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> "DISCHARGING"
                    BatteryManager.BATTERY_STATUS_FULL -> "FULL"
                    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "NOT_CHARGING"
                    else -> "UNKNOWN"
                }

                val present = batteryStatus.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false)
                val temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                val technology = batteryStatus.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"
                val voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
                val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

                // BatteryManager service for extended properties
                val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val currentNow = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                val currentAvg = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
                val chargeCounter = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
                val energyCounter = batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)

                // Match official termux-api output format exactly
                out.name("health").value(healthStr)
                out.name("percentage").value(percentage.toInt())
                out.name("plugged").value(pluggedStr)
                out.name("status").value(statusStr)
                out.name("temperature").value(temperature / 10.0)
                out.name("current").value(currentNow ?: 0)
                if (currentAvg != null) out.name("current_average").value(currentAvg)

                out.endObject()
            }
        })
    }
}
