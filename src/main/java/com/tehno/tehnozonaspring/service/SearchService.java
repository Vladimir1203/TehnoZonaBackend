package com.tehno.tehnozonaspring.service;

import com.tehno.tehnozonaspring.dto.AppliedFiltersDTO;
import com.tehno.tehnozonaspring.dto.ListResponseDTO;
import com.tehno.tehnozonaspring.dto.SortEnum;
import com.tehno.tehnozonaspring.model.Artikal;
import com.tehno.tehnozonaspring.repository.ArtikalQueryRepository;
import com.tehno.tehnozonaspring.util.QueryParserHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class SearchService {

    private final ArtikalQueryRepository artikalQueryRepository;

    public SearchService(ArtikalQueryRepository artikalQueryRepository) {
        this.artikalQueryRepository = artikalQueryRepository;
    }

    public ListResponseDTO<Artikal> search(Long vendorId,
            String q,
            String pageParam,
            String sizeParam,
            String minCenaParam,
            String maxCenaParam,
            String[] proizvodjaciRepeated,
            String proizvodjaciCsv,
            String sortParam) {

        int page = QueryParserHelper.parsePage(pageParam, 0, 100);
        int size = QueryParserHelper.parseSize(sizeParam, 20, 100);
        BigDecimal minCena = QueryParserHelper.parseMinCena(minCenaParam);
        BigDecimal maxCena = QueryParserHelper.parseMaxCena(maxCenaParam);
        List<String> proizvodjaci = QueryParserHelper.parseManufacturers(proizvodjaciRepeated, proizvodjaciCsv);
        SortEnum sort = QueryParserHelper.parseSort(sortParam);

        // Sanitize query
        String sanitizedQ = QueryParserHelper.sanitizeQuery(q);

        AppliedFiltersDTO appliedFilters = new AppliedFiltersDTO(
                minCena, maxCena,
                proizvodjaci.isEmpty() ? null : proizvodjaci,
                sanitizedQ, sort);

        // If sanitized query is null → return empty result
        if (sanitizedQ == null) {
            return new ListResponseDTO<>(
                    Collections.emptyList(),
                    page, size, 0L,
                    null, null,
                    Collections.emptyMap(),
                    appliedFilters);
        }

        // 1. Fetch matching artikli from DB (with Cloudinary URLs)
        List<Artikal> searchResults = fetchByQuery(vendorId, sanitizedQ);

        // 2. Price filter
        List<Artikal> afterPriceFilter = applyPriceFilter(searchResults, minCena, maxCena);

        // 4. Compute minPrice / maxPrice from results after q filter (before
        // manufacturer filter)
        BigDecimal minPrice = computeMinPrice(searchResults);
        BigDecimal maxPrice = computeMaxPrice(searchResults);

        // 5. Compute manufacturerCounts after q + price filters, BEFORE manufacturer
        // filter
        Map<String, Long> manufacturerCounts = computeManufacturerCounts(afterPriceFilter);

        // 6. Manufacturer filter
        List<Artikal> afterManufacturerFilter = applyManufacturerFilter(afterPriceFilter, proizvodjaci);

        // 7. Sort (before pagination)
        List<Artikal> sorted = applySort(afterManufacturerFilter, sort);

        // 8. Pagination
        long total = sorted.size();
        int fromIndex = Math.min(page * size, sorted.size());
        int toIndex = Math.min(fromIndex + size, sorted.size());
        List<Artikal> paginatedItems = sorted.subList(fromIndex, toIndex);

        System.out.println("Endpoint: search");
        System.out.println("Min Price: " + minPrice);
        System.out.println("Max Price: " + maxPrice);

        return new ListResponseDTO<>(
                paginatedItems,
                page, size, total,
                minPrice, maxPrice,
                manufacturerCounts,
                appliedFilters);
    }

    // ─── Helper methods ───────────────────────────────────────────────

    private List<Artikal> fetchByQuery(Long vendorId, String query) {
        if (vendorId == 0) {
            try {
                List<Artikal> results = artikalQueryRepository.search(query);
                if (results.isEmpty()) {
                    results = artikalQueryRepository.searchIlike(query);
                }
                return results;
            } catch (Exception e) {
                return artikalQueryRepository.searchIlike(query);
            }
        } else {
            // Vendor-specific: filter artikal table in memory (vendor datasets are smaller)
            String lowerQuery = query.toLowerCase();
            return artikalQueryRepository.findByVendorId(vendorId).stream()
                    .filter(a -> (a.getNaziv() != null && a.getNaziv().toLowerCase().contains(lowerQuery))
                            || (a.getProizvodjac() != null && a.getProizvodjac().toLowerCase().contains(lowerQuery)))
                    .toList();
        }
    }

    private List<Artikal> applyPriceFilter(List<Artikal> artikli, BigDecimal minCena, BigDecimal maxCena) {
        if (minCena == null && maxCena == null) {
            return artikli;
        }

        List<Artikal> result = new ArrayList<>();
        for (Artikal a : artikli) {
            BigDecimal cena = BigDecimal.valueOf(a.getMpcena());
            if (minCena != null && cena.compareTo(minCena) < 0) {
                continue;
            }
            if (maxCena != null && cena.compareTo(maxCena) > 0) {
                continue;
            }
            result.add(a);
        }
        return result;
    }

    private Map<String, Long> computeManufacturerCounts(List<Artikal> artikli) {
        Map<String, Long> counts = new TreeMap<>();
        for (Artikal a : artikli) {
            String p = a.getProizvodjac();
            if (p == null || p.trim().isEmpty()) {
                continue;
            }
            String key = p.trim().toUpperCase();
            if (key.equals("/") || key.equals("-")) {
                continue;
            }
            counts.merge(key, 1L, (oldValue, newValue) -> oldValue + newValue);
        }
        return counts;
    }

    private BigDecimal computeMinPrice(List<Artikal> artikli) {
        BigDecimal min = null;
        for (Artikal a : artikli) {
            BigDecimal cena = BigDecimal.valueOf(a.getMpcena());
            if (min == null || cena.compareTo(min) < 0) {
                min = cena;
            }
        }
        return min;
    }

    private BigDecimal computeMaxPrice(List<Artikal> artikli) {
        BigDecimal max = null;
        for (Artikal a : artikli) {
            BigDecimal cena = BigDecimal.valueOf(a.getMpcena());
            if (max == null || cena.compareTo(max) > 0) {
                max = cena;
            }
        }
        return max;
    }

    private List<Artikal> applyManufacturerFilter(List<Artikal> artikli, List<String> proizvodjaci) {
        if (proizvodjaci == null || proizvodjaci.isEmpty()) {
            return artikli;
        }

        Set<String> proizvodjaciSet = new HashSet<>();
        for (String p : proizvodjaci) {
            if (p != null) {
                proizvodjaciSet.add(p.trim().toUpperCase());
            }
        }

        List<Artikal> result = new ArrayList<>();
        for (Artikal a : artikli) {
            if (a.getProizvodjac() != null
                    && proizvodjaciSet.contains(a.getProizvodjac().trim().toUpperCase())) {
                result.add(a);
            }
        }
        return result;
    }

    private List<Artikal> applySort(List<Artikal> artikli, SortEnum sort) {
        List<Artikal> sorted = new ArrayList<>(artikli);
        Comparator<Artikal> comparator;

        switch (sort) {
            case PRICE_ASC:
                comparator = Comparator.comparingDouble(Artikal::getMpcena);
                break;
            case PRICE_DESC:
                comparator = Comparator.comparingDouble(Artikal::getMpcena).reversed();
                break;
            case NAME_DESC:
                comparator = Comparator.comparing(
                        a -> a.getNaziv() != null ? a.getNaziv().toLowerCase() : "",
                        Comparator.reverseOrder());
                break;
            case NEWEST:
            case NAME_ASC:
            default:
                comparator = Comparator.comparing(
                        a -> a.getNaziv() != null ? a.getNaziv().toLowerCase() : "");
                break;
        }

        sorted.sort(comparator);
        return sorted;
    }
}
