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
    private final CredentialManager credentialManager;
    private final ArticalImportService artikalImportService;
    private final org.springframework.web.client.RestTemplate restTemplate;

    public FeedRefreshService(FeedSourceRepository feedSourceRepository,
            XmlFeedHistoryRepository historyRepository,
            VendorRepository vendorRepository,
            EmailService emailService,
            JdbcTemplate jdbcTemplate,
            CredentialManager credentialManager,
            ArticalImportService artikalImportService) {
        this.feedSourceRepository = feedSourceRepository;
        this.historyRepository = historyRepository;
        this.vendorRepository = vendorRepository;
        this.emailService = emailService;
        this.jdbcTemplate = jdbcTemplate;
        this.credentialManager = credentialManager;
        this.artikalImportService = artikalImportService;

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
                saveAndImport(vendorId, tempFile, currentHash);
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
            return baseUrl + connector + credentialManager.getUsponParams() + "&slike=1&opis=1";
        } else if (vendorName.contains("linkom")) {
            return baseUrl + connector + credentialManager.getLinkomParams() + "&slike=1&opis=1&karakteristike=1";
        } else if (vendorName.contains("avtera")) {
            return baseUrl + connector + credentialManager.getAvteraParams() + "&slike=1&opis=1";
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
                // Read raw bytes first, then detect charset from XML declaration or Content-Type header
                byte[] rawBytes = response.getBody().readAllBytes();

                // Try to detect encoding from XML declaration (most reliable)
                Charset sourceCharset = detectXmlCharset(rawBytes);
                if (sourceCharset == null) {
                    // Fall back to Content-Type header
                    org.springframework.http.MediaType contentType = response.getHeaders().getContentType();
                    sourceCharset = (contentType != null && contentType.getCharset() != null)
                            ? contentType.getCharset()
                            : StandardCharsets.UTF_8;
                }

                if (sourceCharset.equals(StandardCharsets.UTF_8)) {
                    Files.write(temp.toPath(), rawBytes);
                } else {
                    // Re-encode to UTF-8 and fix XML declaration
                    String content = new String(rawBytes, sourceCharset);
                    content = content.replaceFirst("encoding=['\"][^'\"]*['\"]", "encoding=\"UTF-8\"");
                    Files.write(temp.toPath(), content.getBytes(StandardCharsets.UTF_8));
                }
                return temp;
            });
        }
        return temp;
    }

    private Charset detectXmlCharset(byte[] rawBytes) {
        // Read first 200 bytes as ASCII to find <?xml encoding="..."?>
        String header = new String(rawBytes, 0, Math.min(200, rawBytes.length), StandardCharsets.US_ASCII);
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("encoding=['\"]([^'\"]+)['\"]", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(header);
        if (m.find()) {
            try { return Charset.forName(m.group(1)); } catch (Exception ignored) {}
        }
        return null;
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

        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        jdbcTemplate.update(
                "INSERT INTO xml_feed_history (vendor_id, xml_content, status, hash_sum, created_at) VALUES (?, ?::xml, ?, ?, ?)",
                vendorId, content, "ACTIVE", hash, LocalDateTime.now());
        content = null; // hint GC to reclaim ~200MB before import phase

        vendorRepository.syncVendorXmlFromHistory(vendorId);

        historyRepository.cleanupOldFeeds(vendorId);
    }

    public void saveAndImport(Long vendorId, File file, String hash) throws Exception {
        saveAndActivate(vendorId, file, hash);
        artikalImportService.importFromVendor(vendorId);
    }
}
