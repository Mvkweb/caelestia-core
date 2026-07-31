package com.caelestia.paper.emoji

import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.net.URL
import javax.imageio.ImageIO

object EmojiProcessor {
    fun downloadAndResize(imageUrl: String, targetFile: File, size: Int = 32): Boolean {
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            
            val original: BufferedImage = ImageIO.read(connection.inputStream) ?: return false
            val resized = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
            val graphics: Graphics2D = resized.createGraphics()
            
            // Apply high-quality interpolation to keep the resized image clean
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            graphics.drawImage(original, 0, 0, size, size, null)
            graphics.dispose()
            
            ImageIO.write(resized, "png", targetFile)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun resizeLocal(sourceFile: File, targetFile: File, size: Int = 32): Boolean {
        return try {
            val original: BufferedImage = ImageIO.read(sourceFile) ?: return false
            val resized = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
            val graphics: Graphics2D = resized.createGraphics()
            
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            graphics.drawImage(original, 0, 0, size, size, null)
            graphics.dispose()
            
            ImageIO.write(resized, "png", targetFile)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
