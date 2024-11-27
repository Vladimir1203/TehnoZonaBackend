package com.tehno.tehnozonaspring.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class Vendor {
    @Id
    private Long id;
    private String name;
    @Column(columnDefinition = "xml")
    private String xmlData;

}
