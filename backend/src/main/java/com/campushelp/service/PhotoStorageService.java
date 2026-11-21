package com.campushelp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

/**
 * Stores complaint photos on disk (outside the jar) and serves them back.
 * Directory is configurable: app.upload.dir (UPLOAD_DIR env var), default ./uploads.
 * Filenames are random UUIDs, so photo URLs cannot be guessed.
 */
@Service
public class PhotoStorageService {

    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final List<String> ALLOWED_TYPES =
            List.of("image/jpeg", "image/png", "image/webp");

    private final Path uploadDir;

    public PhotoStorageService(@Value("${app.upload.dir:./uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /** Validates and stores the file; returns the public URL path, e.g. /api/uploads/<uuid>.jpg */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo must be 5 MB or smaller");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Photo must be a JPG, PNG or WebP image");
        }
        String extension = switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        String filename = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(uploadDir);
            Files.copy(file.getInputStream(), uploadDir.resolve(filename),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not save photo");
        }
        return "/api/uploads/" + filename;
    }

    public Resource load(String filename) {
        if (filename == null || filename.contains("..") || filename.contains("/")
                || filename.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }
        try {
            Path file = uploadDir.resolve(filename).normalize();
            if (!file.startsWith(uploadDir) || !Files.isRegularFile(file)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found");
            }
            return new UrlResource(file.toUri());
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found");
        }
    }
}
