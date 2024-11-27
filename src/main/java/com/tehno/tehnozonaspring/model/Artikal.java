package com.tehno.tehnozonaspring.model;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.Arrays;
import java.util.List;

@XmlRootElement(name = "artikal")
public class Artikal {
    @XmlElement(name = "sifra")
    private String sifra;

    @XmlElement(name = "barkod")
    private String barkod;

    @XmlElement(name = "naziv")
    private String naziv;

    @XmlElement(name = "pdv")
    private int pdv;

    @XmlElement(name = "nadgrupa")
    private String nadgrupa;

    @XmlElement(name = "grupa")
    private String grupa;

    @XmlElement(name = "proizvodjac")
    private String proizvodjac;

    @XmlElement(name = "jedinica_mere")
    private String jedinicaMere;

    @XmlElement(name = "model")
    private String model;

    @XmlElement(name = "kolicina")
    private String kolicina;

    @XmlElement(name = "b2bcena")
    private double b2bcena;

    @XmlElement(name = "valuta")
    private String valuta;

    @XmlElement(name = "flag_akcijska_cena")
    private int flagAkcijskaCena;

    @XmlElement(name = "web_cena")
    private double webCena;

    @XmlElement(name = "mpcena")
    private double mpcena;

    @XmlElement(name = "energetska_klasa")
    private String energetskaKlasa;

    @XmlElement(name = "energetska_klasa_link")
    private String energetskaKlasaLink;

    @XmlElement(name = "energetska_klasa_pdf")
    private String energetskaKlasaPdf;

    @XmlElement(name = "deklaracija")
    private String deklaracija;

    @XmlElement(name = "opis")
    private String opis;

    @XmlElementWrapper(name = "slike")
    @XmlElement(name = "slika")
    private List<String> slike;

    @XmlElementWrapper(name = "filteri")
    @XmlElement(name = "filter_grupa")
    private List<FilterGrupa> filteri;

    // GETTER METODE
    public String getSifra() { return sifra; }
    public String getBarkod() { return barkod; }
    public String getNaziv() { return naziv; }
    public int getPdv() { return pdv; }
    public String getNadgrupa() { return nadgrupa; }
    public String getGrupa() { return grupa; }
    public String getProizvodjac() { return proizvodjac; }
    public String getJedinicaMere() { return jedinicaMere; }
    public String getModel() { return model; }
    public String getKolicina() { return kolicina; }
    public double getB2bcena() { return b2bcena; }
    public String getValuta() { return valuta; }
    public int getFlagAkcijskaCena() { return flagAkcijskaCena; }
    public double getWebCena() { return webCena; }
    public double getMpcena() { return mpcena; }
    public String getEnergetskaKlasa() { return energetskaKlasa; }
    public String getEnergetskaKlasaLink() { return energetskaKlasaLink; }
    public String getEnergetskaKlasaPdf() { return energetskaKlasaPdf; }
    public String getDeklaracija() { return deklaracija; }
    public String getOpis() { return opis; }
    public List<String> getSlike() { return slike; }
    public List<FilterGrupa> getFilteri() { return filteri; }

    @XmlRootElement(name = "filter_grupa")
    public static class FilterGrupa {
        @XmlElement(name = "ime")
        private String ime;

        @XmlElement(name = "filter")
        private List<Filter> filter;

        // GETTER METODE
        public String getIme() { return ime; }
        public List<Filter> getFilter() { return filter; }

        @XmlRootElement(name = "filter")
        public static class Filter {
            @XmlElement(name = "ime")
            private String ime;

            @XmlElement(name = "vrednost")
            private String vrednost;

            // GETTER METODE
            public String getIme() { return ime; }
            public String getVrednost() { return vrednost; }
        }
    }

}
