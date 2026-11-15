package com.example.data

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.domain.model.BatteryStatusInfo
import com.example.domain.model.PermissionSummary
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SystemTelemetryRepository(private val context: Context) {

    fun observeBatteryStatus(): Flow<BatteryStatusInfo> = callbackFlow {
        fun readBattery(intent: Intent?): BatteryStatusInfo {
            if (intent == null) return BatteryStatusInfo()
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val pct = if (level >= 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else 100
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            return BatteryStatusInfo(levelPercent = pct, isCharging = charging)
        }

        val initialIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        trySend(readBattery(initialIntent))

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                trySend(readBattery(intent))
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        awaitClose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    fun checkPermissions(): PermissionSummary {
        fun granted(permission: String): Boolean {
            return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PermissionSummary(
                hasBluetoothConnect = granted(Manifest.permission.BLUETOOTH_CONNECT),
                hasBluetoothScan = granted(Manifest.permission.BLUETOOTH_SCAN),
                hasBluetoothAdvertise = granted(Manifest.permission.BLUETOOTH_ADVERTISE),
                hasLegacyLocation = true,
                hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    granted(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    true
                }
            )
        } else {
            PermissionSummary(
                hasBluetoothConnect = granted(Manifest.permission.BLUETOOTH),
                hasBluetoothScan = granted(Manifest.permission.BLUETOOTH_ADMIN),
                hasBluetoothAdvertise = granted(Manifest.permission.BLUETOOTH_ADMIN),
                hasLegacyLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    granted(Manifest.permission.ACCESS_FINE_LOCATION)
                } else {
                    true
                },
                hasNotifications = true
            )
        }
    }

    fun requiredRuntimePermissions(): Array<String> {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
            list.add(Manifest.permission.BLUETOOTH_SCAN)
            list.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            list.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return list.toTypedArray()
    }

    fun isBluetoothHidApiSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
}
