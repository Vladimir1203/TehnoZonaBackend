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

    // Runs every 4 hours to check for updates
    @Scheduled(cron = "0 0 */4 * * *")
    public void scheduleFeedRefresh() {
        List<FeedSource> sources = feedSourceRepository.findAll();
        for (FeedSource source : sources) {
            if (source.isEnabled()) {
                try {
                    feedRefreshService.refreshVendorFeed(source.getVendor().getId());
                } catch (Exception e) {
                    emailService.sendErrorNotification(source.getVendor().getName(), e.getMessage());
                }
            }
        }
    }
}
