package com.davidwilson.delphi.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@Service
public class FileUploadService {
    @Value("${file.upload-dir}")
    private String uploadDir;

    public String saveFile(MultipartFile file) throws IOException {
        // Create dir if not exists or configure BLOB storage
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        Path destinationPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path filePath = destinationPath.resolve(Objects.requireNonNull(file.getOriginalFilename())); // Set the file path

        // Copy the file to the dest path, overwrite if exists
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filePath.toString();
    }
}

