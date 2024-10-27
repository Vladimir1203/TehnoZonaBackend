package com.tehno.tehnozonaspring.repository;

import com.tehno.tehnozonaspring.model.Product;

import java.io.IOException;
import java.util.List;

public interface ProductRepository {

    List<Product> getAllProducts() throws IOException;
}
