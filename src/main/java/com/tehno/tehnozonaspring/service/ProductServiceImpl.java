package com.tehno.tehnozonaspring.service;

import com.tehno.tehnozonaspring.model.Product;
import com.tehno.tehnozonaspring.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<Product> getAllProducts() throws IOException {
        return productRepository.getAllProducts();
    }
}
