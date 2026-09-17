package com.t3code.explorer.data.viewpc

import java.io.File
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.net.URLConnection
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Read-only, token-protected local server. Upload is intentionally not advertised. */
class ViewOnPcServer(private val root: File) {
    private var executor: ExecutorCoroutineDispatcher? = null
    private var socket: ServerSocket? = null
    private var job: Job? = null
    var token: String? = null
        private set
    var port: Int? = null
        private set

    suspend fun start(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            check(job == null) { "Server is already running" }
            require(root.isDirectory) { "Server root is unavailable" }
            token = UUID.randomUUID().toString().replace("-", "")
            executor = Executors.newCachedThreadPool().asCoroutineDispatcher()
            socket = ServerSocket(0)
            port = socket!!.localPort
            job = launch(executor!!) { acceptLoop() }
            port!!
        }
    }

    suspend fun stop() = withContext(Dispatchers.IO) {
        job?.cancel()
        socket?.close()
        executor?.close()
        executor = null
        job = null
        socket = null
        port = null
        token = null
    }

    private suspend fun acceptLoop() = withContext(Dispatchers.IO) {
        while (socket?.isClosed == false) {
            try {
                val client = socket?.accept() ?: continue
                val dispatcher = executor
                if (dispatcher == null) {
                    client.close()
                    break
                }
                launch(dispatcher) { serve(client) }
            } catch (_: IOException) {
                if (socket?.isClosed != true) continue
            }
        }
    }

    private fun serve(client: Socket) {
        client.use { connection ->
            val request = connection.getInputStream().bufferedReader().readLine().orEmpty()
            val parts = request.split(' ')
            val method = parts.getOrNull(0).orEmpty()
            val path = parts.getOrNull(1).orEmpty()
            if (method != "GET") {
                respond(connection, "405 Method Not Allowed", "text/plain; charset=utf-8", "GET only")
                return@use
            }
            val requestedToken = path.substringAfter("token=", "").substringBefore('&')
            if (token.isNullOrBlank() || requestedToken != token) {
                respond(connection, "401 Unauthorized", "text/plain; charset=utf-8", "Unauthorized")
                return@use
            }
            val relative = runCatching {
                URLDecoder.decode(path.substringBefore('?').removePrefix("/"), StandardCharsets.UTF_8.name())
            }.getOrElse {
                respond(connection, "400 Bad Request", "text/plain; charset=utf-8", "Bad request")
                return@use
            }
            val canonicalRoot = root.canonicalFile
            val file = File(canonicalRoot, relative).canonicalFile
            if (!file.path.startsWith(canonicalRoot.path + File.separator) && file != canonicalRoot) {
                respond(connection, "403 Forbidden", "text/plain; charset=utf-8", "Forbidden")
                return@use
            }
            if (!file.exists()) {
                respond(connection, "404 Not Found", "text/plain; charset=utf-8", "Not found")
                return@use
            }
            if (file.isFile) {
                val type = URLConnection.guessContentTypeFromName(file.name) ?: "application/octet-stream"
                val output = connection.getOutputStream()
                output.write("HTTP/1.1 200 OK\r\nContent-Length: ${file.length()}\r\nContent-Type: $type\r\nX-Content-Type-Options: nosniff\r\n\r\n".toByteArray(StandardCharsets.US_ASCII))
                output.flush()
                file.inputStream().use { it.copyTo(output) }
                output.flush()
            } else {
                val html = file.listFiles().orEmpty().joinToString("", prefix = "<html><body>", postfix = "</body></html>") { child ->
                    val safeName = escapeHtml(child.name)
                    val relativeChild = child.relativeTo(canonicalRoot).path.split(File.separator).joinToString("/") {
                        URLEncoder.encode(it, StandardCharsets.UTF_8.name()).replace("+", "%20")
                    }
                    "<a href=\"/${relativeChild}?token=$token\">$safeName</a><br>"
                }
                respond(connection, "200 OK", "text/html; charset=utf-8", html)
            }
        }
    }

    private fun respond(connection: Socket, status: String, type: String, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        connection.getOutputStream().use { output ->
            output.write("HTTP/1.1 $status\r\nContent-Type: $type\r\nContent-Length: ${bytes.size}\r\nX-Content-Type-Options: nosniff\r\n\r\n".toByteArray(StandardCharsets.US_ASCII))
            output.write(bytes)
            output.flush()
        }
    }

    private fun escapeHtml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
