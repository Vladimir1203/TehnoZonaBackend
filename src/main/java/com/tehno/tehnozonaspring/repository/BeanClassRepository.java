package com.tehno.tehnozonaspring.repository;

import com.tehno.tehnozonaspring.model.BeanClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BeanClassRepository extends JpaRepository<BeanClass, Long> {
}
