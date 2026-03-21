package com.tehno.tehnozonaspring.service;

import com.tehno.tehnozonaspring.model.FeedSource;
import com.tehno.tehnozonaspring.repository.FeedSourceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeedSchedulerService {

    private final FeedSourceRepository feedSourceRepository;
    private final FeedRefreshService feedRefreshService;
    private final EmailService emailService;

    public FeedSchedulerService(FeedSourceRepository feedSourceRepository,
            FeedRefreshService feedRefreshService,
            EmailService emailService) {
        this.feedSourceRepository = feedSourceRepository;
        this.feedRefreshService = feedRefreshService;
        this.emailService = emailService;
    }

    // 1. Uspon - 03:00 AM
    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Belgrade")
    public void scheduleUsponRefresh() {
        refreshVendor(1L, "Uspon");
    }

    // 2. Linkom - 03:15 AM
    @Scheduled(cron = "0 15 3 * * *", zone = "Europe/Belgrade")
    public void scheduleLinkomRefresh() {
        refreshVendor(2L, "Linkom");
    }

    // 3. Avtera - 03:30 AM
    @Scheduled(cron = "0 30 3 * * *", zone = "Europe/Belgrade")
    public void scheduleAvteraRefresh() {
        refreshVendor(3L, "Avtera");
    }

    // 4. Spektar - 03:45 AM
    @Scheduled(cron = "0 45 3 * * *", zone = "Europe/Belgrade")
    public void scheduleSpektarRefresh() {
        refreshVendor(4L, "Spektar");
    }

    private void refreshVendor(Long vendorId, String vendorName) {
        System.out.println("SCHEDULER: Starting refresh for " + vendorName + " (ID: " + vendorId + ") at "
                + java.time.LocalDateTime.now());
        try {
            feedRefreshService.refreshVendorFeed(vendorId);
            System.out.println("SCHEDULER: Successfully refreshed " + vendorName);
        } catch (Exception e) {
            System.err.println("SCHEDULER: Error refreshing " + vendorName + ": " + e.getMessage());
            emailService.sendErrorNotification(vendorName, e.getMessage());
        }
    }
}
