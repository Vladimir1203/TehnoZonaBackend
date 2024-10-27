package com.tehno.tehnozonaspring.service;

import com.tehno.tehnozonaspring.model.Product;
import com.tehno.tehnozonaspring.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void getAllProducts_ShouldReturnProductList() throws IOException {
        // Mocking podataka za unit test
        List<Product> products = Arrays.asList(
                new Product(1L, "Frižider", "Kućni aparati", "Opis", 65000, "LG", "na stanju"),
                new Product(2L, "Veš Mašina", "Kućni aparati", "Opis", 55000, "Samsung", "na stanju")
        );

        // Definisanje ponašanja mock-a
        when(productRepository.getAllProducts()).thenReturn(products);

        // Izvršavanje metode i provera rezultata
        List<Product> result = productService.getAllProducts();
        assertEquals(2, result.size());
        assertEquals("Frižider", result.get(0).getName());
    }
}
