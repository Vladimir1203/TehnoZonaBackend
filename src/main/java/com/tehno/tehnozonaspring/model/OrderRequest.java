package com.tehno.tehnozonaspring.model;

import java.util.List;

public class OrderRequest {

    private String ime;
    private String prezime;
    private String email;
    private String adresa;
    private String postanskiBroj;
    private List<Artikal> artikli;

    public String getIme() {
        return ime;
    }

    public void setIme(String ime) {
        this.ime = ime;
    }

    public String getPrezime() {
        return prezime;
    }

    public void setPrezime(String prezime) {
        this.prezime = prezime;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAdresa() {
        return adresa;
    }

    public void setAdresa(String adresa) {
        this.adresa = adresa;
    }

    public String getPostanskiBroj() {
        return postanskiBroj;
    }

    public void setPostanskiBroj(String postanskiBroj) {
        this.postanskiBroj = postanskiBroj;
    }

    public List<Artikal> getArtikli() {
        return artikli;
    }

    public void setArtikli(List<Artikal> artikli) {
        this.artikli = artikli;
    }
}
