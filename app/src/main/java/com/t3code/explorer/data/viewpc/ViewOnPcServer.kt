package com.t3code.explorer.data.viewpc

import java.io.File
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ViewOnPcServer(private val root: File) {
    private val executor = Executors.newCachedThreadPool().asCoroutineDispatcher()
    private var socket: ServerSocket? = null
    private var job: Job? = null
    var token: String? = null
        private set
    var port: Int? = null
        private set

    suspend fun start(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            check(job == null) { "Server is already running" }
            token = UUID.randomUUID().toString().replace("-", "")
            socket = ServerSocket(0)
            port = socket!!.localPort
            job = launch(executor) { acceptLoop() }
            port!!
        }
    }

    suspend fun stop() = withContext(Dispatchers.IO) { job?.cancel(); job = null; socket?.close(); socket = null; port = null; token = null }

    private suspend fun acceptLoop() = withContext(Dispatchers.IO) {
        while (job?.isActive == true) runCatching { socket?.accept()?.let { client -> launch(executor) { serve(client) } } }
    }

    private fun serve(client: Socket) {
        client.use { socket ->
            val request = socket.getInputStream().bufferedReader().readLine().orEmpty()
            val path = request.split(' ').getOrNull(1).orEmpty()
            val requestedToken = path.substringAfter("token=", "").substringBefore('&')
            if (requestedToken != token) return@use
            val relative = URLDecoder.decode(path.substringBefore('?').removePrefix("/"), StandardCharsets.UTF_8.name())
            val file = File(root, relative).canonicalFile
            if (file.path != root.canonicalPath && !file.path.startsWith(root.canonicalPath + File.separator)) return@use
            val output = socket.getOutputStream().bufferedWriter()
            if (file.isFile) {
                output.write("HTTP/1.1 200 OK\r\nContent-Length: ${file.length()}\r\nContent-Type: application/octet-stream\r\n\r\n"); output.flush(); file.inputStream().use { it.copyTo(socket.getOutputStream()) }
            } else {
                val html = file.listFiles()?.joinToString("", prefix = "<html><body>", postfix = "</body></html>") { "<a href=\"/${it.relativeTo(root).path}?token=$token\">${it.name}</a><br>" } ?: "<html><body>Not found</body></html>"
                output.write("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${html.toByteArray().size}\r\n\r\n$html"); output.flush()
            }
        }
    }
}
