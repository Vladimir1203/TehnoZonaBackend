package com.tehno.tehnozonaspring.controller;

import com.tehno.tehnozonaspring.dto.ListResponseDTO;
import com.tehno.tehnozonaspring.model.Artikal;
import com.tehno.tehnozonaspring.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vendors")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/{vendorId}/search")
    public ResponseEntity<ListResponseDTO<Artikal>> search(
            @PathVariable Long vendorId,
            @RequestParam(required = true) String q,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String minCena,
            @RequestParam(required = false) String maxCena,
            @RequestParam(name = "proizvodjaci", required = false) String[] proizvodjaciRepeated,
            @RequestParam(name = "proizvodjaciCsv", required = false) String proizvodjaciCsv,
            @RequestParam(required = false) String sort) {
        ListResponseDTO<Artikal> result = searchService.search(
                vendorId, q,
                page, size,
                minCena, maxCena,
                proizvodjaciRepeated, proizvodjaciCsv,
                sort);
        return ResponseEntity.ok(result);
    }
}
