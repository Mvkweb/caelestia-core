package com.caelestia.paper.emoji

import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import java.security.MessageDigest

object HttpPackServer {
    private var server: HttpServer? = null
    var packHash: ByteArray = ByteArray(0)
    var packBytes: ByteArray = ByteArray(0)

    fun start(port: Int, packFile: File) {
        stop()
        if (!packFile.exists()) return
        
        packBytes = packFile.readBytes()
        val md = MessageDigest.getInstance("SHA-1")
        packHash = md.digest(packBytes)

        server = HttpServer.create(InetSocketAddress(port), 0)
        server?.createContext("/emojis.zip") { exchange ->
            if (exchange.requestMethod.equals("GET", ignoreCase = true)) {
                exchange.responseHeaders.add("Content-Type", "application/zip")
                exchange.sendResponseHeaders(200, packBytes.size.toLong())
                exchange.responseBody.use { os ->
                    os.write(packBytes)
                }
            } else {
                exchange.sendResponseHeaders(405, -1)
            }
        }
        server?.start()
    }

    fun stop() {
        server?.stop(0)
        server = null
    }
}
