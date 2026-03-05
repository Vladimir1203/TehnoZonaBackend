package com.tehno.tehnozonaspring.controller;

import com.tehno.tehnozonaspring.dto.AppliedFiltersDTO;
import com.tehno.tehnozonaspring.dto.FeaturedArtikalResponse;
import com.tehno.tehnozonaspring.dto.ListResponseDTO;
import com.tehno.tehnozonaspring.dto.ProductPageResponse;
import com.tehno.tehnozonaspring.dto.SortEnum;
import com.tehno.tehnozonaspring.model.*;
import com.tehno.tehnozonaspring.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/vendors/")
public class VendorController {

    private final VendorService vendorService;

    @Autowired
    public VendorController(VendorService VendorService) {
        this.vendorService = VendorService;
    }

    @GetMapping
    public List<Vendor> getAllBeans() {
        return vendorService.getAllBeans();
    }

    @GetMapping("/{id}/naziv")
    public String getArtikalNaziv(@PathVariable Long id) {
        return vendorService.findArtikliNaziviByGrupa(id);
    }

    @GetMapping("/{id}/nadgrupe")
    public ResponseEntity<String> getDistinctNadgrupe(@PathVariable Long id) {
        String nadgrupe = vendorService.getDistinctNadgrupe(id);
        return ResponseEntity.ok(nadgrupe);
    }

    @GetMapping("/{id}/nadgrupa/{nadgrupa}/grupe")
    public ResponseEntity<String> getGrupeByNadgrupa(@PathVariable Long id, @PathVariable String nadgrupa) {
        String grupe = vendorService.getGrupeByNadgrupa(id, nadgrupa);
        if (grupe == null || grupe.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(grupe);
    }

    @GetMapping("/artikli")
    public ResponseEntity<List<Artikal>> getArtikli(
            @RequestParam Long vendorId,
            @RequestParam int limit) {
        List<Artikal> artikli = vendorService.getArtikli(vendorId, limit);
        if (artikli == null || artikli.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(artikli);
    }

    @GetMapping("/glavneGrupe")
    public ResponseEntity<List<String>> vratiGlavneGrupe() {
        List<String> glavneGrupe = vendorService.getGlavneGrupe();
        return ResponseEntity.ok(glavneGrupe);
    }

    @GetMapping("/glavneGrupe/{glavnaGrupa}/nadgrupe")
    public ResponseEntity<List<String>> getNadgrupeByGlavnaGrupa(@PathVariable String glavnaGrupa) {
        List<String> nadgrupe = vendorService.getNadgrupeByGlavnaGrupa(glavnaGrupa);
        if (nadgrupe.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(nadgrupe);
    }

    @GetMapping("/{id}/nadgrupa/{nadgrupa}/artikli")
    public ResponseEntity<List<Artikal>> getArtikliByNadgrupa(
            @PathVariable Long id,
            @PathVariable String nadgrupa) {
        List<Artikal> artikli = vendorService.getArtikliByNadgrupa(id, nadgrupa);
        if (artikli == null || artikli.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(artikli);
    }

    @GetMapping("/grupe")
    public ResponseEntity<List<String>> getAllGroups() {
        List<String> groups = vendorService.getAllGroups();
        if (groups == null || groups.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/{id}/glavnaGrupa/{glavnaGrupa}/artikli")
    public ResponseEntity<ProductPageResponse> getArtikliByGlavnaGrupa(
            @PathVariable Long id,
            @PathVariable String glavnaGrupa,
            @RequestParam(required = false) Double minCena,
            @RequestParam(required = false) Double maxCena,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> proizvodjaci,
            @RequestParam(required = false) String sort) {
        ProductPageResponse response = new ProductPageResponse();
        // 1. Get all items for the category (no price filter yet to get absolute range)
        List<Artikal> allCategoryArtikli = vendorService.vratiArtiklePoGlavnojGrupiICeni(id, glavnaGrupa, null, null,
                0, Integer.MAX_VALUE, null, response);

        // initialMin/Max are already set inside vratiArtiklePoGlavnojGrupiICeni

        // 2. Filter by manufacturer
        List<Artikal> afterManufacturer = filtrirajPoProizvodjacima(allCategoryArtikli, proizvodjaci);

        // 3. Calculate current min/max for the slider (based on selected manufacturers)
        double min = afterManufacturer.stream().mapToDouble(Artikal::getMpcena).min().orElse(0.0);
        double max = afterManufacturer.stream().mapToDouble(Artikal::getMpcena).max().orElse(0.0);

        // 4. Apply price filter
        List<Artikal> filtered = filtrirajPoCeni(afterManufacturer, minCena, maxCena);

        // 5. Apply sorting
        filtered.sort(getArtikalComparator(sort));

        int totalCount = filtered.size();
        int fromIndex = Math.min(page * size, totalCount);
        int toIndex = Math.min(fromIndex + size, totalCount);
        List<Artikal> paginated = filtered.subList(fromIndex, toIndex);

        response.setProducts(paginated);
        response.setTotalCount(totalCount);
        response.setMaxCena(max);

        // Sidebar counts (filtered by price but NOT manufacturers)
        List<Artikal> forSidebar = filtrirajPoCeni(allCategoryArtikli, minCena, maxCena);
        Map<String, Integer> manufacturerCounts = forSidebar.stream()
                .filter(a -> a.getProizvodjac() != null && !a.getProizvodjac().trim().isEmpty())
                .collect(Collectors.groupingBy(a -> a.getProizvodjac().trim().toUpperCase(),
                        TreeMap::new, Collectors.summingInt(x -> 1)));

        response.setManufacturerCounts(manufacturerCounts);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/glavnaGrupa/{glavnaGrupa}/nadgrupa/{nadgrupa}/grupa/{grupa}/artikli")
    public ResponseEntity<ProductPageResponse> getArtikliByGlavnaGrupaAndNadgrupaAndGrupa(
            @PathVariable Long id,
            @PathVariable String nadgrupa,
            @PathVariable String glavnaGrupa,
            @PathVariable String grupa,
            @RequestParam(required = false) Double minCena,
            @RequestParam(required = false) Double maxCena,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> proizvodjaci,
            @RequestParam(required = false) String sort) {
        ProductPageResponse response = new ProductPageResponse();
        // 1. Get all items (no price filter yet)
        List<Artikal> allArtikals = vendorService.getArtikliByGrupa(id, nadgrupa, grupa, null, null, response);

        // 2. Filter by manufacturer
        List<Artikal> afterManufacturer = filtrirajPoProizvodjacima(allArtikals, proizvodjaci);

        // 3. Calculate min/max for the slider
        double min = afterManufacturer.stream().mapToDouble(Artikal::getMpcena).min().orElse(0.0);
        double max = afterManufacturer.stream().mapToDouble(Artikal::getMpcena).max().orElse(0.0);

        // 4. Apply price filter
        List<Artikal> filtered = filtrirajPoCeni(afterManufacturer, minCena, maxCena);

        // 5. Apply sorting
        filtered.sort(getArtikalComparator(sort));

        int totalCount = filtered.size();
        int fromIndex = Math.min(page * size, totalCount);
        int toIndex = Math.min(fromIndex + size, totalCount);
        List<Artikal> paginated = filtered.subList(fromIndex, toIndex);

        response.setProducts(paginated);
        response.setTotalCount(totalCount);
        response.setMaxCena(max);

        // Sidebar counts (filtered by price but NOT manufacturers)
        List<Artikal> forSidebar = filtrirajPoCeni(allArtikals, minCena, maxCena);
        Map<String, Integer> manufacturerCounts = forSidebar.stream()
                .filter(a -> a.getProizvodjac() != null && !a.getProizvodjac().trim().isEmpty())
                .collect(Collectors.groupingBy(a -> a.getProizvodjac().trim().toUpperCase(),
                        TreeMap::new, Collectors.summingInt(x -> 1)));

        response.setManufacturerCounts(manufacturerCounts);

        return ResponseEntity.ok(response);
    }

    public static List<Artikal> filtrirajPoProizvodjacima(List<Artikal> artikli, List<String> proizvodjaci) {
        if (proizvodjaci == null || proizvodjaci.isEmpty()) {
            return new ArrayList<>(artikli);
        }
        // proizvodjaci.add("Ugreen");
        // Pretvaramo u Set radi bržeg pretraživanja (O(1) lookup)
        Set<String> proizvodjaciSet = proizvodjaci.stream()
                .filter(Objects::nonNull)
                .map(p -> p.trim().toLowerCase()) // case-insensitive + trim
                .collect(Collectors.toSet());

        List<Artikal> artikli1 = artikli.stream()
                .filter(a -> a.getProizvodjac() != null
                        && proizvodjaciSet.contains(a.getProizvodjac().toLowerCase()))
                .collect(Collectors.toCollection(ArrayList::new));
        return artikli1;
    }

    private Comparator<Artikal> getArtikalComparator(String sort) {
        SortEnum sortEnum = SortEnum.fromString(sort);
        switch (sortEnum) {
            case PRICE_ASC:
                return Comparator.comparingDouble(Artikal::getMpcena);
            case PRICE_DESC:
                return Comparator.comparingDouble(Artikal::getMpcena).reversed();
            case NAME_DESC:
                return Comparator.comparing(a -> a.getNaziv() != null ? a.getNaziv().toLowerCase() : "",
                        Comparator.reverseOrder());
            case NAME_ASC:
            default:
                return Comparator.comparing(a -> a.getNaziv() != null ? a.getNaziv().toLowerCase() : "");
        }
    }

    public static List<Artikal> filtrirajPoCeni(List<Artikal> artikli, Double minCena, Double maxCena) {
        if (artikli == null || artikli.isEmpty())
            return new ArrayList<>();

        double min = (minCena != null) ? minCena : Double.NEGATIVE_INFINITY;
        double max = (maxCena != null) ? maxCena : Double.POSITIVE_INFINITY;

        return artikli.stream()
                .filter(a -> {
                    double cena = a.getMpcena();
                    return cena >= min && cena <= max;
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @GetMapping("/{vendorId}/glavnaGrupa/{glavnaGrupa}/nadgrupa/{nadgrupa}/artikli")
    public ResponseEntity<ProductPageResponse> vratiArtiklePoNadgrupi(
            @PathVariable Long vendorId,
            @PathVariable String nadgrupa,
            @RequestParam(required = false) Double minCena,
            @RequestParam(required = false) Double maxCena,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> proizvodjaci,
            @RequestParam(required = false) String sort) {
        ProductPageResponse response = new ProductPageResponse();
        // 1. Get all items (no price filter yet)
        List<Artikal> allArtikals = vendorService.vratiArtiklePoNadgrupi(
                vendorId, nadgrupa, null, null, 0, Integer.MAX_VALUE, null, response);

        // 2. Filter by manufacturer
        List<Artikal> afterManufacturer = filtrirajPoProizvodjacima(allArtikals, proizvodjaci);

        // 3. Calculate min/max for the slider
        double min = afterManufacturer.stream().mapToDouble(Artikal::getMpcena).min().orElse(0.0);
        double max = afterManufacturer.stream().mapToDouble(Artikal::getMpcena).max().orElse(0.0);

        // 4. Apply price filter
        List<Artikal> filtered = filtrirajPoCeni(afterManufacturer, minCena, maxCena);

        // 5. Apply sorting
        filtered.sort(getArtikalComparator(sort));

        int totalCount = filtered.size();
        int fromIndex = Math.min(page * size, totalCount);
        int toIndex = Math.min(fromIndex + size, totalCount);
        List<Artikal> paginated = filtered.subList(fromIndex, toIndex);

        response.setProducts(paginated);
        response.setTotalCount(totalCount);
        response.setMaxCena(max);

        // Sidebar counts (filtered by price but NOT manufacturers)
        List<Artikal> forSidebar = filtrirajPoCeni(allArtikals, minCena, maxCena);
        Map<String, Integer> manufacturerCounts = forSidebar.stream()
                .filter(a -> a.getProizvodjac() != null && !a.getProizvodjac().trim().isEmpty())
                .collect(Collectors.groupingBy(a -> a.getProizvodjac().trim().toUpperCase(),
                        TreeMap::new, Collectors.summingInt(x -> 1)));

        response.setManufacturerCounts(manufacturerCounts);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{vendorId}/glavne_grupe_i_nadgrupe")
    public ResponseEntity<Map<String, List<String>>> getAllGroupsAndSubgroups(@PathVariable Long vendorId) {
        Map<String, List<String>> groups = vendorService.getAllGroupsAndSubgroups();
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/{vendorId}/nadgrupe_sa_svojim_grupama/{glavnaGrupa}")
    public ResponseEntity<Map<String, List<String>>> vratiSveNadgrupeSaNjihovimGrupama(@PathVariable Long vendorId,
            @PathVariable String glavnaGrupa) {
        Map<String, List<String>> groups = vendorService.vratiSveNadgrupeSaNjihovimGrupama(glavnaGrupa);
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/{vendorid}/svi_idjevi")
    public ResponseEntity<List<Long>> vratiSveIdjeve() {
        List<Long> idjevi = vendorService.vratiSveIdjeve();
        return ResponseEntity.ok(idjevi);
    }

    @GetMapping("/{vendorId}/glavnaGrupa/{glavnaGrupa}/proizvodjaci")
    public ResponseEntity<List<String>> getProizvodjaciByGlavnaGrupa(
            @PathVariable Long vendorId,
            @PathVariable String glavnaGrupa) {
        List<String> proizvodjaci = vendorService.getProizvodjaciByGlavnaGrupa(vendorId, glavnaGrupa);
        if (proizvodjaci.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(proizvodjaci);
    }

    @GetMapping("/{vendorId}/glavnaGrupa/{glavnaGrupa}/proizvodjaci-count")
    public ResponseEntity<Map<String, Integer>> getProizvodjaciWithCountByGlavnaGrupa(
            @PathVariable Long vendorId,
            @PathVariable String glavnaGrupa,
            @RequestParam(required = false) Double minCena,
            @RequestParam(required = false) Double maxCena,
            @RequestParam(required = false) List<String> nadgrupe,
            @RequestParam(required = false) String grupa) {
        List<Artikal> artikli = vendorService.vratiArtiklePoGlavnojGrupiICeni(vendorId, glavnaGrupa, minCena, maxCena,
                0, 0,
                null, new ProductPageResponse());
        Map<String, Integer> rezultat = new HashMap<>();
        if (grupa == null || grupa.equals("null")) {
            rezultat = izvuciProizvodjacePoNadgrupama(artikli, nadgrupe);
        } else {
            rezultat = izvuciProizvodjacePoGrupama(artikli, grupa);
        }
        return rezultat.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(rezultat);
    }

    public static Map<String, Integer> izvuciProizvodjacePoGrupama(List<Artikal> artikals, String grupa) {
        if (artikals == null || artikals.isEmpty()) {
            return Collections.emptyMap();
        }

        Stream<Artikal> stream = artikals.stream();

        if (grupa != null && !grupa.trim().isEmpty()) {
            String decodedGrupa = URLDecoder.decode(grupa, StandardCharsets.UTF_8)
                    .trim()
                    .toUpperCase();
            stream = stream.filter(a -> {
                String g = a.getGrupa();
                return g != null && g.trim().toUpperCase().equals(decodedGrupa);
            });
        }

        return stream
                .map(Artikal::getProizvodjac)
                .filter(Objects::nonNull)
                .map(p -> p.trim().toUpperCase())
                .filter(p -> !p.equals("/") && !p.equals("-") && !p.isEmpty())
                .collect(Collectors.groupingBy(
                        p -> p,
                        TreeMap::new,
                        Collectors.summingInt(x -> 1)));
    }

    public static Map<String, Integer> izvuciProizvodjacePoNadgrupama(List<Artikal> artikals, List<String> nadgrupe) {
        if (artikals == null || artikals.isEmpty()) {
            return Collections.emptyMap();
        }
        Stream<Artikal> stream = artikals.stream();

        if (nadgrupe != null && !nadgrupe.isEmpty()) {
            Set<String> nadgrupeSet = nadgrupe.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .map(String::toUpperCase) // case-insensitive upoređivanje
                    .collect(Collectors.toSet());

            stream = stream.filter(a -> a.getNadgrupa() != null
                    && nadgrupeSet.contains(a.getNadgrupa().trim().toUpperCase()));
        }

        return stream
                .map(Artikal::getProizvodjac)
                .filter(Objects::nonNull)
                .map(p -> p.trim().toUpperCase())
                .filter(p -> !p.equals("/") && !p.equals("-"))
                .collect(Collectors.toMap(
                        p -> p,
                        p -> 1,
                        Integer::sum,
                        TreeMap::new));
    }

    // ne radi kako treba
    @GetMapping("/{vendorId}/max-price")
    public ResponseEntity<BigDecimal> getMaxPrice(@PathVariable Long vendorId) {
        BigDecimal maxPrice = vendorService.getMaxPriceByVendorId(vendorId);

        if (maxPrice != null) {
            return ResponseEntity.ok(maxPrice);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/proizvodjaci")
    public ResponseEntity<List<String>> getAllDistinctProizvodjaci() {
        List<String> proizvodjaci = vendorService.getAllDistinctProizvodjaci();
        if (proizvodjaci.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(proizvodjaci);
    }

    @GetMapping("/glavni-proizvodjaci")
    public ResponseEntity<List<String>> getAllMainProizvodjaci() {
        List<String> glavniProizvodjaci = vendorService.getAllMainProizvodjaci();
        return ResponseEntity.ok(glavniProizvodjaci);
    }

    @GetMapping("/artikli/proizvodjac")
    public ResponseEntity<List<Artikal>> getArtikliByProizvodjac(@RequestParam String proizvodjac) {
        if (proizvodjac == null || proizvodjac.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<Artikal> artikli = vendorService.getArtikliByProizvodjac(proizvodjac);

        if (artikli.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(artikli);
    }

    @GetMapping("/{vendorId}/artikli/brand/{brand}")
    public ResponseEntity<ListResponseDTO<Artikal>> getArtikliByBrand(
            @PathVariable Long vendorId,
            @PathVariable String brand,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Double minCena,
            @RequestParam(required = false) Double maxCena,
            @RequestParam(required = false) List<String> proizvodjaci,
            @RequestParam(required = false) String sort) {
        // 1. Fetch all products for brand
        List<Artikal> allBrandArtikli = vendorService.getArtikliByBrand(vendorId, brand);

        // 2. Compute absolute min/max for this brand BEFORE ANY OTHER FILTERS
        double minOfAll = allBrandArtikli.stream().mapToDouble(Artikal::getMpcena).min().orElse(0.0);
        double maxOfAll = allBrandArtikli.stream().mapToDouble(Artikal::getMpcena).max().orElse(0.0);

        // 3. Filter by manufacturer
        List<Artikal> afterManufacturer = filtrirajPoProizvodjacima(allBrandArtikli, proizvodjaci);

        // 4. Filter by price
        List<Artikal> filtered = filtrirajPoCeni(afterManufacturer, minCena, maxCena);

        // 4. Sort
        filtered.sort(getArtikalComparator(sort));

        // 5. Pagination
        int total = filtered.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<Artikal> paginated = filtered.subList(fromIndex, toIndex);

        // 6. Manufacturer counts for sidebar (should be filtered by price but NOT by
        // selected manufacturers)
        List<Artikal> forCounts = filtrirajPoCeni(allBrandArtikli, minCena, maxCena);
        Map<String, Long> manufacturerCounts = forCounts.stream()
                .filter(a -> a.getProizvodjac() != null && !a.getProizvodjac().trim().isEmpty())
                .collect(Collectors.groupingBy(a -> a.getProizvodjac().trim().toUpperCase(),
                        TreeMap::new, Collectors.counting()));

        AppliedFiltersDTO appliedFilters = new AppliedFiltersDTO(
                minCena != null ? BigDecimal.valueOf(minCena) : null,
                maxCena != null ? BigDecimal.valueOf(maxCena) : null,
                List.of(brand),
                null,
                SortEnum.fromString(sort));

        ListResponseDTO<Artikal> response = new ListResponseDTO<>(
                paginated, page, size, total,
                BigDecimal.valueOf(minOfAll), BigDecimal.valueOf(maxOfAll),
                manufacturerCounts, appliedFilters);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{vendorId}/grupa/{grupa}/artikli")
    public ResponseEntity<List<Artikal>> getArtikliByGrupa(
            @PathVariable Long vendorId,
            @PathVariable String grupa,
            @RequestParam(required = false) Double minCena,
            @RequestParam(required = false) Double maxCena,
            @RequestParam(required = false) String nadgrupa) {

        ProductPageResponse response = new ProductPageResponse();
        List<Artikal> artikals = vendorService.getArtikliByGrupa(vendorId, nadgrupa, grupa, minCena, maxCena,
                response);

        List<Artikal> artikli = filtrirajPoCeni(artikals, minCena, maxCena);

        if (artikli == null || artikli.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(artikli);
    }

    @GetMapping("/{vendorId}/glavnaGrupa/{glavnaGrupa}/nadgrupa/{nadgrupa}/proizvodjaci-count")
    public ResponseEntity<Map<String, Integer>> getProizvodjaciWithCountByGlavnaGrupaAndNadgrupa(
            @PathVariable Long vendorId,
            @PathVariable String glavnaGrupa,
            @PathVariable String nadgrupa,
            @RequestParam(required = false) Double minCena,
            @RequestParam(required = false) Double maxCena) {

        Map<String, Integer> result = vendorService.getProizvodjaciWithCountByGlavnaGrupaAndNadgrupa(vendorId,
                glavnaGrupa, nadgrupa, minCena, maxCena);
        return result.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(result);
    }

    @GetMapping("/{vendorId}/search")
    public ResponseEntity<ProductPageResponse> searchArtikli(
            @PathVariable Long vendorId,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Double minCena,
            @RequestParam(required = false) Double maxCena,
            @RequestParam(required = false) List<String> proizvodjaci,
            @RequestParam(required = false) String sort) {

        if (q == null || q.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ProductPageResponse response = new ProductPageResponse();
        List<Artikal> allResults = vendorService.searchArtikliByNazivOrProizvodjac(vendorId, q.trim());

        // Initial range
        double initMin = allResults.stream().mapToDouble(Artikal::getMpcena).min().orElse(0.0);
        double initMax = allResults.stream().mapToDouble(Artikal::getMpcena).max().orElse(0.0);
        response.setInitialMinCena(initMin);
        response.setInitialMaxCena(initMax);

        // Filter by manufacturer
        List<Artikal> afterManufacturer = filtrirajPoProizvodjacima(allResults, proizvodjaci);

        // Active range
        double currentMin = afterManufacturer.stream().mapToDouble(Artikal::getMpcena).min().orElse(0.0);
        double currentMax = afterManufacturer.stream().mapToDouble(Artikal::getMpcena).max().orElse(0.0);

        // Filter by price
        List<Artikal> filtered = filtrirajPoCeni(afterManufacturer, minCena, maxCena);

        // Sort
        filtered.sort(getArtikalComparator(sort));

        int totalCount = filtered.size();
        int fromIndex = Math.min(page * size, totalCount);
        int toIndex = Math.min(fromIndex + size, totalCount);
        List<Artikal> paginated = filtered.subList(fromIndex, toIndex);

        response.setProducts(paginated);
        response.setTotalCount(totalCount);
        response.setMinCena(currentMin);
        response.setMaxCena(currentMax);

        // Sidebar counts (filtered by price but NOT manufacturers)
        List<Artikal> forSidebar = filtrirajPoCeni(allResults, minCena, maxCena);
        Map<String, Integer> manufacturerCounts = forSidebar.stream()
                .filter(a -> a.getProizvodjac() != null && !a.getProizvodjac().trim().isEmpty())
                .collect(Collectors.groupingBy(a -> a.getProizvodjac().trim().toUpperCase(),
                        TreeMap::new, Collectors.summingInt(x -> 1)));

        response.setManufacturerCounts(manufacturerCounts);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{vendorId}/artikal/{artikalBarCode}")
    public ResponseEntity<Artikal> getProductByArtikalBarCode(
            @PathVariable Long vendorId,
            @PathVariable String artikalBarCode) {
        Artikal artikal = vendorService.getProductByArtikalBarCode(vendorId, artikalBarCode);

        return ResponseEntity.ok(artikal);
    }

    @PostMapping("/{vendorId}/featured")
    public ResponseEntity<FeaturedProduct> addFeaturedProduct(
            @PathVariable Long vendorId,
            @RequestParam String barcode,
            @RequestParam FeatureType featureType,
            @RequestParam(required = false) Integer priority,
            @RequestParam(required = false) String validFrom,
            @RequestParam(required = false) String validTo) {
        FeaturedProduct fp = vendorService.addFeaturedProduct(
                vendorId,
                barcode,
                featureType,
                priority,
                validFrom != null ? LocalDateTime.parse(validFrom) : null,
                validTo != null ? LocalDateTime.parse(validTo) : null);

        return ResponseEntity.ok(fp);
    }

    @GetMapping("/featured/all")
    public ResponseEntity<List<FeaturedArtikalResponse>> getAllFeatured() {
        return ResponseEntity.ok(vendorService.getActiveFeaturedArtikli());
    }

    @GetMapping("/featured")
    public ResponseEntity<List<FeaturedArtikalResponse>> getFeaturedByType(
            @RequestParam FeatureType type) {
        return ResponseEntity.ok(vendorService.getActiveFeaturedArtikliByType(type));
    }

    @GetMapping("/{vendorId}/brand/{brand}/glavnaGrupa/count")
    public ResponseEntity<List<Map<String, Object>>> getCountByGlavnaGrupaForBrand(
            @PathVariable Long vendorId,
            @PathVariable String brand,
            @RequestParam(required = false) Double minCena,
            @RequestParam(required = false) Double maxCena) {
        List<Map<String, Object>> result = vendorService.getCountByGlavnaGrupaForBrand(vendorId, brand, minCena,
                maxCena);

        if (result.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{vendorId}/brand/{brand}/glavnaGrupa/{glavnaGrupa}/artikli")
    public ResponseEntity<ProductPageResponse> getArtikliByBrandAndGlavnaGrupa(
            @PathVariable Long vendorId,
            @PathVariable String brand,
            @PathVariable String glavnaGrupa,
            @RequestParam(required = false) Double minCena,
            @RequestParam(required = false) Double maxCena,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> proizvodjaci,
            @RequestParam(required = false) String sort) {
        ProductPageResponse response = new ProductPageResponse();
        // 1. Get ALL items for brand + category
        List<Artikal> allBrandGroupArtikli = vendorService.getArtikliByBrandAndGlavnaGrupa(vendorId, brand,
                glavnaGrupa);

        // 2. Set absolute min/max range
        double initialMin = allBrandGroupArtikli.stream().mapToDouble(Artikal::getMpcena).min().orElse(0.0);
        double initialMax = allBrandGroupArtikli.stream().mapToDouble(Artikal::getMpcena).max().orElse(0.0);
        response.setInitialMinCena(initialMin);
        response.setInitialMaxCena(initialMax);

        // 3. Filter by manufacturer
        List<Artikal> afterManufacturer = filtrirajPoProizvodjacima(allBrandGroupArtikli, proizvodjaci);

        // 3. Filter by price
        List<Artikal> filtered = filtrirajPoCeni(afterManufacturer, minCena, maxCena);

        // 4. Apply sorting
        filtered.sort(getArtikalComparator(sort));

        // 5. Calculate current active range
        double currentMin = filtered.stream().mapToDouble(Artikal::getMpcena).min().orElse(0.0);
        double currentMax = filtered.stream().mapToDouble(Artikal::getMpcena).max().orElse(0.0);

        // 5. Pagination
        int totalCount = filtered.size();
        int fromIndex = Math.min(page * size, totalCount);
        int toIndex = Math.min(fromIndex + size, totalCount);
        List<Artikal> paginated = filtered.subList(fromIndex, toIndex);

        // 6. Response
        response.setProducts(paginated);
        response.setTotalCount(totalCount);
        response.setMinCena(currentMin);
        response.setMaxCena(currentMax);

        return ResponseEntity.ok(response);
    }

}
