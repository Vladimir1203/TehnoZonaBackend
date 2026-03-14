package com.tehno.tehnozonaspring.service;

import com.tehno.tehnozonaspring.controller.VendorController;
import com.tehno.tehnozonaspring.dto.FeaturedArtikalResponse;
import com.tehno.tehnozonaspring.dto.ProductPageResponse;
import com.tehno.tehnozonaspring.model.FeatureType;
import com.tehno.tehnozonaspring.model.FeaturedProduct;
import com.tehno.tehnozonaspring.repository.FeaturedProductRepository;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

import com.tehno.tehnozonaspring.model.Artikal;
import com.tehno.tehnozonaspring.model.Vendor;
import com.tehno.tehnozonaspring.repository.VendorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.StringReader;
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
    private final FeaturedProductRepository featuredProductRepository;
    private final com.tehno.tehnozonaspring.repository.HomepageItemRepository homepageItemRepository;

    private final Map<String, List<String>> groupMap = Map.of(
            "BELA TEHNIKA I KUĆNI APARATI", List.of(
                    "BELA TEHNIKA", "MALI KUĆNI APARATI", "GREJANJE", "HLADNJACI", "KUĆNA BELA TEHNIKA",
                    "Mali kuhinjski aparati", "Mali kucni aparati", "Klimatizacija i Grejanje"),
            "TV, FOTO, AUDIO I VIDEO", List.of(
                    "AUDIO, HI-FI", "TV, AUDIO, VIDEO", "FOTOAPARATI I KAMERE", "DIGITALNI SNIMAČI",
                    "PROJEKTORI I OPREMA", "ZVUČNICI", "SLUŠALICE I MIKROFONI", "KAMERE", "Televizori", "Commercial TV",
                    "Hotel TV", "Audio-Video"),
            "RAČUNARI, KOMPONENTE I GAMING", List.of(
                    "LAPTOP I TABLET RAČUNARI", "DESKTOP RAČUNARI", "SERVERI", "PROCESORI",
                    "MATIČNE PLOČE", "MEMORIJE", "HARD DISKOVI", "HDD Rack", "GRAFIČKE KARTE",
                    "GAMING", "RAČUNARI", "RAČUNARSKE KOMPONENTE", "RAČUNARSKE PERIFERIJE",
                    "PC KOZMETIKA", "SOFTWARE", "Microsoft", "WIRELESS", "OPTIČKI UREĐAJI", "Čitači kartica",
                    "REKOVI I OPREMA", "TASTATURE", "FIBER", "SSD Diskovi", "HDD Diskovi", "Monitori", "PC Dodaci",
                    "Gaming Dodaci"),
            "TELEFONI, TABLETI I OPREMA", List.of(
                    "MOBILNI I FIKSNI TELEFONI", "OPREMA ZA MOBILNE TELEFONE", "OPREMA ZA LAPTOPOVE",
                    "OPREMA ZA TABLETE", "OPREMA ZA TV", "MEMORIJSKE KARTICE I ČITAČI",
                    "USB FLASH I HDD", "USB KABLOVI", "USB ADAPTERI", "MREŽNA OPREMA", "FIKSNI TELEFONI", "USB Flash",
                    "Memorijska kartica", "Mobile Dodaci", "Pametni Uredjaji", "Externi SSD"),
            "SIGURNOSNI I ALARMNI SISTEMI", List.of(
                    "ALARMNI SISTEMI", "ALARMNI SISTEM PARADOX", "ALARMNI SISTEM ELDES",
                    "VIDEO NADZOR I SIGURNOSNA OPREMA", "OPREMA ZA VIDEO NADZOR", "KUTIJE",
                    "KANALICE", "UTIČNICE", "KONEKTORI I MODULI", "VIDEO NADZOR I  SIGURNOSNA OPREMA", "Video Nadzor"),
            "ALATI I OPREMA ZA DOM", List.of(
                    "ALAT I BAŠTA", "BAŠTA", "LED RASVETA", "SVE ZA KUĆU", "Bašta i alati", "Posudje"),
            "BATERIJE, PUNJAČI I KABLOVI", List.of(
                    "BATERIJE I PUNJAČI", "KABLOVI", "KABLOVI I ADAPTERI", "PCI ADAPTERI", "PC KABLOVI", "ADAPTERI",
                    "Dodatna oprema"),
            "FITNESS I SPORT", List.of(
                    "BICIKLE I FITNES", "NEGA LICA I TELA", "Lepota i Zdravlje"),
            "KANCELARIJSKI I ŠKOLSKI MATERIJAL", List.of(
                    "KANCELARIJSKI MATERIJAL", "ŠKOLSKI PRIBOR", "ŠTAMPAČI", "TONERI",
                    "KERTRIDŽ", "RIBONI", "MASTILA", "CD, DVD MEDIJI", "SKENERI I FOTOKOPIRI", "Potrošni materijal",
                    "Štampač", "Multifunkcijski štampač", "Skener", "Kopir", "Bubanj"),
            "OSTALO I OUTLET", List.of(
                    "OUTLET", "RAZNO", "Rezervni Deo", "Dodatna", "POS Oprema"));

    private static final List<String> GLAVNI_PROIZVODJACI = List.of(
            "BEKO", "BOSCH", "GORENJE", "HISENSE",
            "HUAWEI", "LG", "MIDEA", "PHILIPS", "SAMSUNG", "XIAOMI");

    public List<String> getNadgrupeByGlavnaGrupa(String glavnaGrupa) {
        return groupMap.getOrDefault(glavnaGrupa.toUpperCase(), List.of());
    }

    public String[] getNadgrupeByGlavnaGrupaArray(String glavnaGrupa) {
        return groupMap.getOrDefault(glavnaGrupa.toUpperCase(), List.of())
                .toArray(new String[0]); // Konvertuje List<String> u String[]
    }

    @Autowired
    public VendorService(VendorRepository vendorRepository,
            FeaturedProductRepository featuredProductRepository,
            com.tehno.tehnozonaspring.repository.HomepageItemRepository homepageItemRepository) {
        this.vendorRepository = vendorRepository;
        this.featuredProductRepository = featuredProductRepository;
        this.homepageItemRepository = homepageItemRepository;
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
        List<String> artikalXmlList = vendorRepository.findLimitedArtikliByVendorId(id, limit);
        List<Artikal> artikli = new ArrayList<>();

        try {
            JAXBContext context = JAXBContext.newInstance(Artikal.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            for (String artikalXml : artikalXmlList) {
                StringReader reader = new StringReader(artikalXml);
                Artikal artikal = (Artikal) unmarshaller.unmarshal(reader);
                if (artikal.getCena() >= 100) {
                    artikli.add(artikal);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return artikli;
    }

    public List<String> getGlavneGrupe() {
        List<String> groups = new ArrayList<>(groupMap.keySet());
        System.out.println("HOMEPAGE: Povučene glavne grupe: " + groups);
        return groups;
    }

    public List<Artikal> getArtikliByNadgrupa(Long id, String nadgrupa) {
        List<String> artikalXmlList = vendorRepository.findArtikliByNadgrupa(id, nadgrupa);
        List<Artikal> artikli = new ArrayList<>();

        try {
            JAXBContext context = JAXBContext.newInstance(Artikal.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            for (String artikalXml : artikalXmlList) {
                StringReader reader = new StringReader(artikalXml);
                Artikal artikal = (Artikal) unmarshaller.unmarshal(reader);
                if (artikal.getCena() >= 100) {
                    artikli.add(artikal);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return artikli;
    }

    public List<String> getAllGroups() {
        return vendorRepository.findAllGroups();
    }

    public List<Artikal> vratiArtiklePoGlavnojGrupiICeni(Long vendorId, String glavnaGrupa, Double minCena,
            Double maxCena,
            int page, int size, List<String> proizvodjaci, ProductPageResponse response) {
        String[] nadgrupe = getNadgrupeByGlavnaGrupaArray(glavnaGrupa);
        List<String> artikalXmlList;
        if (vendorId == 0) {
            artikalXmlList = vendorRepository.findUnifiedArtikliByGlavnaGrupa(nadgrupe);
        } else {
            artikalXmlList = vendorRepository.findArtikliByGlavnaGrupa(vendorId, nadgrupe);
        }
        List<Artikal> artikli = new ArrayList<>();

        double globalMin = Double.MAX_VALUE;
        double globalMax = Double.MIN_VALUE;

        try {
            JAXBContext context = JAXBContext.newInstance(Artikal.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            for (String artikalXml : artikalXmlList) {
                StringReader reader = new StringReader(artikalXml);
                Artikal artikal = (Artikal) unmarshaller.unmarshal(reader);

                double cena = artikal.getCena();
                if (cena < 100)
                    continue; // preskoci artikle sa cenom ispod 100

                if (cena < globalMin) {
                    globalMin = cena;
                }
                if (cena > globalMax) {
                    globalMax = cena;
                }

                if ((minCena == null || minCena == 0 || cena >= minCena) &&
                        (maxCena == null || maxCena == 0 || cena <= maxCena)) {
                    artikli.add(artikal);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Greška prilikom parsiranja artikala", e);
        }

        if (globalMin == Double.MAX_VALUE)
            globalMin = 0;
        if (globalMax == Double.MIN_VALUE)
            globalMax = 0;

        response.setInitialMaxCena(globalMax);
        response.setInitialMinCena(globalMin);

        return artikli;
    }

    public List<Artikal> vratiArtiklePoNadgrupi(Long vendorId, String nadgrupa, Double minCena, Double maxCena,
            int page, int size, List<String> proizvodjaci, ProductPageResponse response) {
        List<String> artikliPoNadgrupi;
        if (vendorId == 0) {
            artikliPoNadgrupi = vendorRepository.findUnifiedArtikliByNadgrupa(nadgrupa);
        } else {
            artikliPoNadgrupi = vendorRepository.findArtikliByNadgrupaAndVendorId(vendorId, nadgrupa);
        }

        List<Artikal> artikliPoNadgrupiIceni = new ArrayList<>();

        double globalMin = Double.MAX_VALUE;
        double globalMax = Double.MIN_VALUE;

        try {
            JAXBContext context = JAXBContext.newInstance(Artikal.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            for (String artikalXml : artikliPoNadgrupi) {
                StringReader reader = new StringReader(artikalXml);
                Artikal artikal = (Artikal) unmarshaller.unmarshal(reader);

                double cena = artikal.getCena();
                if (cena < 100)
                    continue; // preskoci artikle sa cenom ispod 100

                if (cena < globalMin) {
                    globalMin = cena;
                }
                if (cena > globalMax) {
                    globalMax = cena;
                }

                if ((minCena == null || cena >= minCena) &&
                        (maxCena == null || cena <= maxCena)) {
                    artikliPoNadgrupiIceni.add(artikal);
                }
            }

            if (globalMin == Double.MAX_VALUE)
                globalMin = 0;
            if (globalMax == Double.MIN_VALUE)
                globalMax = 0;

            response.setInitialMaxCena(globalMax);
            response.setInitialMinCena(globalMin);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Greška prilikom parsiranja artikala", e);
        }

        return filtrirajPoProizvodjacima(artikliPoNadgrupiIceni, proizvodjaci);

    }

    private List<Artikal> filtrirajPoProizvodjacima(List<Artikal> artikli, List<String> proizvodjaci) {
        if (proizvodjaci == null || proizvodjaci.isEmpty()) {
            return artikli; // nema filtera, vrati sve
        }

        Set<String> proizvodjaciSet = proizvodjaci.stream()
                .filter(Objects::nonNull)
                .map(p -> p.trim().toUpperCase()) // case-insensitive upoređivanje
                .collect(Collectors.toSet());

        return artikli.stream()
                .filter(a -> a.getProizvodjac() != null
                        && proizvodjaciSet.contains(a.getProizvodjac().trim().toUpperCase()))
                .collect(Collectors.toList());
    }

    public Map<String, List<String>> getAllGroupsAndSubgroups() {
        return groupMap;
    }

    public Map<String, List<String>> vratiSveNadgrupeSaNjihovimGrupama(String glavnaGrupa) {
        // Pronađi sve nadgrupe koje pripadaju zadatoj glavnoj grupi
        List<String> nadgrupe = groupMap.getOrDefault(glavnaGrupa.toUpperCase(), List.of());

        // Mapiranje nadgrupa na njihove grupe koristeći vendorRepository metodu
        Map<String, List<String>> result = new HashMap<>();
        for (String nadgrupa : nadgrupe) {
            // Pretpostavljamo da `vendorRepository.findDistinctGroupsByNadgrupa` vraća
            // listu grupa
            String grupe = getGrupeByNadgrupa(1L, nadgrupa); // Pretpostavljamo da je vendorId=1
            if (grupe != null && !grupe.isEmpty()) {
                // Dodaj u rezultat kao listu grupa
                result.put(nadgrupa, Arrays.asList(grupe.split(","))); // Pretpostavljamo da su grupe odvojene zarezima
            }
        }

        return result;
    }

    public List<Long> vratiSveIdjeve() {
        return vendorRepository.vratiSveIdjeve();
    }

    public List<String> getProizvodjaciByGlavnaGrupa(Long vendorId, String glavnaGrupa) {
        List<String> nadgrupeList = groupMap.get(glavnaGrupa.toUpperCase());
        if (nadgrupeList == null)
            return Collections.emptyList();

        List<String> proizvodjaci;
        if (vendorId == 0) {
            proizvodjaci = vendorRepository.findUnifiedProizvodjaciByGlavnaGrupa(nadgrupeList);
        } else {
            proizvodjaci = vendorRepository.findProizvodjaciByGlavnaGrupa(vendorId,
                    nadgrupeList.toArray(new String[0]));
        }

        // Obrada liste:
        return proizvodjaci.stream()
                .map(String::toUpperCase) // Pretvori sve u velika slova
                .filter(name -> !(name.equals("/") || name.equals("-"))) // Ukloni "/" i "-"
                .distinct()
                .sorted() // Sortiraj po abecednom redu
                .toList();
    }

    public Map<String, Integer> getProizvodjaciWithCountByGlavnaGrupa(Long vendorId, String glavnaGrupa,
            Integer minCena, Integer maxCena) {
        System.out.println("==== POČETAK getProizvodjaciWithCountByGlavnaGrupa ====");
        System.out.println("Vendor ID: " + vendorId);
        System.out.println("Glavna grupa: " + glavnaGrupa);

        // Dobavljanje nadgrupa
        String[] nadgrupe = getNadgrupeByGlavnaGrupaArray(glavnaGrupa);
        // Poziv repository metode
        List<Object[]> resultList = vendorRepository.findProizvodjaciWithCountByGlavnaGrupa(vendorId, nadgrupe, minCena,
                maxCena);
        System.out.println("Broj rezultata iz repository-a: " + resultList.size());

        // Ispis rezultata za proveru
        for (Object[] arr : resultList) {
            System.out.println("Proizvođač: " + arr[0] + ", Broj artikala: " + arr[1]);
        }

        // Mapiranje rezultata u Map<String, Integer>
        Map<String, Integer> rezultat = resultList.stream()
                .filter(arr -> arr[0] != null && !arr[0].equals("/") && !arr[0].equals("-")) // Izbacujemo "/" i "-"
                .collect(Collectors.toMap(
                        arr -> arr[0].toString(), // Key: Naziv proizvođača
                        arr -> Integer.parseInt(arr[1].toString()), // Value: Broj artikala
                        (oldValue, newValue) -> oldValue, // Ako ima duplikata, zadrži prvi (ne bi trebalo da ih bude)
                        TreeMap::new // Sortira mapu po ključu
                ));

        return rezultat;
    }

    public BigDecimal getMaxPriceByVendorId(Long vendorId) {
        if (vendorId == 0) {
            return vendorRepository.findUnifiedMaxPrice();
        }
        return vendorRepository.findMaxPriceByVendorId(vendorId);
    }

    public List<String> getAllDistinctProizvodjaci() {
        return vendorRepository.findAllDistinctProizvodjaci();
    }

    public List<String> getAllMainProizvodjaci() {
        return GLAVNI_PROIZVODJACI;
    }

    public List<Artikal> getArtikliByProizvodjac(String proizvodjac) {
        List<String> artikalXmlList = vendorRepository.findArtikliByProizvodjac(proizvodjac);
        List<Artikal> artikli = new ArrayList<>();

        if (artikalXmlList == null || artikalXmlList.isEmpty()) {
            return artikli; // Vraća praznu listu ako nema rezultata
        }

        try {
            JAXBContext context = JAXBContext.newInstance(Artikal.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            artikli = artikalXmlList.stream()
                    .map(xml -> parseArtikal(xml, unmarshaller))
                    .filter(Objects::nonNull)
                    .filter(a -> a.getCena() >= 100)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException("Greška prilikom inicijalizacije JAXB-a", e);
        }

        return artikli;
    }

    private Artikal parseArtikal(String xml, Unmarshaller unmarshaller) {
        try {
            if (xml == null || xml.trim().isEmpty()) {
                return null;
            }
            return (Artikal) unmarshaller.unmarshal(new StringReader(xml));
        } catch (Exception e) {
            return null; // Ignoriše nevalidne XML zapise i nastavlja obradu
        }
    }

    public List<Artikal> getArtikliByGrupa(Long vendorId, String nadgrupa, String grupa, Double minCena,
            Double maxCena, com.tehno.tehnozonaspring.dto.ProductPageResponse response) {
        List<String> xmlList;
        if (vendorId == 0) {
            xmlList = vendorRepository.findUnifiedArtikliByNadgrupaAndGrupa(nadgrupa, grupa);
        } else {
            // Revert to searching by nadgrupa and then filtering by grupa in Java to
            // maintain existing repo behavior
            xmlList = vendorRepository.findArtikliByNadgrupaAndVendorId(vendorId, nadgrupa);
        }

        List<Artikal> artikli = new ArrayList<>();
        double globalMin = Double.MAX_VALUE;
        double globalMax = Double.MIN_VALUE;

        try {
            JAXBContext context = JAXBContext.newInstance(Artikal.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            for (String artikalXml : xmlList) {
                StringReader reader = new StringReader(artikalXml);
                Artikal artikal = (Artikal) unmarshaller.unmarshal(reader);

                double cena = artikal.getCena();
                if (cena < 100)
                    continue;

                if ((minCena == null || minCena == 0 || cena >= minCena) &&
                        (maxCena == null || maxCena == 0 || cena <= maxCena)) {
                    if (artikal.getGrupa().trim().equalsIgnoreCase(grupa.trim())) {
                        if (cena < globalMin)
                            globalMin = cena;
                        if (cena > globalMax)
                            globalMax = cena;
                        artikli.add(artikal);
                    }
                }
            }
            if (globalMin == Double.MAX_VALUE)
                globalMin = 0;
            if (globalMax == Double.MIN_VALUE)
                globalMax = 0;

            response.setInitialMaxCena(globalMax);
            response.setInitialMinCena(globalMin);
        } catch (Exception e) {
            throw new RuntimeException("Greška prilikom parsiranja artikala", e);
        }
        return artikli;
    }

    public Map<String, Integer> getProizvodjaciWithCountByGlavnaGrupaAndNadgrupa(Long vendorId, String glavnaGrupa,
            String nadgrupa, Double minCena, Double maxCena) {
        System.out.println("==== POČETAK getProizvodjaciWithCountByGlavnaGrupaAndNadgrupa ====");
        System.out.println("Vendor ID: " + vendorId);
        System.out.println("Glavna grupa: " + glavnaGrupa);
        System.out.println("Nadgrupa: " + nadgrupa);

        // Poziv repository metode bez filtracije po ceni
        List<Object[]> resultList;
        if (vendorId == 0) {
            resultList = vendorRepository.findUnifiedProizvodjaciWithCountByNadgrupa(nadgrupa);
        } else {
            resultList = vendorRepository.findProizvodjaciWithCountByGlavnaGrupaAndNadgrupa(vendorId, nadgrupa);
        }
        System.out.println("Broj rezultata iz repository-a: " + resultList.size());

        Map<String, Integer> rezultat = new TreeMap<>();

        for (Object[] row : resultList) {
            String proizvodjac = row[0].toString();
            List<BigDecimal> cene = Arrays.asList((BigDecimal[]) row[2]); // Preuzimanje niza cena

            // Filtracija cene u servisu
            long filtriraniBroj = cene.stream()
                    .filter(cena -> (minCena == null || cena.compareTo(BigDecimal.valueOf(minCena)) >= 0))
                    .filter(cena -> (maxCena == null || cena.compareTo(BigDecimal.valueOf(maxCena)) <= 0))
                    .count();

            if (filtriraniBroj > 0) {
                rezultat.put(proizvodjac, (int) filtriraniBroj);
            }
        }

        return rezultat;
    }

    public List<Artikal> searchArtikliByNazivOrProizvodjac(Long vendorId, String query) {
        List<String> allXml;
        if (vendorId == 0) {
            allXml = vendorRepository.findUnifiedArtikliXml();
        } else {
            allXml = vendorRepository.findAllArtikliXmlByVendorId(vendorId);
        }
        List<Artikal> rezultati = new ArrayList<>();

        if (allXml == null || allXml.isEmpty()) {
            return rezultati;
        }

        try {
            JAXBContext context = JAXBContext.newInstance(Artikal.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            String lowerQuery = query.toLowerCase();

            for (String xml : allXml) {
                Artikal artikal = parseArtikal(xml, unmarshaller);
                if (artikal != null && artikal.getCena() >= 100) {
                    String naziv = Optional.ofNullable(artikal.getNaziv()).orElse("").toLowerCase();
                    String proizvodjac = Optional.ofNullable(artikal.getProizvodjac()).orElse("").toLowerCase();

                    if (naziv.contains(lowerQuery) || proizvodjac.contains(lowerQuery)) {
                        rezultati.add(artikal);
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Greška prilikom pretrage artikala", e);
        }

        return rezultati;
    }

    public Artikal getProductByArtikalBarCode(Long vendorId, String barCode) {
        String xml;
        if (vendorId == 0) {
            xml = vendorRepository.findUnifiedArtikalByBarkod(barCode);
        } else {
            List<String> xmlList = vendorRepository.getProductByArtikalBarCodeRaw(vendorId, barCode);
            xml = (xmlList != null && !xmlList.isEmpty()) ? xmlList.get(0) : null;
        }

        if (xml == null || xml.isEmpty()) {
            return null;
        }

        try {
            JAXBContext context = JAXBContext.newInstance(Artikal.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            return (Artikal) unmarshaller.unmarshal(new StringReader(xml));

        } catch (Exception e) {
            throw new RuntimeException("Greška prilikom parsiranja XML artikla za barkod " + barCode, e);
        }
    }

    public List<Artikal> getArtikliByBrand(Long vendorId, String brand) {
        String target = brand.trim().toUpperCase();
        List<String> xmlList;
        if (vendorId == 0) {
            xmlList = vendorRepository.findUnifiedArtikliByBrand(brand);
        } else {
            xmlList = vendorRepository.findAllArtikliXmlByVendorId(vendorId);
        }

        List<Artikal> result = new ArrayList<>();
        try {
            JAXBContext context = JAXBContext.newInstance(Artikal.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            for (String xml : xmlList) {
                Artikal artikal = (Artikal) unmarshaller.unmarshal(new StringReader(xml));

                if (artikal.getCena() < 100)
                    continue;

                if (artikal.getProizvodjac() != null &&
                        artikal.getProizvodjac().trim().toUpperCase().equals(target)) {

                    result.add(artikal);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Greška prilikom parsiranja artikala", e);
        }
        return result;
    }

    public FeaturedProduct addFeaturedProduct(Long vendorId, String barcode, FeatureType featureType, Integer priority,
            LocalDateTime validFrom, LocalDateTime validTo, String itemType, String subtitle, String buttonText,
            String buttonRoute, String glavnaGrupa, String nadgrupa, String grupa, String brandName, String customName,
            String customImageUrl) {
        if (priority == null) {
            priority = 1;
        }
        if (validFrom == null) {
            validFrom = LocalDateTime.now();
        }
        if (validTo == null) {
            validTo = LocalDateTime.now().plusMonths(1);
        }

        vendorRepository.insertFeaturedProduct(
                barcode,
                vendorId,
                featureType.name(),
                priority,
                validFrom,
                validTo,
                itemType,
                subtitle,
                buttonText,
                buttonRoute,
                glavnaGrupa,
                nadgrupa,
                grupa,
                brandName,
                customName,
                customImageUrl);

        // kreiramo objekat da vratimo klijentu
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
        List<String> duplicateBarkodovi = vendorRepository.findDuplicateBarkodovi();
        Map<String, List<Artikal>> duplicatesMap = new HashMap<>();

        try {
            JAXBContext context = JAXBContext.newInstance(Artikal.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            for (String barkod : duplicateBarkodovi) {
                List<String> xmlList = vendorRepository.findArtikliByBarkod(barkod);
                List<Artikal> artikli = new ArrayList<>();
                for (String xml : xmlList) {
                    try {
                        Artikal a = (Artikal) unmarshaller.unmarshal(new StringReader(xml));
                        artikli.add(a);
                    } catch (Exception ignored) {
                    }
                }
                duplicatesMap.put(barkod, artikli);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return duplicatesMap;
    }

    public List<FeaturedArtikalResponse> getActiveFeaturedArtikli() {
        List<FeaturedProduct> featuredList = featuredProductRepository.getAllActiveFeatured();

        List<FeaturedArtikalResponse> result = new ArrayList<>();

        for (FeaturedProduct fp : featuredList) {
            Artikal artikal = getProductByArtikalBarCode(fp.getVendorId(), fp.getBarcode());

            if (artikal != null) {
                result.add(new FeaturedArtikalResponse(artikal, fp));
            }
        }

        System.out.println("HOMEPAGE: Povučeni svi istaknuti artikli. Broj: " + result.size());
        return result;
    }

    public List<FeaturedArtikalResponse> getActiveFeaturedArtikliByType(FeatureType type) {

        // 1. Prvo dohvati sve featured iz tabele
        List<FeaturedProduct> featuredList = featuredProductRepository.getActiveFeaturedByType(type.name());

        List<FeaturedArtikalResponse> result = new ArrayList<>();

        // 2. Za svaki featured — izvuci artikal po barkodu
        for (FeaturedProduct fp : featuredList) {
            Artikal artikal = getProductByArtikalBarCode(fp.getVendorId(), fp.getBarcode());

            if (artikal != null) {
                result.add(new FeaturedArtikalResponse(artikal, fp));
            }
        }

        System.out.println("HOMEPAGE: Povučeni istaknuti artikli tipa " + type + ". Broj: " + result.size());
        return result;
    }

    public List<Map<String, Object>> getCountByGlavnaGrupaForBrand(Long vendorId, String brand, Double minCena,
            Double maxCena) {

        // 1. Dohvati sve artikle tog brenda
        List<Artikal> artikli = getArtikliByBrand(vendorId, brand);

        List<Artikal> filtrirani = VendorController.filtrirajPoCeni(artikli, minCena, maxCena);

        if (filtrirani == null || filtrirani.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Map za brojanje po glavnim grupama
        Map<String, Integer> counter = new HashMap<>();

        for (Artikal artikal : filtrirani) {

            String nadgrupa = artikal.getNadgrupa();
            if (nadgrupa == null || nadgrupa.trim().isEmpty()) {
                continue;
            }

            // 3. Pronađi glavnu grupu kojoj pripada nadgrupa
            String glavnaGrupa = findGlavnaGrupaForNadgrupa(nadgrupa);

            // Ako nije pronađena, skip
            if (glavnaGrupa == null)
                continue;

            // 4. Povećaj counter
            counter.merge(glavnaGrupa, 1, Integer::sum);
        }

        // 5. Pretvori u listu mapova za frontend
        return counter.entrySet().stream()
                .map(e -> Map.<String, Object>of(
                        "glavnaGrupa", e.getKey(),
                        "count", e.getValue()))
                .toList();
    }

    private String findGlavnaGrupaForNadgrupa(String nadgrupa) {
        if (nadgrupa == null)
            return null;
        String n = nadgrupa.trim().toUpperCase();

        for (Map.Entry<String, List<String>> entry : groupMap.entrySet()) {
            List<String> nadgrupe = entry.getValue();

            // Da li se nadgrupa iz artikla nalazi u listi nadgrupa za glavnu grupu?
            for (String ng : nadgrupe) {
                if (ng.equalsIgnoreCase(n)) {
                    return entry.getKey(); // glavna grupa
                }
            }
        }
        return null;
    }

    public List<Artikal> getArtikliByBrandAndGlavnaGrupa(Long vendorId, String brand, String glavnaGrupa) {
        List<Artikal> allBrandArtikli = getArtikliByBrand(vendorId, brand);
        List<Artikal> result = new ArrayList<>();

        for (Artikal a : allBrandArtikli) {
            String artGlavnaGrupa = findGlavnaGrupaForNadgrupa(a.getNadgrupa());
            if (artGlavnaGrupa != null && artGlavnaGrupa.equalsIgnoreCase(glavnaGrupa)) {
                result.add(a);
            }
        }
        return result;
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

        // Ako dodajemo novi HERO, sklanjamo stare (postavljamo validTo na sadašnje
        // vreme)
        if (request.getSection() == com.tehno.tehnozonaspring.model.enums.HomepageSection.HERO) {
            List<com.tehno.tehnozonaspring.model.HomepageItem> existingHeores = homepageItemRepository
                    .findByVendorIdAndSectionAndValidToAfter(vendorId, request.getSection(), LocalDateTime.now());

            for (com.tehno.tehnozonaspring.model.HomepageItem oldHero : existingHeores) {
                oldHero.setValidTo(LocalDateTime.now());
            }
            homepageItemRepository.saveAll(existingHeores);
        }

        // Product fields
        item.setBarcode(request.getBarcode());

        // Category fields
        item.setGlavnaGrupa(request.getGlavnaGrupa());
        item.setNadgrupa(request.getNadgrupa());
        item.setGrupa(request.getGrupa());

        // Brand fields
        item.setBrandName(request.getBrandName());

        // Custom overriding fields
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
