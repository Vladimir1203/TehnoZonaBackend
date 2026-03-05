package com.tehno.tehnozonaspring.util;

import com.tehno.tehnozonaspring.dto.SortEnum;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class QueryParserHelper {

    private QueryParserHelper() {
    }

    public static int parsePage(String page, int defaultValue, int maxAllowed) {
        if (page == null) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(page.trim());
            if (value < 0) {
                return defaultValue;
            }
            return Math.min(value, maxAllowed);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static int parseSize(String size, int defaultValue, int maxAllowed) {
        if (size == null) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(size.trim());
            if (value < 0) {
                return defaultValue;
            }
            return Math.min(value, maxAllowed);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static BigDecimal parseMinCena(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static BigDecimal parseMaxCena(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static List<String> parseManufacturers(String[] repeatedParams, String csvParam) {
        List<String> result = new ArrayList<>();

        if (repeatedParams != null) {
            for (String param : repeatedParams) {
                if (param != null) {
                    for (String part : param.split(",")) {
                        String trimmed = part.trim();
                        if (!trimmed.isEmpty()) {
                            result.add(trimmed);
                        }
                    }
                }
            }
        }

        if (csvParam != null) {
            for (String part : csvParam.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty() && !result.contains(trimmed)) {
                    result.add(trimmed);
                }
            }
        }

        return result;
    }

    public static String sanitizeQuery(String q) {
        if (q == null) {
            return null;
        }
        String trimmed = q.trim();
        String collapsed = trimmed.replaceAll("\\s+", " ");
        if (collapsed.length() < 2) {
            return null;
        }
        if (collapsed.length() > 80) {
            collapsed = collapsed.substring(0, 80);
        }
        return collapsed;
    }

    public static SortEnum parseSort(String sort) {
        return SortEnum.fromString(sort);
    }
}
