package com.tehno.tehnozonaspring.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CloudinaryService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryService.class);

    private final Cloudinary cloudinary;

    public CloudinaryService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    /**
     * Uploaduje sliku sa URL-a na Cloudinary i vraca https URL.
     * Ako upload ne uspe, vraca originalni URL.
     * Koristi public_id baziran na originalnom URL-u da bi se izbeglo
     * dupliranje iste slike pri ponovnom importu.
     */
    public String uploadFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return imageUrl;
        try {
            String publicId = "avtera/" + extractPublicId(imageUrl);
            Map<?, ?> result = cloudinary.uploader().upload(imageUrl, ObjectUtils.asMap(
                    "public_id", publicId,
                    "overwrite", false,
                    "resource_type", "image"
            ));
            String secureUrl = (String) result.get("secure_url");
            if (secureUrl != null) return secureUrl;
        } catch (Exception e) {
            log.warn("Cloudinary upload failed for {}: {}", imageUrl, e.getMessage());
        }
        return imageUrl;
    }

    private String extractPublicId(String url) {
        String id = url.replaceAll("https?://", "")
                       .replaceAll("[^a-zA-Z0-9_.-]", "_");
        if (id.length() > 200) id = id.substring(id.length() - 200);
        return id;
    }
}
