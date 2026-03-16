package com.tehno.tehnozonaspring.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class CategoryImageService {

    @org.springframework.beans.factory.annotation.Value("${category.images.path:category-images}")
    private String storagePath;

    public String getCachedImageUrl(String nadgrupaName) {
        if (!StringUtils.hasText(nadgrupaName)) {
            return null;
        }
        String fileName = sanitizeFileName(nadgrupaName) + ".jpg";
        File imageFile = new File(storagePath, fileName);
        if (imageFile.exists()) {
            return "/api/images/categories/" + fileName;
        }
        return null;
    }

    public String getOrDownloadImage(String nadgrupaName, String sampleUrl) {
        if (!StringUtils.hasText(nadgrupaName)) {
            return null;
        }

        String fileName = sanitizeFileName(nadgrupaName) + ".jpg";
        File directory = new File(storagePath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File imageFile = new File(directory, fileName);

        // Ako slika već postoji, samo vrati putanju
        if (imageFile.exists()) {
            return "/api/images/categories/" + fileName;
        }

        // Ako ne postoji i imamo URL, skini je
        if (StringUtils.hasText(sampleUrl)) {
            try {
                URL url = new URL(sampleUrl);
                try (InputStream in = url.openStream()) {
                    Files.copy(in, imageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("IMAGE CACHE: Downloaded and saved category image: " + fileName);
                    return "/api/images/categories/" + fileName;
                }
            } catch (Exception e) {
                System.err.println(
                        "IMAGE CACHE ERROR: Failed to download image from " + sampleUrl + ": " + e.getMessage());
            }
        }

        return null;
    }

    private String sanitizeFileName(String name) {
        return name.toLowerCase()
                .replace("š", "s")
                .replace("đ", "dj")
                .replace("č", "c")
                .replace("ć", "c")
                .replace("ž", "z")
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
