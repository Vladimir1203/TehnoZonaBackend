package com.tehno.tehnozonaspring.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public class AppliedFiltersDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private BigDecimal minCena;
    private BigDecimal maxCena;
    private List<String> proizvodjaci;
    private String q;
    private SortEnum sort;

    public AppliedFiltersDTO() {
    }

    public AppliedFiltersDTO(BigDecimal minCena, BigDecimal maxCena, List<String> proizvodjaci,
            String q, SortEnum sort) {
        this.minCena = minCena;
        this.maxCena = maxCena;
        this.proizvodjaci = proizvodjaci;
        this.q = q;
        this.sort = sort;
    }

    public BigDecimal getMinCena() {
        return minCena;
    }

    public void setMinCena(BigDecimal minCena) {
        this.minCena = minCena;
    }

    public BigDecimal getMaxCena() {
        return maxCena;
    }

    public void setMaxCena(BigDecimal maxCena) {
        this.maxCena = maxCena;
    }

    public List<String> getProizvodjaci() {
        return proizvodjaci;
    }

    public void setProizvodjaci(List<String> proizvodjaci) {
        this.proizvodjaci = proizvodjaci;
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public SortEnum getSort() {
        return sort;
    }

    public void setSort(SortEnum sort) {
        this.sort = sort;
    }
}
