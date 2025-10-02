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

    public XmlDataInitializer(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }
    @Override
    public void run(String... args) throws Exception {
        if (!vendorRepository.existsById(2L)) {
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

            vendorRepository.insertVendor(2L, "uspon", xmlContent);
            System.out.println("XML data inserted successfully.");
        }
    }
}
