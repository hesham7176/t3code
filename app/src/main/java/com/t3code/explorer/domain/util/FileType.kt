package com.t3code.explorer.domain.util

import com.t3code.explorer.domain.model.FileCategory
import java.util.Locale

object FileType {
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "avif")
    private val videoExtensions = setOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "m4v", "ts", "mpeg", "mpg")
    private val audioExtensions = setOf("mp3", "wav", "flac", "aac", "m4a", "ogg", "opus", "wma")
    private val archiveExtensions = setOf("zip", "rar", "7z", "tar", "gz", "bz2")
    private val documentExtensions = setOf("txt", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "json", "xml", "html", "htm", "css", "js", "kt", "java", "csv")

    fun extension(name: String): String = name.substringAfterLast('.', "").lowercase(Locale.ROOT)

    fun category(name: String, mimeType: String = ""): FileCategory {
        val ext = extension(name)
        return when {
            mimeType.startsWith("image/") || ext in imageExtensions -> FileCategory.IMAGE
            mimeType.startsWith("video/") || ext in videoExtensions -> FileCategory.VIDEO
            mimeType.startsWith("audio/") || ext in audioExtensions -> FileCategory.AUDIO
            ext == "apk" || mimeType == "application/vnd.android.package-archive" -> FileCategory.APK
            ext in archiveExtensions -> FileCategory.ARCHIVE
            mimeType.startsWith("text/") || ext in documentExtensions -> FileCategory.DOCUMENT
            else -> FileCategory.OTHER
        }
    }

    fun mimeType(name: String): String = when (extension(name)) {
        in imageExtensions -> "image/*"
        in videoExtensions -> "video/*"
        in audioExtensions -> "audio/*"
        "pdf" -> "application/pdf"
        "zip" -> "application/zip"
        "json" -> "application/json"
        "xml" -> "text/xml"
        "html", "htm" -> "text/html"
        "txt", "csv", "css", "js", "kt", "java" -> "text/plain"
        "apk" -> "application/vnd.android.package-archive"
        else -> "application/octet-stream"
    }

    fun isImage(name: String) = category(name) == FileCategory.IMAGE
    fun isVideo(name: String) = category(name) == FileCategory.VIDEO
    fun isAudio(name: String) = category(name) == FileCategory.AUDIO
    fun isText(name: String) = category(name) == FileCategory.DOCUMENT && extension(name) in documentExtensions
}

fun Long.readableFileSize(): String {
    if (this < 1024) return "$this B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = toDouble()
    var index = -1
    do { value /= 1024; index++ } while (value >= 1024 && index < units.lastIndex)
    return "%.1f %s".format(Locale.getDefault(), value, units[index])
}
