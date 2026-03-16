package com.tehno.tehnozonaspring.service;

import com.tehno.tehnozonaspring.dto.AppliedFiltersDTO;
import com.tehno.tehnozonaspring.dto.ListResponseDTO;
import com.tehno.tehnozonaspring.dto.SortEnum;
import com.tehno.tehnozonaspring.model.Artikal;
import com.tehno.tehnozonaspring.repository.VendorRepository;
import com.tehno.tehnozonaspring.util.QueryParserHelper;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.*;

@Service
public class SearchService {

    private final VendorRepository vendorRepository;

    public SearchService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
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

        // 1. Vendor scope — fetch and parse all artikli
        List<Artikal> allArtikli = fetchArtikliForVendor(vendorId);

        // 2. Search filter
        List<Artikal> searchResults = applySearchFilter(allArtikli, sanitizedQ);

        // 3. Price filter
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

    private List<Artikal> fetchArtikliForVendor(Long vendorId) {
        List<String> xmlList;
        if (vendorId == 0) {
            // Unified search across all vendors with lowest price deduplication
            xmlList = vendorRepository.findUnifiedArtikliXml();
        } else {
            xmlList = vendorRepository.findAllArtikliXmlByVendorId(vendorId);
        }

        if (xmlList == null || xmlList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Artikal> artikli = new ArrayList<>();
        try {
            JAXBContext context = JAXBContext.newInstance(Artikal.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            for (String xml : xmlList) {
                if (xml == null || xml.trim().isEmpty()) {
                    continue;
                }
                try {
                    Artikal artikal = (Artikal) unmarshaller.unmarshal(new StringReader(xml));
                    artikli.add(artikal);
                } catch (Exception ignored) {
                    // skip invalid XML entries
                }
            }
        } catch (Exception e) {
            return Collections.emptyList();
        }
        return artikli;
    }

    private List<Artikal> applySearchFilter(List<Artikal> artikli, String query) {
        String lowerQuery = query.toLowerCase();
        boolean isNumeric = query.matches("\\d+");

        List<Artikal> exactMatches = new ArrayList<>();
        List<Artikal> containsMatches = new ArrayList<>();

        for (Artikal a : artikli) {
            // If query is numeric → check exact match on sifra or barkod first
            if (isNumeric) {
                String sifra = a.getSifra() != null ? a.getSifra().trim() : "";
                String barkod = a.getBarkod() != null ? a.getBarkod().trim() : "";
                if (sifra.equals(query) || barkod.equals(query)) {
                    exactMatches.add(a);
                    continue;
                }
            }

            // Case-insensitive contains on naziv and proizvodjac
            String naziv = a.getNaziv() != null ? a.getNaziv().toLowerCase() : "";
            String proizvodjac = a.getProizvodjac() != null ? a.getProizvodjac().toLowerCase() : "";

            if (naziv.contains(lowerQuery) || proizvodjac.contains(lowerQuery)) {
                containsMatches.add(a);
            }
        }

        // Exact matches first, then contains matches
        List<Artikal> result = new ArrayList<>(exactMatches);
        result.addAll(containsMatches);
        return result;
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
