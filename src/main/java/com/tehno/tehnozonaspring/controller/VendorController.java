package com.tehno.tehnozonaspring.controller;

import com.tehno.tehnozonaspring.model.Artikal;
import com.tehno.tehnozonaspring.model.Vendor;
import com.tehno.tehnozonaspring.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
    public ResponseEntity<List<Artikal>> getArtikliByGlavnaGrupa(
            @PathVariable Long id,
            @PathVariable String glavnaGrupa
    ) {
        List<Artikal> artikli = vendorService.getArtikliByGlavnaGrupa(id, glavnaGrupa);
        if (artikli.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(artikli);
    }

    @GetMapping("/{vendorId}/glavnaGrupa/{glavnaGrupa}/nadgrupa/{nadgrupa}/artikli")
    public ResponseEntity<List<Artikal>> getArtikliByGlavnaGrupaAndNadgrupa(
            @PathVariable Long vendorId,
            @PathVariable String glavnaGrupa,
            @PathVariable String nadgrupa,
            @RequestParam(required = false) Integer minCena,
            @RequestParam(required = false) Integer maxCena) {
        List<Artikal> artikli = null;
        try {
            artikli = vendorService.getArtikliByGlavnaGrupaAndNadgrupa(vendorId, glavnaGrupa, nadgrupa, minCena, maxCena);
            return ResponseEntity.ok(artikli);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(artikli);
        }
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
            @RequestParam(required = false) Integer maxCena) {
        Map<String, Integer> result = vendorService.getProizvodjaciWithCountByGlavnaGrupa(vendorId, glavnaGrupa, minCena, maxCena);
        return result.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(result);
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

}
