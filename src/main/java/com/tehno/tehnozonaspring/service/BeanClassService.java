package com.tehno.tehnozonaspring.service;

import com.tehno.tehnozonaspring.model.BeanClass;
import com.tehno.tehnozonaspring.repository.BeanClassRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BeanClassService {

    private final BeanClassRepository beanClassRepository;

    @Autowired
    public BeanClassService(BeanClassRepository beanClassRepository) {
        this.beanClassRepository = beanClassRepository;
    }

    public List<BeanClass> getAllBeans() {
        return beanClassRepository.findAll();
    }
}
