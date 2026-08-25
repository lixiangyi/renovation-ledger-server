package com.renovation.ledger.server.auth

import com.renovation.ledger.server.error.ApiException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

@Service
class AvatarStorageService(
    @Value("\${app.avatar.dir:./data/avatars}") private val dirConfig: String,
    @Value("\${app.avatar.max-bytes:2097152}") private val maxBytes: Long,
) {
    private val root: Path by lazy {
        Path.of(dirConfig).toAbsolutePath().normalize().also { it.createDirectories() }
    }

    fun save(file: MultipartFile): String {
        if (file.isEmpty) {
            throw ApiException(400, "BAD_REQUEST", "请选择图片")
        }
        if (file.size > maxBytes) {
            throw ApiException(400, "BAD_REQUEST", "图片不能超过 2MB")
        }
        val ext = resolveExt(file)
        val name = "${UUID.randomUUID()}.$ext"
        val target = root.resolve(name)
        file.inputStream.use { input ->
            Files.copy(input, target)
        }
        return "/avatars/$name"
    }

    fun deleteByUrl(avatarUrl: String?) {
        val name = fileNameOf(avatarUrl) ?: return
        root.resolve(name).deleteIfExists()
    }

    fun resolveFile(relativePath: String): Path? {
        val name = relativePath.trim().removePrefix("/").removePrefix("avatars/").trim('/')
        if (name.isEmpty() || name.contains("..") || name.contains('/')) return null
        val file = root.resolve(name).normalize()
        if (!file.startsWith(root)) return null
        return file.takeIf { it.exists() && it.isRegularFile() }
    }

    private fun resolveExt(file: MultipartFile): String {
        val contentType = file.contentType?.lowercase().orEmpty()
        val original = file.originalFilename?.lowercase().orEmpty()
        return when {
            contentType == "image/jpeg" || original.endsWith(".jpg") || original.endsWith(".jpeg") -> "jpg"
            contentType == "image/png" || original.endsWith(".png") -> "png"
            else -> throw ApiException(400, "BAD_REQUEST", "仅支持 JPG / PNG")
        }
    }

    private fun fileNameOf(avatarUrl: String?): String? {
        val raw = avatarUrl?.trim().orEmpty()
        if (!raw.startsWith("/avatars/")) return null
        val name = raw.removePrefix("/avatars/")
        if (name.isEmpty() || name.contains("..") || name.contains('/')) return null
        return name
    }
}
