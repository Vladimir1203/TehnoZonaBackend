package com.tehno.tehnozonaspring.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "feed_source")
public class FeedSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    private String endpointUrl;
    private String xsdPath;
    private String cronExpression;
    private boolean enabled = true;
}
