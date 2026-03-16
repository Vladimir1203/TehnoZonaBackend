package com.tehno.tehnozonaspring.dto;

import java.util.List;

public class NadgrupaDTO {
    private String name;
    private String image;
    private List<String> grupe;

    public NadgrupaDTO() {
    }

    public NadgrupaDTO(String name, String image) {
        this.name = name;
        this.image = image;
    }

    public NadgrupaDTO(String name, String image, List<String> grupe) {
        this.name = name;
        this.image = image;
        this.grupe = grupe;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<String> getGrupe() {
        return grupe;
    }

    public void setGrupe(List<String> grupe) {
        this.grupe = grupe;
    }
}
