package com.tehno.tehnozonaspring.controller;

import com.tehno.tehnozonaspring.service.ArticalImportService;
import com.tehno.tehnozonaspring.service.FeedRefreshService;
import com.tehno.tehnozonaspring.util.CredentialManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/feeds")
public class FeedController {

    private final FeedRefreshService feedRefreshService;
    private final ArticalImportService artikalImportService;
    private final com.tehno.tehnozonaspring.service.EmailService emailService;
    private final CredentialManager credentialManager;

    public FeedController(FeedRefreshService feedRefreshService,
            ArticalImportService artikalImportService,
            com.tehno.tehnozonaspring.service.EmailService emailService,
            CredentialManager credentialManager) {
        this.feedRefreshService = feedRefreshService;
        this.artikalImportService = artikalImportService;
        this.emailService = emailService;
        this.credentialManager = credentialManager;
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
}
