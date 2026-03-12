package com.tehno.tehnozonaspring.dto;

import com.tehno.tehnozonaspring.model.enums.HomepageSection;
import com.tehno.tehnozonaspring.model.enums.ItemType;
import java.time.LocalDateTime;

public class HomepageItemRequest {
    private ItemType itemType;
    private HomepageSection section;
    private Integer priority;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;

    // PRODUCT
    private String barcode;

    // CATEGORY
    private String glavnaGrupa;
    private String nadgrupa;
    private String grupa;

    // BRAND
    private String brandName;

    // CUSTOM
    private String customName;
    private String customImageUrl;
    private String subtitle;
    private String buttonText;
    private String buttonRoute;

    // Getters and Setters
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
