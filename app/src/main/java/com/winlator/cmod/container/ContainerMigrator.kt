package com.winlator.cmod.container

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.winlator.cmod.core.FileUtils
import com.winlator.cmod.xenvironment.ImageFs
import java.io.File
import org.json.JSONObject

object ContainerMigrator {
    private const val TAG = "ContainerMigrator"
    private const val LATEST_CONTAINER_MIGRATION_VERSION = 1
    private val mainHandler = Handler(Looper.getMainLooper())

    private inline fun postMain(crossinline block: () -> Unit) = mainHandler.post { block() }

    private fun createContainerMigrationVersionFile(context: Context, version: Int) {
        val imageFs = ImageFs.find(context)
        val configDir = imageFs.configDir
        configDir.mkdirs()
        val versionFile = File(configDir, ".container_migration_version")
        try {
            versionFile.createNewFile()
            FileUtils.writeString(versionFile, version.toString())
        } catch (error: Exception) {
            Log.e(TAG, "Failed to create container migration version file", error)
        }
    }

    private fun getContainerMigrationVersion(context: Context): Int {
        val imageFs = ImageFs.find(context)
        val versionFile = File(imageFs.configDir, ".container_migration_version")
        return if (versionFile.exists()) {
            try {
                FileUtils.readLines(versionFile)[0].toInt()
            } catch (error: Exception) {
                Log.e(TAG, "Failed to read container migration version", error)
                0
            }
        } else {
            0
        }
    }

    fun isContainerMigrationNeeded(context: Context): Boolean {
        val currentVersion = getContainerMigrationVersion(context)
        return currentVersion < LATEST_CONTAINER_MIGRATION_VERSION
    }

    fun migrateLegacyContainersIfNeeded(
        context: Context,
        onProgressUpdate: ((currentContainer: String, migratedContainers: Int, totalContainers: Int) -> Unit)? = null,
        onComplete: ((migratedCount: Int) -> Unit)? = null,
    ) {
        try {
            if (!isContainerMigrationNeeded(context)) {
                postMain { onComplete?.invoke(0) }
                return
            }

            val imageFs = ImageFs.find(context)
            val homeDir = File(imageFs.rootDir, "home")
            val legacyContainers = homeDir.listFiles()?.filter { file ->
                file.isDirectory &&
                    file.name != ImageFs.USER &&
                    file.name.startsWith("${ImageFs.USER}-") &&
                    file.name.removePrefix("${ImageFs.USER}-").matches(Regex("\\d+")) &&
                    File(file, ".container").exists()
            } ?: emptyList()

            val totalContainers = legacyContainers.size
            var migratedContainers = 0

            for (legacyDir in legacyContainers) {
                val legacyId = legacyDir.name.removePrefix("${ImageFs.USER}-")
                val newContainerId = "STEAM_$legacyId"
                val newDir = File(homeDir, "${ImageFs.USER}-$newContainerId")

                postMain {
                    onProgressUpdate?.invoke(legacyId, migratedContainers, totalContainers)
                }

                try {
                    var finalContainerId = newContainerId
                    var finalNewDir = newDir
                    var counter = 1
                    while (finalNewDir.exists()) {
                        finalContainerId = "STEAM_$legacyId($counter)"
                        finalNewDir = File(homeDir, "${ImageFs.USER}-$finalContainerId")
                        counter++
                    }

                    if (legacyDir.renameTo(finalNewDir)) {
                        updateContainerConfig(finalNewDir, finalContainerId)

                        val activeSymlink = File(homeDir, ImageFs.USER)
                        if (activeSymlink.exists() && activeSymlink.canonicalPath.endsWith(legacyId)) {
                            activeSymlink.delete()
                            FileUtils.symlink("./${ImageFs.USER}-$finalContainerId", activeSymlink.path)
                        }

                        migratedContainers++
                    }
                } catch (error: Exception) {
                    Log.e(TAG, "Error migrating container $legacyId", error)
                }
            }

            createContainerMigrationVersionFile(context, LATEST_CONTAINER_MIGRATION_VERSION)
            postMain { onComplete?.invoke(migratedContainers) }
        } catch (error: Exception) {
            Log.e(TAG, "Error during container migration", error)
            createContainerMigrationVersionFile(context, LATEST_CONTAINER_MIGRATION_VERSION)
            postMain { onComplete?.invoke(0) }
        }
    }

    private fun updateContainerConfig(containerDir: File, newContainerId: String) {
        try {
            val configFile = File(containerDir, ".container")
            val configContent = FileUtils.readString(configFile)
            val data = JSONObject(configContent)
            data.put("id", newContainerId)
            FileUtils.writeString(configFile, data.toString())
        } catch (error: Exception) {
            Log.e(TAG, "Failed to update container config for $newContainerId", error)
        }
    }
}
