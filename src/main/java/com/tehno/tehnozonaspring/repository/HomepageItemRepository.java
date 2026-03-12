package com.tehno.tehnozonaspring.repository;

import com.tehno.tehnozonaspring.model.HomepageItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface HomepageItemRepository extends JpaRepository<HomepageItem, Long> {

    @Query("SELECT h FROM HomepageItem h WHERE h.vendorId = :vendorId " +
            "AND h.validFrom <= :now AND h.validTo >= :now " +
            "ORDER BY h.priority ASC")
    List<HomepageItem> findActiveItemsByVendorId(@Param("vendorId") Long vendorId, @Param("now") LocalDateTime now);

    List<HomepageItem> findByVendorIdAndSectionAndValidToAfter(Long vendorId, com.tehno.tehnozonaspring.model.enums.HomepageSection section, LocalDateTime now);
}
