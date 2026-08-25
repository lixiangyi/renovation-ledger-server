package com.renovation.ledger.server.auth

import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class AvatarController(
    private val authService: AuthService,
    private val avatarStorage: AvatarStorageService,
) {
    @PostMapping("/me/avatar")
    fun upload(@RequestParam("file") file: MultipartFile): MeResponse =
        authService.updateAvatar(file)

    @DeleteMapping("/me/avatar")
    fun clear(): MeResponse = authService.clearAvatar()

    @GetMapping("/avatars/{name}")
    fun get(@PathVariable name: String): ResponseEntity<FileSystemResource> {
        val path = avatarStorage.resolveFile(name)
            ?: return ResponseEntity.notFound().build()
        val media = when (path.fileName.toString().substringAfterLast('.', "").lowercase()) {
            "png" -> MediaType.IMAGE_PNG
            else -> MediaType.IMAGE_JPEG
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
            .contentType(media)
            .body(FileSystemResource(path))
    }
}
