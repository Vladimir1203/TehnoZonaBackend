package com.tehno.tehnozonaspring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.annotation.PostConstruct;

@Configuration
public class DatabaseConfig {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        try {
            jdbcTemplate.execute("DROP VIEW IF EXISTS unified_artikli CASCADE;");
        } catch (Exception e) {
            System.err.println("DATABASE INIT: Error dropping view: " + e.getMessage());
        }

        String sql = """
                CREATE OR REPLACE VIEW unified_artikli AS
                    -- USPON (ID 1)
                    SELECT
                        1 as vendor_id,
                        (xpath('//sifra/text()', art_xml))[1]::text as sifra,
                        (xpath('//barkod/text()', art_xml))[1]::text as barkod,
                        (xpath('//naziv/text()', art_xml))[1]::text as naziv,
                        CASE
                            WHEN COALESCE(NULLIF((xpath('//mpcena/text()', art_xml))[1]::text, ''), '0')::numeric > 0
                            THEN COALESCE(NULLIF((xpath('//mpcena/text()', art_xml))[1]::text, ''), '0')::numeric
                            ELSE COALESCE(NULLIF((xpath('//cena/text()', art_xml))[1]::text, ''), '0')::numeric * 1.2
                        END as mpcena,
                        UPPER(TRIM(COALESCE((xpath('//nadgrupa/text()', art_xml))[1]::text, ''))) as nadgrupa,
                        UPPER(TRIM(COALESCE((xpath('//grupa/text()', art_xml))[1]::text, ''))) as grupa,
                        UPPER(TRIM(COALESCE((xpath('//proizvodjac/text()', art_xml))[1]::text, ''))) as proizvodjac,
                        XMLELEMENT(NAME artikal,
                            XMLELEMENT(NAME "vendorId", 1),
                            XMLELEMENT(NAME sifra, (xpath('//sifra/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME barkod, (xpath('//barkod/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME naziv, (xpath('//naziv/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME pdv, (xpath('//pdv/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME nadgrupa, (xpath('//nadgrupa/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME grupa, (xpath('//grupa/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME proizvodjac, (xpath('//proizvodjac/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME jedinica_mere, (xpath('//jedinica_mere/text() | //jm/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME kolicina, (xpath('//kolicina/text() | //stock/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME b2bcena, (xpath('//b2bcena/text() | //cena/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME valuta, (xpath('//valuta/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME mpcena,
                                CASE
                                    WHEN COALESCE(NULLIF((xpath('//mpcena/text()', art_xml))[1]::text, ''), '0')::numeric > 0
                                    THEN COALESCE(NULLIF((xpath('//mpcena/text()', art_xml))[1]::text, ''), '0')::numeric
                                    ELSE COALESCE(NULLIF((xpath('//cena/text()', art_xml))[1]::text, ''), '0')::numeric * 1.2
                                END
                            ),
                            XMLELEMENT(NAME opis,
                                (SELECT xmlagg(n) FROM (SELECT unnest(xpath('//opis/node() | //opis_dugi/node() | //karakteristike/node() | //description/node() | //long_description/node() | //longdescription/node()', art_xml)) as n) sub)
                            ),
                            XMLELEMENT(NAME slike,
                                (SELECT xmlagg(XMLELEMENT(NAME slika, s)) FROM (
                                    SELECT trim(both ' ' from replace(replace(replace(unnest(xpath('//slike//slika/text() | //slika/text() | //picture/text() | //image/text()', art_xml))::text, '![CDATA[ ', ''), ' ]]', ''), ']]', '')) as s
                                ) sub)
                            )
                        )::text as original_xml
                    FROM (SELECT unnest(xpath('/artikli/artikal', xml_data)) as art_xml FROM vendor WHERE id = 1) t1

                    UNION ALL

                    -- LINKOM (ID 2)
                    SELECT
                        2 as vendor_id,
                        (xpath('//sifra/text()', art_xml))[1]::text as sifra,
                        (xpath('//barkod/text()', art_xml))[1]::text as barkod,
                        (xpath('//naziv/text()', art_xml))[1]::text as naziv,
                        CASE
                            WHEN COALESCE(NULLIF((xpath('//mpcena/text()', art_xml))[1]::text, ''), '0')::numeric > 0
                            THEN COALESCE(NULLIF((xpath('//mpcena/text()', art_xml))[1]::text, ''), '0')::numeric
                            ELSE COALESCE(NULLIF((xpath('//cena/text()', art_xml))[1]::text, ''), '0')::numeric * 1.2
                        END as mpcena,
                        UPPER(TRIM(COALESCE((xpath('//nadgrupa/text()', art_xml))[1]::text, ''))) as nadgrupa,
                        UPPER(TRIM(COALESCE((xpath('//grupa/text()', art_xml))[1]::text, ''))) as grupa,
                        UPPER(TRIM(COALESCE((xpath('//proizvodjac/text()', art_xml))[1]::text, ''))) as proizvodjac,
                        XMLELEMENT(NAME artikal,
                            XMLELEMENT(NAME "vendorId", 2),
                            XMLELEMENT(NAME sifra, (xpath('//sifra/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME barkod, (xpath('//barkod/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME naziv, (xpath('//naziv/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME pdv, (xpath('//pdv/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME nadgrupa, (xpath('//nadgrupa/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME grupa, (xpath('//grupa/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME proizvodjac, (xpath('//proizvodjac/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME jedinica_mere, (xpath('//jedinica_mere/text() | //jm/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME kolicina, (xpath('//kolicina/text() | //stock/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME b2bcena, (xpath('//b2bcena/text() | //cena/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME valuta, (xpath('//valuta/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME mpcena,
                                CASE
                                    WHEN COALESCE(NULLIF((xpath('//mpcena/text()', art_xml))[1]::text, ''), '0')::numeric > 0
                                    THEN COALESCE(NULLIF((xpath('//mpcena/text()', art_xml))[1]::text, ''), '0')::numeric
                                    ELSE COALESCE(NULLIF((xpath('//cena/text()', art_xml))[1]::text, ''), '0')::numeric * 1.2
                                END
                            ),
                            XMLELEMENT(NAME opis,
                                (SELECT xmlagg(n) FROM (SELECT unnest(xpath('//opis/node() | //opis_dugi/node() | //karakteristike/node() | //description/node() | //long_description/node() | //longdescription/node()', art_xml)) as n) sub)
                            ),
                            XMLELEMENT(NAME slike,
                                (SELECT xmlagg(XMLELEMENT(NAME slika, s)) FROM (
                                    SELECT trim(both ' ' from replace(replace(replace(unnest(xpath('//slike//slika/text() | //slika/text() | //picture/text() | //image/text()', art_xml))::text, '![CDATA[ ', ''), ' ]]', ''), ']]', '')) as s
                                ) sub)
                            )
                        )::text as original_xml
                    FROM (SELECT unnest(xpath('/artikli/artikal', xml_data)) as art_xml FROM vendor WHERE id = 2) t2

                    UNION ALL

                    -- AVTERA (ID 3)
                    SELECT
                        3 as vendor_id,
                        (xpath('//ident/text()', art_xml))[1]::text as sifra,
                        (xpath('//ean1/text()', art_xml))[1]::text as barkod,
                        (xpath('//title/text()', art_xml))[1]::text as naziv,
                        (COALESCE(NULLIF((xpath('//b2cpricewotax/text()', art_xml))[1]::text, ''), '0')::numeric * 1.2) as mpcena,
                        COALESCE(UPPER(TRIM(split_part((xpath('//classtitle/text()', art_xml))[1]::text, '\\\\', 2))), '') as nadgrupa,
                        UPPER(TRIM(COALESCE((xpath('//classtitle/text()', art_xml))[1]::text, ''))) as grupa,
                        UPPER(TRIM(COALESCE((xpath('//articlebrand/text()', art_xml))[1]::text, ''))) as proizvodjac,
                        XMLELEMENT(NAME artikal,
                            XMLELEMENT(NAME "vendorId", 3),
                            XMLELEMENT(NAME sifra, (xpath('//ident/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME barkod, (xpath('//ean1/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME naziv, (xpath('//title/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME pdv, 20),
                            XMLELEMENT(NAME nadgrupa, (split_part((xpath('//classtitle/text()', art_xml))[1]::text, '\\\\', 2))),
                            XMLELEMENT(NAME grupa, (xpath('//classtitle/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME proizvodjac, (xpath('//articlebrand/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME jedinica_mere, (xpath('//unit/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME kolicina, (xpath('//stock/text()', art_xml))[1]::text),
                            XMLELEMENT(NAME b2bcena, (COALESCE(NULLIF((xpath('//price/text()', art_xml))[1]::text, ''), '0')::numeric)),
                            XMLELEMENT(NAME valuta, 'RSD'),
                            XMLELEMENT(NAME mpcena, (COALESCE(NULLIF((xpath('//b2cpricewotax/text()', art_xml))[1]::text, ''), '0')::numeric * 1.2)),
                            XMLELEMENT(NAME opis,
                                (SELECT xmlagg(n) FROM (SELECT unnest(xpath('//description/node() | //longdescription/node() | //opis/node() | //karakteristike/node()', art_xml)) as n) sub)
                            ),
                            XMLELEMENT(NAME slike,
                                (SELECT xmlagg(XMLELEMENT(NAME slika, s)) FROM (
                                    SELECT trim(both ' ' from replace(replace(replace(unnest(xpath('//slikaVelika/text() | //dodatneSlike//*/text()', art_xml))::text, '![CDATA[ ', ''), ' ]]', ''), ']]', '')) as s
                                ) sub)
                            )
                        )::text as original_xml
                    FROM (SELECT unnest(xpath('/xmlData/Article', xml_data)) as art_xml FROM vendor WHERE id = 3) t3;
                """;

        try {
            jdbcTemplate.execute(sql);
            System.out.println("DATABASE INIT: Unified view uspešno kreiran/ažuran.");
        } catch (Exception e) {
            System.err.println("DATABASE INIT GREŠKA: " + e.getMessage());
        }
    }
}