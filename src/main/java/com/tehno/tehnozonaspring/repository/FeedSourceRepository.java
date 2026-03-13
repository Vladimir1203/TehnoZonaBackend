package com.tehno.tehnozonaspring.repository;

import com.tehno.tehnozonaspring.model.FeedSource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FeedSourceRepository extends JpaRepository<FeedSource, Long> {
    Optional<FeedSource> findByVendorId(Long vendorId);
}
