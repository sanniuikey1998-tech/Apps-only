package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.hid.CustomShortcut
import com.example.domain.hid.FourFingerAction
import com.example.domain.model.AppPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.deckLinkDataStore: DataStore<Preferences> by preferencesDataStore(name = "decklink_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val POINTER_SPEED = floatPreferencesKey("pointer_speed")
        val MOUSE_SENSITIVITY = floatPreferencesKey("mouse_sensitivity")
        val NATURAL_SCROLLING = booleanPreferencesKey("natural_scrolling")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        val AUTO_CONNECT_CONNECTED = booleanPreferencesKey("auto_connect_connected_devices")
        val CONNECTION_TIMEOUT_SEC = intPreferencesKey("connection_timeout_sec")
        val FOREGROUND_SERVICE = booleanPreferencesKey("foreground_service")
        val CONFIRM_DANGEROUS = booleanPreferencesKey("confirm_dangerous_shortcuts")
        val FOUR_FINGER_UP = stringPreferencesKey("four_finger_up")
        val FOUR_FINGER_DOWN = stringPreferencesKey("four_finger_down")
        val FOUR_FINGER_LEFT = stringPreferencesKey("four_finger_left")
        val FOUR_FINGER_RIGHT = stringPreferencesKey("four_finger_right")
        val FOUR_FINGER_TAP = stringPreferencesKey("four_finger_tap")
        val LAST_ADDRESS = stringPreferencesKey("last_connected_address")
        val LAST_NAME = stringPreferencesKey("last_connected_name")
        val AUTO_CONNECT_ADDRESSES = stringSetPreferencesKey("auto_connect_addresses")
        val DISABLED_AUTO_CONNECT_ADDRESSES = stringSetPreferencesKey("disabled_auto_connect_addresses")
        val CUSTOM_SHORTCUTS = stringPreferencesKey("custom_shortcuts_v1")
    }

    val preferencesFlow: Flow<AppPreferences> = context.deckLinkDataStore.data.map { prefs ->
        AppPreferences(
            pointerSpeed = prefs[Keys.POINTER_SPEED] ?: 1.35f,
            mouseSensitivity = prefs[Keys.MOUSE_SENSITIVITY] ?: 1.15f,
            naturalScrolling = prefs[Keys.NATURAL_SCROLLING] ?: true,
            hapticFeedback = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
            autoReconnect = prefs[Keys.AUTO_RECONNECT] ?: true,
            autoConnectConnectedDevices = prefs[Keys.AUTO_CONNECT_CONNECTED] ?: true,
            connectionTimeoutSec = prefs[Keys.CONNECTION_TIMEOUT_SEC] ?: 15,
            foregroundServiceEnabled = prefs[Keys.FOREGROUND_SERVICE] ?: true,
            confirmDangerousShortcuts = prefs[Keys.CONFIRM_DANGEROUS] ?: true,
            fourFingerUp = parseAction(prefs[Keys.FOUR_FINGER_UP], FourFingerAction.TASK_VIEW),
            fourFingerDown = parseAction(prefs[Keys.FOUR_FINGER_DOWN], FourFingerAction.SHOW_DESKTOP),
            fourFingerLeft = parseAction(prefs[Keys.FOUR_FINGER_LEFT], FourFingerAction.SWITCH_DESKTOP_NEXT),
            fourFingerRight = parseAction(prefs[Keys.FOUR_FINGER_RIGHT], FourFingerAction.SWITCH_DESKTOP_PREV),
            fourFingerTap = parseAction(prefs[Keys.FOUR_FINGER_TAP], FourFingerAction.SNIP_SCREEN),
            lastConnectedAddress = prefs[Keys.LAST_ADDRESS],
            lastConnectedName = prefs[Keys.LAST_NAME],
            autoConnectAddresses = prefs[Keys.AUTO_CONNECT_ADDRESSES] ?: emptySet(),
            disabledAutoConnectAddresses = prefs[Keys.DISABLED_AUTO_CONNECT_ADDRESSES] ?: emptySet(),
            customShortcuts = decodeCustomShortcuts(prefs[Keys.CUSTOM_SHORTCUTS])
        )
    }

    suspend fun updatePointerSpeed(speed: Float) {
        context.deckLinkDataStore.edit { it[Keys.POINTER_SPEED] = speed.coerceIn(0.4f, 3.0f) }
    }

    suspend fun updateMouseSensitivity(sensitivity: Float) {
        context.deckLinkDataStore.edit { it[Keys.MOUSE_SENSITIVITY] = sensitivity.coerceIn(0.4f, 2.5f) }
    }

    suspend fun updateNaturalScrolling(enabled: Boolean) {
        context.deckLinkDataStore.edit { it[Keys.NATURAL_SCROLLING] = enabled }
    }

    suspend fun updateHapticFeedback(enabled: Boolean) {
        context.deckLinkDataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun updateAutoReconnect(enabled: Boolean) {
        context.deckLinkDataStore.edit { it[Keys.AUTO_RECONNECT] = enabled }
    }

    suspend fun updateAutoConnectConnectedDevices(enabled: Boolean) {
        context.deckLinkDataStore.edit { it[Keys.AUTO_CONNECT_CONNECTED] = enabled }
    }

    suspend fun setDeviceAutoConnect(address: String, enabled: Boolean) {
        context.deckLinkDataStore.edit { prefs ->
            val enabledSet = (prefs[Keys.AUTO_CONNECT_ADDRESSES] ?: emptySet()).toMutableSet()
            val disabledSet = (prefs[Keys.DISABLED_AUTO_CONNECT_ADDRESSES] ?: emptySet()).toMutableSet()
            if (enabled) {
                enabledSet.add(address)
                disabledSet.remove(address)
            } else {
                enabledSet.remove(address)
                disabledSet.add(address)
            }
            prefs[Keys.AUTO_CONNECT_ADDRESSES] = enabledSet
            prefs[Keys.DISABLED_AUTO_CONNECT_ADDRESSES] = disabledSet
        }
    }

    suspend fun updateConnectionTimeout(seconds: Int) {
        context.deckLinkDataStore.edit { it[Keys.CONNECTION_TIMEOUT_SEC] = seconds.coerceIn(5, 60) }
    }

    suspend fun updateForegroundServiceEnabled(enabled: Boolean) {
        context.deckLinkDataStore.edit { it[Keys.FOREGROUND_SERVICE] = enabled }
    }

    suspend fun updateConfirmDangerousShortcuts(enabled: Boolean) {
        context.deckLinkDataStore.edit { it[Keys.CONFIRM_DANGEROUS] = enabled }
    }

    suspend fun updateFourFingerGesture(direction: String, action: FourFingerAction) {
        context.deckLinkDataStore.edit { prefs ->
            when (direction) {
                "up" -> prefs[Keys.FOUR_FINGER_UP] = action.name
                "down" -> prefs[Keys.FOUR_FINGER_DOWN] = action.name
                "left" -> prefs[Keys.FOUR_FINGER_LEFT] = action.name
                "right" -> prefs[Keys.FOUR_FINGER_RIGHT] = action.name
                "tap" -> prefs[Keys.FOUR_FINGER_TAP] = action.name
            }
        }
    }

    suspend fun saveLastConnectedDevice(address: String, name: String) {
        context.deckLinkDataStore.edit {
            it[Keys.LAST_ADDRESS] = address
            it[Keys.LAST_NAME] = name
        }
    }

    suspend fun addCustomShortcut(shortcut: CustomShortcut) {
        context.deckLinkDataStore.edit { prefs ->
            val current = decodeCustomShortcuts(prefs[Keys.CUSTOM_SHORTCUTS]).toMutableList()
            current.removeAll { it.id == shortcut.id }
            current.add(shortcut)
            prefs[Keys.CUSTOM_SHORTCUTS] = encodeCustomShortcuts(current)
        }
    }

    suspend fun deleteCustomShortcut(id: String) {
        context.deckLinkDataStore.edit { prefs ->
            val current = decodeCustomShortcuts(prefs[Keys.CUSTOM_SHORTCUTS])
                .filterNot { it.id == id }
            prefs[Keys.CUSTOM_SHORTCUTS] = encodeCustomShortcuts(current)
        }
    }

    private fun parseAction(raw: String?, fallback: FourFingerAction): FourFingerAction {
        return raw?.let { runCatching { FourFingerAction.valueOf(it) }.getOrNull() } ?: fallback
    }

    companion object {
        internal fun encodeCustomShortcuts(list: List<CustomShortcut>): String {
            return list.joinToString("\n") { s ->
                val safeTitle = s.title.replace("|", " ").replace("\n", " ")
                val safeCombo = s.comboLabel.replace("|", " ").replace("\n", " ")
                "${s.id}|$safeTitle|$safeCombo|${s.modifierMask}|${s.keyCode}"
            }
        }

        internal fun decodeCustomShortcuts(raw: String?): List<CustomShortcut> {
            if (raw.isNullOrBlank()) return emptyList()
            return raw.lines().mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size < 5) return@mapNotNull null
                val mod = parts[3].toIntOrNull() ?: return@mapNotNull null
                val code = parts[4].toIntOrNull() ?: return@mapNotNull null
                CustomShortcut(
                    id = parts[0],
                    title = parts[1],
                    comboLabel = parts[2],
                    modifierMask = mod,
                    keyCode = code
                )
            }
        }
    }
}
