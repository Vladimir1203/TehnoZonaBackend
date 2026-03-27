package com.tehno.tehnozonaspring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tehno.tehnozonaspring.model.Artikal;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ArticalImportService {

    private final JdbcTemplate jdbcTemplate;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    // Velicina batch-a za bulk INSERT
    private static final int BATCH_SIZE = 500;

    public ArticalImportService(JdbcTemplate jdbcTemplate,
                                EmailService emailService,
                                ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
    }

    /**
     * Glavni entry point — poziva se iz FeedRefreshService nakon uspesnog XML refresh-a.
     * Cita vendor.xml_data iz baze, parsira JAXB-om, i puni artikal tabelu.
     */
    @Transactional
    public void importFromVendor(Long vendorId) {
        try {
            List<String> xmlStrings = fetchArtikalXmlStrings(vendorId);
            if (xmlStrings.isEmpty()) {
                System.out.println("IMPORT: Vendor " + vendorId + " — nema artikala u XML-u.");
                return;
            }

            List<Artikal> artikli = parseArtikli(xmlStrings, vendorId);
            System.out.println("IMPORT: Vendor " + vendorId + " — parsirano " + artikli.size() + " artikala.");

            // Ucitaj sve poznate nadgrupe iz baze jednom
            Map<String, String> mappingCache = loadMappingCache();

            // Skupi nepoznate nadgrupe za email alert
            List<String> nepoznateNadgrupe = new ArrayList<>();

            // Obrisi stare artikle za ovog vendora i ubaci nove
            jdbcTemplate.update("DELETE FROM artikal WHERE vendor_id = ?", vendorId);

            List<Object[]> batch = new ArrayList<>();
            for (Artikal a : artikli) {
                // mpcena > webCena > b2bcena*1.2 (Linkom/Uspon koriste <cena> koji JAXB cita kao b2bcena)
                double mpcena = a.getMpcena() > 0 ? a.getMpcena()
                              : a.getWebCena() > 0 ? a.getWebCena()
                              : a.getB2bcena() > 0 ? a.getB2bcena() * 1.2
                              : 0;
                if (mpcena <= 0) continue; // preskoci bez cene

                String sifra = nullIfBlank(a.getSifra());
                if (sifra == null) continue; // preskoci bez sifre

                String nadgrupa = normalizeText(a.getNadgrupa());
                String grupa = normalizeText(a.getGrupa());
                // Ako nema nadgrupe (npr. Linkom), pokusaj fallback iz grupe
                if (nadgrupa == null && grupa != null) {
                    String[] fallback = GRUPA_TO_NADGRUPA.get(grupa);
                    if (fallback != null) {
                        nadgrupa = fallback[0];
                    }
                }
                String glavnaGrupa = resolveGlavnaGrupa(nadgrupa, mappingCache, nepoznateNadgrupe);

                String barkod = cleanBarkod(a.getBarkod());
                boolean barkodValid = isValidEan(barkod);

                String filteriJson = serializeFilteri(a.getFilteri());
                String[] slikeArray = buildSlikeArray(a.getSlike());

                batch.add(new Object[]{
                        vendorId,
                        sifra,
                        barkod,
                        barkodValid,
                        nadgrupa,
                        grupa,
                        glavnaGrupa,
                        a.getNaziv(),
                        normalizeText(a.getProizvodjac()),
                        nullIfBlank(a.getModel()),
                        nullIfBlank(a.getJedinicaMere()),
                        nullIfBlank(a.getKolicina()),
                        BigDecimal.valueOf(mpcena),
                        a.getB2bcena() > 0 ? BigDecimal.valueOf(a.getB2bcena()) : null,
                        a.getWebCena() > 0 ? BigDecimal.valueOf(a.getWebCena()) : null,
                        a.getPdv(),
                        nullIfBlank(a.getValuta()),
                        nullIfBlank(a.getOpis()),
                        nullIfBlank(a.getDeklaracija()),
                        nullIfBlank(a.getEnergetskaKlasa()),
                        nullIfBlank(a.getEnergetskaKlasaLink()),
                        nullIfBlank(a.getEnergetskaKlasaPdf()),
                        slikeArray,
                        filteriJson
                });

                if (batch.size() >= BATCH_SIZE) {
                    executeBatch(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                executeBatch(batch);
            }

            System.out.println("IMPORT: Vendor " + vendorId + " — import zavrsен uspesno.");

            // Posalji email za nepoznate nadgrupe (deduplicirano)
            if (!nepoznateNadgrupe.isEmpty()) {
                List<String> unique = nepoznateNadgrupe.stream().distinct().sorted().toList();
                alertNepoznateNadgrupe(vendorId, unique);
            }

        } catch (Exception e) {
            System.err.println("IMPORT ERROR vendor " + vendorId + ": " + e.getMessage());
            throw new RuntimeException("Artikal import failed za vendor " + vendorId, e);
        }
    }

    // ------------------------------------------------------------------
    // Pomocne metode
    // ------------------------------------------------------------------

    /**
     * Vraca listu XML stringova za svaki artikal iz vendor.xml_data.
     * Koristi isti XPath pristup kao unified_artikli view, ali na Java strani.
     * Cisti dvostruko kodirane HTML entitete (npr. &amp;#382; → ž) pre vracanja.
     */
    private List<String> fetchArtikalXmlStrings(Long vendorId) {
        String xpathExpr = switch (vendorId.intValue()) {
            case 1, 2 -> "/artikli/artikal";
            case 3    -> "/xmlData/Article";
            case 4    -> "/products/product";
            default   -> "/artikli/artikal";
        };

        String sql = """
                SELECT unnest(xpath(?, xml_data))::text
                FROM vendor
                WHERE id = ?
                """;
        List<String> raw = jdbcTemplate.queryForList(sql, String.class, xpathExpr, vendorId);
        // Linkom sadrzi dvostruko kodirane entitete (&amp;#382; itd) — dekodiramo ih
        return raw.stream().map(this::fixDoubleEncodedEntities).toList();
    }

    /**
     * Konvertuje &amp;#NNN; u odgovarajuci XML karakter &#NNN;
     * kako bi JAXB mogao ispravno da parsira.
     */
    private String fixDoubleEncodedEntities(String xml) {
        if (xml == null) return null;
        // &amp;#NNN; → &#NNN;  (numericke reference)
        return xml.replaceAll("&amp;#(\\d+);", "&#$1;")
                  .replaceAll("&amp;#x([0-9a-fA-F]+);", "&#x$1;");
    }

    /**
     * Parsira listu XML stringova u Artikal objekte koristeci JAXB.
     * Za Avteru (vendor 3) prilagodava classtitle pre parsiranja.
     */
    private List<Artikal> parseArtikli(List<String> xmlStrings, Long vendorId) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(Artikal.class);
        Unmarshaller u = ctx.createUnmarshaller();
        List<Artikal> result = new ArrayList<>();

        for (String xml : xmlStrings) {
            try {
                String prepared = switch (vendorId.intValue()) {
                    case 2 -> prepareLinkomXml(xml);
                    case 3 -> prepareAvteraXml(xml);
                    case 4 -> prepareSpektarXml(xml);
                    default -> xml;
                };
                Artikal a = (Artikal) u.unmarshal(new StringReader(prepared));
                a.setVendorId(vendorId);
                result.add(a);
            } catch (Exception ignored) {
                // los XML element — preskoci
            }
        }
        return result;
    }

    /**
     * Avtera ima razlicit XML format — mapiramo polja na Artikal strukturu
     * pre nego sto JAXB pokusa da parsira.
     * classtitle format: "BREND\NADGRUPA\GRUPA" → nadgrupa = segment[1], grupa = segment[2]
     */
    private String prepareAvteraXml(String xml) {
        // Transformisi Avtera-specificna polja u standardni Artikal format
        // ident → sifra
        xml = xml.replaceAll("<ident>", "<sifra>").replaceAll("</ident>", "</sifra>");
        // ean1 → barkod
        xml = xml.replaceAll("<ean1>", "<barkod>").replaceAll("</ean1>", "</barkod>");
        // title → naziv
        xml = xml.replaceAll("<title>", "<naziv>").replaceAll("</title>", "</naziv>");
        // articlebrand → proizvodjac
        xml = xml.replaceAll("<articlebrand>", "<proizvodjac>").replaceAll("</articlebrand>", "</proizvodjac>");
        // unit → jedinica_mere
        xml = xml.replaceAll("<unit>", "<jedinica_mere>").replaceAll("</unit>", "</jedinica_mere>");
        // stock → kolicina
        xml = xml.replaceAll("<stock>", "<kolicina>").replaceAll("</stock>", "</kolicina>");
        // description/longdescription → opis
        xml = xml.replaceAll("<longdescription>", "<opis>").replaceAll("</longdescription>", "</opis>");
        // b2cpricewotax * 1.2 → mpcena (radimo posle parsiranja, ovde samo mapiramo tag)
        xml = xml.replaceAll("<b2cpricewotax>", "<mpcena_raw>").replaceAll("</b2cpricewotax>", "</mpcena_raw>");

        // Prvo zameni root element pa onda ubaci kategorije unutar njega
        xml = xml.replaceFirst("(?i)<Article[^>]*>", "<artikal>")
                 .replaceFirst("(?i)</Article>", "</artikal>");

        // Slike: slikaVelika + sve slike iz dodatneSlike → <slike><slika>...</slika></slike>
        xml = extractAvteraSlike(xml);

        // Parsiramo classtitle i ubacujemo nadgrupa/grupa tagove unutar <artikal>
        xml = extractAvteraKategorije(xml);

        return xml;
    }

    /**
     * Izvlaci slike iz Avtera XML-a i konvertuje ih u standardni <slike><slika> format.
     * Avtera koristi:
     *   <slikaVelika>![CDATA[ URL ]]</slikaVelika>  — glavna slika
     *   <dodatneSlike/> ili <dodatneSlike><slika1>URL</slika1>...</dodatneSlike> — dodatne
     * URL moze sadrzati &amp; koje treba dekodirati u &.
     */
    private String extractAvteraSlike(String xml) {
        List<String> urls = new ArrayList<>();

        // 1. slikaVelika — sadrzaj moze biti CDATA wrapper ili cist URL
        java.util.regex.Matcher m1 = java.util.regex.Pattern
                .compile("<slikaVelika>(.*?)</slikaVelika>", java.util.regex.Pattern.DOTALL)
                .matcher(xml);
        if (m1.find()) {
            String url = cleanAvteraUrl(m1.group(1));
            if (url != null) urls.add(url);
        }

        // 2. sve slike unutar <dodatneSlike>
        java.util.regex.Matcher mBlok = java.util.regex.Pattern
                .compile("<dodatneSlike>(.*?)</dodatneSlike>", java.util.regex.Pattern.DOTALL)
                .matcher(xml);
        if (mBlok.find()) {
            String blok = mBlok.group(1);
            java.util.regex.Matcher mUrl = java.util.regex.Pattern
                    .compile("<[^/][^>]*>(.*?)</[^>]+>", java.util.regex.Pattern.DOTALL)
                    .matcher(blok);
            while (mUrl.find()) {
                String url = cleanAvteraUrl(mUrl.group(1));
                if (url != null && !urls.contains(url)) urls.add(url);
            }
        }

        if (urls.isEmpty()) return xml;

        StringBuilder slikeTag = new StringBuilder("<slike>");
        for (String url : urls) {
            slikeTag.append("<slika>").append(escapeXml(url)).append("</slika>");
        }
        slikeTag.append("</slike>");

        // Ubaci <slike> blok pre </artikal>
        return xml.replaceFirst("</artikal>\\s*$", slikeTag + "</artikal>");
    }

    /**
     * Cisti Avtera URL od CDATA wrappera i &amp; entiteta.
     * Ulaz: "![CDATA[ http://... &amp;pos=1 ]]" ili "http://..."
     * Izlaz: "http://...&pos=1" ili null ako je prazan/neispravan
     */
    private String cleanAvteraUrl(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        // Ukloni CDATA wrapper ako postoji
        if (s.startsWith("![CDATA[")) {
            s = s.substring(8); // ukloni "![CDATA["
            int end = s.lastIndexOf("]]");
            if (end >= 0) s = s.substring(0, end);
            s = s.trim();
        }
        // Dekoduj &amp; -> &
        s = s.replace("&amp;", "&");
        return s.isBlank() || !s.startsWith("http") ? null : s;
    }

    /**
     * Format: "BREND\NADGRUPA\GRUPA" ili "NADGRUPA\GRUPA"
     */
    private String extractAvteraKategorije(String xml) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("<classtitle>([^<]*)</classtitle>")
                .matcher(xml);

        String nadgrupa = "";
        String grupa = "";

        if (m.find()) {
            String classtitle = m.group(1).trim();
            String[] parts = classtitle.split("\\\\");

            if (parts.length >= 3) {
                nadgrupa = parts[1].trim().toUpperCase();
                grupa = parts[parts.length - 1].trim().toUpperCase();
            } else if (parts.length == 2) {
                nadgrupa = parts[0].trim().toUpperCase();
                grupa = parts[1].trim().toUpperCase();
            } else if (parts.length == 1) {
                nadgrupa = parts[0].trim().toUpperCase();
                grupa = nadgrupa;
            }
        }

        // Ubaci mpcena iz mpcena_raw sa *1.2 multiplikatorom
        xml = applyAvteraPriceFactor(xml);

        // Ubaci nadgrupa i grupa tagove UNUTAR root elementa (pre </artikal>)
        String nadgrupaTag = "<nadgrupa>" + escapeXml(nadgrupa) + "</nadgrupa>";
        String grupaTag    = "<grupa>"    + escapeXml(grupa)    + "</grupa>";
        xml = xml.replaceFirst("</artikal>\\s*$", nadgrupaTag + grupaTag + "</artikal>");

        return xml;
    }

    /**
     * Za Avteru: mpcena = b2cpricewotax * 1.2
     */
    private String applyAvteraPriceFactor(String xml) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("<mpcena_raw>([^<]*)</mpcena_raw>")
                .matcher(xml);
        if (!m.find()) return xml;

        try {
            double raw = Double.parseDouble(m.group(1).trim());
            double mpcena = raw * 1.2;
            xml = xml.replace(m.group(0), "<mpcena>" + mpcena + "</mpcena>");
        } catch (NumberFormatException ignored) {
            xml = xml.replace(m.group(0), "<mpcena>0</mpcena>");
        }
        return xml;
    }

    /**
     * Transformise Spektar XML format u standardni Artikal format.
     * Spektar koristi CDATA sekcije i drugacija imena polja.
     */
    private String prepareSpektarXml(String xml) {
        xml = xml.replace("<![CDATA[", "").replace("]]>", "").replace("]]", "");

        xml = xml.replaceAll("<code>", "<sifra>").replaceAll("</code>", "</sifra>");
        xml = xml.replaceAll("<ean>", "<barkod>").replaceAll("</ean>", "</barkod>");
        xml = xml.replaceAll("<name>", "<naziv>").replaceAll("</name>", "</naziv>");
        xml = xml.replaceAll("<manufacturer>", "<proizvodjac>").replaceAll("</manufacturer>", "</proizvodjac>");
        xml = xml.replaceAll("<price>", "<mpcena>").replaceAll("</price>", "</mpcena>");
        xml = xml.replaceAll("<declaration>", "<deklaracija>").replaceAll("</declaration>", "</deklaracija>");
        xml = xml.replaceAll("<stock>", "<kolicina>").replaceAll("</stock>", "</kolicina>");
        xml = xml.replaceAll("<currency>", "<valuta>").replaceAll("</currency>", "</valuta>");
        xml = xml.replaceAll("<category>", "<nadgrupa>").replaceAll("</category>", "</nadgrupa>");

        if (!xml.contains("<pdv>")) {
            xml = xml.replace("</sifra>", "</sifra><pdv>20</pdv>");
        }

        xml = xml.replaceAll("<images>", "<slike>").replaceAll("</images>", "</slike>");
        xml = xml.replaceAll("<image>", "<slika>").replaceAll("</image>", "</slika>");

        xml = xml.replaceFirst("(?i)<product[^>]*>", "<artikal>")
                 .replaceFirst("(?i)</product>", "</artikal>");
        return xml;
    }

    /**
     * Linkom XML je isti format kao Uspon (<artikal> root) ali koristi <cena>
     * umesto <mpcena>. Ako je mpcena=0 a cena>0, konvertujemo cena*1.2 u mpcena.
     */
    private String prepareLinkomXml(String xml) {
        java.util.regex.Matcher cenaMatcher = java.util.regex.Pattern
                .compile("<cena>([^<]+)</cena>")
                .matcher(xml);
        java.util.regex.Matcher mpcenaMatcher = java.util.regex.Pattern
                .compile("<mpcena>([^<]*)</mpcena>")
                .matcher(xml);

        if (cenaMatcher.find() && mpcenaMatcher.find()) {
            try {
                double cena = Double.parseDouble(cenaMatcher.group(1).trim());
                double mpcena = Double.parseDouble(mpcenaMatcher.group(1).trim());
                if (mpcena <= 0 && cena > 0) {
                    xml = xml.replace(mpcenaMatcher.group(0),
                            "<mpcena>" + (cena * 1.2) + "</mpcena>");
                }
            } catch (NumberFormatException ignored) {}
        }
        return xml;
    }

    /**
     * Ucitava ceo glavna_grupa_mapping iz baze jednom — keširamo u Map za O(1) lookup.
     */
    private Map<String, String> loadMappingCache() {
        return jdbcTemplate.query(
                "SELECT nadgrupa, glavna_grupa FROM glavna_grupa_mapping WHERE confirmed = true",
                rs -> {
                    Map<String, String> map = new java.util.HashMap<>();
                    while (rs.next()) {
                        map.put(rs.getString("nadgrupa"), rs.getString("glavna_grupa"));
                    }
                    return map;
                }
        );
    }

    // Fallback: kada artikal nema nadgrupu (npr. Linkom), koristimo grupu za odredjivanje glavne grupe
    private static final Map<String, String[]> GRUPA_TO_NADGRUPA = Map.ofEntries(
        Map.entry("AUDIO / VIDEO KABLOVI",          new String[]{"KABLOVI I ADAPTERI",    "BATERIJE, PUNJAČI I KABLOVI"}),
        Map.entry("S-ATA KABL. / KONTROLERI",       new String[]{"KABLOVI I ADAPTERI",    "BATERIJE, PUNJAČI I KABLOVI"}),
        Map.entry("PRODUŽNI KABLOVI",               new String[]{"KABLOVI I ADAPTERI",    "BATERIJE, PUNJAČI I KABLOVI"}),
        Map.entry("NAPONSKI KABLOVI",               new String[]{"KABLOVI I ADAPTERI",    "BATERIJE, PUNJAČI I KABLOVI"}),
        Map.entry("NOSAČI ZA TELEVIZORE",           new String[]{"TV, AUDIO, VIDEO",      "TV, FOTO, AUDIO I VIDEO"}),
        Map.entry("MALI KUCNI APARATI",             new String[]{"MALI KUĆNI APARATI",    "BELA TEHNIKA I KUĆNI APARATI"}),
        Map.entry("KFD - PUNJACI ZA LAPTOP",        new String[]{"BATERIJE I PUNJAČI",    "BATERIJE, PUNJAČI I KABLOVI"}),
        Map.entry("ŠTAMPAČI, SKENERI I FOTOKOPIRI", new String[]{"ŠTAMPAČI",              "KANCELARIJSKI I ŠKOLSKI MATERIJAL"}),
        Map.entry("KANCELARIJSKI MATERIJAL",        new String[]{"KANCELARIJSKI MATERIJAL","KANCELARIJSKI I ŠKOLSKI MATERIJAL"}),
        Map.entry("RAČUNARSKE PERIFERIJE",          new String[]{"RAČUNARSKE PERIFERIJE", "RAČUNARI, KOMPONENTE I GAMING"}),
        Map.entry("SVE ZA MREŽU",                   new String[]{"MREŽNA OPREMA",         "TELEFONI, TABLETI I OPREMA"}),
        Map.entry("MREŽNA OPREMA",                  new String[]{"MREŽNA OPREMA",         "TELEFONI, TABLETI I OPREMA"})
    );

    /**
     * Trazi glavnu grupu za datu nadgrupu.
     * Ako ne nadje → stavlja u OSTALO i dodaje u listu za email alert.
     * Takodje insertuje nepoznatu nadgrupu u mapping tabelu sa confirmed=false.
     */
    private String resolveGlavnaGrupa(String nadgrupa,
                                       Map<String, String> cache,
                                       List<String> nepoznate) {
        if (nadgrupa == null || nadgrupa.isBlank()) return "OSTALO I OUTLET";

        String glavna = cache.get(nadgrupa);
        if (glavna != null) return glavna;

        // Nepoznata — ubaci u tabelu ako vec nije
        jdbcTemplate.update("""
                INSERT INTO glavna_grupa_mapping (nadgrupa, glavna_grupa, confirmed)
                VALUES (?, 'OSTALO I OUTLET', false)
                ON CONFLICT (nadgrupa) DO NOTHING
                """, nadgrupa);

        // Azuriraj cache da ne bi radili INSERT svaki put za isti artikal
        cache.put(nadgrupa, "OSTALO I OUTLET");
        nepoznate.add(nadgrupa);

        return "OSTALO I OUTLET";
    }

    /**
     * Validira EAN barcode — proverava checksum po GS1 standardu.
     * Podrzava EAN-13, EAN-8, UPC-A (12 cifara).
     */
    static boolean isValidEan(String barkod) {
        if (barkod == null || barkod.isBlank()) return false;
        if (!barkod.matches("\\d{8,13}")) return false;

        int len = barkod.length();
        int sum = 0;
        for (int i = 0; i < len - 1; i++) {
            int digit = barkod.charAt(i) - '0';
            sum += (i % 2 == (len % 2 == 0 ? 0 : 1)) ? digit * 3 : digit;
        }
        int checkDigit = (10 - (sum % 10)) % 10;
        return checkDigit == (barkod.charAt(len - 1) - '0');
    }

    /**
     * Cisti barkod od CDATA ostataka i whitespace-a.
     */
    private String cleanBarkod(String barkod) {
        if (barkod == null) return null;
        String cleaned = barkod
                .replace("<![CDATA[", "")
                .replace("]]>", "")
                .replace("]]", "")
                .trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    /**
     * UPPER + TRIM normalizacija — ista kao u unified_artikli view-u.
     */
    private String normalizeText(String s) {
        if (s == null) return null;
        String result = s.trim().toUpperCase();
        return result.isBlank() ? null : result;
    }

    private String nullIfBlank(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim();
    }

    private String escapeXml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Serijalizuje listu FilterGrupa objekata u JSON za JSONB kolonu.
     */
    private String serializeFilteri(List<Artikal.FilterGrupa> filteri) {
        if (filteri == null || filteri.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(filteri);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * Konvertuje listu slika u String[] za PostgreSQL TEXT[] kolonu.
     */
    private String[] buildSlikeArray(List<String> slike) {
        if (slike == null || slike.isEmpty()) return new String[0];
        return slike.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .toArray(String[]::new);
    }

    /**
     * Izvrsava batch INSERT u artikal tabelu.
     */
    private void executeBatch(List<Object[]> batch) {
        jdbcTemplate.batchUpdate("""
                INSERT INTO artikal (
                    vendor_id, vendor_sifra, barkod, barkod_valid,
                    nadgrupa, grupa, glavna_grupa,
                    naziv, proizvodjac, model, jedinica_mere, kolicina,
                    mpcena, b2bcena, web_cena, pdv, valuta,
                    opis, deklaracija,
                    energetska_klasa, energetska_klasa_link, energetska_klasa_pdf,
                    slike, filteri
                ) VALUES (
                    ?, ?, ?, ?,
                    ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?,
                    ?, ?, ?,
                    ?, ?::jsonb
                )
                ON CONFLICT (vendor_id, vendor_sifra) DO UPDATE SET
                    barkod           = EXCLUDED.barkod,
                    barkod_valid     = EXCLUDED.barkod_valid,
                    nadgrupa         = EXCLUDED.nadgrupa,
                    grupa            = EXCLUDED.grupa,
                    glavna_grupa     = EXCLUDED.glavna_grupa,
                    naziv            = EXCLUDED.naziv,
                    proizvodjac      = EXCLUDED.proizvodjac,
                    model            = EXCLUDED.model,
                    jedinica_mere    = EXCLUDED.jedinica_mere,
                    kolicina         = EXCLUDED.kolicina,
                    mpcena           = EXCLUDED.mpcena,
                    b2bcena          = EXCLUDED.b2bcena,
                    web_cena         = EXCLUDED.web_cena,
                    pdv              = EXCLUDED.pdv,
                    valuta           = EXCLUDED.valuta,
                    opis             = EXCLUDED.opis,
                    deklaracija      = EXCLUDED.deklaracija,
                    energetska_klasa      = EXCLUDED.energetska_klasa,
                    energetska_klasa_link = EXCLUDED.energetska_klasa_link,
                    energetska_klasa_pdf  = EXCLUDED.energetska_klasa_pdf,
                    slike            = EXCLUDED.slike,
                    filteri          = EXCLUDED.filteri,
                    updated_at       = now()
                """, batch);
    }

    /**
     * Salje email alert za nadgrupe koje nisu prepoznate u mappingu.
     */
    private void alertNepoznateNadgrupe(Long vendorId, List<String> nadgrupe) {
        String subject = "TehnoZona: Nepoznate nadgrupe — vendor " + vendorId;
        StringBuilder body = new StringBuilder();
        body.append("<h3>Pronadjene nepoznate nadgrupe pri importu (vendor ").append(vendorId).append(")</h3>");
        body.append("<p>Sledece nadgrupe nisu prepoznate i smestene su u <b>OSTALO I OUTLET</b>.</p>");
        body.append("<p>Molimo azurirajte <code>glavna_grupa_mapping</code> tabelu:</p><ul>");
        for (String n : nadgrupe) {
            body.append("<li>").append(n).append("</li>");
        }
        body.append("</ul>");
        try {
            emailService.sendErrorNotification(subject, body.toString());
        } catch (Exception e) {
            System.err.println("IMPORT: Nije mogao da posalje email za nepoznate nadgrupe: " + e.getMessage());
        }
    }
}
