package com.tehno.tehnozonaspring.repository;

import com.tehno.tehnozonaspring.model.BeanClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BeanClassRepository extends JpaRepository<BeanClass, Long> {
    @Query(value = "SELECT (xpath('/artikli/artikal/naziv/text()', xml_data))[1]::TEXT FROM bean_class WHERE id = :id", nativeQuery = true)
    String findArtikalNazivById(@Param("id") Long id);
}
