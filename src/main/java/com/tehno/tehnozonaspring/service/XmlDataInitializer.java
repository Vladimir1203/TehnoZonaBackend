package com.tehno.tehnozonaspring.service;

import com.tehno.tehnozonaspring.model.Vendor;
import com.tehno.tehnozonaspring.repository.VendorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

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
            String xmlContent = new String(resource.getInputStream().readAllBytes());

            Vendor vendor = new Vendor();
            vendor.setId(1L);
            vendor.setName("uspon");
            vendor.setXmlData(xmlContent);

            vendorRepository.insertVendor(2L, "uspon", xmlContent);
            System.out.println("XML data inserted successfully.");
        }
    }
}
