package com.tehno.tehnozonaspring.repository;

import com.tehno.tehnozonaspring.model.XmlFeedHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

public interface XmlFeedHistoryRepository extends JpaRepository<XmlFeedHistory, Long> {
    Optional<XmlFeedHistory> findTopByVendorIdAndStatusOrderByCreatedAtDesc(Long vendorId, String status);

    @Query(value = "SELECT hash_sum FROM xml_feed_history WHERE vendor_id = :vendorId AND status = 'ACTIVE' ORDER BY created_at DESC LIMIT 1", nativeQuery = true)
    String findLastHashByVendorId(@Param("vendorId") Long vendorId);

    @Modifying
    @Transactional
    @Query(value = """
                DELETE FROM xml_feed_history
                WHERE vendor_id = :vendorId
                AND id NOT IN (
                    SELECT id FROM xml_feed_history
                    WHERE vendor_id = :vendorId
                    ORDER BY created_at DESC
                    LIMIT 2
                )
            """, nativeQuery = true)
    void cleanupOldFeeds(@Param("vendorId") Long vendorId);

    @Modifying
    @Transactional
    @Query(value = """
                UPDATE xml_feed_history
                SET status = 'ARCHIVED'
                WHERE vendor_id = :vendorId AND status = 'ACTIVE'
            """, nativeQuery = true)
    void archiveCurrentActive(@Param("vendorId") Long vendorId);

    @Modifying
    @Transactional
    @Query(value = """
                INSERT INTO xml_feed_history (vendor_id, xml_content, status, hash_sum, created_at)
                VALUES (:vendorId, CAST(:xmlContent AS xml), :status, :hashSum, :createdAt)
            """, nativeQuery = true)
    void insertWithXml(@Param("vendorId") Long vendorId,
            @Param("xmlContent") String xmlContent,
            @Param("status") String status,
            @Param("hashSum") String hashSum,
            @Param("createdAt") LocalDateTime createdAt);
}
