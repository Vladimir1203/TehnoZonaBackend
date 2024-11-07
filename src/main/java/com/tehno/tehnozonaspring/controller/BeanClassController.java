package com.tehno.tehnozonaspring.controller;

import com.tehno.tehnozonaspring.model.BeanClass;
import com.tehno.tehnozonaspring.service.BeanClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/beans")
public class BeanClassController {

    private final BeanClassService beanClassService;

    @Autowired
    public BeanClassController(BeanClassService beanClassService) {
        this.beanClassService = beanClassService;
    }

    @GetMapping
    public List<BeanClass> getAllBeans() {
        return beanClassService.getAllBeans();
    }

    @GetMapping("/{id}/naziv")
    public String getArtikalNaziv(@PathVariable Long id) {
        return beanClassService.findArtikliNaziviByGrupa(id);
    }
}
