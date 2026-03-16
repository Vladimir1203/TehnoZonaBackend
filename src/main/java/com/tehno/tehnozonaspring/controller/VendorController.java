package com.tehno.tehnozonaspring.controller;

import com.tehno.tehnozonaspring.dto.FeaturedArtikalResponse;
import com.tehno.tehnozonaspring.dto.NadgrupaDTO;
import com.tehno.tehnozonaspring.dto.ProductPageResponse;
import com.tehno.tehnozonaspring.model.*;
import com.tehno.tehnozonaspring.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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

    @GetMapping("/duplicates")
    public ResponseEntity<Map<String, List<Artikal>>> getDuplicates() {
        Map<String, List<Artikal>> duplicates = vendorService.getDuplicateProducts();
        return ResponseEntity.ok(duplicates);
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

    @GetMapping("/menu-structure")
    public ResponseEntity<List<Map<String, Object>>> getMenuStructure() {
        return ResponseEntity.ok(vendorService.getFullMenuStructure());
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

    @GetMapping("/glavneGrupe/{glavnaGrupa}/nadgrupe-extended")
    public ResponseEntity<List<NadgrupaDTO>> getNadgrupeExtendedByGlavnaGrupa(@PathVariable String glavnaGrupa) {
        List<NadgrupaDTO> nadgrupe = vendorService.getNadgrupeWithImages(glavnaGrupa);
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

        List<Artikal> artikliPoCeni = vendorService.vratiArtiklePoGlavnojGrupiICeni(id, glavnaGrupa, minCena, maxCena,
                0, Integer.MAX_VALUE, null, response);

        List<Artikal> filtriraniPoProizvodjacima = filtrirajPoProizvodjacima(artikliPoCeni, proizvodjaci);

        double min, max;
        if (proizvodjaci == null || proizvodjaci.isEmpty()) {
            min = filtriraniPoProizvodjacima.stream().mapToDouble(Artikal::getCena).min().orElse(0.0);
            max = filtriraniPoProizvodjacima.stream().mapToDouble(Artikal::getCena).max().orElse(0.0);
        } else {
            min = minCena != null ? minCena : 0;
            max = maxCena != null ? maxCena : 0;
        }

        // List<Artikal> sviArtikli = vendorService.getArtikliByGlavnaGrupa(id,
        // glavnaGrupa, minCena, maxCena, page, size, proizvodjaci);
        int totalCount = filtriraniPoProizvodjacima.size();

        List<Artikal> sortirani = sortirajArtikle(filtriraniPoProizvodjacima, sort);

        int fromIndex = Math.min(page * size, totalCount);
        int toIndex = Math.min(fromIndex + size, totalCount);
        List<Artikal> paginated = sortirani.subList(fromIndex, toIndex);

        response.setProducts(paginated);
        response.setTotalCount(totalCount);
        response.setMinCena(min);
        response.setManufacturerCounts(izracunajManufacturerCounts(artikliPoCeni));

        response.setMaxCena(max);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/glavnaGrupa/{glavnaGrupa}/nadgrupa/{nadgrupa}/artikli")
    public ResponseEntity<ProductPageResponse> getArtikliByGlavnaGrupaAndNadgrupa(
            @PathVariable Long id,
            @PathVariable String nadgrupa,
            @PathVariable String glavnaGrupa,
            @RequestParam(required = false) Double minCena,
            @RequestParam(required = false) Double maxCena,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) List<String> proizvodjaci,
            @RequestParam(required = false) String sort) {

        String decodedNadgrupa = safeDecode(nadgrupa);
        ProductPageResponse response = new ProductPageResponse();

        List<Artikal> artikli = vendorService.vratiArtiklePoNadgrupi(id, decodedNadgrupa, minCena, maxCena, page, size,
                proizvodjaci,
                response);

        List<Artikal> filtriraniPoProizvodjacima = filtrirajPoProizvodjacima(artikli, proizvodjaci);

        double min, max;

        if (proizvodjaci == null || proizvodjaci.isEmpty()) {
            min = filtriraniPoProizvodjacima.stream().mapToDouble(Artikal::getCena).min().orElse(0.0);
            max = filtriraniPoProizvodjacima.stream().mapToDouble(Artikal::getCena).max().orElse(0.0);
        } else {
            min = minCena != null ? minCena : 0;
            max = maxCena != null ? maxCena : 0;
        }

        int totalCount = filtriraniPoProizvodjacima.size();
        List<Artikal> sortirani = sortirajArtikle(filtriraniPoProizvodjacima, sort);
        int fromIndex = Math.min(page * size, totalCount);
        int toIndex = Math.min(fromIndex + size, totalCount);
        List<Artikal> paginated = sortirani.subList(fromIndex, toIndex);

        response.setProducts(paginated);
        response.setTotalCount(totalCount);
        response.setMinCena(min);
        response.setMaxCena(max);

        response.setManufacturerCounts(izracunajManufacturerCounts(artikli));
        return ResponseEntity.ok(response);
    }

    private String safeDecode(String value) {
        if (value == null)
            return null;
        try {
            // Spring dekodira %20 automatski, ali %5C (\) često ostaje ili biva blokiran.
            // URLDecoder.decode će raditi ispravno u oba slučaja.
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return value;
        }
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

        // Ručno dekodiranje jer Spring Firewall odbija %5C u PathVariable
        String decodedNadgrupa = safeDecode(nadgrupa);
        String decodedGrupa = safeDecode(grupa);

        System.out.println("DEBUG: Decoded Nadgrupa: " + decodedNadgrupa + ", Decoded Grupa: " + decodedGrupa);

        List<Artikal> artikals = vendorService.getArtikliByGrupa(id, decodedNadgrupa, decodedGrupa, minCena, maxCena,
                response);
        List<Artikal> filtriraniPoProizvodjacima = filtrirajPoProizvodjacima(artikals, proizvodjaci);

        double min, max;

        if (proizvodjaci == null || proizvodjaci.isEmpty()) {
            min = filtriraniPoProizvodjacima.stream().mapToDouble(Artikal::getCena).min().orElse(0.0);
            max = filtriraniPoProizvodjacima.stream().mapToDouble(Artikal::getCena).max().orElse(0.0);
        } else {
            min = minCena != null ? minCena : 0;
            max = maxCena != null ? maxCena : 0;
        }

        int totalCount = filtriraniPoProizvodjacima.size();
        List<Artikal> sortirani = sortirajArtikle(filtriraniPoProizvodjacima, sort);
        int fromIndex = Math.min(page * size, totalCount);
        int toIndex = Math.min(fromIndex + size, totalCount);
        List<Artikal> paginated = sortirani.subList(fromIndex, toIndex);

        response.setProducts(paginated);
        response.setTotalCount(totalCount);
        response.setMinCena(min);
        response.setMaxCena(max);

        response.setManufacturerCounts(izracunajManufacturerCounts(artikals));
        return ResponseEntity.ok(response);

    }

    @GetMapping("/{id}/artikli-paginated")
    public ResponseEntity<ProductPageResponse> getArtikliFiltered(
            @PathVariable Long id,
            @RequestParam(required = false) String glavnaGrupa,
            @RequestParam(required = false) String nadgrupa,
            @RequestParam(required = false) String grupa,
            @RequestParam(required = false) Double minCena,
            @RequestParam(required = false) Double maxCena,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> proizvodjaci,
            @RequestParam(required = false) String sort) {

        ProductPageResponse response = new ProductPageResponse();
        List<Artikal> artikli;

        // Logika za određivanje šta dohvatiti na osnovu parametara
        if (grupa != null && !grupa.isEmpty() && nadgrupa != null) {
            artikli = vendorService.getArtikliByGrupa(id, nadgrupa, grupa, minCena, maxCena, response);
        } else if (nadgrupa != null && !nadgrupa.isEmpty()) {
            artikli = vendorService.vratiArtiklePoNadgrupi(id, nadgrupa, minCena, maxCena, page, size, proizvodjaci,
                    response);
        } else if (glavnaGrupa != null && !glavnaGrupa.isEmpty()) {
            artikli = vendorService.vratiArtiklePoGlavnojGrupiICeni(id, glavnaGrupa, minCena, maxCena, 0,
                    Integer.MAX_VALUE, null, response);
        } else {
            artikli = vendorService.getArtikli(id, Integer.MAX_VALUE);
        }

        List<Artikal> filtriraniPoProizvodjacima = filtrirajPoProizvodjacima(artikli, proizvodjaci);

        double min, max;
        if (proizvodjaci == null || proizvodjaci.isEmpty()) {
            min = filtriraniPoProizvodjacima.stream().mapToDouble(Artikal::getCena).min().orElse(0.0);
            max = filtriraniPoProizvodjacima.stream().mapToDouble(Artikal::getCena).max().orElse(0.0);
        } else {
            min = minCena != null ? minCena : response.getInitialMinCena();
            max = maxCena != null ? maxCena : response.getInitialMaxCena();
        }

        int totalCount = filtriraniPoProizvodjacima.size();
        List<Artikal> sortirani = sortirajArtikle(filtriraniPoProizvodjacima, sort);
        int fromIndex = Math.min(page * size, totalCount);
        int toIndex = Math.min(fromIndex + size, totalCount);
        List<Artikal> paginated = sortirani.subList(fromIndex, toIndex);

        response.setProducts(paginated);
        response.setTotalCount(totalCount);
        response.setMinCena(min);
        response.setMaxCena(max);
        response.setManufacturerCounts(izracunajManufacturerCounts(artikli));

        return ResponseEntity.ok(response);
    }

    public static Map<String, Integer> izracunajManufacturerCounts(List<Artikal> artikli) {
        if (artikli == null || artikli.isEmpty()) {
            return Collections.emptyMap();
        }
        return artikli.stream()
                .map(Artikal::getProizvodjac)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(p -> !p.isEmpty() && !p.equals("/") && !p.equals("-"))
                .collect(Collectors.toMap(
                        p -> p.toUpperCase(),
                        p -> 1,
                        Integer::sum,
                        TreeMap::new));
    }

    public static List<Artikal> filtrirajPoProizvodjacima(List<Artikal> artikli, List<String> proizvodjaci) {
        if (proizvodjaci == null || proizvodjaci.isEmpty()) {
            return artikli;
        }

        Set<String> set = new HashSet<>();
        for (String p : proizvodjaci) {
            if (p != null) {
                for (String part : p.split(",")) {
                    String s = part.trim().toLowerCase();
                    if (!s.isEmpty())
                        set.add(s);
                }
            }
        }

        return artikli.stream()
                .filter(a -> a.getProizvodjac() != null && set.contains(a.getProizvodjac().trim().toLowerCase()))
                .collect(Collectors.toList());
    }

    public static List<Artikal> filtrirajPoCeni(List<Artikal> artikli, Double minCena, Double maxCena) {
        if (artikli == null || artikli.isEmpty())
            return List.of();

        double min = (minCena != null) ? minCena : Double.NEGATIVE_INFINITY;
        double max = (maxCena != null) ? maxCena : Double.POSITIVE_INFINITY;

        return artikli.stream()
                .filter(a -> {
                    double cena = a.getCena();
                    return cena >= 100 && cena >= min && cena <= max;
                })
                .toList();
    }

    public static List<Artikal> sortirajArtikle(List<Artikal> artikli, String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            return artikli;
        }
        List<Artikal> sortirani = new ArrayList<>(artikli);
        switch (sort.toLowerCase()) {
            case "cena_asc":
                sortirani.sort(Comparator.comparingDouble(Artikal::getCena));
                break;
            case "cena_desc":
                sortirani.sort(Comparator.comparingDouble(Artikal::getCena).reversed());
                break;
            case "naziv_asc":
                sortirani.sort(Comparator.comparing(
                        a -> a.getNaziv() != null ? a.getNaziv().toLowerCase() : ""));
                break;
            case "naziv_desc":
                sortirani.sort(Comparator.comparing(
                        (Artikal a) -> a.getNaziv() != null ? a.getNaziv().toLowerCase() : "").reversed());
                break;
            case "najnovije":
                // XML nema datum, pa ostavi originalni redosled
                break;
            default:
                break;
        }
        System.out.println("sortirano (" + sort + ") cene: "
                + sortirani.stream().map(a -> String.valueOf(a.getCena())).collect(Collectors.toList()));
        return sortirani;
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
                        p -> p.toUpperCase(),
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
                        p -> p.toUpperCase(),
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
    public ResponseEntity<ProductPageResponse> getArtikliByProizvodjac(
            @RequestParam String proizvodjac,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Double minCena,
            @RequestParam(required = false) Double maxCena,
            @RequestParam(required = false) String sort) {
        if (proizvodjac == null || proizvodjac.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<Artikal> artikli = vendorService.getArtikliByProizvodjac(proizvodjac);

        // DEBUG: ispisi sve cene za svaki artikal
        for (Artikal a : artikli) {
            System.out.println("DEBUG CENE: " + a.getNaziv() + " | mpcena=" + a.getMpcena() + " | webCena="
                    + a.getWebCena() + " | b2bcena=" + a.getB2bcena() + " | getCena()=" + a.getCena());
        }

        // Ukupan raspon cena PRE filtriranja (za price slider na frontu)
        double initialMin = artikli.stream().mapToDouble(Artikal::getCena).min().orElse(0.0);
        double initialMax = artikli.stream().mapToDouble(Artikal::getCena).max().orElse(0.0);
        System.out.println("DEBUG: initialMin=" + initialMin + " initialMax=" + initialMax);

        List<Artikal> filtriraniPoCeni = filtrirajPoCeni(artikli, minCena, maxCena);

        if (filtriraniPoCeni.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        double min, max;
        if (minCena == null && maxCena == null) {
            min = initialMin;
            max = initialMax;
        } else {
            min = minCena != null ? minCena : 0;
            max = maxCena != null ? maxCena : 0;
        }

        int totalCount = filtriraniPoCeni.size();
        List<Artikal> sortirani = sortirajArtikle(filtriraniPoCeni, sort);
        int fromIndex = Math.min(page * size, totalCount);
        int toIndex = Math.min(fromIndex + size, totalCount);
        List<Artikal> paginated = sortirani.subList(fromIndex, toIndex);

        ProductPageResponse response = new ProductPageResponse();
        response.setProducts(paginated);
        response.setTotalCount(totalCount);
        response.setMinCena(min);
        response.setMaxCena(max);
        response.setInitialMinCena(initialMin);
        response.setInitialMaxCena(initialMax);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{vendorId}/grupa/{grupa}/artikli")
    public ResponseEntity<List<Artikal>> getArtikliByGrupa(
            @PathVariable Long vendorId,
            @PathVariable String grupa,
            @RequestParam(required = false) Double minCena,
            @RequestParam(required = false) Double maxCena,
            @RequestParam(required = false) String nadgrupa) {

        List<Artikal> artikals = vendorService.getArtikliByGrupa(vendorId, nadgrupa, grupa, minCena, maxCena,
                new ProductPageResponse());
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

    @GetMapping("/{vendorId}/artikli/search")
    public ResponseEntity<List<Artikal>> searchArtikliByNazivOrProizvodjac(
            @PathVariable Long vendorId,
            @RequestParam String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<Artikal> rezultati = vendorService.searchArtikliByNazivOrProizvodjac(vendorId, query.trim());

        if (rezultati == null || rezultati.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(rezultati);
    }

    @GetMapping("/{vendorId}/artikal/{artikalBarCode}")
    public ResponseEntity<Artikal> getProductByArtikalBarCode(
            @PathVariable Long vendorId,
            @PathVariable String artikalBarCode) {
        Artikal artikal = vendorService.getProductByArtikalBarCode(vendorId, artikalBarCode);

        return ResponseEntity.ok(artikal);
    }

    @GetMapping("/{vendorId}/artikli/brand/{brand}")
    public ResponseEntity<ProductPageResponse> getArtikliByBrand(
            @PathVariable Long vendorId,
            @PathVariable String brand,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Double minCena,
            @RequestParam(required = false) Double maxCena,
            @RequestParam(required = false) String sort) {
        List<Artikal> artikli = vendorService.getArtikliByBrand(vendorId, brand);

        double initialMin = artikli.stream().mapToDouble(Artikal::getCena).min().orElse(0.0);
        double initialMax = artikli.stream().mapToDouble(Artikal::getCena).max().orElse(0.0);

        List<Artikal> filtriraniPoCeni = filtrirajPoCeni(artikli, minCena, maxCena);

        double min, max;
        if (minCena == null && maxCena == null) {
            min = initialMin;
            max = initialMax;
        } else {
            min = minCena != null ? minCena : 0;
            max = maxCena != null ? maxCena : 0;
        }

        int totalCount = filtriraniPoCeni.size();
        List<Artikal> sortirani = sortirajArtikle(filtriraniPoCeni, sort);
        int fromIndex = Math.min(page * size, totalCount);
        int toIndex = Math.min(fromIndex + size, totalCount);
        List<Artikal> paginated = sortirani.subList(fromIndex, toIndex);

        ProductPageResponse response = new ProductPageResponse();
        response.setProducts(paginated);
        response.setManufacturerCounts(izracunajManufacturerCounts(filtriraniPoCeni));

        response.setTotalCount(totalCount);
        response.setMinCena(min);
        response.setMaxCena(max);
        response.setInitialMinCena(initialMin);
        response.setInitialMaxCena(initialMax);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{vendorId}/featured")
    public ResponseEntity<FeaturedProduct> addFeaturedProduct(
            @PathVariable Long vendorId,
            @RequestBody com.tehno.tehnozonaspring.dto.HomepageItemRequest request) {

        // Mapiramo HomepageSection (koji dolazi sa fronta) u FeatureType (kao što beka
        // očekuje)
        // ili koristimo direktno FeatureType ako je tako poslato.
        // Na frontu itemType=BANNER, section=HERO.
        // Na beku FeatureType dobija vrednosti BANNER, HERO...

        FeatureType ft;
        try {
            ft = FeatureType.valueOf(request.getSection().name());
        } catch (Exception e) {
            ft = FeatureType.valueOf(request.getItemType().name());
        }

        FeaturedProduct fp = vendorService.addFeaturedProduct(
                vendorId,
                request.getBarcode(),
                ft,
                request.getPriority(),
                request.getValidFrom(),
                request.getValidTo(),
                request.getItemType() != null ? request.getItemType().name() : null,
                request.getSubtitle(),
                request.getButtonText(),
                request.getButtonRoute(),
                request.getGlavnaGrupa(),
                request.getNadgrupa(),
                request.getGrupa(),
                request.getBrandName(),
                request.getCustomName(),
                request.getCustomImageUrl());

        return ResponseEntity.ok(fp);
    }

    @GetMapping("/featured/all")
    public ResponseEntity<List<FeaturedArtikalResponse>> getAllFeatured() {
        List<FeaturedArtikalResponse> result = vendorService.getActiveFeaturedArtikli();
        System.out.println("CONTROLLER: Šaljem " + result.size() + " istaknutih artikala frontu.");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/featured")
    public ResponseEntity<List<FeaturedArtikalResponse>> getFeaturedByType(
            @RequestParam FeatureType type) {
        List<FeaturedArtikalResponse> result = vendorService.getActiveFeaturedArtikliByType(type);
        System.out.println("CONTROLLER: Šaljem " + result.size() + " istaknutih artikala tipa " + type + " frontu.");
        return ResponseEntity.ok(result);
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
            @RequestParam(required = false) String sort) {
        ProductPageResponse response = new ProductPageResponse();

        List<Artikal> artikli = vendorService.getArtikliByBrandAndGlavnaGrupa(vendorId, brand, glavnaGrupa);

        List<Artikal> filtriraniPoCeni = filtrirajPoCeni(artikli, minCena, maxCena);

        // 3. Računanje min/max nakon filtriranja
        double min = filtriraniPoCeni.stream()
                .mapToDouble(Artikal::getCena)
                .min()
                .orElse(0.0);

        double max = filtriraniPoCeni.stream()
                .mapToDouble(Artikal::getCena)
                .max()
                .orElse(0.0);

        // 4. Paginacija
        int totalCount = filtriraniPoCeni.size();
        List<Artikal> sortirani = sortirajArtikle(filtriraniPoCeni, sort);
        int fromIndex = Math.min(page * size, totalCount);
        int toIndex = Math.min(fromIndex + size, totalCount);

        List<Artikal> paginated = sortirani.subList(fromIndex, toIndex);

        // 5. Upis u response
        response.setProducts(paginated);
        response.setManufacturerCounts(izracunajManufacturerCounts(filtriraniPoCeni));

        response.setTotalCount(totalCount);
        response.setMinCena(min);
        response.setMaxCena(max);

        return ResponseEntity.ok(response);
    }
    // ===============================================
    // HOMEPAGE ITEMS API
    // ===============================================

    @PostMapping("/{vendorId}/homepage-items")
    public ResponseEntity<com.tehno.tehnozonaspring.model.HomepageItem> addHomepageItem(
            @PathVariable Long vendorId,
            @RequestBody com.tehno.tehnozonaspring.dto.HomepageItemRequest request) {

        try {
            com.tehno.tehnozonaspring.model.HomepageItem item = vendorService.addHomepageItem(vendorId, request);
            return ResponseEntity.ok(item);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/homepage-items/all")
    public ResponseEntity<List<com.tehno.tehnozonaspring.dto.HomepageItemResponse>> getAllHomepageItems(
            @RequestParam(required = false, defaultValue = "1") Long vendorId) {

        try {
            List<com.tehno.tehnozonaspring.dto.HomepageItemResponse> items = vendorService
                    .getActiveHomepageItems(vendorId);
            System.out.println("CONTROLLER: Šaljem " + items.size() + " homepage stavki frontu.");
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

}
