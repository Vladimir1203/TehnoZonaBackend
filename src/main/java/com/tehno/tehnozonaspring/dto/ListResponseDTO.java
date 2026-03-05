package com.tehno.tehnozonaspring.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ListResponseDTO<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> items;
    private int page;
    private int size;
    private long total;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Map<String, Long> manufacturerCounts;
    private AppliedFiltersDTO appliedFilters;

    public ListResponseDTO() {
    }

    public ListResponseDTO(List<T> items, int page, int size, long total,
            BigDecimal minPrice, BigDecimal maxPrice,
            Map<String, Long> manufacturerCounts,
            AppliedFiltersDTO appliedFilters) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.total = total;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.manufacturerCounts = manufacturerCounts;
        this.appliedFilters = appliedFilters;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Map<String, Long> getManufacturerCounts() {
        return manufacturerCounts;
    }

    public void setManufacturerCounts(Map<String, Long> manufacturerCounts) {
        this.manufacturerCounts = manufacturerCounts;
    }

    public AppliedFiltersDTO getAppliedFilters() {
        return appliedFilters;
    }

    public void setAppliedFilters(AppliedFiltersDTO appliedFilters) {
        this.appliedFilters = appliedFilters;
    }
}
