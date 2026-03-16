DROP VIEW IF EXISTS unified_artikli;
CREATE VIEW unified_artikli AS
-- USPON (ID 1)
SELECT 
    1 as vendor_id,
    COALESCE((xpath('/artikal/sifra/text()', art_xml))[1]::text, '') as sifra,
    COALESCE((xpath('/artikal/barkod/text()', art_xml))[1]::text, '') as barkod,
    COALESCE((xpath('/artikal/naziv/text()', art_xml))[1]::text, '') as naziv,
    CASE 
        WHEN COALESCE(NULLIF((xpath('/artikal/mpcena/text()', art_xml))[1]::text, ''), '0')::numeric > 0 
        THEN COALESCE(NULLIF((xpath('/artikal/mpcena/text()', art_xml))[1]::text, ''), '0')::numeric
        ELSE COALESCE(NULLIF((xpath('/artikal/cena/text()', art_xml))[1]::text, ''), '0')::numeric * 1.2
    END as mpcena,
    UPPER(TRIM(COALESCE((xpath('/artikal/nadgrupa/text()', art_xml))[1]::text, ''))) as nadgrupa,
    UPPER(TRIM(COALESCE((xpath('/artikal/grupa/text()', art_xml))[1]::text, ''))) as grupa,
    UPPER(TRIM(COALESCE((xpath('/artikal/proizvodjac/text()', art_xml))[1]::text, ''))) as proizvodjac,
    -- Reconstruction using XPath to get all children of the root and avoid nesting
    XMLELEMENT(NAME artikal,
        XMLELEMENT(NAME "vendorId", 1),
        XMLELEMENT(NAME mpcena, 
            CASE 
                WHEN COALESCE(NULLIF((xpath('/artikal/mpcena/text()', art_xml))[1]::text, ''), '0')::numeric > 0 
                THEN COALESCE(NULLIF((xpath('/artikal/mpcena/text()', art_xml))[1]::text, ''), '0')::numeric
                ELSE COALESCE(NULLIF((xpath('/artikal/cena/text()', art_xml))[1]::text, ''), '0')::numeric * 1.2
            END
        ),
        xmlconcat(VARIADIC xpath('/*/*', art_xml))
    )::text as original_xml
FROM (SELECT unnest(xpath('/artikli/artikal', xml_data)) as art_xml FROM vendor WHERE id = 1) t1

UNION ALL

-- LINKOM (ID 2)
SELECT 
    2 as vendor_id,
    COALESCE((xpath('/artikal/sifra/text()', art_xml))[1]::text, '') as sifra,
    COALESCE((xpath('/artikal/barkod/text()', art_xml))[1]::text, '') as barkod,
    COALESCE((xpath('/artikal/naziv/text()', art_xml))[1]::text, '') as naziv,
    CASE 
        WHEN COALESCE(NULLIF((xpath('/artikal/mpcena/text()', art_xml))[1]::text, ''), '0')::numeric > 0 
        THEN COALESCE(NULLIF((xpath('/artikal/mpcena/text()', art_xml))[1]::text, ''), '0')::numeric
        ELSE COALESCE(NULLIF((xpath('/artikal/cena/text()', art_xml))[1]::text, ''), '0')::numeric * 1.2
    END as mpcena,
    UPPER(TRIM(COALESCE((xpath('/artikal/nadgrupa/text()', art_xml))[1]::text, ''))) as nadgrupa,
    UPPER(TRIM(COALESCE((xpath('/artikal/grupa/text()', art_xml))[1]::text, ''))) as grupa,
    UPPER(TRIM(COALESCE((xpath('/artikal/proizvodjac/text()', art_xml))[1]::text, ''))) as proizvodjac,
    -- Reconstruction using XPath to get all children of the root and avoid nesting
    XMLELEMENT(NAME artikal,
        XMLELEMENT(NAME "vendorId", 2),
        XMLELEMENT(NAME mpcena, 
            CASE 
                WHEN COALESCE(NULLIF((xpath('/artikal/mpcena/text()', art_xml))[1]::text, ''), '0')::numeric > 0 
                THEN COALESCE(NULLIF((xpath('/artikal/mpcena/text()', art_xml))[1]::text, ''), '0')::numeric
                ELSE COALESCE(NULLIF((xpath('/artikal/cena/text()', art_xml))[1]::text, ''), '0')::numeric * 1.2
            END
        ),
        xmlconcat(VARIADIC xpath('/*/*', art_xml))
    )::text as original_xml
FROM (SELECT unnest(xpath('/artikli/artikal', xml_data)) as art_xml FROM vendor WHERE id = 2) t2

UNION ALL

-- AVTERA (ID 3)
SELECT 
    3 as vendor_id,
    COALESCE((xpath('/Article/ident/text()', art_xml))[1]::text, '') as sifra,
    COALESCE((xpath('/Article/ean1/text()', art_xml))[1]::text, '') as barkod,
    COALESCE((xpath('/Article/title/text()', art_xml))[1]::text, '') as naziv,
    (COALESCE(NULLIF((xpath('/Article/b2cpricewotax/text()', art_xml))[1]::text, ''), '0')::numeric * 1.2) as mpcena,
    COALESCE(UPPER(TRIM(split_part((xpath('/Article/classtitle/text()', art_xml))[1]::text, '\\', 2))), '') as nadgrupa,
    UPPER(TRIM(COALESCE((xpath('/Article/classtitle/text()', art_xml))[1]::text, ''))) as grupa,
    UPPER(TRIM(COALESCE((xpath('/Article/articlebrand/text()', art_xml))[1]::text, ''))) as proizvodjac,
    XMLELEMENT(NAME artikal,
        XMLELEMENT(NAME "vendorId", 3),
        XMLELEMENT(NAME sifra, (xpath('/Article/ident/text()', art_xml))[1]::text),
        XMLELEMENT(NAME barkod, (xpath('/Article/ean1/text()', art_xml))[1]::text),
        XMLELEMENT(NAME naziv, (xpath('/Article/title/text()', art_xml))[1]::text),
        XMLELEMENT(NAME pdv, 20),
        XMLELEMENT(NAME nadgrupa, split_part((xpath('/Article/classtitle/text()', art_xml))[1]::text, '\\', 2)),
        XMLELEMENT(NAME grupa, (xpath('/Article/classtitle/text()', art_xml))[1]::text),
        XMLELEMENT(NAME proizvodjac, (xpath('/Article/articlebrand/text()', art_xml))[1]::text),
        XMLELEMENT(NAME jedinica_mere, (xpath('/Article/unit/text()', art_xml))[1]::text),
        XMLELEMENT(NAME kolicina, (xpath('/Article/stock/text()', art_xml))[1]::text),
        XMLELEMENT(NAME b2bcena, (COALESCE(NULLIF((xpath('/Article/price/text()', art_xml))[1]::text, ''), '0')::numeric)),
        XMLELEMENT(NAME valuta, 'RSD'),
        XMLELEMENT(NAME mpcena, (COALESCE(NULLIF((xpath('/Article/b2cpricewotax/text()', art_xml))[1]::text, ''), '0')::numeric * 1.2)),
        XMLELEMENT(NAME opis, 
            xmlconcat(
                VARIADIC xpath('/Article/longdescription/node() | /Article/description/node() | /Article/opis/node()', art_xml)
            )
        ),
        XMLELEMENT(NAME deklaracija, 
            xmlconcat(
                VARIADIC xpath('/Article/deklaracija/node() | /Article/declaration/node()', art_xml)
            )
        ),
        XMLELEMENT(NAME slike, 
            XMLELEMENT(NAME slika, 
                trim(both ' ' from replace(replace((xpath('/Article/slikaVelika/text()', art_xml))[1]::text, '![CDATA[ ', ''), ' ]]', ''))
            )
        ),
        XMLELEMENT(NAME filteri, 
            (xpath('/Article/TechSpec', art_xml))[1]
        )
    )::text as original_xml
FROM (SELECT unnest(xpath('/xmlData/Article', xml_data)) as art_xml FROM vendor WHERE id = 3) t3;
