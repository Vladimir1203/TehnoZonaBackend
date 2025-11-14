package com.tehno.tehnozonaspring.dto;

import com.tehno.tehnozonaspring.model.Artikal;
import com.tehno.tehnozonaspring.model.FeaturedProduct;

public class FeaturedArtikalResponse {
    private Artikal artikal;
    private FeaturedProduct featured;

    public FeaturedArtikalResponse(Artikal artikal, FeaturedProduct featured) {
        this.artikal = artikal;
        this.featured = featured;
    }

    public Artikal getArtikal() { return artikal; }
    public FeaturedProduct getFeatured() { return featured; }
}
