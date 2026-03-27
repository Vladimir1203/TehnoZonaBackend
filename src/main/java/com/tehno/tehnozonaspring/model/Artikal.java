package com.tehno.tehnozonaspring.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.Arrays;
import java.util.List;

@XmlRootElement(name = "artikal")
@XmlAccessorType(XmlAccessType.FIELD)
public class Artikal {
    @XmlElement(name = "vendorId")
    private Long vendorId;

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

    // Polje koje dolazi iz artikal tabele (ne postoji u XML-u)
    private String glavnaGrupa;

    // GETTER I SETTER METODE
    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public String getSifra() { return sifra; }
    public void setSifra(String sifra) { this.sifra = sifra; }

    public String getBarkod() { return barkod; }
    public void setBarkod(String barkod) { this.barkod = barkod; }

    public String getNaziv() { return naziv; }
    public void setNaziv(String naziv) { this.naziv = naziv; }

    public int getPdv() { return pdv; }
    public void setPdv(int pdv) { this.pdv = pdv; }

    public String getNadgrupa() { return nadgrupa; }
    public void setNadgrupa(String nadgrupa) { this.nadgrupa = nadgrupa; }

    public String getGrupa() { return grupa; }
    public void setGrupa(String grupa) { this.grupa = grupa; }

    public String getGlavnaGrupa() { return glavnaGrupa; }
    public void setGlavnaGrupa(String glavnaGrupa) { this.glavnaGrupa = glavnaGrupa; }

    public String getProizvodjac() { return proizvodjac; }
    public void setProizvodjac(String proizvodjac) { this.proizvodjac = proizvodjac; }

    public String getJedinicaMere() { return jedinicaMere; }
    public void setJedinicaMere(String jedinicaMere) { this.jedinicaMere = jedinicaMere; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getKolicina() { return kolicina; }
    public void setKolicina(String kolicina) { this.kolicina = kolicina; }

    public double getB2bcena() { return b2bcena; }
    public void setB2bcena(double b2bcena) { this.b2bcena = b2bcena; }

    public String getValuta() { return valuta; }
    public void setValuta(String valuta) { this.valuta = valuta; }

    public int getFlagAkcijskaCena() { return flagAkcijskaCena; }
    public void setFlagAkcijskaCena(int flagAkcijskaCena) { this.flagAkcijskaCena = flagAkcijskaCena; }

    public double getWebCena() { return webCena; }
    public void setWebCena(double webCena) { this.webCena = webCena; }

    public double getMpcena() { return mpcena; }
    public void setMpcena(double mpcena) { this.mpcena = mpcena; }

    // Helper: koristi mpcena, fallback na webCena, ako ni ona ne postoji vraca 0
    public double getCena() { return mpcena > 0 ? mpcena : webCena; }

    public String getEnergetskaKlasa() { return energetskaKlasa; }
    public void setEnergetskaKlasa(String energetskaKlasa) { this.energetskaKlasa = energetskaKlasa; }

    public String getEnergetskaKlasaLink() { return energetskaKlasaLink; }
    public void setEnergetskaKlasaLink(String energetskaKlasaLink) { this.energetskaKlasaLink = energetskaKlasaLink; }

    public String getEnergetskaKlasaPdf() { return energetskaKlasaPdf; }
    public void setEnergetskaKlasaPdf(String energetskaKlasaPdf) { this.energetskaKlasaPdf = energetskaKlasaPdf; }

    public String getDeklaracija() { return deklaracija; }
    public void setDeklaracija(String deklaracija) { this.deklaracija = deklaracija; }

    public String getOpis() { return opis; }
    public void setOpis(String opis) { this.opis = opis; }

    public List<String> getSlike() { return slike; }
    public void setSlike(List<String> slike) { this.slike = slike; }

    public List<FilterGrupa> getFilteri() { return filteri; }
    public void setFilteri(List<FilterGrupa> filteri) { this.filteri = filteri; }

    @XmlRootElement(name = "filter_grupa")
    public static class FilterGrupa {
        @XmlElement(name = "ime")
        private String ime;

        @XmlElement(name = "filter")
        private List<Filter> filter;

        // GETTER METODE
        public String getIme() {
            return ime;
        }

        public List<Filter> getFilter() {
            return filter;
        }

        @XmlRootElement(name = "filter")
        public static class Filter {
            @XmlElement(name = "ime")
            private String ime;

            @XmlElement(name = "vrednost")
            private String vrednost;

            // GETTER METODE
            public String getIme() {
                return ime;
            }

            public String getVrednost() {
                return vrednost;
            }
        }
    }

}
