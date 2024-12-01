package com.tehno.tehnozonaspring.service;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

import com.tehno.tehnozonaspring.model.Artikal;
import com.tehno.tehnozonaspring.model.Vendor;
import com.tehno.tehnozonaspring.repository.VendorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
}
