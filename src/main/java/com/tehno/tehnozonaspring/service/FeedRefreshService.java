package com.tehno.tehnozonaspring.service;

import com.tehno.tehnozonaspring.model.FeedSource;
import com.tehno.tehnozonaspring.repository.FeedSourceRepository;
import com.tehno.tehnozonaspring.repository.XmlFeedHistoryRepository;
import com.tehno.tehnozonaspring.repository.VendorRepository;
import com.tehno.tehnozonaspring.util.CredentialManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class FeedRefreshService {

    private final FeedSourceRepository feedSourceRepository;
    private final XmlFeedHistoryRepository historyRepository;
    private final VendorRepository vendorRepository;
    private final EmailService emailService;
    private final JdbcTemplate jdbcTemplate;
    private final org.springframework.web.client.RestTemplate restTemplate;

    public FeedRefreshService(FeedSourceRepository feedSourceRepository,
            XmlFeedHistoryRepository historyRepository,
            VendorRepository vendorRepository,
            EmailService emailService,
            JdbcTemplate jdbcTemplate) {
        this.feedSourceRepository = feedSourceRepository;
        this.historyRepository = historyRepository;
        this.vendorRepository = vendorRepository;
        this.emailService = emailService;
        this.jdbcTemplate = jdbcTemplate;

        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(60000);
        this.restTemplate = new org.springframework.web.client.RestTemplate(factory);
    }

    public boolean refreshVendorFeed(Long vendorId) throws Exception {
        FeedSource source = feedSourceRepository.findByVendorId(vendorId)
                .orElseThrow(() -> new RuntimeException("Feed source not found for vendor " + vendorId));

        try {
            String finalUrl = buildAuthenticatedUrl(source);
            File tempFile = downloadToTemp(finalUrl);
            try {
                String currentHash = calculateHash(tempFile);
                String lastHash = historyRepository.findLastHashByVendorId(vendorId);
                if (lastHash == null) lastHash = "";

                if (currentHash.equals(lastHash)) {
                    System.out.println("No changes detected for vendor " + vendorId);
                    return false;
                }

                validateXml(tempFile, source.getXsdPath());
                saveAndActivate(vendorId, tempFile, currentHash);
                emailService.sendSuccessNotification(source.getVendor().getName(), currentHash);
                return true;
            } finally {
                Files.deleteIfExists(tempFile.toPath());
            }
        } catch (Exception e) {
            emailService.sendErrorNotification(source.getVendor().getName(), e.getMessage());
            throw e;
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
            return baseUrl + connector + CredentialManager.getAvteraParams() + "&slike=1&opis=1";
        }

        return baseUrl;
    }

    private File downloadToTemp(String urlString) throws Exception {
        File temp = File.createTempFile("feed_", ".xml");
        if (urlString == null || urlString.startsWith("classpath:")) {
            String path = urlString != null ? urlString.replace("classpath:", "") : "TehnoZona-uspon.txt";
            try (java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
                if (is == null) throw new RuntimeException("Resource not found: " + path);
                Files.copy(is, temp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } else {
            restTemplate.execute(urlString, org.springframework.http.HttpMethod.GET, null, response -> {
                // Detect charset from Content-Type header, default to UTF-8
                org.springframework.http.MediaType contentType = response.getHeaders().getContentType();
                Charset sourceCharset = (contentType != null && contentType.getCharset() != null)
                        ? contentType.getCharset()
                        : StandardCharsets.UTF_8;

                if (sourceCharset.equals(StandardCharsets.UTF_8)) {
                    // No conversion needed
                    Files.copy(response.getBody(), temp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } else {
                    // Re-encode to UTF-8 and fix XML declaration
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody(), sourceCharset));
                         PrintWriter writer = new PrintWriter(Files.newBufferedWriter(temp.toPath(), StandardCharsets.UTF_8))) {
                        String line;
                        boolean firstLine = true;
                        while ((line = reader.readLine()) != null) {
                            if (firstLine) {
                                // Replace or inject UTF-8 XML declaration
                                if (line.startsWith("<?xml")) {
                                    line = line.replaceAll("encoding=['\"][^'\"]*['\"]", "encoding=\"UTF-8\"");
                                    if (!line.contains("encoding=")) {
                                        line = line.replace("?>", " encoding=\"UTF-8\"?>");
                                    }
                                }
                                firstLine = false;
                            }
                            writer.println(line);
                        }
                    }
                }
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
        if (xsdPath == null || xsdPath.isEmpty()) return;

        javax.xml.validation.SchemaFactory factory = javax.xml.validation.SchemaFactory
                .newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try (java.io.InputStream xsdStream = getClass().getClassLoader().getResourceAsStream(xsdPath)) {
            if (xsdStream == null) throw new RuntimeException("XSD not found: " + xsdPath);
            javax.xml.validation.Schema schema = factory.newSchema(new javax.xml.transform.stream.StreamSource(xsdStream));
            schema.newValidator().validate(new javax.xml.transform.stream.StreamSource(file));
        }
    }

    @Transactional
    protected void saveAndActivate(Long vendorId, File file, String hash) throws Exception {
        historyRepository.archiveCurrentActive(vendorId);

        // Use PGobject to pass XML to JDBC without triple-copying through String + Hibernate
        // File is read once, wrapped in PGobject, sent to DB, then eligible for GC
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        org.postgresql.util.PGobject xmlObj = new org.postgresql.util.PGobject();
        xmlObj.setType("xml");
        xmlObj.setValue(content);

        jdbcTemplate.update(
                "INSERT INTO xml_feed_history (vendor_id, xml_content, status, hash_sum, created_at) VALUES (?, ?, ?, ?, ?)",
                vendorId, xmlObj, "ACTIVE", hash, LocalDateTime.now());

        // content String is no longer referenced after this point - eligible for GC
        // Copy XML from history to vendor entirely within DB - zero additional Java heap
        vendorRepository.syncVendorXmlFromHistory(vendorId);

        historyRepository.cleanupOldFeeds(vendorId);
    }
}
