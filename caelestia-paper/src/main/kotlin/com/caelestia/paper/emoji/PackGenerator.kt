package com.caelestia.paper.emoji

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object PackGenerator {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun generatePack(outputZip: File, mappings: Map<String, String>, sourceImagesDir: File) {
        val tempDir = File(outputZip.parentFile, "temp_pack")
        tempDir.deleteRecursively()
        tempDir.mkdirs()

        // 1. Write pack.mcmeta
        val packMcmeta = File(tempDir, "pack.mcmeta")
        val root = JsonObject()
        val pack = JsonObject()
        pack.addProperty("description", "Caelestia Emojis")
        
        // Multi-version support
        pack.addProperty("pack_format", 75)
        val supportedFormats = JsonArray()
        supportedFormats.add(75)
        supportedFormats.add(88)
        pack.add("supported_formats", supportedFormats)
        
        root.add("pack", pack)
        packMcmeta.writeText(gson.toJson(root))

        // 2. Write font images
        val fontDir = File(tempDir, "assets/minecraft/textures/font")
        fontDir.mkdirs()
        sourceImagesDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".png")) {
                file.copyTo(File(fontDir, file.name), overwrite = true)
            }
        }

        // 3. Write default.json
        val fontConfigDir = File(tempDir, "assets/minecraft/font")
        fontConfigDir.mkdirs()
        val defaultJson = File(fontConfigDir, "default.json")
        
        val fontRoot = JsonObject()
        val providers = JsonArray()
        
        mappings.forEach { (name, unicode) ->
            val provider = JsonObject()
            provider.addProperty("type", "bitmap")
            provider.addProperty("file", "minecraft:font/$name.png")
            provider.addProperty("ascent", 8)
            provider.addProperty("height", 8)
            val chars = JsonArray()
            chars.add(unicode)
            provider.add("chars", chars)
            providers.add(provider)
        }
        
        fontRoot.add("providers", providers)
        defaultJson.writeText(gson.toJson(fontRoot))

        // 4. Zip the folder
        zipDirectory(tempDir, outputZip)
        tempDir.deleteRecursively()
    }

    private fun zipDirectory(dir: File, outputZip: File) {
        ZipOutputStream(FileOutputStream(outputZip)).use { zipOut ->
            dir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val entryName = file.relativeTo(dir).path.replace("\\\\", "/")
                    zipOut.putNextEntry(ZipEntry(entryName))
                    file.inputStream().use { it.copyTo(zipOut) }
                    zipOut.closeEntry()
                }
            }
        }
    }
}
