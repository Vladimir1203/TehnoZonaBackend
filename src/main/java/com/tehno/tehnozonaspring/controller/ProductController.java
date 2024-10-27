package com.tehno.tehnozonaspring.controller;

import com.tehno.tehnozonaspring.model.Product;
import com.tehno.tehnozonaspring.service.ProductServiceImpl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductServiceImpl productServiceImpl;

    public ProductController(ProductServiceImpl productServiceImpl) {
        this.productServiceImpl = productServiceImpl;
    }

    @GetMapping
    public List<Product> getProducts() throws IOException {
        return productServiceImpl.getAllProducts();
    }
}
