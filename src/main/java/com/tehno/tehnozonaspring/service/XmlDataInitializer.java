package com.tehno.tehnozonaspring.service;

import com.tehno.tehnozonaspring.model.FeedSource;
import com.tehno.tehnozonaspring.model.Vendor;
import com.tehno.tehnozonaspring.repository.FeedSourceRepository;
import com.tehno.tehnozonaspring.repository.VendorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

@Service
public class XmlDataInitializer implements CommandLineRunner {

    private final VendorRepository vendorRepository;
    private final FeedSourceRepository feedSourceRepository;

    public XmlDataInitializer(VendorRepository vendorRepository, FeedSourceRepository feedSourceRepository) {
        this.vendorRepository = vendorRepository;
        this.feedSourceRepository = feedSourceRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. USPON
        initVendor(1L, "uspon", "https://www.uspon.rs/api/v1/partner-export-xml", "schemas/uspon.xsd", "0 0 3 * * *");

        // 2. LINKOM
        initVendor(2L, "linkom", "https://www.linkom.rs/api/v1/partner-export-xml", "schemas/linkom.xsd", "0 0 4 * * *");

        // 3. AVTERA
        initVendor(3L, "avtera", "http://b2b.avtera.rs/src/export_xml_asr.php", "schemas/avtera.xsd", "0 0 5 * * *");

        System.out.println("✅ Database initialization complete. All vendors synced.");
    }

    private void initVendor(Long id, String name, String url, String xsd, String cron) {
        if (!vendorRepository.existsById(id)) {
            vendorRepository.insertVendor(id, name, "");
            System.out.println("Initialized Vendor: " + name);
        }

        if (feedSourceRepository.findByVendorId(id).isEmpty()) {
            FeedSource source = new FeedSource();
            source.setVendor(vendorRepository.getReferenceById(id));
            source.setEndpointUrl(url);
            source.setXsdPath(xsd);
            source.setCronExpression(cron);
            source.setEnabled(true);
            feedSourceRepository.save(source);
            System.out.println("Initialized FeedSource for: " + name);
        }
    }
}
