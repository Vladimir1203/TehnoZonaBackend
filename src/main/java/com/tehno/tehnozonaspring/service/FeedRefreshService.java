package com.tehno.tehnozonaspring.service;

import com.tehno.tehnozonaspring.model.FeedSource;
import com.tehno.tehnozonaspring.model.XmlFeedHistory;
import com.tehno.tehnozonaspring.repository.FeedSourceRepository;
import com.tehno.tehnozonaspring.repository.XmlFeedHistoryRepository;
import com.tehno.tehnozonaspring.repository.VendorRepository;
import com.tehno.tehnozonaspring.util.CredentialManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.File;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class FeedRefreshService {

    private final FeedSourceRepository feedSourceRepository;
    private final XmlFeedHistoryRepository historyRepository;
    private final VendorRepository vendorRepository;
    private final EmailService emailService;
    private final org.springframework.web.client.RestTemplate restTemplate;

    public FeedRefreshService(FeedSourceRepository feedSourceRepository,
            XmlFeedHistoryRepository historyRepository,
            VendorRepository vendorRepository,
            EmailService emailService) {
        this.feedSourceRepository = feedSourceRepository;
        this.historyRepository = historyRepository;
        this.vendorRepository = vendorRepository;
        this.emailService = emailService;

        // Initialize RestTemplate with timeouts (30s connect, 60s read)
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(60000);
        this.restTemplate = new org.springframework.web.client.RestTemplate(factory);
    }

    public boolean refreshVendorFeed(Long vendorId) throws Exception {
        FeedSource source = feedSourceRepository.findByVendorId(vendorId)
                .orElseThrow(() -> new RuntimeException("Feed source not found for vendor " + vendorId));

        try {
            // Build the full URL with hidden credentials
            String finalUrl = buildAuthenticatedUrl(source);

            // 1. Download to Temp File using Streaming
            File tempFile = downloadToTemp(finalUrl);
            try {
                // 2. Hash Calculation
                String currentHash = calculateHash(tempFile);
                String lastHash = historyRepository.findLastHashByVendorId(vendorId);
                if (lastHash == null)
                    lastHash = "";

                if (currentHash.equals(lastHash)) {
                    System.out.println("No changes detected for vendor " + vendorId);
                    return false;
                }

                // 3. Validation
                validateXml(tempFile, source.getXsdPath());

                // 4. Save and Activate
                saveAndActivate(source, tempFile, currentHash);

                // Send success notification
                emailService.sendSuccessNotification(source.getVendor().getName(), currentHash);
                return true;
            } finally {
                Files.deleteIfExists(tempFile.toPath());
            }
        } catch (Exception e) {
            // Send email notification on failure
            emailService.sendErrorNotification(source.getVendor().getName(), e.getMessage());
            throw e; // Rethrow to maintain original behavior (e.g. controller receives it)
        }
    }

    private String buildAuthenticatedUrl(FeedSource source) {
        String baseUrl = source.getEndpointUrl();
        if (baseUrl == null || baseUrl.startsWith("classpath:"))
            return baseUrl;

        String vendorName = source.getVendor().getName().toLowerCase();
        String connector = baseUrl.contains("?") ? "&" : "?";

        if (vendorName.contains("uspon")) {
            return baseUrl + connector + CredentialManager.getUsponParams() + "&slike=1&opis=1";
        } else if (vendorName.contains("linkom")) {
            return baseUrl + connector + CredentialManager.getLinkomParams() + "&slike=1&opis=1&karakteristike=1";
        } else if (vendorName.contains("avtera")) {
            return baseUrl + connector + CredentialManager.getAvteraParams();
        }

        return baseUrl;
    }

    private File downloadToTemp(String urlString) throws Exception {
        File temp = File.createTempFile("feed_", ".xml");
        if (urlString == null || urlString.startsWith("classpath:")) {
            // Support local testing with classpath files
            String path = urlString != null ? urlString.replace("classpath:", "") : "TehnoZona-uspon.txt";
            try (java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
                if (is == null)
                    throw new RuntimeException("Resource not found: " + path);
                Files.copy(is, temp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } else {
            restTemplate.execute(urlString, org.springframework.http.HttpMethod.valueOf("GET"), null, response -> {
                Files.copy(response.getBody(), temp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return temp;
            });
        }
        return temp;
    }

    private String calculateHash(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (java.io.InputStream is = Files.newInputStream(file.toPath())) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private void validateXml(File file, String xsdPath) throws Exception {
        if (xsdPath == null || xsdPath.isEmpty())
            return;

        javax.xml.validation.SchemaFactory factory = javax.xml.validation.SchemaFactory
                .newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try (java.io.InputStream xsdStream = getClass().getClassLoader().getResourceAsStream(xsdPath)) {
            if (xsdStream == null)
                throw new RuntimeException("XSD not found: " + xsdPath);
            javax.xml.validation.Schema schema = factory
                    .newSchema(new javax.xml.transform.stream.StreamSource(xsdStream));
            javax.xml.validation.Validator validator = schema.newValidator();
            validator.validate(new javax.xml.transform.stream.StreamSource(file));
        }
    }

    @Transactional
    protected void saveAndActivate(FeedSource source, File file, String hash) throws Exception {
        Long vendorId = source.getVendor().getId();
        String content = Files.readString(file.toPath());

        // Archive current active record using native query
        historyRepository.archiveCurrentActive(vendorId);

        // Create new history record using native query for XML casting
        historyRepository.insertWithXml(
                vendorId,
                content,
                "ACTIVE",
                hash,
                java.time.LocalDateTime.now());

        // Update main Vendor table
        vendorRepository.insertVendor(vendorId, source.getVendor().getName(), content);

        // Cleanup: keep only the latest ACTIVE and one latest ARCHIVED
        historyRepository.cleanupOldFeeds(vendorId);
    }
}
