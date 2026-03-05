package com.tehno.tehnozonaspring.dto;

public enum SortEnum {

    PRICE_ASC,
    PRICE_DESC,
    NAME_ASC,
    NAME_DESC,
    NEWEST;

    public static SortEnum fromString(String value) {
        if (value == null) {
            return NAME_ASC;
        }
        try {
            return SortEnum.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NAME_ASC;
        }
    }
}
