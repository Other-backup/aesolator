package com.winlator.cmod.container

import android.content.Context
import android.os.Looper
import android.util.Log
import com.winlator.cmod.PrefManager
import com.winlator.cmod.contents.ContentsManager
import com.winlator.cmod.core.DXVKHelper
import com.winlator.cmod.core.FileUtils
import com.winlator.cmod.core.WineRegistryEditor
import com.winlator.cmod.core.WineThemeManager
import com.winlator.cmod.data.GameSource
import com.winlator.cmod.winhandler.WinHandler
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject

object ContainerUtils {
    private const val TAG = "ContainerUtils"
    private const val SESSION_APP_ID = "appId"
    private const val SESSION_GAME_SOURCE = "gameSource"
    private const val SESSION_DISPLAY_NAME = "displayName"
    private const val CREATE_TIMEOUT_SECONDS = 90L

    data class GpuInfo(
        val deviceId: Int,
        val vendorId: Int,
        val name: String,
    )

    fun getGPUCards(context: Context): Map<Int, GpuInfo> {
        return try {
            val gpuNames = JSONArray(FileUtils.readString(context, "gpu_cards.json"))
            buildMap {
                for (index in 0 until gpuNames.length()) {
                    val item = gpuNames.getJSONObject(index)
                    val deviceId = item.optInt("deviceID", -1)
                    if (deviceId >= 0) {
                        put(
                            deviceId,
                            GpuInfo(
                                deviceId = deviceId,
                                vendorId = item.optInt("vendorID", 0x10de),
                                name = item.optString("name", "Unknown GPU"),
                            ),
                        )
                    }
                }
            }
        } catch (error: Exception) {
            Log.w(TAG, "Failed to read gpu_cards.json", error)
            emptyMap()
        }
    }

    fun getDefaultContainerData(): ContainerData {
        return ContainerData(
            screenSize = PrefManager.screenSize,
            envVars = PrefManager.envVars,
            graphicsDriver = PrefManager.graphicsDriver,
            graphicsDriverVersion = PrefManager.graphicsDriverVersion,
            graphicsDriverConfig = PrefManager.graphicsDriverConfig,
            dxwrapper = PrefManager.dxWrapper,
            dxwrapperConfig = PrefManager.dxWrapperConfig,
            audioDriver = PrefManager.audioDriver,
            wincomponents = PrefManager.winComponents,
            drives = PrefManager.drives,
            execArgs = PrefManager.execArgs,
            showFPS = false,
            launchRealSteam = PrefManager.launchRealSteam,
            cpuList = PrefManager.cpuList,
            cpuListWoW64 = PrefManager.cpuListWoW64,
            wow64Mode = PrefManager.wow64Mode,
            startupSelection = PrefManager.startupSelection.toByte(),
            box86Version = PrefManager.box86Version,
            box64Version = PrefManager.box64Version,
            box86Preset = PrefManager.box86Preset,
            box64Preset = PrefManager.box64Preset,
            desktopTheme = WineThemeManager.DEFAULT_DESKTOP_THEME,
            language = PrefManager.containerLanguage,
            containerVariant = PrefManager.containerVariant,
            forceDlc = PrefManager.forceDlc,
            steamOfflineMode = PrefManager.steamOfflineMode,
            useLegacyDRM = PrefManager.useLegacyDRM,
            unpackFiles = PrefManager.unpackFiles,
            suspendPolicy = PrefManager.suspendPolicy,
            wineVersion = PrefManager.wineVersion,
            emulator = PrefManager.emulator,
            fexcoreVersion = PrefManager.fexcoreVersion,
            fexcoreTSOMode = PrefManager.fexcoreTSOMode,
            fexcoreX87Mode = PrefManager.fexcoreX87Mode,
            fexcoreMultiBlock = PrefManager.fexcoreMultiBlock,
            fexcorePreset = PrefManager.fexcorePreset,
            renderer = PrefManager.renderer,
            csmt = PrefManager.csmt,
            videoPciDeviceID = PrefManager.videoPciDeviceID,
            offScreenRenderingMode = PrefManager.offScreenRenderingMode,
            strictShaderMath = PrefManager.strictShaderMath,
            useDRI3 = PrefManager.useDRI3,
            videoMemorySize = PrefManager.videoMemorySize,
            mouseWarpOverride = PrefManager.mouseWarpOverride,
            useSteamInput = PrefManager.useSteamInput,
            enableXInput = PrefManager.xinputEnabled,
            enableDInput = PrefManager.dinputEnabled,
            dinputMapperType = PrefManager.dinputMapperType.toByte(),
            disableMouseInput = PrefManager.disableMouseInput,
            portraitMode = PrefManager.portraitMode,
            externalDisplayMode = PrefManager.externalDisplayInputMode,
            externalDisplaySwap = PrefManager.externalDisplaySwap,
            sharpnessEffect = PrefManager.sharpnessEffect,
            sharpnessLevel = PrefManager.sharpnessLevel,
            sharpnessDenoise = PrefManager.sharpnessDenoise,
        )
    }

    fun setDefaultContainerData(containerData: ContainerData) {
        PrefManager.screenSize = containerData.screenSize
        PrefManager.envVars = containerData.envVars
        PrefManager.graphicsDriver = containerData.graphicsDriver
        PrefManager.graphicsDriverVersion = containerData.graphicsDriverVersion
        PrefManager.graphicsDriverConfig = containerData.graphicsDriverConfig
        PrefManager.dxWrapper = containerData.dxwrapper
        PrefManager.dxWrapperConfig = containerData.dxwrapperConfig
        PrefManager.audioDriver = containerData.audioDriver
        PrefManager.winComponents = containerData.wincomponents
        PrefManager.drives = containerData.drives
        PrefManager.execArgs = containerData.execArgs
        PrefManager.launchRealSteam = containerData.launchRealSteam
        PrefManager.cpuList = containerData.cpuList
        PrefManager.cpuListWoW64 = containerData.cpuListWoW64
        PrefManager.wow64Mode = containerData.wow64Mode
        PrefManager.startupSelection = containerData.startupSelection.toInt()
        PrefManager.box86Version = containerData.box86Version
        PrefManager.box64Version = containerData.box64Version
        PrefManager.box86Preset = containerData.box86Preset
        PrefManager.box64Preset = containerData.box64Preset
        PrefManager.containerLanguage = containerData.language
        PrefManager.containerVariant = containerData.containerVariant
        PrefManager.wineVersion = containerData.wineVersion
        PrefManager.emulator = containerData.emulator
        PrefManager.fexcoreVersion = containerData.fexcoreVersion
        PrefManager.fexcoreTSOMode = containerData.fexcoreTSOMode
        PrefManager.fexcoreX87Mode = containerData.fexcoreX87Mode
        PrefManager.fexcoreMultiBlock = containerData.fexcoreMultiBlock
        PrefManager.fexcorePreset = containerData.fexcorePreset
        PrefManager.renderer = containerData.renderer
        PrefManager.csmt = containerData.csmt
        PrefManager.videoPciDeviceID = containerData.videoPciDeviceID
        PrefManager.offScreenRenderingMode = containerData.offScreenRenderingMode
        PrefManager.strictShaderMath = containerData.strictShaderMath
        PrefManager.videoMemorySize = containerData.videoMemorySize
        PrefManager.mouseWarpOverride = containerData.mouseWarpOverride
        PrefManager.useDRI3 = containerData.useDRI3
        PrefManager.useSteamInput = containerData.useSteamInput
        PrefManager.xinputEnabled = containerData.enableXInput
        PrefManager.dinputEnabled = containerData.enableDInput
        PrefManager.dinputMapperType = containerData.dinputMapperType.toInt()
        PrefManager.disableMouseInput = containerData.disableMouseInput
        PrefManager.externalDisplayInputMode = containerData.externalDisplayMode
        PrefManager.externalDisplaySwap = containerData.externalDisplaySwap
        PrefManager.forceDlc = containerData.forceDlc
        PrefManager.steamOfflineMode = containerData.steamOfflineMode
        PrefManager.useLegacyDRM = containerData.useLegacyDRM
        PrefManager.unpackFiles = containerData.unpackFiles
        PrefManager.suspendPolicy = containerData.suspendPolicy
        PrefManager.portraitMode = containerData.portraitMode
        PrefManager.sharpnessEffect = containerData.sharpnessEffect
        PrefManager.sharpnessLevel = containerData.sharpnessLevel
        PrefManager.sharpnessDenoise = containerData.sharpnessDenoise
    }

    fun toContainerData(container: Container): ContainerData {
        val defaultData = getDefaultContainerData()
        var renderer = defaultData.renderer
        var csmt = defaultData.csmt
        var videoPciDeviceID = defaultData.videoPciDeviceID
        var offScreenRenderingMode = defaultData.offScreenRenderingMode
        var strictShaderMath = defaultData.strictShaderMath
        var videoMemorySize = defaultData.videoMemorySize
        var mouseWarpOverride = defaultData.mouseWarpOverride

        val userRegFile = File(container.rootDir, ".wine/user.reg")
        if (userRegFile.isFile()) {
            try {
                WineRegistryEditor(userRegFile).use { registryEditor ->
                    renderer = registryEditor.getStringValue("Software\\Wine\\Direct3D", "renderer", renderer)
                    csmt = registryEditor.getDwordValue("Software\\Wine\\Direct3D", "csmt", if (csmt) 3 else 0) != 0
                    videoPciDeviceID =
                        registryEditor.getDwordValue("Software\\Wine\\Direct3D", "VideoPciDeviceID", videoPciDeviceID)
                    offScreenRenderingMode =
                        registryEditor.getStringValue(
                            "Software\\Wine\\Direct3D",
                            "OffScreenRenderingMode",
                            offScreenRenderingMode,
                        )
                    strictShaderMath =
                        registryEditor.getDwordValue(
                            "Software\\Wine\\Direct3D",
                            "strict_shader_math",
                            if (strictShaderMath) 1 else 0,
                        ) != 0
                    videoMemorySize =
                        registryEditor.getStringValue(
                            "Software\\Wine\\Direct3D",
                            "VideoMemorySize",
                            videoMemorySize,
                        )
                    mouseWarpOverride =
                        registryEditor.getStringValue(
                            "Software\\Wine\\DirectInput",
                            "MouseWarpOverride",
                            mouseWarpOverride,
                        )
                }
            } catch (error: Exception) {
                Log.w(TAG, "Failed to read container registry values for ${container.id}", error)
            }
        }

        val inputType = container.inputType
        val enableXInput = (inputType and WinHandler.FLAG_INPUT_TYPE_XINPUT.toInt()) != 0
        val enableDInput = (inputType and WinHandler.FLAG_INPUT_TYPE_DINPUT.toInt()) != 0
        val useSteamInput = container.getExtra("useSteamInput", "false").toBoolean()

        return ContainerData(
            name = container.name,
            screenSize = container.screenSize,
            envVars = container.envVars,
            graphicsDriver = container.graphicsDriver,
            graphicsDriverVersion = container.graphicsDriverVersion,
            graphicsDriverConfig = container.graphicsDriverConfig,
            dxwrapper = container.dxWrapper,
            dxwrapperConfig = container.dxWrapperConfig,
            audioDriver = container.audioDriver,
            wincomponents = container.winComponents,
            drives = container.drives,
            execArgs = container.execArgs,
            executablePath = container.executablePath,
            installPath = container.installPath,
            showFPS = container.isShowFPS,
            launchRealSteam = container.isLaunchRealSteam,
            allowSteamUpdates = container.isAllowSteamUpdates,
            steamType = container.steamType,
            cpuList = container.getCPUList(true),
            cpuListWoW64 = container.getCPUListWoW64(true),
            wow64Mode = container.isWoW64Mode,
            startupSelection = container.startupSelection,
            box86Version = container.box86Version,
            box64Version = container.box64Version,
            box86Preset = container.box86Preset,
            box64Preset = container.box64Preset,
            desktopTheme = container.desktopTheme,
            containerVariant = container.containerVariant,
            wineVersion = container.wineVersion,
            emulator = container.emulator,
            fexcoreVersion = container.getFEXCoreVersion(),
            fexcorePreset = container.getFEXCorePreset(),
            renderer = renderer,
            csmt = csmt,
            videoPciDeviceID = videoPciDeviceID,
            offScreenRenderingMode = offScreenRenderingMode,
            strictShaderMath = strictShaderMath,
            useDRI3 = container.isUseDRI3,
            videoMemorySize = videoMemorySize,
            mouseWarpOverride = mouseWarpOverride,
            sdlControllerAPI = container.isSdlControllerAPI,
            useSteamInput = useSteamInput,
            enableXInput = enableXInput,
            enableDInput = enableDInput,
            dinputMapperType = container.dinputMapperType,
            disableMouseInput = container.isDisableMouseInput,
            touchscreenMode = container.isTouchscreenMode,
            shooterMode = container.isShooterMode,
            gestureConfig = container.gestureConfig,
            externalDisplayMode = container.externalDisplayMode,
            externalDisplaySwap = container.isExternalDisplaySwap,
            language = container.language,
            forceDlc = container.isForceDlc,
            steamOfflineMode = container.isSteamOfflineMode,
            useLegacyDRM = container.isUseLegacyDRM,
            unpackFiles = container.isUnpackFiles,
            suspendPolicy = container.suspendPolicy,
            portraitMode = container.isPortraitMode,
            sharpnessEffect = container.getExtra("sharpnessEffect", "None"),
            sharpnessLevel = container.getExtra("sharpnessLevel", "100").toIntOrNull() ?: 100,
            sharpnessDenoise = container.getExtra("sharpnessDenoise", "100").toIntOrNull() ?: 100,
        )
    }

    fun applyBestConfigMapToContainerData(
        containerData: ContainerData,
        bestConfigMap: Map<String, Any?>,
    ): ContainerData {
        if (bestConfigMap.isEmpty()) return containerData
        val merged = containerData.toMap().toMutableMap()
        for ((key, value) in bestConfigMap) {
            if (value != null) {
                merged[key] = value
            }
        }
        return ContainerData.fromMap(merged)
    }

    fun applyToContainer(context: Context, container: Container, containerData: ContainerData) {
        applyToContainer(context, container, containerData, saveToDisk = true)
    }

    fun applyToContainer(
        context: Context,
        container: Container,
        containerData: ContainerData,
        saveToDisk: Boolean,
    ) {
        val userRegFile = File(container.rootDir, ".wine/user.reg")
        if (userRegFile.parentFile?.isDirectory == true) {
            try {
                WineRegistryEditor(userRegFile).use { registryEditor ->
                    val gpuCard = getGPUCards(context)[containerData.videoPciDeviceID]
                    registryEditor.setStringValue("Software\\Wine\\Direct3D", "renderer", containerData.renderer)
                    registryEditor.setDwordValue("Software\\Wine\\Direct3D", "csmt", if (containerData.csmt) 3 else 0)
                    registryEditor.setDwordValue(
                        "Software\\Wine\\Direct3D",
                        "VideoPciDeviceID",
                        containerData.videoPciDeviceID,
                    )
                    registryEditor.setDwordValue(
                        "Software\\Wine\\Direct3D",
                        "VideoPciVendorID",
                        gpuCard?.vendorId ?: 0x10de,
                    )
                    registryEditor.setStringValue(
                        "Software\\Wine\\Direct3D",
                        "OffScreenRenderingMode",
                        containerData.offScreenRenderingMode,
                    )
                    registryEditor.setDwordValue(
                        "Software\\Wine\\Direct3D",
                        "strict_shader_math",
                        if (containerData.strictShaderMath) 1 else 0,
                    )
                    registryEditor.setStringValue(
                        "Software\\Wine\\Direct3D",
                        "VideoMemorySize",
                        containerData.videoMemorySize,
                    )
                    registryEditor.setStringValue(
                        "Software\\Wine\\DirectInput",
                        "MouseWarpOverride",
                        containerData.mouseWarpOverride,
                    )
                    registryEditor.setStringValue("Software\\Wine\\Direct3D", "shader_backend", containerData.shaderBackend)
                    registryEditor.setStringValue("Software\\Wine\\Direct3D", "UseGLSL", containerData.useGLSL)
                }
            } catch (error: Exception) {
                Log.w(TAG, "Failed to update registry-backed container config for ${container.id}", error)
            }
        }

        container.name = containerData.name
        container.screenSize = containerData.screenSize
        container.envVars = containerData.envVars
        container.graphicsDriver = containerData.graphicsDriver
        container.graphicsDriverVersion = containerData.graphicsDriverVersion
        container.graphicsDriverConfig = containerData.graphicsDriverConfig
        container.dxWrapper = containerData.dxwrapper
        container.dxWrapperConfig = if (containerData.dxwrapperConfig.isNotEmpty()) {
            containerData.dxwrapperConfig
        } else {
            DXVKHelper.DEFAULT_CONFIG
        }
        container.audioDriver = containerData.audioDriver
        container.winComponents = containerData.wincomponents
        container.drives = containerData.drives
        container.execArgs = containerData.execArgs
        if (container.executablePath != containerData.executablePath && container.executablePath.isNotEmpty()) {
            container.isNeedsUnpacking = true
        }
        container.executablePath = containerData.executablePath
        container.installPath = containerData.installPath
        container.isShowFPS = containerData.showFPS
        container.isLaunchRealSteam = containerData.launchRealSteam
        container.isAllowSteamUpdates = containerData.allowSteamUpdates
        container.setSteamType(containerData.steamType)
        container.cpuList = containerData.cpuList
        container.cpuListWoW64 = containerData.cpuListWoW64
        container.isWoW64Mode = containerData.wow64Mode
        container.startupSelection = containerData.startupSelection
        container.box86Version = containerData.box86Version
        container.box64Version = containerData.box64Version
        container.box86Preset = containerData.box86Preset
        container.box64Preset = containerData.box64Preset
        container.desktopTheme = containerData.desktopTheme
        container.containerVariant = containerData.containerVariant
        container.wineVersion = containerData.wineVersion
        container.emulator = containerData.emulator
        container.setFEXCoreVersion(containerData.fexcoreVersion)
        container.setFEXCorePreset(containerData.fexcorePreset)
        container.isSdlControllerAPI = containerData.sdlControllerAPI
        container.putExtra("useSteamInput", containerData.useSteamInput.toString())
        container.language = containerData.language
        container.setLC_ALL(mapLanguageToLocale(containerData.language))
        container.setDisableMouseInput(containerData.disableMouseInput)
        container.setTouchscreenMode(containerData.touchscreenMode)
        container.setShooterMode(containerData.shooterMode)
        container.setGestureConfig(containerData.gestureConfig)
        container.setExternalDisplayMode(containerData.externalDisplayMode)
        container.setExternalDisplaySwap(containerData.externalDisplaySwap)
        container.setForceDlc(containerData.forceDlc)
        container.setSteamOfflineMode(containerData.steamOfflineMode)
        container.setUseLegacyDRM(containerData.useLegacyDRM)
        container.setUnpackFiles(containerData.unpackFiles)
        container.setSuspendPolicy(containerData.suspendPolicy)
        container.setPortraitMode(containerData.portraitMode)
        container.putExtra("sharpnessEffect", containerData.sharpnessEffect)
        container.putExtra("sharpnessLevel", containerData.sharpnessLevel.toString())
        container.putExtra("sharpnessDenoise", containerData.sharpnessDenoise.toString())
        val appId = resolveAppId(container)
        if (appId.isNotEmpty()) {
            container.putSessionMetadata(SESSION_GAME_SOURCE, extractGameSourceFromContainerId(appId).name)
            container.putSessionMetadata(SESSION_APP_ID, appId)
        }

        var inputType = 0
        if (containerData.enableXInput) inputType = inputType or WinHandler.FLAG_INPUT_TYPE_XINPUT.toInt()
        if (containerData.enableDInput) inputType = inputType or WinHandler.FLAG_INPUT_TYPE_DINPUT.toInt()
        if (inputType == 0) inputType = WinHandler.DEFAULT_INPUT_TYPE.toInt()
        container.inputType = inputType
        container.dinputMapperType = containerData.dinputMapperType
        container.setUseDRI3(containerData.useDRI3)

        if (saveToDisk) {
            container.putExtra("config_changed", "true")
            container.saveData()
        }
    }

    fun hasContainer(context: Context, appId: String): Boolean =
        findContainerByAppId(ContainerManager(context), appId) != null

    fun getContainer(context: Context, appId: String): Container {
        return findContainerByAppId(ContainerManager(context), appId)
            ?: throw IllegalArgumentException("Container does not exist for appId=$appId")
    }

    fun getOrCreateContainer(context: Context, appId: String): Container {
        val manager = ContainerManager(context)
        findContainerByAppId(manager, appId)?.let { return it }
        return createNewContainer(context, appId, manager, null)
    }

    fun getOrCreateContainerWithOverride(context: Context, appId: String): Container {
        val manager = ContainerManager(context)
        val existing = findContainerByAppId(manager, appId)
        if (existing != null) {
            if (IntentLaunchManager.hasTemporaryOverride(appId)) {
                val overrideConfig = IntentLaunchManager.getTemporaryOverride(appId)
                if (overrideConfig != null) {
                    if (IntentLaunchManager.getOriginalConfig(appId) == null) {
                        IntentLaunchManager.setOriginalConfig(appId, toContainerData(existing))
                    }
                    val effectiveConfig = IntentLaunchManager.getEffectiveContainerConfig(context, appId)
                    if (effectiveConfig != null) {
                        applyToContainer(context, existing, effectiveConfig, saveToDisk = false)
                    }
                }
            }
            return existing
        }
        val overrideConfig = IntentLaunchManager.getTemporaryOverride(appId)
        return createNewContainer(context, appId, manager, overrideConfig)
    }

    fun deleteContainer(context: Context, appId: String) {
        val manager = ContainerManager(context)
        val container = findContainerByAppId(manager, appId) ?: return
        val latch = CountDownLatch(1)
        manager.removeContainerAsync(container) {
            latch.countDown()
        }
        try {
            latch.await(30, TimeUnit.SECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    fun extractGameIdFromContainerId(containerId: String): Int {
        val idWithoutSuffix = containerId.substringBefore("(")
        val lastPart = idWithoutSuffix.split("_").lastOrNull()
            ?: throw IllegalArgumentException("Invalid container ID format: $containerId")
        return lastPart.toIntOrNull()
            ?: throw IllegalArgumentException("Could not extract game ID from container ID: $containerId")
    }

    fun extractGameSourceFromContainerId(containerId: String): GameSource {
        return when {
            containerId.startsWith("STEAM_") -> GameSource.STEAM
            containerId.startsWith("CUSTOM_GAME_") -> GameSource.CUSTOM_GAME
            containerId.startsWith("GOG_") -> GameSource.GOG
            containerId.startsWith("EPIC_") -> GameSource.EPIC
            containerId.startsWith("AMAZON_") -> GameSource.AMAZON
            else -> GameSource.STEAM
        }
    }

    fun resolveGameName(containerId: String): String {
        return containerId.substringAfter('_', containerId).replace('_', ' ')
    }

    fun getADrivePath(drives: String): String? {
        for (drive in Container.drivesIterator(drives)) {
            if (drive[0] == "A") return drive[1]
        }
        return null
    }

    fun scanExecutablesInADrive(drives: String): List<String> {
        val aDrivePath = getADrivePath(drives) ?: return emptyList()
        val aDir = File(aDrivePath)
        if (!aDir.isDirectory) return emptyList()
        return aDir.walkTopDown()
            .filter { it.isFile && it.name.lowercase(Locale.ROOT).endsWith(".exe") }
            .map { it.absolutePath }
            .toList()
    }

    private fun findContainerByAppId(manager: ContainerManager, appId: String): Container? {
        val normalized = appId.trim()
        return manager.containers.firstOrNull { container ->
            resolveAppId(container).equals(normalized, ignoreCase = true)
        }
    }

    private fun resolveAppId(container: Container): String {
        val sessionAppId = container.getSessionMetadata(SESSION_APP_ID, "")
        if (sessionAppId.isNotEmpty()) return sessionAppId
        return container.getExtra(SESSION_APP_ID, "")
    }

    private fun createNewContainer(
        context: Context,
        appId: String,
        manager: ContainerManager,
        customConfig: ContainerData?,
    ): Container {
        check(Looper.myLooper() != Looper.getMainLooper()) {
            "ContainerUtils.getOrCreateContainer() must not block the main thread"
        }

        val containerData = (customConfig ?: getDefaultContainerData()).copy(
            name = (customConfig?.name?.takeIf { it.isNotBlank() } ?: resolveGameName(appId)),
        )

        val payload = JSONObject(containerData.toMap()).apply {
            put(
                "sessionMetadata",
                JSONObject()
                    .put(SESSION_APP_ID, appId)
                    .put(SESSION_GAME_SOURCE, extractGameSourceFromContainerId(appId).name)
                    .put(SESSION_DISPLAY_NAME, resolveGameName(appId)),
            )
        }

        val contentsManager = ContentsManager(context)
        val latch = CountDownLatch(1)
        var created: Container? = null
        var creationError: Throwable? = null

        manager.createContainerAsync(payload, contentsManager) { container ->
            created = container
            if (container == null) {
                creationError = IllegalStateException("ContainerManager.createContainerAsync returned null for $appId")
            } else {
                container.putSessionMetadata(SESSION_APP_ID, appId)
                container.putSessionMetadata(SESSION_GAME_SOURCE, extractGameSourceFromContainerId(appId).name)
                container.putSessionMetadata(SESSION_DISPLAY_NAME, resolveGameName(appId))
                container.saveData()
            }
            latch.countDown()
        }

        if (!latch.await(CREATE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw IllegalStateException("Timed out while creating container for $appId")
        }
        creationError?.let { throw IllegalStateException("Failed to create container for $appId", it) }
        return created ?: throw IllegalStateException("Missing container instance after creation for $appId")
    }

    private fun mapLanguageToLocale(language: String): String {
        return when (language.lowercase(Locale.ROOT)) {
            "arabic" -> "ar_SA.utf8"
            "bulgarian" -> "bg_BG.utf8"
            "schinese" -> "zh_CN.utf8"
            "tchinese" -> "zh_TW.utf8"
            "czech" -> "cs_CZ.utf8"
            "danish" -> "da_DK.utf8"
            "dutch" -> "nl_NL.utf8"
            "english" -> "en_US.utf8"
            "finnish" -> "fi_FI.utf8"
            "french" -> "fr_FR.utf8"
            "german" -> "de_DE.utf8"
            "greek" -> "el_GR.utf8"
            "hungarian" -> "hu_HU.utf8"
            "italian" -> "it_IT.utf8"
            "japanese" -> "ja_JP.utf8"
            "koreana" -> "ko_KR.utf8"
            "norwegian" -> "nb_NO.utf8"
            "polish" -> "pl_PL.utf8"
            "portuguese" -> "pt_PT.utf8"
            "brazilian" -> "pt_BR.utf8"
            "romanian" -> "ro_RO.utf8"
            "russian" -> "ru_RU.utf8"
            "spanish" -> "es_ES.utf8"
            "latam" -> "es_MX.utf8"
            "swedish" -> "sv_SE.utf8"
            "thai" -> "th_TH.utf8"
            "turkish" -> "tr_TR.utf8"
            "ukrainian" -> "uk_UA.utf8"
            "vietnamese" -> "vi_VN.utf8"
            else -> "en_US.utf8"
        }
    }
}
