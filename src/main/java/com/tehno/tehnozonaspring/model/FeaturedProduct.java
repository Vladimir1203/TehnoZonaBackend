package com.tehno.tehnozonaspring.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "featured_products")
public class FeaturedProduct {

    public FeaturedProduct() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private String barcode;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "feature_type", nullable = false)
    private FeatureType featureType;

    @Column(name = "item_type")
    private String itemType;

    private String subtitle;

    @Column(name = "button_text")
    private String buttonText;

    @Column(name = "button_route")
    private String buttonRoute;

    @Column(name = "glavna_grupa")
    private String glavnaGrupa;

    private String nadgrupa;

    private String grupa;

    @Column(name = "brand_name")
    private String brandName;

    @Column(name = "custom_name")
    private String customName;

    @Column(name = "custom_image_url")
    private String customImageUrl;

    // niži broj = viši prioritet
    private Integer priority;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    // GETTERI I SETTERI

    public Long getId() {
        return id;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public FeatureType getFeatureType() {
        return featureType;
    }

    public void setFeatureType(FeatureType featureType) {
        this.featureType = featureType;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
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
}
