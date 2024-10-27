package com.tehno.tehnozonaspring.controller;

import com.tehno.tehnozonaspring.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ProductControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void getProducts_ShouldReturnProductList() {
        // Poziv GET zahteva na /api/products
        ResponseEntity<Product[]> response = restTemplate.getForEntity("/api/products", Product[].class);

        // Provera odgovora
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(4, response.getBody().length); // Provera očekivanog broja proizvoda
    }
}
