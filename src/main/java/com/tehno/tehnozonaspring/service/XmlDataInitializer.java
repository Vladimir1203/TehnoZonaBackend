package com.tehno.tehnozonaspring.service;

import com.tehno.tehnozonaspring.model.FeedSource;
import com.tehno.tehnozonaspring.model.Vendor;
import com.tehno.tehnozonaspring.repository.FeedSourceRepository;
import com.tehno.tehnozonaspring.repository.VendorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class XmlDataInitializer implements CommandLineRunner {

    private final VendorRepository vendorRepository;
    private final FeedSourceRepository feedSourceRepository;
    private final ArticalImportService artikalImportService;
    private final JdbcTemplate jdbcTemplate;

    public XmlDataInitializer(VendorRepository vendorRepository,
                               FeedSourceRepository feedSourceRepository,
                               ArticalImportService artikalImportService,
                               JdbcTemplate jdbcTemplate) {
        this.vendorRepository = vendorRepository;
        this.feedSourceRepository = feedSourceRepository;
        this.artikalImportService = artikalImportService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. USPON
        initVendor(1L, "uspon", "https://www.uspon.rs/api/v1/partner-export-xml", "schemas/uspon.xsd", "0 0 3 * * *");

        // 2. LINKOM
        initVendor(2L, "linkom", "https://www.linkom.rs/api/v1/partner-export-xml", "schemas/linkom.xsd", "0 0 4 * * *");

        // 3. AVTERA
        initVendor(3L, "avtera", "http://b2b.avtera.rs/src/export_xml_asr.php", "schemas/avtera.xsd", "0 0 5 * * *");

        // 4. SPEKTAR
        initVendor(4L, "spektar", "https://api.v2.spektar.rs/storage//exports/xml/tehno-zona-SPB1u8ERxpTRlaES4cvUlWmVAJjOSCYc.xml", null, "0 45 3 * * *");

        System.out.println("✅ Database initialization complete. All vendors synced.");

        // Ako je artikal tabela prazna (prvi start ili reset), importuj iz postojecih XML-ova
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM artikal", Integer.class);
        if (count == null || count == 0) {
            System.out.println("IMPORT: artikal tabela je prazna — pokrecem inicijalni import...");
            for (long vendorId = 1; vendorId <= 4; vendorId++) {
                try {
                    artikalImportService.importFromVendor(vendorId);
                } catch (Exception e) {
                    System.err.println("IMPORT: Greska pri importu vendora " + vendorId + ": " + e.getMessage());
                }
            }
            System.out.println("IMPORT: Inicijalni import zavrsен.");
        }
    }

    private void initVendor(Long id, String name, String url, String xsd, String cron) {
        if (!vendorRepository.existsById(id)) {
            vendorRepository.insertVendor(id, name);
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
