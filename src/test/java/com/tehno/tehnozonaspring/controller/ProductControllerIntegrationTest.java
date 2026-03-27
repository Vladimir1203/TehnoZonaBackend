package com.tehno.tehnozonaspring.controller;

import com.tehno.tehnozonaspring.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.sql.init.mode=never",
        "app.data.products-file=static/data/products.json"
})
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
