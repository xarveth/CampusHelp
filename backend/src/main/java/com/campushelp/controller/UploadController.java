package com.campushelp.controller;

import com.campushelp.service.PhotoStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

/**
 * Serves complaint photos. Filenames are random UUIDs (not guessable) and the
 * endpoint is public so <img> tags can load them without a JWT header.
 */
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final PhotoStorageService photos;

    public UploadController(PhotoStorageService photos) {
        this.photos = photos;
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serve(@PathVariable String filename) throws IOException {
        Resource resource = photos.load(filename);
        String contentType = Files.probeContentType(resource.getFile().toPath());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + resource.getFilename() + "\"")
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                .body(resource);
    }
}
