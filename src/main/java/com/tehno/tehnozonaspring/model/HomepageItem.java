package com.tehno.tehnozonaspring.model;

import com.tehno.tehnozonaspring.model.enums.HomepageSection;
import com.tehno.tehnozonaspring.model.enums.ItemType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "homepage_items")
public class HomepageItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    private ItemType itemType;

    @Enumerated(EnumType.STRING)
    @Column(name = "section", nullable = false)
    private HomepageSection section;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    // --- PRODUCT FIELDS ---
    @Column(name = "barcode")
    private String barcode;

    // --- CATEGORY FIELDS ---
    @Column(name = "glavna_grupa")
    private String glavnaGrupa;

    @Column(name = "nadgrupa")
    private String nadgrupa;

    @Column(name = "grupa")
    private String grupa;

    // --- BRAND FIELDS ---
    @Column(name = "brand_name")
    private String brandName;

    // --- CUSTOM FIELDS ---
    @Column(name = "custom_name")
    private String customName;

    @Column(name = "custom_image_url")
    private String customImageUrl;

    @Column(name = "subtitle")
    private String subtitle;

    @Column(name = "button_text")
    private String buttonText;

    @Column(name = "button_route")
    private String buttonRoute;

    // Constructors
    public HomepageItem() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }

    public HomepageSection getSection() {
        return section;
    }

    public void setSection(HomepageSection section) {
        this.section = section;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDateTime validTo) {
        this.validTo = validTo;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getGlavnaGrupa() {
        return glavnaGrupa;
    }

    public void setGlavnaGrupa(String glavnaGrupa) {
        this.glavnaGrupa = glavnaGrupa;
    }

    public String getNadgrupa() {
        return nadgrupa;
    }

    public void setNadgrupa(String nadgrupa) {
        this.nadgrupa = nadgrupa;
    }

    public String getGrupa() {
        return grupa;
    }

    public void setGrupa(String grupa) {
        this.grupa = grupa;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    public String getCustomImageUrl() {
        return customImageUrl;
    }

    public void setCustomImageUrl(String customImageUrl) {
        this.customImageUrl = customImageUrl;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getButtonText() {
        return buttonText;
    }

    public void setButtonText(String buttonText) {
        this.buttonText = buttonText;
    }

    public String getButtonRoute() {
        return buttonRoute;
    }

    public void setButtonRoute(String buttonRoute) {
        this.buttonRoute = buttonRoute;
    }
}
