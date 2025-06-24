package com.spring.outfit_rater.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl;

    public String saveImage(MultipartFile file) {
        if (file.isEmpty() || !file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("Please upload a valid image file");
        }

        if (file.getSize() > 10 * 1024 * 1024) { // 10MB
            throw new IllegalArgumentException("Image must be smaller than 10MB");
        }

        try {
            // Create directory if not exists
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);

            // Generate unique filename
            String filename = UUID.randomUUID().toString() + getFileExtension(file.getOriginalFilename());
            Path filePath = uploadPath.resolve(filename);

            // Save file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String imageUrl = baseUrl + "/images/" + filename;
            log.info("Image saved: {}", imageUrl);
            return imageUrl;

        } catch (IOException e) {
            log.error("Failed to save image", e);
            throw new RuntimeException("Failed to save image");
        }
    }

    private String getFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return ".jpg";
    }
}