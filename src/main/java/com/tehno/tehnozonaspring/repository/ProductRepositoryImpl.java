package com.tehno.tehnozonaspring.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tehno.tehnozonaspring.model.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;

@Repository
public class ProductRepositoryImpl implements ProductRepository {

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.data.products-file}")
    private String productsFilePath;

    @Override
    public List<Product> getAllProducts() throws IOException {
        ClassPathResource resource = new ClassPathResource(productsFilePath);
        return mapper.readValue(resource.getInputStream(), new TypeReference<>() {});
    }
}
