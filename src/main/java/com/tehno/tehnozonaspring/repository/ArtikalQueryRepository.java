package com.tehno.tehnozonaspring.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tehno.tehnozonaspring.model.Artikal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Centralni repository za upite na artikal/artikal_dedup tabelu.
 * Koristi JdbcTemplate + RowMapper umesto JAXB — direktno mapira SQL red na Artikal objekat.
 */
@Repository
public class ArtikalQueryRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    // SQL za SELECT svih kolona iz artikal/dedup tabele
    private static final String SELECT_COLS = """
            vendor_id, vendor_sifra AS sifra, barkod, barkod_valid,
            nadgrupa, grupa, glavna_grupa,
            naziv, proizvodjac, model, jedinica_mere, kolicina,
            mpcena, b2bcena, web_cena, pdv, valuta,
            opis, deklaracija,
            energetska_klasa, energetska_klasa_link, energetska_klasa_pdf,
            slike, filteri
            """;

    // PostgreSQL ne moze da odredi tip NULL parametra — koristimo CAST
    private static final String MIN_FILTER = "(CAST(? AS numeric) IS NULL OR mpcena >= CAST(? AS numeric))";
    private static final String MAX_FILTER = "(CAST(? AS numeric) IS NULL OR mpcena <= CAST(? AS numeric))";

    public ArtikalQueryRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------
    // Unified (svi vendori) — koristi artikal_dedup view
    // ------------------------------------------------------------------

    public List<Artikal> findByGlavnaGrupa(String[] nadgrupe, Double minCena, Double maxCena) {
        if (nadgrupe == null || nadgrupe.length == 0) return Collections.emptyList();
        Array sqlArray = createTextArray(nadgrupe);
        return jdbc.query(
                "SELECT " + SELECT_COLS + " FROM artikal_dedup" +
                " WHERE UPPER(TRIM(nadgrupa)) = ANY(?)" +
                "   AND mpcena > 0" +
                "   AND " + MIN_FILTER +
                "   AND " + MAX_FILTER +
                " ORDER BY mpcena ASC",
                rowMapper(),
                sqlArray,
                toDecimal(minCena), toDecimal(minCena),
                toDecimal(maxCena), toDecimal(maxCena));
    }

    public List<Artikal> findByNadgrupa(String nadgrupa, Double minCena, Double maxCena) {
        return jdbc.query(
                "SELECT " + SELECT_COLS + " FROM artikal_dedup" +
                " WHERE UPPER(TRIM(nadgrupa)) = UPPER(TRIM(?))" +
                "   AND mpcena > 0" +
                "   AND " + MIN_FILTER +
                "   AND " + MAX_FILTER +
                " ORDER BY mpcena ASC",
                rowMapper(),
                nadgrupa,
                toDecimal(minCena), toDecimal(minCena),
                toDecimal(maxCena), toDecimal(maxCena));
    }

    public List<Artikal> findByNadgrupaAndGrupa(String nadgrupa, String grupa, Double minCena, Double maxCena) {
        return jdbc.query(
                "SELECT " + SELECT_COLS + " FROM artikal_dedup" +
                " WHERE UPPER(TRIM(nadgrupa)) = UPPER(TRIM(?))" +
                "   AND UPPER(TRIM(grupa)) = UPPER(TRIM(?))" +
                "   AND mpcena > 0" +
                "   AND " + MIN_FILTER +
                "   AND " + MAX_FILTER +
                " ORDER BY mpcena ASC",
                rowMapper(),
                nadgrupa, grupa,
                toDecimal(minCena), toDecimal(minCena),
                toDecimal(maxCena), toDecimal(maxCena));
    }

    public List<Artikal> findByBrand(String brand) {
        return jdbc.query(
                "SELECT " + SELECT_COLS + " FROM artikal_dedup" +
                " WHERE UPPER(TRIM(proizvodjac)) = UPPER(TRIM(?))" +
                " ORDER BY mpcena ASC",
                rowMapper(), brand);
    }

    public List<Artikal> findByBrandAndGlavnaGrupa(String brand, String[] nadgrupe) {
        Array sqlArray = createTextArray(nadgrupe);
        return jdbc.query(
                "SELECT " + SELECT_COLS + " FROM artikal_dedup" +
                " WHERE UPPER(TRIM(proizvodjac)) = UPPER(TRIM(?))" +
                "   AND UPPER(TRIM(nadgrupa)) = ANY(?)" +
                " ORDER BY mpcena ASC",
                rowMapper(), brand, sqlArray);
    }

    public Artikal findByBarkod(String barkod) {
        List<Artikal> list = jdbc.query(
                "SELECT " + SELECT_COLS + " FROM artikal_dedup" +
                " WHERE barkod = ? ORDER BY mpcena ASC LIMIT 1",
                rowMapper(), barkod);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Artikal> findAllByBarkod(String barkod) {
        return jdbc.query(
                "SELECT " + SELECT_COLS + " FROM artikal" +
                " WHERE barkod = ? ORDER BY mpcena ASC",
                rowMapper(), barkod);
    }

    public List<Artikal> findAll() {
        return jdbc.query(
                "SELECT " + SELECT_COLS + " FROM artikal_dedup ORDER BY mpcena ASC",
                rowMapper());
    }

    public List<Artikal> search(String query) {
        String tsQuery = query.trim().replace(" ", " & ") + ":*";
        return jdbc.query(
                "SELECT " + SELECT_COLS + " FROM artikal_dedup" +
                " WHERE to_tsvector('simple', COALESCE(naziv,'') || ' ' || COALESCE(proizvodjac,'') || ' ' || COALESCE(model,''))" +
                "       @@ to_tsquery('simple', unaccent(?))" +
                " ORDER BY mpcena ASC",
                rowMapper(), tsQuery);
    }

    // Fullback: trigram/ilike pretraga kada FTS ne vrati rezultate
    public List<Artikal> searchIlike(String query) {
        String pattern = "%" + query.toLowerCase() + "%";
        return jdbc.query(
                "SELECT " + SELECT_COLS + " FROM artikal_dedup" +
                " WHERE LOWER(naziv) LIKE ? OR LOWER(proizvodjac) LIKE ?" +
                " ORDER BY mpcena ASC",
                rowMapper(), pattern, pattern);
    }

    // ------------------------------------------------------------------
    // Vendor-specific (vendorId != 0)
    // ------------------------------------------------------------------

    public List<Artikal> findByVendorId(Long vendorId) {
        return jdbc.query(
                "SELECT " + SELECT_COLS + " FROM artikal WHERE vendor_id = ? ORDER BY mpcena ASC",
                rowMapper(), vendorId);
    }

    public List<Artikal> findByVendorIdLimited(Long vendorId, int limit) {
        return jdbc.query(
                "SELECT " + SELECT_COLS + " FROM artikal WHERE vendor_id = ? ORDER BY mpcena ASC LIMIT ?",
                rowMapper(), vendorId, limit);
    }

    public List<Artikal> findByVendorAndNadgrupa(Long vendorId, String nadgrupa, Double minCena, Double maxCena) {
        return jdbc.query(
                "SELECT " + SELECT_COLS + " FROM artikal" +
                " WHERE vendor_id = ?" +
                "   AND UPPER(TRIM(nadgrupa)) = UPPER(TRIM(?))" +
                "   AND mpcena > 0" +
                "   AND " + MIN_FILTER +
                "   AND " + MAX_FILTER +
                " ORDER BY mpcena ASC",
                rowMapper(),
                vendorId, nadgrupa,
                toDecimal(minCena), toDecimal(minCena),
                toDecimal(maxCena), toDecimal(maxCena));
    }

    public List<Artikal> findByVendorAndGlavnaGrupa(Long vendorId, String[] nadgrupe, Double minCena, Double maxCena) {
        Array sqlArray = createTextArray(nadgrupe);
        return jdbc.query(
                "SELECT " + SELECT_COLS + " FROM artikal" +
                " WHERE vendor_id = ?" +
                "   AND UPPER(TRIM(nadgrupa)) = ANY(?)" +
                "   AND mpcena > 0" +
                "   AND " + MIN_FILTER +
                "   AND " + MAX_FILTER +
                " ORDER BY mpcena ASC",
                rowMapper(),
                vendorId, sqlArray,
                toDecimal(minCena), toDecimal(minCena),
                toDecimal(maxCena), toDecimal(maxCena));
    }

    public List<Artikal> findByVendorAndNadgrupaAndGrupa(Long vendorId, String nadgrupa, String grupa,
                                                          Double minCena, Double maxCena) {
        return jdbc.query(
                "SELECT " + SELECT_COLS + " FROM artikal" +
                " WHERE vendor_id = ?" +
                "   AND UPPER(TRIM(nadgrupa)) = UPPER(TRIM(?))" +
                "   AND UPPER(TRIM(grupa)) = UPPER(TRIM(?))" +
                "   AND mpcena > 0" +
                "   AND " + MIN_FILTER +
                "   AND " + MAX_FILTER +
                " ORDER BY mpcena ASC",
                rowMapper(),
                vendorId, nadgrupa, grupa,
                toDecimal(minCena), toDecimal(minCena),
                toDecimal(maxCena), toDecimal(maxCena));
    }

    public Artikal findByVendorAndBarkod(Long vendorId, String barkod) {
        List<Artikal> list = jdbc.query(
                "SELECT " + SELECT_COLS + " FROM artikal" +
                " WHERE vendor_id = ? AND barkod = ? ORDER BY mpcena ASC LIMIT 1",
                rowMapper(), vendorId, barkod);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Artikal> findByVendorAndBrand(Long vendorId, String brand) {
        return jdbc.query(
                "SELECT " + SELECT_COLS + " FROM artikal" +
                " WHERE vendor_id = ? AND UPPER(TRIM(proizvodjac)) = UPPER(TRIM(?))" +
                " ORDER BY mpcena ASC",
                rowMapper(), vendorId, brand);
    }

    // ------------------------------------------------------------------
    // Agregacioni upiti (za filter panele)
    // ------------------------------------------------------------------

    public List<Object[]> countProizvodjaciByGlavnaGrupa(String[] nadgrupe, BigDecimal minCena, BigDecimal maxCena) {
        Array sqlArray = createTextArray(nadgrupe);
        return jdbc.query(
                "SELECT UPPER(TRIM(proizvodjac)) AS proizvodjac, COUNT(*) AS br" +
                " FROM artikal_dedup" +
                " WHERE UPPER(TRIM(nadgrupa)) = ANY(?)" +
                "   AND mpcena > 0" +
                "   AND (CAST(? AS numeric) IS NULL OR mpcena >= CAST(? AS numeric))" +
                "   AND (CAST(? AS numeric) IS NULL OR mpcena <= CAST(? AS numeric))" +
                " GROUP BY UPPER(TRIM(proizvodjac))" +
                " ORDER BY UPPER(TRIM(proizvodjac))",
                (rs, i) -> new Object[]{ rs.getString("proizvodjac"), rs.getLong("br") },
                sqlArray, minCena, minCena, maxCena, maxCena);
    }

    public List<Object[]> countProizvodjaciByNadgrupa(String nadgrupa, BigDecimal minCena, BigDecimal maxCena) {
        return jdbc.query(
                "SELECT UPPER(TRIM(proizvodjac)) AS proizvodjac, COUNT(*) AS br" +
                " FROM artikal_dedup" +
                " WHERE UPPER(TRIM(nadgrupa)) = UPPER(TRIM(?))" +
                "   AND mpcena > 0" +
                "   AND (CAST(? AS numeric) IS NULL OR mpcena >= CAST(? AS numeric))" +
                "   AND (CAST(? AS numeric) IS NULL OR mpcena <= CAST(? AS numeric))" +
                " GROUP BY UPPER(TRIM(proizvodjac))" +
                " ORDER BY UPPER(TRIM(proizvodjac))",
                (rs, i) -> new Object[]{ rs.getString("proizvodjac"), rs.getLong("br") },
                nadgrupa, minCena, minCena, maxCena, maxCena);
    }

    public BigDecimal findMaxPrice() {
        return jdbc.queryForObject("SELECT MAX(mpcena) FROM artikal_dedup", BigDecimal.class);
    }

    public BigDecimal findMaxPriceByVendor(Long vendorId) {
        return jdbc.queryForObject("SELECT MAX(mpcena) FROM artikal WHERE vendor_id = ?", BigDecimal.class, vendorId);
    }

    public List<String> findDistinctProizvodjaci() {
        return jdbc.queryForList(
                "SELECT DISTINCT UPPER(TRIM(proizvodjac)) FROM artikal_dedup" +
                " WHERE proizvodjac IS NOT NULL ORDER BY 1",
                String.class);
    }

    public List<String> findDistinctProizvodjaciByGlavnaGrupa(String[] nadgrupe) {
        Array sqlArray = createTextArray(nadgrupe);
        return jdbc.queryForList(
                "SELECT DISTINCT UPPER(TRIM(proizvodjac)) FROM artikal_dedup" +
                " WHERE UPPER(TRIM(nadgrupa)) = ANY(?) AND proizvodjac IS NOT NULL ORDER BY 1",
                String.class, sqlArray);
    }

    public List<Object[]> findDistinctNadgrupeAndGrupe() {
        return jdbc.query(
                "SELECT DISTINCT nadgrupa, grupa FROM artikal WHERE nadgrupa IS NOT NULL ORDER BY nadgrupa, grupa",
                (rs, i) -> new Object[]{ rs.getString("nadgrupa"), rs.getString("grupa") });
    }

    public String findSampleImageForNadgrupa(String nadgrupa) {
        List<String> list = jdbc.queryForList(
                "SELECT slike[1] FROM artikal" +
                " WHERE UPPER(TRIM(nadgrupa)) = UPPER(TRIM(?))" +
                "   AND array_length(slike, 1) > 0 LIMIT 1",
                String.class, nadgrupa);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<String> findDuplicateBarkodovi() {
        return jdbc.queryForList(
                "SELECT barkod FROM artikal" +
                " WHERE barkod IS NOT NULL AND barkod != ''" +
                " GROUP BY barkod HAVING count(DISTINCT vendor_id) > 1",
                String.class);
    }

    // ------------------------------------------------------------------
    // Pomocne metode
    // ------------------------------------------------------------------

    private RowMapper<Artikal> rowMapper() {
        return (rs, rowNum) -> {
            Artikal a = new Artikal();
            a.setVendorId((long) rs.getInt("vendor_id"));
            a.setSifra(rs.getString("sifra"));
            a.setBarkod(rs.getString("barkod"));
            a.setNadgrupa(rs.getString("nadgrupa"));
            a.setGrupa(rs.getString("grupa"));
            a.setGlavnaGrupa(rs.getString("glavna_grupa"));
            a.setNaziv(rs.getString("naziv"));
            a.setProizvodjac(rs.getString("proizvodjac"));
            a.setModel(rs.getString("model"));
            a.setJedinicaMere(rs.getString("jedinica_mere"));
            a.setKolicina(rs.getString("kolicina"));
            BigDecimal mpcena = rs.getBigDecimal("mpcena");
            a.setMpcena(mpcena != null ? mpcena.doubleValue() : 0.0);
            BigDecimal b2bcena = rs.getBigDecimal("b2bcena");
            a.setB2bcena(b2bcena != null ? b2bcena.doubleValue() : 0.0);
            BigDecimal webCena = rs.getBigDecimal("web_cena");
            a.setWebCena(webCena != null ? webCena.doubleValue() : 0.0);
            a.setPdv(rs.getInt("pdv"));
            a.setValuta(rs.getString("valuta"));
            a.setOpis(rs.getString("opis"));
            a.setDeklaracija(rs.getString("deklaracija"));
            a.setEnergetskaKlasa(rs.getString("energetska_klasa"));
            a.setEnergetskaKlasaLink(rs.getString("energetska_klasa_link"));
            a.setEnergetskaKlasaPdf(rs.getString("energetska_klasa_pdf"));

            // TEXT[] -> List<String>
            Array slikeArr = rs.getArray("slike");
            if (slikeArr != null) {
                a.setSlike(Arrays.asList((String[]) slikeArr.getArray()));
            }

            // JSONB filteri — deserijalizujemo kao List<Artikal.FilterGrupa>
            String filteriJson = rs.getString("filteri");
            if (filteriJson != null && !filteriJson.equals("[]")) {
                try {
                    a.setFilteri(objectMapper.readValue(filteriJson,
                            new TypeReference<List<Artikal.FilterGrupa>>() {}));
                } catch (Exception ignored) {}
            }

            return a;
        };
    }

    private Array createTextArray(String[] values) {
        try (Connection conn = jdbc.getDataSource().getConnection()) {
            return conn.createArrayOf("text", values);
        } catch (Exception e) {
            throw new RuntimeException("Greška pri kreiranju SQL array", e);
        }
    }

    private BigDecimal toDecimal(Double d) {
        return (d == null || d == 0) ? null : BigDecimal.valueOf(d);
    }
}
