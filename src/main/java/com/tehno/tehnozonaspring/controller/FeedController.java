package com.tehno.tehnozonaspring.controller;

import com.tehno.tehnozonaspring.service.ArticalImportService;
import com.tehno.tehnozonaspring.service.FeedRefreshService;
import com.tehno.tehnozonaspring.util.CredentialManager;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/feeds")
public class FeedController {

    private final FeedRefreshService feedRefreshService;
    private final ArticalImportService artikalImportService;
    private final com.tehno.tehnozonaspring.service.EmailService emailService;
    private final CredentialManager credentialManager;
    private final JdbcTemplate jdbcTemplate;

    public FeedController(FeedRefreshService feedRefreshService,
            ArticalImportService artikalImportService,
            com.tehno.tehnozonaspring.service.EmailService emailService,
            CredentialManager credentialManager,
            JdbcTemplate jdbcTemplate) {
        this.feedRefreshService = feedRefreshService;
        this.artikalImportService = artikalImportService;
        this.emailService = emailService;
        this.credentialManager = credentialManager;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/refresh/{vendorId}")
    public ResponseEntity<String> refreshFeed(@PathVariable Long vendorId) {
        try {
            boolean changed = feedRefreshService.refreshVendorFeed(vendorId);
            if (changed) {
                return ResponseEntity.ok("Feed successfully refreshed and updated in database for vendor: " + vendorId);
            } else {
                return ResponseEntity.ok("No changes detected. Database remains unchanged for vendor: " + vendorId);
            }
        } catch (Exception e) {
            emailService.sendErrorNotification("Vendor " + vendorId, e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to refresh feed: " + e.getMessage());
        }
    }

    /** Re-importuje artikle iz vec postojeceg XML-a u bazi (bez ponovnog preuzimanja). */
    @PostMapping("/reimport/{vendorId}")
    public ResponseEntity<String> reimportFeed(@PathVariable Long vendorId) {
        try {
            artikalImportService.importFromVendor(vendorId);
            return ResponseEntity.ok("Reimport uspesno zavrsen za vendor: " + vendorId);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Reimport greska: " + e.getMessage());
        }
    }

    @PostMapping("/test-alert")
    public ResponseEntity<String> testAlert() {
        emailService.sendErrorNotification("TEST VENDOR", "Ovo je testna poruka sistema za obaveštavanje.");
        return ResponseEntity.ok("Test alert sent to " + credentialManager.getMailUser());
    }

    @GetMapping("/mapping/unconfirmed")
    public ResponseEntity<List<Map<String, Object>>> getUnconfirmedMappings() {
        List<Map<String, Object>> result = jdbcTemplate.queryForList(
                "SELECT nadgrupa, glavna_grupa FROM glavna_grupa_mapping WHERE confirmed = false ORDER BY nadgrupa");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/mapping/confirm")
    public ResponseEntity<String> confirmMapping(@RequestBody List<Map<String, String>> mappings) {
        for (Map<String, String> m : mappings) {
            jdbcTemplate.update(
                    "UPDATE glavna_grupa_mapping SET glavna_grupa = ?, confirmed = true WHERE nadgrupa = ?",
                    m.get("glavnaGrupa"), m.get("nadgrupa"));
        }
        return ResponseEntity.ok("Sačuvano " + mappings.size() + " mapiranja.");
    }
}
