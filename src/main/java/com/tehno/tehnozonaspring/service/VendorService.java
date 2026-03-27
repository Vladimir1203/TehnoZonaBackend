package com.tehno.tehnozonaspring.service;

import java.util.Arrays;

import com.tehno.tehnozonaspring.controller.VendorController;
import com.tehno.tehnozonaspring.dto.FeaturedArtikalResponse;
import com.tehno.tehnozonaspring.dto.ProductPageResponse;
import com.tehno.tehnozonaspring.model.FeatureType;
import com.tehno.tehnozonaspring.model.FeaturedProduct;
import com.tehno.tehnozonaspring.repository.ArtikalQueryRepository;
import com.tehno.tehnozonaspring.repository.FeaturedProductRepository;
import jakarta.annotation.PostConstruct;

import com.tehno.tehnozonaspring.model.Artikal;
import com.tehno.tehnozonaspring.model.Vendor;
import com.tehno.tehnozonaspring.dto.NadgrupaDTO;
import com.tehno.tehnozonaspring.repository.VendorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VendorService {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Belgrade"));
        System.out.println(
                "HOMEPAGE: Vremenska zona aplikacije postavljena na Europe/Belgrade. Sat: " + LocalDateTime.now());
    }

    private final VendorRepository vendorRepository;
    private final ArtikalQueryRepository artikalRepository;
    private final FeaturedProductRepository featuredProductRepository;
    private final com.tehno.tehnozonaspring.repository.HomepageItemRepository homepageItemRepository;
    private final CategoryImageService categoryImageService;
    private final Map<String, String> imagePathCache = new java.util.concurrent.ConcurrentHashMap<>();

    private final Map<String, List<String>> groupMap;

    {
        groupMap = new java.util.LinkedHashMap<>();
        groupMap.put("BELA TEHNIKA I KUĆNI APARATI", List.of(
                "BELA TEHNIKA", "MALI KUĆNI APARATI", "GREJANJE", "HLADNJACI", "KUĆNA BELA TEHNIKA",
                "MALI KUHINJSKI APARATI", "KLIMATIZACIJA I GREJANJE", "ASPIRATORI"));
        groupMap.put("TV, FOTO, AUDIO I VIDEO", List.of(
                "AUDIO, HI-FI", "TV, AUDIO, VIDEO", "FOTOAPARATI I KAMERE", "DIGITALNI SNIMAČI",
                "PROJEKTORI I OPREMA", "ZVUČNICI", "SLUŠALICE I MIKROFONI", "KAMERE", "TELEVIZORI", "COMMERCIAL TV",
                "HOTEL TV", "AUDIO-VIDEO", "SMART TV BOX"));
        groupMap.put("RAČUNARI, KOMPONENTE I GAMING", List.of(
                "LAPTOP I TABLET RAČUNARI", "DESKTOP RAČUNARI", "SERVERI", "PROCESORI",
                "MATIČNE PLOČE", "MEMORIJE", "HARD DISKOVI", "HDD RACK", "GRAFIČKE KARTE",
                "GAMING", "RAČUNARI", "RAČUNARSKE KOMPONENTE", "RAČUNARSKE PERIFERIJE",
                "PC KOZMETIKA", "SOFTWARE", "MICROSOFT", "WIRELESS", "OPTIČKI UREĐAJI", "ČITAČI KARTICA",
                "REKOVI I OPREMA", "TASTATURE", "FIBER", "SSD DISKOVI", "HDD DISKOVI", "MONITORI", "PC DODACI",
                "GAMING DODACI", "KONZOLE ZA IGRANJE", "KONZOLE I DODATNA OPREMA"));
        groupMap.put("TELEFONI, TABLETI I OPREMA", List.of(
                "MOBILNI I FIKSNI TELEFONI", "OPREMA ZA MOBILNE TELEFONE", "OPREMA ZA LAPTOPOVE",
                "OPREMA ZA TABLETE", "OPREMA ZA TV", "MEMORIJSKE KARTICE I ČITAČI",
                "USB FLASH I HDD", "USB KABLOVI", "USB ADAPTERI", "MREŽNA OPREMA", "FIKSNI TELEFONI", "USB FLASH",
                "MEMORIJSKA KARTICA", "MOBILE DODACI", "PAMETNI UREĐAJI", "EXTERNI SSD"));
        groupMap.put("SIGURNOSNI I ALARMNI SISTEMI", List.of(
                "ALARMNI SISTEMI", "ALARMNI SISTEM PARADOX", "ALARMNI SISTEM ELDES",
                "VIDEO NADZOR I SIGURNOSNA OPREMA", "OPREMA ZA VIDEO NADZOR", "KUTIJE",
                "KANALICE", "UTIČNICE", "KONEKTORI I MODULI", "VIDEO NADZOR I  SIGURNOSNA OPREMA", "VIDEO NADZOR"));
        groupMap.put("KANCELARIJSKI I ŠKOLSKI MATERIJAL", List.of(
                "KANCELARIJSKI MATERIJAL", "ŠKOLSKI PRIBOR", "ŠTAMPAČI", "TONERI",
                "KERTRIDŽ", "RIBONI", "MASTILA", "CD, DVD MEDIJI", "SKENERI I FOTOKOPIRI", "POTROŠNI MATERIJAL",
                "ŠTAMPAČ", "MULTIFUNKCIJSKI ŠTAMPAČ", "SKENER", "KOPIR", "BUBANJ", "REZERVNI DEO", "PR"));
        groupMap.put("BATERIJE, PUNJAČI I KABLOVI", List.of(
                "BATERIJE I PUNJAČI", "KABLOVI", "KABLOVI I ADAPTERI", "PCI ADAPTERI", "PC KABLOVI", "ADAPTERI",
                "DODATNA OPREMA"));
        groupMap.put("ALATI I OPREMA ZA DOM", List.of(
                "ALAT I BAŠTA", "BAŠTA", "LED RASVETA", "SVE ZA KUĆU", "BAŠTA I ALATI", "POSUĐE"));
        groupMap.put("FITNESS I SPORT", List.of(
                "BICIKLE I FITNES", "NEGA LICA I TELA", "LEPOTA I ZDRAVLJE"));
        groupMap.put("OSTALO I OUTLET", List.of(
                "OUTLET", "RAZNO", "REZERVNI DEO", "DODATNA", "POS OPREMA", "OSTALO"));
    }

    private static final List<String> GLAVNI_PROIZVODJACI = List.of(
            "BEKO", "BOSCH", "GORENJE", "HISENSE",
            "HUAWEI", "LG", "MIDEA", "PHILIPS", "SAMSUNG", "XIAOMI");

    public List<String> getNadgrupeByGlavnaGrupa(String glavnaGrupa) {
        return getFullMenuStructure().stream()
                .filter(m -> m.get("name").toString().equalsIgnoreCase(glavnaGrupa))
                .findFirst()
                .map(m -> new ArrayList<>(((Map<String, List<String>>) m.get("nadgrupe")).keySet()))
                .orElse(new ArrayList<>());
    }

    public List<NadgrupaDTO> getNadgrupeWithImages(String glavnaGrupa) {
        List<String> names = getNadgrupeByGlavnaGrupa(glavnaGrupa);

        Map<String, List<String>> structure = getFullMenuStructure().stream()
                .filter(m -> m.get("name").toString().equalsIgnoreCase(glavnaGrupa))
                .findFirst()
                .map(m -> (Map<String, List<String>>) m.get("nadgrupe"))
                .orElse(new HashMap<>());

        return names.stream()
                .map(name -> {
                    String cacheKey = name.toUpperCase();
                    String localImg = imagePathCache.get(cacheKey);

                    if (localImg == null) {
                        localImg = categoryImageService.getCachedImageUrl(name);

                        if (localImg == null) {
                            String sampleUrl = artikalRepository.findSampleImageForNadgrupa(name);
                            if (sampleUrl != null) {
                                localImg = categoryImageService.getOrDownloadImage(name, sampleUrl);
                            }
                        }

                        if (localImg != null) {
                            imagePathCache.put(cacheKey, localImg);
                        }
                    }

                    List<String> grupe = structure.get(cacheKey);
                    return new NadgrupaDTO(name, localImg, grupe);
                })
                .collect(Collectors.toList());
    }

    public String[] getNadgrupeByGlavnaGrupaArray(String glavnaGrupa) {
        return getNadgrupeByGlavnaGrupa(glavnaGrupa).toArray(new String[0]);
    }

    @Autowired
    public VendorService(VendorRepository vendorRepository,
            ArtikalQueryRepository artikalRepository,
            FeaturedProductRepository featuredProductRepository,
            com.tehno.tehnozonaspring.repository.HomepageItemRepository homepageItemRepository,
            CategoryImageService categoryImageService) {
        this.vendorRepository = vendorRepository;
        this.artikalRepository = artikalRepository;
        this.featuredProductRepository = featuredProductRepository;
        this.homepageItemRepository = homepageItemRepository;
        this.categoryImageService = categoryImageService;
    }

    public List<Vendor> getAllBeans() {
        return vendorRepository.findAll();
    }

    public String findArtikliNaziviByGrupa(Long id) {
        return vendorRepository.findArtikliNaziviByGrupa(id);
    }

    public String getDistinctNadgrupe(Long id) {
        return vendorRepository.findDistinctNadgrupeById(id);
    }

    public String getGrupeByNadgrupa(Long id, String nadgrupa) {
        return vendorRepository.findDistinctGroupsByNadgrupa(id, nadgrupa);
    }

    public List<Artikal> getArtikli(Long id, int limit) {
        if (id == 0) {
            return artikalRepository.findAll().stream().limit(limit).collect(Collectors.toList());
        }
        return artikalRepository.findByVendorIdLimited(id, limit);
    }

    public List<Map<String, Object>> getFullMenuStructure() {
        List<Object[]> rows = artikalRepository.findDistinctNadgrupeAndGrupe();
        Map<String, Map<String, List<String>>> tree = new LinkedHashMap<>();
        for (String glavna : groupMap.keySet()) {
            tree.put(glavna, new LinkedHashMap<>());
        }
        for (Object[] row : rows) {
            String nadgrupa = (row[0] != null) ? row[0].toString().trim().toUpperCase() : "";
            String grupa = (row[1] != null) ? row[1].toString().trim() : "";
            if (nadgrupa.isEmpty())
                continue;
            String parentGlavna = null;
            for (Map.Entry<String, List<String>> entry : groupMap.entrySet()) {
                if (entry.getValue().stream().anyMatch(s -> s.equalsIgnoreCase(nadgrupa))) {
                    parentGlavna = entry.getKey();
                    break;
                }
            }
            if (parentGlavna != null) {
                tree.get(parentGlavna)
                        .computeIfAbsent(nadgrupa, k -> new ArrayList<>())
                        .add(grupa);
                Collections.sort(tree.get(parentGlavna).get(nadgrupa));
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<String>>> entry : tree.entrySet()) {
            if (entry.getValue().isEmpty())
                continue;
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("name", entry.getKey());
            Map<String, List<String>> sortedNadgrupe = new TreeMap<>((a, b) -> a.compareToIgnoreCase(b));
            sortedNadgrupe.putAll(entry.getValue());
            node.put("nadgrupe", sortedNadgrupe);
            result.add(node);
        }
        return result;
    }

    public List<String> getGlavneGrupe() {
        List<String> groups = getFullMenuStructure().stream().map(m -> m.get("name").toString())
                .collect(Collectors.toList());
        System.out.println("HOMEPAGE: Povučene glavne grupe: " + groups);
        return groups;
    }

    public List<Artikal> getArtikliByNadgrupa(Long id, String nadgrupa) {
        if (id == 0) {
            return artikalRepository.findByNadgrupa(nadgrupa.trim().toUpperCase(), null, null);
        }
        return artikalRepository.findByVendorAndNadgrupa(id, nadgrupa.trim().toUpperCase(), null, null);
    }

    public List<String> getAllGroups() {
        return vendorRepository.findAllGroups();
    }

    public List<Artikal> vratiArtiklePoGlavnojGrupiICeni(Long vendorId, String glavnaGrupa, Double minCena,
            Double maxCena, int page, int size, List<String> proizvodjaci, ProductPageResponse response) {
        String[] nadgrupe = getNadgrupeByGlavnaGrupaArray(glavnaGrupa);
        List<Artikal> artikli;
        if (vendorId == 0) {
            artikli = artikalRepository.findByGlavnaGrupa(nadgrupe, minCena, maxCena);
        } else {
            artikli = artikalRepository.findByVendorAndGlavnaGrupa(vendorId, nadgrupe, minCena, maxCena);
        }

        double globalMin = artikli.stream().mapToDouble(Artikal::getCena).filter(c -> c > 0).min().orElse(0);
        double globalMax = artikli.stream().mapToDouble(Artikal::getCena).max().orElse(0);

        response.setInitialMinCena(globalMin);
        response.setInitialMaxCena(globalMax);

        return artikli;
    }

    public List<Artikal> vratiArtiklePoNadgrupi(Long vendorId, String nadgrupa, Double minCena, Double maxCena,
            int page, int size, List<String> proizvodjaci, ProductPageResponse response) {
        List<Artikal> artikli;
        if (vendorId == 0) {
            artikli = artikalRepository.findByNadgrupa(nadgrupa, minCena, maxCena);
        } else {
            artikli = artikalRepository.findByVendorAndNadgrupa(vendorId, nadgrupa.trim().toUpperCase(), minCena, maxCena);
        }

        double globalMin = artikli.stream().mapToDouble(Artikal::getCena).filter(c -> c > 0).min().orElse(0);
        double globalMax = artikli.stream().mapToDouble(Artikal::getCena).max().orElse(0);

        response.setInitialMinCena(globalMin);
        response.setInitialMaxCena(globalMax);

        return artikli;
    }

    private List<Artikal> filtrirajPoProizvodjacima(List<Artikal> artikli, List<String> proizvodjaci) {
        if (proizvodjaci == null || proizvodjaci.isEmpty()) {
            return artikli;
        }
        Set<String> set = proizvodjaci.stream()
                .filter(Objects::nonNull)
                .map(p -> p.trim().toUpperCase())
                .collect(Collectors.toSet());
        return artikli.stream()
                .filter(a -> a.getProizvodjac() != null && set.contains(a.getProizvodjac().trim().toUpperCase()))
                .collect(Collectors.toList());
    }

    public Map<String, List<String>> getAllGroupsAndSubgroups() {
        return groupMap;
    }

    public Map<String, List<String>> vratiSveNadgrupeSaNjihovimGrupama(String glavnaGrupa) {
        List<String> nadgrupe = groupMap.getOrDefault(glavnaGrupa.toUpperCase(), List.of());
        Map<String, List<String>> result = new HashMap<>();
        for (String nadgrupa : nadgrupe) {
            String grupe = getGrupeByNadgrupa(1L, nadgrupa);
            if (grupe != null && !grupe.isEmpty()) {
                result.put(nadgrupa, Arrays.asList(grupe.split(",")));
            }
        }
        return result;
    }

    public List<Long> vratiSveIdjeve() {
        return vendorRepository.vratiSveIdjeve();
    }

    public List<String> getProizvodjaciByGlavnaGrupa(Long vendorId, String glavnaGrupa) {
        String[] nadgrupe = getNadgrupeByGlavnaGrupaArray(glavnaGrupa);
        if (nadgrupe.length == 0) return Collections.emptyList();

        List<String> proizvodjaci;
        if (vendorId == 0) {
            proizvodjaci = artikalRepository.findDistinctProizvodjaciByGlavnaGrupa(nadgrupe);
        } else {
            proizvodjaci = vendorRepository.findProizvodjaciByGlavnaGrupa(vendorId, nadgrupe);
        }

        return proizvodjaci.stream()
                .map(String::toUpperCase)
                .filter(name -> !(name.equals("/") || name.equals("-")))
                .distinct()
                .sorted()
                .toList();
    }

    public Map<String, Integer> getProizvodjaciWithCountByGlavnaGrupa(Long vendorId, String glavnaGrupa,
            Integer minCena, Integer maxCena) {
        System.out.println("==== POČETAK getProizvodjaciWithCountByGlavnaGrupa ====");
        System.out.println("Vendor ID: " + vendorId + ", Glavna grupa: " + glavnaGrupa);

        String[] nadgrupe = getNadgrupeByGlavnaGrupaArray(glavnaGrupa);
        BigDecimal min = minCena != null ? BigDecimal.valueOf(minCena) : null;
        BigDecimal max = maxCena != null ? BigDecimal.valueOf(maxCena) : null;

        List<Object[]> resultList;
        if (vendorId == 0) {
            resultList = artikalRepository.countProizvodjaciByGlavnaGrupa(nadgrupe, min, max);
        } else {
            resultList = vendorRepository.findProizvodjaciWithCountByGlavnaGrupa(vendorId, nadgrupe, minCena, maxCena);
        }

        System.out.println("Broj rezultata iz repository-a: " + resultList.size());

        return resultList.stream()
                .filter(arr -> arr[0] != null && !arr[0].equals("/") && !arr[0].equals("-"))
                .collect(Collectors.toMap(
                        arr -> arr[0].toString(),
                        arr -> Integer.parseInt(arr[1].toString()),
                        (oldValue, newValue) -> oldValue,
                        TreeMap::new));
    }

    public BigDecimal getMaxPriceByVendorId(Long vendorId) {
        if (vendorId == 0) {
            return artikalRepository.findMaxPrice();
        }
        return artikalRepository.findMaxPriceByVendor(vendorId);
    }

    public List<String> getAllDistinctProizvodjaci() {
        return artikalRepository.findDistinctProizvodjaci();
    }

    public List<String> getAllMainProizvodjaci() {
        return GLAVNI_PROIZVODJACI;
    }

    public List<Artikal> getArtikliByProizvodjac(String proizvodjac) {
        return artikalRepository.findByBrand(proizvodjac);
    }

    public List<Artikal> getArtikliByGrupa(Long vendorId, String nadgrupa, String grupa, Double minCena,
            Double maxCena, com.tehno.tehnozonaspring.dto.ProductPageResponse response) {
        List<Artikal> artikli;
        if (vendorId == 0) {
            artikli = artikalRepository.findByNadgrupaAndGrupa(nadgrupa.trim().toUpperCase(),
                    grupa.trim().toUpperCase(), minCena, maxCena);
        } else {
            artikli = artikalRepository.findByVendorAndNadgrupaAndGrupa(vendorId,
                    nadgrupa.trim().toUpperCase(), grupa.trim().toUpperCase(), minCena, maxCena);
        }

        double globalMin = artikli.stream().mapToDouble(Artikal::getCena).filter(c -> c > 0).min().orElse(0);
        double globalMax = artikli.stream().mapToDouble(Artikal::getCena).max().orElse(0);
        response.setInitialMinCena(globalMin);
        response.setInitialMaxCena(globalMax);

        return artikli;
    }

    public Map<String, Integer> getProizvodjaciWithCountByGlavnaGrupaAndNadgrupa(Long vendorId, String glavnaGrupa,
            String nadgrupa, Double minCena, Double maxCena) {
        System.out.println("==== POČETAK getProizvodjaciWithCountByGlavnaGrupaAndNadgrupa ====");
        System.out.println("Vendor ID: " + vendorId + ", Nadgrupa: " + nadgrupa);

        BigDecimal min = minCena != null ? BigDecimal.valueOf(minCena) : null;
        BigDecimal max = maxCena != null ? BigDecimal.valueOf(maxCena) : null;

        List<Object[]> resultList;
        if (vendorId == 0) {
            resultList = artikalRepository.countProizvodjaciByNadgrupa(nadgrupa, min, max);
        } else {
            resultList = vendorRepository.findProizvodjaciWithCountByGlavnaGrupaAndNadgrupa(vendorId, nadgrupa);
        }

        // Vendor-specific metoda vraca cene kao array — filtriramo u Javi
        if (vendorId != 0) {
            Map<String, Integer> rezultat = new TreeMap<>();
            for (Object[] row : resultList) {
                String proizvodjac = row[0].toString();
                List<BigDecimal> cene = Arrays.asList((BigDecimal[]) row[2]);
                long filtriraniBroj = cene.stream()
                        .filter(c -> (minCena == null || c.compareTo(BigDecimal.valueOf(minCena)) >= 0))
                        .filter(c -> (maxCena == null || c.compareTo(BigDecimal.valueOf(maxCena)) <= 0))
                        .count();
                if (filtriraniBroj > 0) rezultat.put(proizvodjac, (int) filtriraniBroj);
            }
            return rezultat;
        }

        return resultList.stream()
                .filter(arr -> arr[0] != null && !arr[0].equals("/") && !arr[0].equals("-"))
                .collect(Collectors.toMap(
                        arr -> arr[0].toString(),
                        arr -> Integer.parseInt(arr[1].toString()),
                        (oldValue, newValue) -> oldValue,
                        TreeMap::new));
    }

    public List<Artikal> searchArtikliByNazivOrProizvodjac(Long vendorId, String query) {
        if (vendorId != 0) {
            // Vendor-specific: filter u Javi
            List<Artikal> all = artikalRepository.findByVendorId(vendorId);
            String lower = query.toLowerCase();
            return all.stream()
                    .filter(a -> {
                        String naziv = Optional.ofNullable(a.getNaziv()).orElse("").toLowerCase();
                        String pr = Optional.ofNullable(a.getProizvodjac()).orElse("").toLowerCase();
                        return naziv.contains(lower) || pr.contains(lower);
                    })
                    .collect(Collectors.toList());
        }
        // Unified: pokusaj FTS, fallback na ilike
        List<Artikal> fts = artikalRepository.search(query);
        if (!fts.isEmpty()) return fts;
        return artikalRepository.searchIlike(query);
    }

    public Artikal getProductByArtikalBarCode(Long vendorId, String barCode) {
        if (vendorId == 0) {
            return artikalRepository.findByBarkod(barCode);
        }
        return artikalRepository.findByVendorAndBarkod(vendorId, barCode);
    }

    public List<Artikal> getArtikliByBrand(Long vendorId, String brand) {
        if (vendorId == 0) {
            return artikalRepository.findByBrand(brand.trim().toUpperCase());
        }
        return artikalRepository.findByVendorAndBrand(vendorId, brand.trim().toUpperCase());
    }

    public FeaturedProduct addFeaturedProduct(Long vendorId, String barcode, FeatureType featureType, Integer priority,
            LocalDateTime validFrom, LocalDateTime validTo, String itemType, String subtitle, String buttonText,
            String buttonRoute, String glavnaGrupa, String nadgrupa, String grupa, String brandName, String customName,
            String customImageUrl) {
        if (priority == null) priority = 1;
        if (validFrom == null) validFrom = LocalDateTime.now();
        if (validTo == null) validTo = LocalDateTime.now().plusMonths(1);

        vendorRepository.insertFeaturedProduct(
                barcode, vendorId, featureType.name(), priority,
                validFrom, validTo, itemType, subtitle, buttonText, buttonRoute,
                glavnaGrupa, nadgrupa, grupa, brandName, customName, customImageUrl);

        FeaturedProduct fp = new FeaturedProduct();
        fp.setBarcode(barcode);
        fp.setVendorId(vendorId);
        fp.setFeatureType(featureType);
        fp.setPriority(priority);
        fp.setValidFrom(validFrom);
        fp.setValidTo(validTo);
        fp.setItemType(itemType);
        fp.setSubtitle(subtitle);
        fp.setButtonText(buttonText);
        fp.setButtonRoute(buttonRoute);
        fp.setGlavnaGrupa(glavnaGrupa);
        fp.setNadgrupa(nadgrupa);
        fp.setGrupa(grupa);
        fp.setBrandName(brandName);
        fp.setCustomName(customName);
        fp.setCustomImageUrl(customImageUrl);

        return fp;
    }

    public Map<String, List<Artikal>> getDuplicateProducts() {
        List<String> duplicateBarkodovi = artikalRepository.findDuplicateBarkodovi();
        Map<String, List<Artikal>> duplicatesMap = new HashMap<>();
        for (String barkod : duplicateBarkodovi) {
            duplicatesMap.put(barkod, artikalRepository.findAllByBarkod(barkod));
        }
        return duplicatesMap;
    }

    public List<FeaturedArtikalResponse> getActiveFeaturedArtikli() {
        List<FeaturedProduct> featuredList = featuredProductRepository.getAllActiveFeatured();
        List<FeaturedArtikalResponse> result = new ArrayList<>();
        for (FeaturedProduct fp : featuredList) {
            Artikal artikal = getProductByArtikalBarCode(fp.getVendorId(), fp.getBarcode());
            if (artikal != null) result.add(new FeaturedArtikalResponse(artikal, fp));
        }
        System.out.println("HOMEPAGE: Povučeni svi istaknuti artikli. Broj: " + result.size());
        return result;
    }

    public List<FeaturedArtikalResponse> getActiveFeaturedArtikliByType(FeatureType type) {
        List<FeaturedProduct> featuredList = featuredProductRepository.getActiveFeaturedByType(type.name());
        List<FeaturedArtikalResponse> result = new ArrayList<>();
        for (FeaturedProduct fp : featuredList) {
            Artikal artikal = getProductByArtikalBarCode(fp.getVendorId(), fp.getBarcode());
            if (artikal != null) result.add(new FeaturedArtikalResponse(artikal, fp));
        }
        System.out.println("HOMEPAGE: Povučeni istaknuti artikli tipa " + type + ". Broj: " + result.size());
        return result;
    }

    public List<Map<String, Object>> getCountByGlavnaGrupaForBrand(Long vendorId, String brand, Double minCena,
            Double maxCena) {
        List<Artikal> artikli = getArtikliByBrand(vendorId, brand);
        List<Artikal> filtrirani = VendorController.filtrirajPoCeni(artikli, minCena, maxCena);

        if (filtrirani == null || filtrirani.isEmpty()) return Collections.emptyList();

        Map<String, Integer> counter = new HashMap<>();
        for (Artikal artikal : filtrirani) {
            String nadgrupa = artikal.getNadgrupa();
            String glavnaGrupa = findGlavnaGrupaForNadgrupa(nadgrupa);
            if (glavnaGrupa == null) continue;
            counter.merge(glavnaGrupa, 1, Integer::sum);
        }

        return counter.entrySet().stream()
                .map(e -> Map.<String, Object>of("glavnaGrupa", e.getKey(), "count", e.getValue()))
                .toList();
    }

    private String findGlavnaGrupaForNadgrupa(String nadgrupa) {
        if (nadgrupa == null) return null;
        String n = nadgrupa.trim().toUpperCase();
        for (Map.Entry<String, List<String>> entry : groupMap.entrySet()) {
            for (String ng : entry.getValue()) {
                if (ng.equalsIgnoreCase(n)) return entry.getKey();
            }
        }
        return null;
    }

    public List<Artikal> getArtikliByBrandAndGlavnaGrupa(Long vendorId, String brand, String glavnaGrupa) {
        String[] nadgrupe = getNadgrupeByGlavnaGrupaArray(glavnaGrupa);
        if (vendorId == 0) {
            return artikalRepository.findByBrandAndGlavnaGrupa(brand.trim().toUpperCase(), nadgrupe);
        }
        // Vendor-specific: dohvati po brandu, filtriraj po glavnoj grupi u Javi
        return artikalRepository.findByVendorAndBrand(vendorId, brand.trim().toUpperCase()).stream()
                .filter(a -> glavnaGrupa.equalsIgnoreCase(findGlavnaGrupaForNadgrupa(a.getNadgrupa())))
                .collect(Collectors.toList());
    }

    public com.tehno.tehnozonaspring.model.HomepageItem addHomepageItem(Long vendorId,
            com.tehno.tehnozonaspring.dto.HomepageItemRequest request) {
        com.tehno.tehnozonaspring.model.HomepageItem item = new com.tehno.tehnozonaspring.model.HomepageItem();
        item.setVendorId(vendorId);
        item.setItemType(request.getItemType());
        item.setSection(request.getSection());
        int priority = request.getPriority() != null ? request.getPriority() : 1;
        item.setPriority(priority);

        LocalDateTime from = request.getValidFrom() != null ? request.getValidFrom() : LocalDateTime.now();
        LocalDateTime to = request.getValidTo() != null ? request.getValidTo() : LocalDateTime.now().plusMonths(1);
        item.setValidFrom(from);
        item.setValidTo(to);

        if (request.getSection() == com.tehno.tehnozonaspring.model.enums.HomepageSection.HERO) {
            List<com.tehno.tehnozonaspring.model.HomepageItem> existingHeores = homepageItemRepository
                    .findByVendorIdAndSectionAndValidToAfter(vendorId, request.getSection(), LocalDateTime.now());
            for (com.tehno.tehnozonaspring.model.HomepageItem oldHero : existingHeores) {
                oldHero.setValidTo(LocalDateTime.now());
            }
            homepageItemRepository.saveAll(existingHeores);
        }

        item.setBarcode(request.getBarcode());
        item.setGlavnaGrupa(request.getGlavnaGrupa());
        item.setNadgrupa(request.getNadgrupa());
        item.setGrupa(request.getGrupa());
        item.setBrandName(request.getBrandName());
        item.setCustomName(request.getCustomName());
        item.setCustomImageUrl(request.getCustomImageUrl());
        item.setSubtitle(request.getSubtitle());
        item.setButtonText(request.getButtonText());
        item.setButtonRoute(request.getButtonRoute());

        return homepageItemRepository.save(item);
    }

    public List<com.tehno.tehnozonaspring.dto.HomepageItemResponse> getActiveHomepageItems(Long vendorId) {
        LocalDateTime now = LocalDateTime.now();
        List<com.tehno.tehnozonaspring.model.HomepageItem> activeItems = homepageItemRepository
                .findActiveItemsByVendorId(vendorId, now);

        List<com.tehno.tehnozonaspring.dto.HomepageItemResponse> responses = new ArrayList<>();
        for (com.tehno.tehnozonaspring.model.HomepageItem item : activeItems) {
            Artikal associatedArtikal = null;
            if (item.getItemType() == com.tehno.tehnozonaspring.model.enums.ItemType.PRODUCT
                    && item.getBarcode() != null) {
                associatedArtikal = getProductByArtikalBarCode(vendorId, item.getBarcode());
            }
            responses.add(new com.tehno.tehnozonaspring.dto.HomepageItemResponse(item, associatedArtikal));
        }

        System.out.println("HOMEPAGE: Povučeni homepage items za vendorId=" + vendorId + " (Vreme: " + now + "). Broj: "
                + responses.size());
        return responses;
    }
}
