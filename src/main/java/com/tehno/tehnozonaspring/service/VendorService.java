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
                    "PROJEKTORI I OPREMA", "ZVUČNICI", "SLUŠALICE I MIKROFONI"
            ),
            "RAČUNARI, KOMPONENTE I GAMING", List.of(
                    "LAPTOP I TABLET RAČUNARI", "DESKTOP RAČUNARI", "SERVERI", "PROCESORI",
                    "MATIČNE PLOČE", "MEMORIJE", "HARD DISKOVI", "HDD RACK", "GRAFIČKE KARTE",
                    "GAMING", "RAČUNARI", "RAČUNARSKE KOMPONENTE", "RAČUNARSKE PERIFERIJE",
                    "PC KOZMETIKA", "SOFTWARE", "MICROSOFT", "WIRELESS", "OPTIČKI UREĐAJI", "ČITAČI KARTICA"
            ),
            "TELEFONI, TABLETI I OPREMA", List.of(
                    "MOBILNI I FIKSNI TELEFONI", "OPREMA ZA MOBILNE TELEFONE", "OPREMA ZA LAPTOPOVE",
                    "OPREMA ZA TABLETE", "OPREMA ZA TV", "MEMORIJSKE KARTICE I ČITAČI",
                    "USB FLASH I HDD", "USB KABLOVI", "USB ADAPTERI", "MREŽNA OPREMA", "FIKSNI TELEFONI"
            ),
            "SIGURNOSNI I ALARMNI SISTEMI", List.of(
                    "ALARMNI SISTEMI", "ALARMNI SISTEM PARADOX", "ALARMNI SISTEM ELDES",
                    "VIDEO NADZOR I SIGURNOSNA OPREMA", "OPREMA ZA VIDEO NADZOR", "KUTIJE",
                    "KANALICE", "UTIČNICE", "KONEKTORI I MODULI"
            ),
            "ALATI I OPREMA ZA DOM", List.of(
                    "ALAT I BAŠTA", "BAŠTA", "LED RASVETA", "SVE ZA KUĆU"
            ),
            "BATERIJE, PUNJAČI I KABLOVI", List.of(
                    "BATERIJE I PUNJAČI", "KABLOVI", "KABLOVI I ADAPTERI", "PCI ADAPTERI", "PC KABLOVI"
            ),
            "FITNESS I SPORT", List.of(
                    "BICIKLE I FITNES"
            ),
            "KANCELARIJSKI I ŠKOLSKI MATERIJAL", List.of(
                    "KANCELARIJSKI MATERIJAL", "ŠKOLSKI PRIBOR", "ŠTAMPAČI", "TONERI",
                    "KERTRIDŽ", "RIBONI", "MASTILA", "CD, DVD MEDIJI"
            ),
            "OSTALO I OUTLET", List.of(
                    "OUTLET", "RAZNO"
            )
    );

    public List<String> getNadgrupeByGlavnaGrupa(String glavnaGrupa) {
        return groupMap.getOrDefault(glavnaGrupa.toUpperCase(), List.of());
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
}
