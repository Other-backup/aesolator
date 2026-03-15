package com.winlator.cmod

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.winlator.cmod.box64.Box64Preset
import com.winlator.cmod.container.Container
import com.winlator.cmod.core.DefaultVersion
import com.winlator.cmod.core.WineInfo
import com.winlator.cmod.fexcore.FEXCorePreset
import java.util.concurrent.CompletableFuture

/**
 * SharedPreferences-backed donor-compatible preference bridge.
 *
 * Ae.solator still stores product settings in SharedPreferences, so donor
 * transfer lanes use this object as the compatibility surface instead of
 * bringing DataStore online before the first honest compile.
 */
object PrefManager {
    private var preferences: SharedPreferences? = null

    @JvmStatic
    fun init(context: Context) {
        if (preferences == null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        }
    }

    @JvmStatic
    fun deInit() {
        preferences = null
    }

    @JvmStatic
    fun getString(key: String, defaultValue: String): String =
        preferences?.getString(key, defaultValue) ?: defaultValue

    @JvmStatic
    fun putString(key: String, value: String): CompletableFuture<Unit> =
        edit { putString(key, value) }

    @JvmStatic
    fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        preferences?.getBoolean(key, defaultValue) ?: defaultValue

    @JvmStatic
    fun putBoolean(key: String, value: Boolean): CompletableFuture<Unit> =
        edit { putBoolean(key, value) }

    @JvmStatic
    fun getInt(key: String, defaultValue: Int): Int =
        preferences?.getInt(key, defaultValue) ?: defaultValue

    @JvmStatic
    fun putInt(key: String, value: Int): CompletableFuture<Unit> =
        edit { putInt(key, value) }

    @JvmStatic
    fun getLong(key: String, defaultValue: Long): Long =
        preferences?.getLong(key, defaultValue) ?: defaultValue

    @JvmStatic
    fun putLong(key: String, value: Long): CompletableFuture<Unit> =
        edit { putLong(key, value) }

    @JvmStatic
    fun getFloat(key: String, defaultValue: Float): Float =
        preferences?.getFloat(key, defaultValue) ?: defaultValue

    @JvmStatic
    fun putFloat(key: String, value: Float): CompletableFuture<Unit> =
        edit { putFloat(key, value) }

    private fun edit(block: SharedPreferences.Editor.() -> Unit): CompletableFuture<Unit> {
        val prefs = preferences ?: return CompletableFuture.completedFuture(Unit)
        prefs.edit().apply {
            block()
            apply()
        }
        return CompletableFuture.completedFuture(Unit)
    }

    private fun hasKey(key: String): Boolean = preferences?.contains(key) == true

    private fun getStringCompat(defaultValue: String, vararg keys: String): String {
        val prefs = preferences ?: return defaultValue
        for (key in keys) {
            if (prefs.contains(key)) {
                return prefs.getString(key, defaultValue) ?: defaultValue
            }
        }
        return defaultValue
    }

    private fun getBooleanCompat(defaultValue: Boolean, vararg keys: String): Boolean {
        val prefs = preferences ?: return defaultValue
        for (key in keys) {
            if (prefs.contains(key)) {
                return prefs.getBoolean(key, defaultValue)
            }
        }
        return defaultValue
    }

    private fun getIntCompat(defaultValue: Int, vararg keys: String): Int {
        val prefs = preferences ?: return defaultValue
        for (key in keys) {
            if (prefs.contains(key)) {
                return prefs.getInt(key, defaultValue)
            }
        }
        return defaultValue
    }

    private fun setStringCompat(value: String, primaryKey: String, vararg aliases: String) {
        edit {
            putString(primaryKey, value)
            for (alias in aliases) putString(alias, value)
        }
    }

    private fun setBooleanCompat(value: Boolean, primaryKey: String, vararg aliases: String) {
        edit {
            putBoolean(primaryKey, value)
            for (alias in aliases) putBoolean(alias, value)
        }
    }

    private fun setIntCompat(value: Int, primaryKey: String, vararg aliases: String) {
        edit {
            putInt(primaryKey, value)
            for (alias in aliases) putInt(alias, value)
        }
    }

    var screenSize: String
        get() = getStringCompat(Container.DEFAULT_SCREEN_SIZE, "screen_size")
        set(value) = setStringCompat(value, "screen_size")

    var envVars: String
        get() = getStringCompat(Container.DEFAULT_ENV_VARS, "env_vars")
        set(value) = setStringCompat(value, "env_vars")

    var graphicsDriver: String
        get() = getStringCompat(Container.DEFAULT_GRAPHICS_DRIVER, "graphics_driver")
        set(value) = setStringCompat(value, "graphics_driver")

    var graphicsDriverVersion: String
        get() = getStringCompat("", "graphics_driver_version")
        set(value) = setStringCompat(value, "graphics_driver_version")

    var graphicsDriverConfig: String
        get() = getStringCompat(
            Container.DEFAULT_GRAPHICSDRIVERCONFIG,
            "graphics_driver_config",
            "graphicsDriverConfig",
        )
        set(value) = setStringCompat(value, "graphics_driver_config", "graphicsDriverConfig")

    var dxWrapper: String
        get() = getStringCompat(Container.DEFAULT_DXWRAPPER, "dxwrapper")
        set(value) = setStringCompat(value, "dxwrapper")

    var dxWrapperConfig: String
        get() = getStringCompat(Container.DEFAULT_DXWRAPPERCONFIG, "dxwrapperConfig")
        set(value) = setStringCompat(value, "dxwrapperConfig")

    var audioDriver: String
        get() = getStringCompat(Container.DEFAULT_AUDIO_DRIVER, "audio_driver")
        set(value) = setStringCompat(value, "audio_driver")

    var winComponents: String
        get() = getStringCompat(Container.DEFAULT_WINCOMPONENTS, "wincomponents")
        set(value) = setStringCompat(value, "wincomponents")

    var drives: String
        get() = getStringCompat(Container.DEFAULT_DRIVES, "drives")
        set(value) = setStringCompat(value, "drives")

    var execArgs: String
        get() = getStringCompat("", "exec_args")
        set(value) = setStringCompat(value, "exec_args")

    var launchRealSteam: Boolean
        get() = getBooleanCompat(false, "launch_real_steam")
        set(value) = setBooleanCompat(value, "launch_real_steam")

    var forceDlc: Boolean
        get() = getBooleanCompat(false, "force_dlc")
        set(value) = setBooleanCompat(value, "force_dlc")

    var steamOfflineMode: Boolean
        get() = getBooleanCompat(false, "steam_offline_mode")
        set(value) = setBooleanCompat(value, "steam_offline_mode")

    var useLegacyDRM: Boolean
        get() = getBooleanCompat(false, "use_legacy_drm")
        set(value) = setBooleanCompat(value, "use_legacy_drm")

    var unpackFiles: Boolean
        get() = getBooleanCompat(false, "unpack_files")
        set(value) = setBooleanCompat(value, "unpack_files")

    var suspendPolicy: String
        get() = Container.normalizeSuspendPolicy(
            getStringCompat(Container.SUSPEND_POLICY_MANUAL, "suspend_policy"),
        )
        set(value) = setStringCompat(Container.normalizeSuspendPolicy(value), "suspend_policy")

    var cpuList: String
        get() = getStringCompat(Container.getFallbackCPUList(), "cpu_list")
        set(value) = setStringCompat(value, "cpu_list")

    var cpuListWoW64: String
        get() = getStringCompat(Container.getFallbackCPUListWoW64(), "cpu_list_wow64")
        set(value) = setStringCompat(value, "cpu_list_wow64")

    var wow64Mode: Boolean
        get() = getBooleanCompat(true, "wow64_mode")
        set(value) = setBooleanCompat(value, "wow64_mode")

    var startupSelection: Int
        get() = getIntCompat(Container.STARTUP_SELECTION_ESSENTIAL.toInt(), "startup_selection")
        set(value) = setIntCompat(value, "startup_selection")

    var containerLanguage: String
        get() = getStringCompat("english", "container_language")
        set(value) = setStringCompat(value, "container_language")

    var box86Preset: String
        get() = getStringCompat(Box64Preset.COMPATIBILITY, "box86_preset")
        set(value) = setStringCompat(value, "box86_preset")

    var box64Preset: String
        get() = getStringCompat(Box64Preset.COMPATIBILITY, "box64_preset")
        set(value) = setStringCompat(value, "box64_preset")

    var fexcorePreset: String
        get() = getStringCompat(FEXCorePreset.INTERMEDIATE, "fexcore_preset")
        set(value) = setStringCompat(value, "fexcore_preset")

    var containerVariant: String
        get() = getStringCompat(Container.DEFAULT_VARIANT, "container_variant")
        set(value) = setStringCompat(value, "container_variant")

    var wineVersion: String
        get() = getStringCompat(WineInfo.MAIN_WINE_VERSION.identifier(), "wine_version")
        set(value) = setStringCompat(value, "wine_version")

    var emulator: String
        get() = getStringCompat(Container.DEFAULT_EMULATOR, "emulator")
        set(value) = setStringCompat(value, "emulator")

    var fexcoreVersion: String
        get() = getStringCompat(DefaultVersion.FEXCORE, "fexcore_version")
        set(value) = setStringCompat(value, "fexcore_version")

    var fexcoreTSOMode: String
        get() = getStringCompat("Fast", "fexcore_tso_mode")
        set(value) = setStringCompat(value, "fexcore_tso_mode")

    var fexcoreX87Mode: String
        get() = getStringCompat("Fast", "fexcore_x87_mode")
        set(value) = setStringCompat(value, "fexcore_x87_mode")

    var fexcoreMultiBlock: String
        get() = getStringCompat("Disabled", "fexcore_multiblock")
        set(value) = setStringCompat(value, "fexcore_multiblock")

    var renderer: String
        get() = getStringCompat("gl", "renderer")
        set(value) = setStringCompat(value, "renderer")

    var csmt: Boolean
        get() = getBooleanCompat(true, "csmt")
        set(value) = setBooleanCompat(value, "csmt")

    var videoPciDeviceID: Int
        get() = getIntCompat(1728, "videoPciDeviceID", "video_pci_device_id")
        set(value) = setIntCompat(value, "videoPciDeviceID", "video_pci_device_id")

    var offScreenRenderingMode: String
        get() = getStringCompat("fbo", "offScreenRenderingMode", "offscreen_rendering_mode")
        set(value) = setStringCompat(value, "offScreenRenderingMode", "offscreen_rendering_mode")

    var strictShaderMath: Boolean
        get() = getBooleanCompat(true, "strictShaderMath", "strict_shader_math")
        set(value) = setBooleanCompat(value, "strictShaderMath", "strict_shader_math")

    var useDRI3: Boolean
        get() = when {
            hasKey("useDRI3") -> getBoolean("useDRI3", true)
            hasKey("use_dri3") -> getBoolean("use_dri3", true)
            else -> true
        }
        set(value) {
            edit {
                putBoolean("useDRI3", value)
                putBoolean("use_dri3", value)
            }
        }

    var videoMemorySize: String
        get() = getStringCompat("2048", "videoMemorySize", "video_memory_size")
        set(value) = setStringCompat(value, "videoMemorySize", "video_memory_size")

    var mouseWarpOverride: String
        get() = getStringCompat("disable", "mouseWarpOverride", "mouse_warp_override")
        set(value) = setStringCompat(value, "mouseWarpOverride", "mouse_warp_override")

    var useSteamInput: Boolean
        get() = getBooleanCompat(false, "use_steam_input")
        set(value) = setBooleanCompat(value, "use_steam_input")

    var xinputEnabled: Boolean
        get() = when {
            hasKey("xinput_enabled") -> getBoolean("xinput_enabled", true)
            hasKey("xinput_toggle") -> !getBoolean("xinput_toggle", false)
            else -> true
        }
        set(value) {
            edit {
                putBoolean("xinput_enabled", value)
                putBoolean("xinput_toggle", !value)
            }
        }

    var dinputEnabled: Boolean
        get() = getBooleanCompat(true, "dinput_enabled")
        set(value) = setBooleanCompat(value, "dinput_enabled")

    var dinputMapperType: Int
        get() = getIntCompat(1, "dinput_mapper_type")
        set(value) = setIntCompat(value, "dinput_mapper_type")

    var disableMouseInput: Boolean
        get() = getBooleanCompat(false, "disable_mouse_input")
        set(value) = setBooleanCompat(value, "disable_mouse_input")

    var portraitMode: Boolean
        get() = getBooleanCompat(false, "portrait_mode")
        set(value) = setBooleanCompat(value, "portrait_mode")

    var box86Version: String
        get() = getStringCompat(DefaultVersion.BOX64, "box86_version")
        set(value) = setStringCompat(value, "box86_version")

    var box64Version: String
        get() = getStringCompat(DefaultVersion.BOX64, "box64_version")
        set(value) = setStringCompat(value, "box64_version")

    var externalDisplayInputMode: String
        get() = getStringCompat(
            Container.DEFAULT_EXTERNAL_DISPLAY_MODE,
            "external_display_input_mode",
        )
        set(value) = setStringCompat(value, "external_display_input_mode")

    var externalDisplaySwap: Boolean
        get() = getBooleanCompat(false, "external_display_swap")
        set(value) = setBooleanCompat(value, "external_display_swap")

    var sharpnessEffect: String
        get() = getStringCompat("None", "sharpness_effect")
        set(value) = setStringCompat(value, "sharpness_effect")

    var sharpnessLevel: Int
        get() = getIntCompat(100, "sharpness_level")
        set(value) = setIntCompat(value.coerceIn(0, 100), "sharpness_level")

    var sharpnessDenoise: Int
        get() = getIntCompat(100, "sharpness_denoise")
        set(value) = setIntCompat(value.coerceIn(0, 100), "sharpness_denoise")
}
