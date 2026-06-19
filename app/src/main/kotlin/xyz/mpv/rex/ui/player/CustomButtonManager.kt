package xyz.mpv.rex.ui.player

import android.content.Context
import android.util.Log
import xyz.mpv.rex.preferences.AdvancedPreferences
import xyz.mpv.rex.preferences.PlayerPreferences
import xyz.mpv.rex.ui.preferences.CustomButton
import xyz.mpv.rex.ui.preferences.CustomButtonSlots
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Manages custom user-defined buttons, including Lua script generation and MPV integration.
 */
class CustomButtonManager(
    private val context: Context,
    private val playerPreferences: PlayerPreferences,
    private val advancedPreferences: AdvancedPreferences,
    private val json: Json,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "CustomButtonManager"
        private const val CUSTOM_BUTTONS_LOADED_FLAG = "user-data/mpvex/custombuttons_loaded"
    }

    data class CustomButtonState(
        val id: String,
        val label: String,
        val isLeft: Boolean,
    )

    private val _customButtons = MutableStateFlow<List<CustomButtonState>>(emptyList())
    val customButtons = _customButtons.asStateFlow()

    private var setupJob: Job? = null
    private val loadMutex = Mutex()
    private var isMpvReady = false
    private var scriptPath: String? = null

    fun onMpvInitialized() {
        isMpvReady = true
        reloadScript("mpv_initialized")
    }

    fun setup() {
        setupJob?.cancel()
        setupJob = scope.launch(Dispatchers.IO) {
            try {
                val uiButtons = mutableListOf<CustomButtonState>()
                if (!advancedPreferences.enableLuaScripts.get()) {
                    _customButtons.value = uiButtons
                    scriptPath = null
                    runCatching { MPVLib.setPropertyString(CUSTOM_BUTTONS_LOADED_FLAG, "0") }
                    return@launch
                }

                val scriptContent = buildString {
                    val jsonString = playerPreferences.customButtons.get()
                    if (jsonString.isNotBlank()) {
                        try {
                            // Try new slot-based format first
                            val slotsData = json.decodeFromString<CustomButtonSlots>(jsonString)
                            slotsData.slots.forEachIndexed { index, btn ->
                                if (btn != null && btn.enabled) {
                                    val safeId = btn.id.replace("-", "_")
                                    val isLeft = index < 4
                                    processButton(btn.id, safeId, btn.title, btn.content, btn.longPressContent, btn.onStartup, isLeft, uiButtons)
                                }
                            }
                        } catch (e: Exception) {
                            // Fallback to old format
                            try {
                                val customButtonsList = json.decodeFromString<List<CustomButton>>(jsonString)
                                customButtonsList.forEachIndexed { index, btn ->
                                    val safeId = btn.id.replace("-", "_")
                                    val isLeft = index < 4
                                    processButton(btn.id, safeId, btn.title, btn.content, btn.longPressContent, btn.onStartup, isLeft, uiButtons)
                                }
                            } catch (e2: Exception) {
                                Log.e(TAG, "Failed to parse custom buttons JSON", e2)
                            }
                        }
                    }

                    if (uiButtons.isNotEmpty()) {
                        append("mp.set_property_native('$CUSTOM_BUTTONS_LOADED_FLAG', '1')\n")
                    }
                }

                _customButtons.value = uiButtons

                if (scriptContent.isNotEmpty()) {
                    val scriptsDir = File(context.filesDir, "scripts")
                    if (!scriptsDir.exists()) scriptsDir.mkdirs()

                    val file = File(scriptsDir, "custombuttons.lua")
                    file.writeText(scriptContent)
                    scriptPath = file.absolutePath

                    if (isMpvReady) {
                        if (!loadScript(file)) {
                            Log.w(TAG, "Failed to load custombuttons.lua")
                        }
                    } else {
                        Log.d(TAG, "Deferring custombuttons.lua load until MPV is ready")
                    }
                } else {
                    scriptPath = null
                    runCatching { MPVLib.setPropertyString(CUSTOM_BUTTONS_LOADED_FLAG, "0") }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up custom buttons", e)
            }
        }
    }

    private fun reloadScript(reason: String) {
        if (!isMpvReady) return

        scope.launch(Dispatchers.IO) {
            loadMutex.withLock {
                if (!advancedPreferences.enableLuaScripts.get()) return@withLock

                val currentPath = scriptPath ?: return@withLock
                if (isScriptLoaded()) return@withLock

                val file = File(currentPath)
                if (!file.exists()) {
                    Log.w(TAG, "custombuttons.lua missing during $reason, rebuilding")
                    setup()
                    return@withLock
                }

                if (!loadScript(file)) {
                    Log.w(TAG, "custombuttons.lua load failed during $reason")
                }
            }
        }
    }

    private fun isScriptLoaded(): Boolean =
        runCatching { MPVLib.getPropertyString(CUSTOM_BUTTONS_LOADED_FLAG) == "1" }
            .getOrDefault(false)

    private fun loadScript(file: File): Boolean {
        runCatching { MPVLib.setPropertyString(CUSTOM_BUTTONS_LOADED_FLAG, "0") }
        return runCatching {
            MPVLib.command("load-script", file.absolutePath)
            true
        }.getOrElse {
            Log.w(TAG, "load-script failed: ${it.message}")
            false
        }
    }

    fun callButton(id: String) {
        val safeId = id.replace("-", "_")
        MPVLib.command("script-message", "call_button_$safeId")
    }

    fun callButtonLongPress(id: String) {
        val safeId = id.replace("-", "_")
        MPVLib.command("script-message", "call_button_long_$safeId")
    }

    private fun StringBuilder.processButton(
        originalId: String,
        safeId: String,
        label: String,
        command: String,
        longPressCommand: String,
        onStartup: String,
        isLeft: Boolean,
        uiList: MutableList<CustomButtonState>
    ) {
        if (label.isNotBlank()) {
            uiList.add(CustomButtonState(originalId, label, isLeft))

            if (onStartup.isNotBlank()) {
                append(onStartup)
                append("\n")
            }

            if (command.isNotBlank()) {
                append(
                    """
                    function button_${safeId}()
                        ${command}
                    end
                    mp.register_script_message('call_button_${safeId}', button_${safeId})
                    """.trimIndent()
                )
                append("\n")
            }

            if (longPressCommand.isNotBlank()) {
                append(
                    """
                    function button_long_${safeId}()
                        ${longPressCommand}
                    end
                    mp.register_script_message('call_button_long_${safeId}', button_long_${safeId})
                    """.trimIndent()
                )
                append("\n")
            }
        }
    }
}
