package com.tehno.tehnozonaspring.controller;

import com.tehno.tehnozonaspring.dto.ProductPageResponse;
import com.tehno.tehnozonaspring.model.Artikal;
import com.tehno.tehnozonaspring.model.Vendor;
import com.tehno.tehnozonaspring.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
            @RequestParam int limit
    ) {
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
            @PathVariable String nadgrupa
    ) {
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
            @RequestParam(required = false) Integer minCena,
            @RequestParam(required = false) Integer maxCena,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> proizvodjaci
    ) {
        List<Artikal> sviArtikliBezFiltera = vendorService.getArtikliByGlavnaGrupa(id, glavnaGrupa, null, null, 0, Integer.MAX_VALUE, null);

        List<Artikal> filtrirani = filtrirajPoProizvodjacima(sviArtikliBezFiltera, proizvodjaci);



        double min = filtrirani.stream().mapToDouble(Artikal::getB2bcena).min().orElse(0.0);
        double max = filtrirani.stream().mapToDouble(Artikal::getB2bcena).max().orElse(0.0);

//        List<Artikal> sviArtikli = vendorService.getArtikliByGlavnaGrupa(id, glavnaGrupa, minCena, maxCena, page, size, proizvodjaci);
        int totalCount = filtrirani.size();

        int fromIndex = Math.min(page * size, totalCount);
        int toIndex = Math.min(fromIndex + size, totalCount);
        List<Artikal> paginated = filtrirani.subList(fromIndex, toIndex);

        ProductPageResponse response = new ProductPageResponse();
        response.setProducts(paginated);
        response.setTotalCount(totalCount);
        response.setMinCena(min);
        response.setMaxCena(max);

        return ResponseEntity.ok(response);
    }

    public static List<Artikal> filtrirajPoProizvodjacima(List<Artikal> artikli, List<String> proizvodjaci) {
        if(proizvodjaci == null || proizvodjaci.isEmpty()) {
            return artikli;
        }
//        proizvodjaci.add("Ugreen");
        // Pretvaramo u Set radi bržeg pretraživanja (O(1) lookup)
        Set<String> proizvodjaciSet = proizvodjaci.stream()
                .filter(Objects::nonNull)
                .map(p -> p.trim().toLowerCase()) // case-insensitive + trim
                .collect(Collectors.toSet());

        artikli.stream()
                .forEach(a -> System.out.println("Proizvodjac: '" + a.getProizvodjac() + "'"));
        System.out.println("Proizvodjaci set: " + proizvodjaciSet);


        List<Artikal> artikli1 =  artikli.stream()
                .filter(a -> a.getProizvodjac() != null
                        && proizvodjaciSet.contains(a.getProizvodjac().toLowerCase()))
                .collect(Collectors.toList());
         return artikli1;
    }




    @GetMapping("/{vendorId}/glavnaGrupa/{glavnaGrupa}/nadgrupa/{nadgrupa}/artikli")
    public ResponseEntity<List<Artikal>> getArtikliByGlavnaGrupaAndNadgrupa(
            @PathVariable Long vendorId,
            @PathVariable String glavnaGrupa,
            @PathVariable String nadgrupa,
            @RequestParam(required = false) Integer minCena,
            @RequestParam(required = false) Integer maxCena,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) List<String> proizvodjaci // NOVO
    ) {
        List<Artikal> artikli = vendorService.getArtikliByGlavnaGrupaAndNadgrupa(
                vendorId, glavnaGrupa, nadgrupa, minCena, maxCena, page, size, proizvodjaci
        );

        double min = artikli.stream().mapToDouble(Artikal::getB2bcena).min().orElse(0.0);
        double max = artikli.stream().mapToDouble(Artikal::getB2bcena).max().orElse(0.0);

        int totalCount = artikli.size();
        int fromIndex = Math.min(page * size, totalCount);
        int toIndex = Math.min(fromIndex + size, totalCount);
        List<Artikal> paginated = artikli.subList(fromIndex, toIndex);

        ProductPageResponse response = new ProductPageResponse();
        response.setProducts(paginated);
        response.setTotalCount(totalCount);
        response.setMinCena(min);
        response.setMaxCena(max);

        return paginated.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(paginated);
    }


    @GetMapping("/{vendorId}/glavne_grupe_i_nadgrupe")
    public ResponseEntity<Map<String, List<String>>> getAllGroupsAndSubgroups(@PathVariable Long vendorId) {
        Map<String, List<String>> groups = vendorService.getAllGroupsAndSubgroups();
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/{vendorId}/nadgrupe_sa_svojim_grupama/{glavnaGrupa}")
    public ResponseEntity<Map<String, List<String>>> vratiSveNadgrupeSaNjihovimGrupama(@PathVariable Long vendorId, @PathVariable String glavnaGrupa) {
        Map<String, List<String>> groups = vendorService.vratiSveNadgrupeSaNjihovimGrupama(glavnaGrupa);
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/{vendorid}/svi_idjevi")
    public ResponseEntity<List<Long>> vratiSveIdjeve(){
        List<Long> idjevi = vendorService.vratiSveIdjeve();
        return ResponseEntity.ok(idjevi);
    }

    @GetMapping("/{vendorId}/glavnaGrupa/{glavnaGrupa}/proizvodjaci")
    public ResponseEntity<List<String>> getProizvodjaciByGlavnaGrupa(
            @PathVariable Long vendorId,
            @PathVariable String glavnaGrupa
    ) {
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
            @RequestParam(required = false) Integer minCena,
            @RequestParam(required = false) Integer maxCena,
            @RequestParam(required = false) List<String> nadgrupe) {
        // VL : 28.09.2025.
        List<Artikal> artikals = vendorService.getArtikliByGlavnaGrupa(vendorId, glavnaGrupa, minCena, maxCena, 0, 0, null);
        Map<String, Integer> rezultat = izvuciProizvodjaceIzListeArtikala(artikals, nadgrupe);
        return rezultat.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(rezultat);
//        Map<String, Integer> result = vendorService.getProizvodjaciWithCountByGlavnaGrupa(vendorId, glavnaGrupa, minCena, maxCena);
//        return result.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(result);
    }

    public static Map<String, Integer> izvuciProizvodjaceIzListeArtikala(List<Artikal> artikals, List<String> nadgrupe) {
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
                        TreeMap::new
                ));
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

    @GetMapping("/{vendorId}/grupa/{grupa}/artikli")
    public ResponseEntity<List<Artikal>> getArtikliByGrupa(
            @PathVariable Long vendorId,
            @PathVariable String grupa,
            @RequestParam(required = false) Integer minCena,
            @RequestParam(required = false) Integer maxCena) {

        List<Artikal> artikli = vendorService.getArtikliByGrupa(vendorId, grupa, minCena, maxCena);

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
            @RequestParam(required = false) Integer minCena,
            @RequestParam(required = false) Integer maxCena) {

        Map<String, Integer> result = vendorService.getProizvodjaciWithCountByGlavnaGrupaAndNadgrupa(vendorId, glavnaGrupa, nadgrupa, minCena, maxCena);
        return result.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(result);
    }

    @GetMapping("/{vendorId}/artikli/search")
    public ResponseEntity<List<Artikal>> searchArtikliByNazivOrProizvodjac(
            @PathVariable Long vendorId,
            @RequestParam String query
    ) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<Artikal> rezultati = vendorService.searchArtikliByNazivOrProizvodjac(vendorId, query.trim());

        if (rezultati == null || rezultati.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(rezultati);
    }


}
