package com.tehno.tehnozonaspring.service;

import com.tehno.tehnozonaspring.model.Product;
import java.io.IOException;
import java.util.List;

public interface ProductService {
    List<Product> getAllProducts() throws IOException;
}
