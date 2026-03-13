package com.tehno.tehnozonaspring.controller;

import com.tehno.tehnozonaspring.service.FeedRefreshService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/feeds")
public class FeedController {

    private final FeedRefreshService feedRefreshService;
    private final com.tehno.tehnozonaspring.service.EmailService emailService;

    public FeedController(FeedRefreshService feedRefreshService,
            com.tehno.tehnozonaspring.service.EmailService emailService) {
        this.feedRefreshService = feedRefreshService;
        this.emailService = emailService;
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

    @PostMapping("/test-alert")
    public ResponseEntity<String> testAlert() {
        emailService.sendErrorNotification("TEST VENDOR", "Ovo je testna poruka sistema za obaveštavanje.");
        return ResponseEntity.ok("Test alert sent to vladimir12934@gmail.com");
    }
}
