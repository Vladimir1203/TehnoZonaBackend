package com.tehno.tehnozonaspring.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ImageWarmupService {

    private final VendorService vendorService;

    public ImageWarmupService(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmupOnStartup() {
        Thread warmupThread = new Thread(() -> {
            System.out.println("IMAGE WARMUP: Starting background image prefetch...");
            int total = 0;
            int downloaded = 0;

            try {
                for (String glavnaGrupa : vendorService.getGlavneGrupe()) {
                    try {
                        int before = downloaded;
                        // Reuse existing logic - fetches from disk or downloads if missing
                        vendorService.getNadgrupeWithImages(glavnaGrupa);
                        total++;
                    } catch (Exception e) {
                        System.err.println("IMAGE WARMUP: Error for group " + glavnaGrupa + ": " + e.getMessage());
                    }
                }
                System.out.println("IMAGE WARMUP: Done. Processed " + total + " glavnih grupa.");
            } catch (Exception e) {
                System.err.println("IMAGE WARMUP: Fatal error: " + e.getMessage());
            }
        }, "image-warmup");

        warmupThread.setDaemon(true);
        warmupThread.start();
    }
}
