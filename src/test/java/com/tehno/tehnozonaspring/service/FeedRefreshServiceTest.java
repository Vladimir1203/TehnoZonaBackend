package com.tehno.tehnozonaspring.service;

import com.tehno.tehnozonaspring.model.FeedSource;
import com.tehno.tehnozonaspring.model.Vendor;
import com.tehno.tehnozonaspring.repository.FeedSourceRepository;
import com.tehno.tehnozonaspring.repository.VendorRepository;
import com.tehno.tehnozonaspring.repository.XmlFeedHistoryRepository;
import com.tehno.tehnozonaspring.util.CredentialManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FeedRefreshServiceTest {

    private FeedRefreshService feedRefreshService;

    @Mock private FeedSourceRepository feedSourceRepository;
    @Mock private XmlFeedHistoryRepository historyRepository;
    @Mock private VendorRepository vendorRepository;
    @Mock private EmailService emailService;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private CredentialManager credentialManager;
    @Mock private ArticalImportService artikalImportService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        feedRefreshService = new FeedRefreshService(
                feedSourceRepository, historyRepository, vendorRepository,
                emailService, jdbcTemplate, credentialManager, artikalImportService);
    }

    @Test
    void testRefreshVendorFeed_NoChangesDetected_WhenHashMatches() throws Exception {
        Long vendorId = 1L;
        FeedSource source = new FeedSource();
        Vendor vendor = new Vendor();
        vendor.setId(vendorId);
        vendor.setName("uspon");
        source.setVendor(vendor);
        source.setEndpointUrl("classpath:TehnoZona-uspon.txt");
        source.setXsdPath(null);

        when(feedSourceRepository.findByVendorId(vendorId)).thenReturn(Optional.of(source));

        // Calculate real hash of the classpath file so we can simulate "no change"
        java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("TehnoZona-uspon.txt");
        assumeResourceExists(is);
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) > 0) digest.update(buffer, 0, read);
        String realHash = java.util.HexFormat.of().formatHex(digest.digest());

        when(historyRepository.findLastHashByVendorId(vendorId)).thenReturn(realHash);

        boolean result = feedRefreshService.refreshVendorFeed(vendorId);

        assertFalse(result, "Should return false when feed has not changed");
        verify(jdbcTemplate, never()).update(anyString(), any(), any(), any(), any(), any());
        verify(vendorRepository, never()).syncVendorXmlFromHistory(any());
    }

    @Test
    void testRefreshVendorFeed_SavesAndActivates_WhenHashDiffers() throws Exception {
        Long vendorId = 1L;
        FeedSource source = new FeedSource();
        Vendor vendor = new Vendor();
        vendor.setId(vendorId);
        vendor.setName("uspon");
        source.setVendor(vendor);
        source.setEndpointUrl("classpath:TehnoZona-uspon.txt");
        source.setXsdPath(null); // skip XSD validation

        when(feedSourceRepository.findByVendorId(vendorId)).thenReturn(Optional.of(source));
        when(historyRepository.findLastHashByVendorId(vendorId)).thenReturn("old-hash-that-wont-match");

        boolean result = feedRefreshService.refreshVendorFeed(vendorId);

        assertTrue(result, "Should return true when feed has changed");
        verify(historyRepository).archiveCurrentActive(vendorId);
        verify(jdbcTemplate).update(anyString(), eq(vendorId), any(), eq("ACTIVE"), anyString(), any());
        verify(vendorRepository).syncVendorXmlFromHistory(vendorId);
        verify(historyRepository).cleanupOldFeeds(vendorId);
        verify(emailService).sendSuccessNotification(eq("uspon"), anyString());
    }

    @Test
    void testRefreshVendorFeed_SendsErrorEmail_OnException() throws Exception {
        Long vendorId = 1L;
        FeedSource source = new FeedSource();
        Vendor vendor = new Vendor();
        vendor.setId(vendorId);
        vendor.setName("uspon");
        source.setVendor(vendor);
        source.setEndpointUrl("classpath:nonexistent-file.xml");
        source.setXsdPath(null);

        when(feedSourceRepository.findByVendorId(vendorId)).thenReturn(Optional.of(source));

        assertThrows(Exception.class, () -> feedRefreshService.refreshVendorFeed(vendorId));
        verify(emailService).sendErrorNotification(eq("uspon"), anyString());
    }

    private void assumeResourceExists(java.io.InputStream is) {
        if (is == null) {
            // classpath resource not present - skip test gracefully
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "TehnoZona-uspon.txt not found in classpath, skipping");
        }
    }
}
