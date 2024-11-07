package com.tehno.tehnozonaspring.repository;

import com.tehno.tehnozonaspring.model.BeanClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BeanClassRepository extends JpaRepository<BeanClass, Long> {
    @Query(value = """
    SELECT array_to_string(xpath('/artikli/artikal[grupa="VIDEO KABLOVI"]/sifra/text()', xml_data), ', ')
    FROM bean_class
    WHERE id = :id
""", nativeQuery = true)
    String findArtikliNaziviByGrupa(@Param("id") Long id);

}
