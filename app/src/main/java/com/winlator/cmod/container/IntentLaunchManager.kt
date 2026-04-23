package com.winlator.cmod.container

import android.content.Context
import android.content.Intent
import android.util.Log
import com.winlator.cmod.PrefManager
import com.winlator.cmod.core.DXVKHelper
import com.winlator.cmod.data.GameSource
import org.json.JSONObject

object IntentLaunchManager {
    private const val TAG = "IntentLaunchManager"
    private const val EXTRA_APP_ID = "app_id"
    private const val EXTRA_GAME_SOURCE = "game_source"
    private const val EXTRA_CONTAINER_CONFIG = "container_config"
    private const val ACTION_LAUNCH_GAME = "app.gamenative.LAUNCH_GAME"
    private const val MAX_CONFIG_JSON_SIZE = 50_000

    data class LaunchRequest(
        val appId: String,
        val containerConfig: ContainerData? = null,
    )

    fun parseLaunchIntent(intent: Intent): LaunchRequest? {
        if (intent.action != ACTION_LAUNCH_GAME) {
            return null
        }

        val gameId = intent.getIntExtra(EXTRA_APP_ID, -1)
        if (gameId <= 0) {
            Log.w(TAG, "Invalid or missing app_id in launch intent: $gameId")
            return null
        }

        var gameSource = intent.getStringExtra(EXTRA_GAME_SOURCE)?.uppercase()
        val validGameSource = GameSource.values().any { it.name == gameSource }
        if (!validGameSource) {
            gameSource = GameSource.STEAM.name
        }

        val appId = "${gameSource}_$gameId"
        val containerConfigJson = intent.getStringExtra(EXTRA_CONTAINER_CONFIG)
        val containerConfig = if (containerConfigJson != null) {
            runCatching { parseContainerConfig(containerConfigJson) }
                .onFailure { Log.e(TAG, "Failed to parse container config", it) }
                .getOrNull()
        } else {
            null
        }

        return LaunchRequest(appId, containerConfig)
    }

    fun applyTemporaryConfigOverride(context: Context, appId: String, configOverride: ContainerData) {
        TemporaryConfigStore.setOverride(appId, configOverride)

        if (ContainerUtils.hasContainer(context, appId)) {
            val container = ContainerUtils.getContainer(context, appId)
            if (TemporaryConfigStore.getOriginalConfig(appId) == null) {
                TemporaryConfigStore.setOriginalConfig(appId, ContainerUtils.toContainerData(container))
            }

            val effectiveConfig = getEffectiveContainerConfig(context, appId)
            if (effectiveConfig != null) {
                ContainerUtils.applyToContainer(context, container, effectiveConfig, saveToDisk = false)
            }
        }
    }

    fun getEffectiveContainerConfig(context: Context, appId: String): ContainerData? {
        val baseConfig = if (ContainerUtils.hasContainer(context, appId)) {
            ContainerUtils.toContainerData(ContainerUtils.getContainer(context, appId))
        } else {
            null
        }
        val override = TemporaryConfigStore.getOverride(appId)
        return when {
            override != null && baseConfig != null -> mergeConfigurations(baseConfig, override)
            override != null -> override
            else -> baseConfig
        }
    }

    fun clearTemporaryOverride(appId: String) {
        TemporaryConfigStore.clearOverride(appId)
    }

    fun clearAllTemporaryOverrides() {
        TemporaryConfigStore.clearAll()
    }

    fun restoreOriginalConfiguration(context: Context, appId: String) {
        val originalConfig = TemporaryConfigStore.getOriginalConfig(appId) ?: return
        if (ContainerUtils.hasContainer(context, appId)) {
            val container = ContainerUtils.getContainer(context, appId)
            ContainerUtils.applyToContainer(context, container, originalConfig, saveToDisk = false)
        }
    }

    fun hasTemporaryOverride(appId: String): Boolean = TemporaryConfigStore.hasOverride(appId)

    fun getTemporaryOverride(appId: String): ContainerData? = TemporaryConfigStore.getOverride(appId)

    fun getOriginalConfig(appId: String): ContainerData? = TemporaryConfigStore.getOriginalConfig(appId)

    fun setOriginalConfig(appId: String, config: ContainerData) {
        TemporaryConfigStore.setOriginalConfig(appId, config)
    }

    private fun validateContainerConfig(config: ContainerData): List<String> {
        val issues = mutableListOf<String>()
        if (!config.screenSize.matches(Regex("\\d+x\\d+"))) {
            issues.add("Invalid screen size format: ${config.screenSize}")
        }
        if (config.cpuList.isNotEmpty() && !config.cpuList.matches(Regex("\\d+(,\\d+)*"))) {
            issues.add("Invalid CPU list format: ${config.cpuList}")
        }
        if (!config.videoMemorySize.matches(Regex("\\d+"))) {
            issues.add("Invalid video memory size: ${config.videoMemorySize}")
        }
        if (config.drives.isNotEmpty() && !config.drives.matches(Regex("([A-Z]:([^:]+))*"))) {
            issues.add("Invalid drives format: ${config.drives}")
        }
        return issues
    }

    private fun parseContainerConfig(jsonString: String): ContainerData {
        require(jsonString.length <= MAX_CONFIG_JSON_SIZE) {
            "Container configuration JSON too large (max ${MAX_CONFIG_JSON_SIZE / 1000}KB)"
        }

        val json = JSONObject(jsonString)
        val rawGraphicsDriver = json.optString("graphicsDriver", Container.DEFAULT_GRAPHICS_DRIVER)
        val config = ContainerData(
            name = json.optString("name", ""),
            screenSize = json.optString("screenSize", Container.DEFAULT_SCREEN_SIZE),
            envVars = json.optString("envVars", Container.DEFAULT_ENV_VARS),
            graphicsDriver = Container.normalizeGraphicsDriver(rawGraphicsDriver),
            graphicsDriverVersion = json.optString("graphicsDriverVersion", ""),
            graphicsDriverConfig = Container.reconcileLegacyGraphicsConfig(
                rawGraphicsDriver,
                json.optString("graphicsDriverConfig", Container.DEFAULT_GRAPHICSDRIVERCONFIG)
            ),
            dxwrapper = json.optString("dxwrapper", Container.DEFAULT_DXWRAPPER),
            dxwrapperConfig = if (json.has("dxwrapperConfig")) {
                "version=" + json.optString("dxwrapperConfig", "")
            } else {
                DXVKHelper.DEFAULT_CONFIG
            },
            audioDriver = json.optString("audioDriver", Container.DEFAULT_AUDIO_DRIVER),
            wincomponents = json.optString("wincomponents", Container.DEFAULT_WINCOMPONENTS),
            drives = json.optString("drives", Container.DEFAULT_DRIVES),
            execArgs = json.optString("execArgs", ""),
            executablePath = json.optString("executablePath", ""),
            installPath = json.optString("installPath", ""),
            showFPS = json.optBoolean("showFPS", false),
            launchRealSteam = json.optBoolean("launchRealSteam", false),
            cpuList = json.optString("cpuList", Container.getFallbackCPUList()),
            cpuListWoW64 = json.optString("cpuListWoW64", Container.getFallbackCPUListWoW64()),
            wow64Mode = json.optBoolean("wow64Mode", true),
            startupSelection = json.optInt("startupSelection", Container.STARTUP_SELECTION_ESSENTIAL.toInt()).toByte(),
            box86Version = json.optString("box86Version", ""),
            box64Version = json.optString("box64Version", ""),
            box86Preset = json.optString("box86Preset", ""),
            box64Preset = json.optString("box64Preset", ""),
            desktopTheme = json.optString("desktopTheme", ""),
            containerVariant = json.optString("containerVariant", Container.DEFAULT_VARIANT),
            wineVersion = json.optString("wineVersion", PrefManager.wineVersion),
            emulator = json.optString("emulator", PrefManager.emulator),
            fexcoreVersion = json.optString("fexcoreVersion", PrefManager.fexcoreVersion),
            fexcorePreset = json.optString("fexcorePreset", PrefManager.fexcorePreset),
            renderer = json.optString("renderer", "gl"),
            csmt = json.optBoolean("csmt", true),
            videoPciDeviceID = json.optInt("videoPciDeviceID", 1728),
            offScreenRenderingMode = json.optString("offScreenRenderingMode", "fbo"),
            strictShaderMath = json.optBoolean("strictShaderMath", true),
            videoMemorySize = json.optString("videoMemorySize", "2048"),
            mouseWarpOverride = json.optString("mouseWarpOverride", "disable"),
            sdlControllerAPI = json.optBoolean("sdlControllerAPI", true),
            enableXInput = json.optBoolean("enableXInput", true),
            enableDInput = json.optBoolean("enableDInput", true),
            dinputMapperType = json.optInt("dinputMapperType", 1).toByte(),
            disableMouseInput = json.optBoolean("disableMouseInput", false),
            suspendPolicy = Container.normalizeSuspendPolicy(json.optString("suspendPolicy", PrefManager.suspendPolicy)),
            shaderBackend = json.optString("shaderBackend", "glsl"),
            useGLSL = json.optString("useGLSL", "enabled"),
        )

        val issues = validateContainerConfig(config)
        if (issues.isNotEmpty()) {
            Log.w(TAG, "Container config validation issues: ${issues.joinToString("; ")}")
        }
        return config
    }

    private fun mergeConfigurations(base: ContainerData, override: ContainerData): ContainerData {
        if (override == base) return base

        return ContainerData(
            name = override.name.ifEmpty { base.name },
            screenSize = if (override.screenSize != Container.DEFAULT_SCREEN_SIZE) override.screenSize else base.screenSize,
            envVars = if (override.envVars != Container.DEFAULT_ENV_VARS) override.envVars else base.envVars,
            graphicsDriver = if (Container.normalizeGraphicsDriver(override.graphicsDriver) != Container.DEFAULT_GRAPHICS_DRIVER) {
                Container.normalizeGraphicsDriver(override.graphicsDriver)
            } else {
                Container.normalizeGraphicsDriver(base.graphicsDriver)
            },
            graphicsDriverVersion = override.graphicsDriverVersion.ifEmpty { base.graphicsDriverVersion },
            graphicsDriverConfig = override.graphicsDriverConfig.ifEmpty { base.graphicsDriverConfig },
            dxwrapper = if (override.dxwrapper != Container.DEFAULT_DXWRAPPER) override.dxwrapper else base.dxwrapper,
            dxwrapperConfig = when {
                override.dxwrapperConfig.isNotEmpty() -> override.dxwrapperConfig
                base.dxwrapperConfig.isNotEmpty() -> base.dxwrapperConfig
                else -> DXVKHelper.DEFAULT_CONFIG
            },
            audioDriver = if (override.audioDriver != Container.DEFAULT_AUDIO_DRIVER) override.audioDriver else base.audioDriver,
            wincomponents = if (override.wincomponents != Container.DEFAULT_WINCOMPONENTS) override.wincomponents else base.wincomponents,
            drives = if (override.drives != Container.DEFAULT_DRIVES) override.drives else base.drives,
            execArgs = override.execArgs.ifEmpty { base.execArgs },
            executablePath = override.executablePath.ifEmpty { base.executablePath },
            installPath = override.installPath.ifEmpty { base.installPath },
            showFPS = if (override.showFPS) override.showFPS else base.showFPS,
            launchRealSteam = if (override.launchRealSteam) override.launchRealSteam else base.launchRealSteam,
            cpuList = if (override.cpuList != Container.getFallbackCPUList()) override.cpuList else base.cpuList,
            cpuListWoW64 = if (override.cpuListWoW64 != Container.getFallbackCPUListWoW64()) override.cpuListWoW64 else base.cpuListWoW64,
            wow64Mode = if (!override.wow64Mode) override.wow64Mode else base.wow64Mode,
            startupSelection = if (override.startupSelection != Container.STARTUP_SELECTION_ESSENTIAL.toByte()) override.startupSelection else base.startupSelection,
            box86Version = override.box86Version.ifEmpty { base.box86Version },
            box64Version = override.box64Version.ifEmpty { base.box64Version },
            box86Preset = override.box86Preset.ifEmpty { base.box86Preset },
            box64Preset = override.box64Preset.ifEmpty { base.box64Preset },
            desktopTheme = override.desktopTheme.ifEmpty { base.desktopTheme },
            containerVariant = override.containerVariant.ifEmpty { base.containerVariant },
            wineVersion = override.wineVersion.ifEmpty { base.wineVersion },
            emulator = override.emulator.ifEmpty { base.emulator },
            fexcoreVersion = override.fexcoreVersion.ifEmpty { base.fexcoreVersion },
            fexcorePreset = override.fexcorePreset.ifEmpty { base.fexcorePreset },
            renderer = override.renderer.ifEmpty { base.renderer },
            csmt = if (!override.csmt) override.csmt else base.csmt,
            videoPciDeviceID = if (override.videoPciDeviceID != 1728) override.videoPciDeviceID else base.videoPciDeviceID,
            offScreenRenderingMode = if (override.offScreenRenderingMode != "fbo") override.offScreenRenderingMode else base.offScreenRenderingMode,
            strictShaderMath = if (!override.strictShaderMath) override.strictShaderMath else base.strictShaderMath,
            videoMemorySize = if (override.videoMemorySize != "2048") override.videoMemorySize else base.videoMemorySize,
            mouseWarpOverride = if (override.mouseWarpOverride != "disable") override.mouseWarpOverride else base.mouseWarpOverride,
            sdlControllerAPI = if (!override.sdlControllerAPI) override.sdlControllerAPI else base.sdlControllerAPI,
            enableXInput = if (!override.enableXInput) override.enableXInput else base.enableXInput,
            enableDInput = if (!override.enableDInput) override.enableDInput else base.enableDInput,
            dinputMapperType = if (override.dinputMapperType.toInt() != 1) override.dinputMapperType else base.dinputMapperType,
            disableMouseInput = if (override.disableMouseInput) override.disableMouseInput else base.disableMouseInput,
            suspendPolicy = if (override.suspendPolicy != PrefManager.suspendPolicy) override.suspendPolicy else base.suspendPolicy,
            shaderBackend = override.shaderBackend.ifEmpty { base.shaderBackend },
            useGLSL = override.useGLSL.ifEmpty { base.useGLSL },
        )
    }
}

private object TemporaryConfigStore {
    private val overrides = mutableMapOf<String, ContainerData>()
    private val originalConfigs = mutableMapOf<String, ContainerData>()
    private val lock = Any()

    fun setOverride(appId: String, config: ContainerData) = synchronized(lock) {
        overrides[appId] = config
    }

    fun getOverride(appId: String): ContainerData? = synchronized(lock) {
        overrides[appId]
    }

    fun clearOverride(appId: String) = synchronized(lock) {
        overrides.remove(appId)
        originalConfigs.remove(appId)
    }

    fun hasOverride(appId: String): Boolean = synchronized(lock) {
        overrides.containsKey(appId)
    }

    fun setOriginalConfig(appId: String, config: ContainerData) = synchronized(lock) {
        originalConfigs[appId] = config
    }

    fun getOriginalConfig(appId: String): ContainerData? = synchronized(lock) {
        originalConfigs[appId]
    }

    fun clearAll() = synchronized(lock) {
        overrides.clear()
        originalConfigs.clear()
    }
}
