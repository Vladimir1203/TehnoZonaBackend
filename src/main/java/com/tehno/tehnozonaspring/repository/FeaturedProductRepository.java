package com.tehno.tehnozonaspring.repository;

import com.tehno.tehnozonaspring.model.FeaturedProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeaturedProductRepository extends JpaRepository<FeaturedProduct, Long> {

    @Query(value = """
        SELECT
            id,
            barcode,
            vendor_id,
            feature_type,
            priority,
            valid_from,
            valid_to
        FROM featured_products
        WHERE valid_from <= NOW()
          AND valid_to >= NOW()
        ORDER BY priority ASC
        """, nativeQuery = true)
    List<FeaturedProduct> getAllActiveFeatured();

    @Query(value = """
    SELECT
        id,
        barcode,
        vendor_id,
        feature_type,
        priority,
        valid_from,
        valid_to
    FROM featured_products
    WHERE feature_type = :type
      AND valid_from <= NOW()
      AND valid_to >= NOW()
    ORDER BY priority ASC
    """, nativeQuery = true)
    List<FeaturedProduct> getActiveFeaturedByType(@Param("type") String type);
}
