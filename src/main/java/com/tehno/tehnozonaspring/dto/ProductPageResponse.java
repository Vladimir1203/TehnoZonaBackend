package com.tehno.tehnozonaspring.dto;

import com.tehno.tehnozonaspring.model.Artikal;

import java.util.List;
public class ProductPageResponse {
    private List<Artikal> products;
    private int totalCount;
    private double minCena;
    private double maxCena;

    public List<Artikal> getProducts() {
        return products;
    }

    public void setProducts(List<Artikal> products) {
        this.products = products;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public double getMinCena() {
        return minCena;
    }

    public void setMinCena(double minCena) {
        this.minCena = minCena;
    }

    public double getMaxCena() {
        return maxCena;
    }

    public void setMaxCena(double maxCena) {
        this.maxCena = maxCena;
    }
}

