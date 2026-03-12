package com.tehno.tehnozonaspring.dto;

import com.tehno.tehnozonaspring.model.Artikal;
import com.tehno.tehnozonaspring.model.HomepageItem;

public class HomepageItemResponse {

    // Original DB Item
    private HomepageItem homepageItem;

    // Populated details if it's a product
    private Artikal artikal;

    public HomepageItemResponse() {
    }

    public HomepageItemResponse(HomepageItem homepageItem, Artikal artikal) {
        this.homepageItem = homepageItem;
        this.artikal = artikal;
    }

    public HomepageItem getHomepageItem() {
        return homepageItem;
    }

    public void setHomepageItem(HomepageItem homepageItem) {
        this.homepageItem = homepageItem;
    }

    public Artikal getArtikal() {
        return artikal;
    }

    public void setArtikal(Artikal artikal) {
        this.artikal = artikal;
    }
}
