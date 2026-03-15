package com.winlator.cmod.container

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.text.Charsets

object ContainerConfigTransfer {
    suspend fun exportConfig(
        context: Context,
        appId: String,
        uri: Uri,
    ): Boolean {
        return try {
            val jsonText = withContext(Dispatchers.IO) {
                val container = ContainerUtils.getOrCreateContainer(context, appId)
                JSONObject(container.containerJson).toString(2)
            }

            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonText.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun importConfig(
        context: Context,
        appId: String,
        uri: Uri,
    ): Boolean {
        return try {
            val jsonText = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.orEmpty()

            if (jsonText.isBlank()) return false

            val importedMap = withContext(Dispatchers.Default) {
                jsonObjectToMap(JSONObject(jsonText))
            }

            withContext(Dispatchers.IO) {
                val container = ContainerUtils.getOrCreateContainer(context, appId)
                val currentData = ContainerUtils.toContainerData(container)
                val updatedData = ContainerUtils.applyBestConfigMapToContainerData(currentData, importedMap)
                ContainerUtils.applyToContainer(context, container, updatedData)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun jsonObjectToMap(jsonObject: JSONObject): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        val iterator = jsonObject.keys()
        while (iterator.hasNext()) {
            val key = iterator.next()
            val value = jsonObject.opt(key)
            when (value) {
                null,
                JSONObject.NULL -> result[key] = null
                is JSONObject -> {
                    // Keep nested objects out of the flat container snapshot lane for now.
                }
                else -> result[key] = value
            }
        }
        return result
    }
}
