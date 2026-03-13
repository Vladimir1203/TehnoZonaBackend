package com.tehno.tehnozonaspring.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "xml_feed_history")
public class XmlFeedHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    @Column(columnDefinition = "xml")
    private String xmlContent;

    private String status; // PENDING, ACTIVE, FAILED, ARCHIVED
    private String hashSum;
    private String errorMessage;
    private LocalDateTime createdAt = LocalDateTime.now();
}
