package com.tehno.tehnozonaspring.service;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

import com.tehno.tehnozonaspring.model.Artikal;
import com.tehno.tehnozonaspring.model.Vendor;
import com.tehno.tehnozonaspring.repository.VendorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;

    private final Map<String, List<String>> groupMap = Map.of(
            "BELA TEHNIKA I KUĆNI APARATI", List.of(
                    "BELA TEHNIKA", "MALI KUĆNI APARATI", "GREJANJE", "HLADNJACI", "KUĆNA BELA TEHNIKA"
            ),
            "TV, FOTO, AUDIO I VIDEO", List.of(
                    "AUDIO, HI-FI", "TV, AUDIO, VIDEO", "FOTOAPARATI I KAMERE", "DIGITALNI SNIMAČI",
                    "PROJEKTORI I OPREMA", "ZVUČNICI", "SLUŠALICE I MIKROFONI", "KAMERE"
            ),
            "RAČUNARI, KOMPONENTE I GAMING", List.of(
                    "LAPTOP I TABLET RAČUNARI", "DESKTOP RAČUNARI", "SERVERI", "PROCESORI",
                    "MATIČNE PLOČE", "MEMORIJE", "HARD DISKOVI", "HDD Rack", "GRAFIČKE KARTE",
                    "GAMING", "RAČUNARI", "RAČUNARSKE KOMPONENTE", "RAČUNARSKE PERIFERIJE",
                    "PC KOZMETIKA", "SOFTWARE", "Microsoft", "WIRELESS", "OPTIČKI UREĐAJI", "Čitači kartica", "REKOVI I OPREMA", "TASTATURE", "FIBER"
            ),
            "TELEFONI, TABLETI I OPREMA", List.of(
                    "MOBILNI I FIKSNI TELEFONI", "OPREMA ZA MOBILNE TELEFONE", "OPREMA ZA LAPTOPOVE",
                    "OPREMA ZA TABLETE", "OPREMA ZA TV", "MEMORIJSKE KARTICE I ČITAČI",
                    "USB FLASH I HDD", "USB KABLOVI", "USB ADAPTERI", "MREŽNA OPREMA", "FIKSNI TELEFONI"
            ),
            "SIGURNOSNI I ALARMNI SISTEMI", List.of(
                    "ALARMNI SISTEMI", "ALARMNI SISTEM PARADOX", "ALARMNI SISTEM ELDES",
                    "VIDEO NADZOR I SIGURNOSNA OPREMA", "OPREMA ZA VIDEO NADZOR", "KUTIJE",
                    "KANALICE", "UTIČNICE", "KONEKTORI I MODULI", "VIDEO NADZOR I  SIGURNOSNA OPREMA"
            ),
            "ALATI I OPREMA ZA DOM", List.of(
                    "ALAT I BAŠTA", "BAŠTA", "LED RASVETA", "SVE ZA KUĆU"
            ),
            "BATERIJE, PUNJAČI I KABLOVI", List.of(
                    "BATERIJE I PUNJAČI", "KABLOVI", "KABLOVI I ADAPTERI", "PCI ADAPTERI", "PC KABLOVI", "ADAPTERI"
            ),
            "FITNESS I SPORT", List.of(
                    "BICIKLE I FITNES", "NEGA LICA I TELA"
            ),
            "KANCELARIJSKI I ŠKOLSKI MATERIJAL", List.of(
                    "KANCELARIJSKI MATERIJAL", "ŠKOLSKI PRIBOR", "ŠTAMPAČI", "TONERI",
                    "KERTRIDŽ", "RIBONI", "MASTILA", "CD, DVD MEDIJI", "SKENERI I FOTOKOPIRI"
            ),
            "OSTALO I OUTLET", List.of(
                    "OUTLET", "RAZNO"
            )
    );

    public List<String> getNadgrupeByGlavnaGrupa(String glavnaGrupa) {
        return groupMap.getOrDefault(glavnaGrupa.toUpperCase(), List.of());
    }

    public String[] getNadgrupeByGlavnaGrupaArray(String glavnaGrupa) {
        return groupMap.getOrDefault(glavnaGrupa.toUpperCase(), List.of())
                .toArray(new String[0]); // Konvertuje List<String> u String[]
    }


    @Autowired
    public VendorService(VendorRepository VendorRepository) {
        this.vendorRepository = VendorRepository;
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
                artikli.add(artikal);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        return artikli;
    }

    public List<String> getGlavneGrupe() {
        return new ArrayList<>(groupMap.keySet());
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
                artikli.add(artikal);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return artikli;
    }

    public List<String> getAllGroups() {
        return vendorRepository.findAllGroups();
    }


    public List<Artikal> getArtikliByGlavnaGrupa(Long vendorId, String glavnaGrupa) {
        String[] nadgrupe = getNadgrupeByGlavnaGrupaArray(glavnaGrupa);

//        String[] nadgrupe = {"BELA TEHNIKA", "MALI KUĆNI APARATI", "GREJANJE", "HLADNJACI", "KUĆNA BELA TEHNIKA"};
        List<String> artikalXmlList = vendorRepository.findArtikliByGlavnaGrupa(1L, nadgrupe);

//        List<String> artikalXmlList = vendorRepository.findArtikliByGlavnaGrupa(vendorId, nadgrupe);
        List<Artikal> artikli = new ArrayList<>();

        try {
            JAXBContext context = JAXBContext.newInstance(Artikal.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            for (String artikalXml : artikalXmlList) {
                StringReader reader = new StringReader(artikalXml);
                Artikal artikal = (Artikal) unmarshaller.unmarshal(reader);
                artikli.add(artikal);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Greška prilikom parsiranja artikala", e);
        }

        return artikli;
    }

    public List<Artikal> getArtikliByGlavnaGrupaAndNadgrupa(Long vendorId, String glavnaGrupa, String nadgrupa) {

        // Preuzmi sve nadgrupe koje pripadaju glavnoj grupi
        String[] nadgrupe = getNadgrupeByGlavnaGrupaArray(glavnaGrupa);

        List<String> artikalXmlList = vendorRepository.findArtikliByGlavnaGrupaAndNadgrupa(vendorId, nadgrupe);
        // Poziv repository sloja

        List<Artikal> artikli = new ArrayList<>();

        try {
            JAXBContext context = JAXBContext.newInstance(Artikal.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            for (String artikalXml : artikalXmlList) {
                StringReader reader = new StringReader(artikalXml);
                Artikal artikal = (Artikal) unmarshaller.unmarshal(reader);
                artikli.add(artikal);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Greška prilikom parsiranja artikala", e);
        }

        return artikli;
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
            // Pretpostavljamo da `vendorRepository.findDistinctGroupsByNadgrupa` vraća listu grupa
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
        // Dohvati sve nadgrupe povezane sa glavnom grupom
        String[] nadgrupe = getNadgrupeByGlavnaGrupaArray(glavnaGrupa);

        // Poziv repository metode za dobijanje proizvođača
        List<String> proizvodjaci = vendorRepository.findProizvodjaciByGlavnaGrupa(vendorId, nadgrupe);

        // Obrada liste:
        return proizvodjaci.stream()
                .map(String::toUpperCase)                    // Pretvori sve u velika slova
                .filter(name -> !(name.equals("/") || name.equals("-"))) // Ukloni "/" i "-"
                .distinct()
                .sorted()                                    // Sortiraj po abecednom redu
                .toList();
    }

    public Map<String, Integer> getProizvodjaciWithCountByGlavnaGrupa(Long vendorId, String glavnaGrupa) {
        System.out.println("==== POČETAK getProizvodjaciWithCountByGlavnaGrupa ====");
        System.out.println("Vendor ID: " + vendorId);
        System.out.println("Glavna grupa: " + glavnaGrupa);

        // Dobavljanje nadgrupa
        String[] nadgrupe = getNadgrupeByGlavnaGrupaArray(glavnaGrupa);
        System.out.println("Nadgrupe: " + Arrays.toString(nadgrupe));

        // Poziv repository metode
        List<Object[]> resultList = vendorRepository.findProizvodjaciWithCountByGlavnaGrupa(vendorId, nadgrupe);
        System.out.println("Broj rezultata iz repository-a: " + resultList.size());

        // Ispis rezultata za proveru
        for (Object[] arr : resultList) {
            System.out.println("Proizvođač: " + arr[0] + ", Broj artikala: " + arr[1]);
        }

        // Mapiranje rezultata u Map<String, Integer>
        Map<String, Integer> rezultat = resultList.stream()
                .filter(arr -> arr[0] != null && !arr[0].equals("/") && !arr[0].equals("-")) // Izbacujemo "/" i "-"
                .peek(arr -> System.out.println("Filtrirani proizvođač: " + arr[0] + ", Broj artikala: " + arr[1]))
                .collect(Collectors.toMap(
                        arr -> arr[0].toString(),                  // Key: Naziv proizvođača
                        arr -> Integer.parseInt(arr[1].toString()),// Value: Broj artikala
                        (oldValue, newValue) -> oldValue,          // Ako ima duplikata, zadrži prvi (ne bi trebalo da ih bude)
                        TreeMap::new                               // Sortira mapu po ključu
                ));

        System.out.println("Konačan rezultat: " + rezultat);
        System.out.println("==== KRAJ getProizvodjaciWithCountByGlavnaGrupa ====");

        return rezultat;
    }

    public BigDecimal getMaxPriceByVendorId(Long vendorId) {
        return vendorRepository.findMaxPriceByVendorId(vendorId);
    }



}
