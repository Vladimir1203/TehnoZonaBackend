package com.tehno.tehnozonaspring.service;

import com.tehno.tehnozonaspring.model.Vendor;
import com.tehno.tehnozonaspring.repository.VendorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
public class XmlDataInitializer implements CommandLineRunner {

    private final VendorRepository vendorRepository;
    private final com.tehno.tehnozonaspring.repository.FeedSourceRepository feedSourceRepository;

    public XmlDataInitializer(VendorRepository vendorRepository,
            com.tehno.tehnozonaspring.repository.FeedSourceRepository feedSourceRepository) {
        this.vendorRepository = vendorRepository;
        this.feedSourceRepository = feedSourceRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Init Vendor
        if (!vendorRepository.existsById(1L)) {
            ClassPathResource resource = new ClassPathResource("TehnoZona-uspon.txt");
            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append("\n");
                }
            }
            String xmlContent = builder.toString();

            Vendor vendor = new Vendor();
            vendor.setId(1L);
            vendor.setName("uspon");
            vendor.setXmlData(xmlContent);

            vendorRepository.insertVendor(1L, "uspon", xmlContent);
            System.out.println("XML data inserted successfully.");
        }

        // Init FeedSource for Testing
        if (feedSourceRepository.findByVendorId(1L).isEmpty()) {
            com.tehno.tehnozonaspring.model.FeedSource source = new com.tehno.tehnozonaspring.model.FeedSource();
            source.setVendor(vendorRepository.getReferenceById(1L));
            // Base URL only, credentials handled by CredentialManager
            source.setEndpointUrl("https://www.uspon.rs/api/v1/partner-export-xml");
            source.setXsdPath("schemas/uspon.xsd");
            source.setCronExpression("0 0 3 * * *");
            source.setEnabled(true);
            feedSourceRepository.save(source);
            System.out.println("Default FeedSource for USPON created.");
        }

        // Init Vendor 2 (LINKOM)
        if (!vendorRepository.existsById(2L)) {
            Vendor vendor = new Vendor();
            vendor.setId(2L);
            vendor.setName("linkom");
            vendorRepository.insertVendor(2L, "linkom", "");
            System.out.println("Vendor LINKOM initialized.");
        }

        // Init FeedSource for LINKOM (Vendor 2)
        if (feedSourceRepository.findByVendorId(2L).isEmpty()) {
            com.tehno.tehnozonaspring.model.FeedSource source = new com.tehno.tehnozonaspring.model.FeedSource();
            source.setVendor(vendorRepository.getReferenceById(2L));
            source.setEndpointUrl("https://www.linkom.rs/api/v1/partner-export-xml");
            source.setXsdPath("schemas/linkom.xsd");
            source.setCronExpression("0 0 4 * * *");
            source.setEnabled(true);
            feedSourceRepository.save(source);
            System.out.println("Default FeedSource for LINKOM created.");
        }
    }
}
