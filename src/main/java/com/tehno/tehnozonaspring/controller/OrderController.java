package com.tehno.tehnozonaspring.controller;

import com.tehno.tehnozonaspring.model.OrderRequest;
import com.tehno.tehnozonaspring.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<String> sendOrder(@RequestBody OrderRequest orderRequest) {
        try {
            orderService.sendOrderEmail(orderRequest);
            return ResponseEntity.ok("Porudžbina uspešno poslata.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Greška prilikom slanja porudžbine: " + e.getMessage());
        }
    }
}
