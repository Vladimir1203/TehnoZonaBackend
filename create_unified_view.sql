DROP VIEW IF EXISTS unified_artikli;
CREATE VIEW unified_artikli AS
-- USPON (ID 1)
SELECT 
    1 as vendor_id,
    COALESCE((xpath('/artikal/sifra/text()', art_xml))[1]::text, '') as sifra,
    COALESCE((xpath('/artikal/barkod/text()', art_xml))[1]::text, '') as barkod,
    COALESCE((xpath('/artikal/naziv/text()', art_xml))[1]::text, '') as naziv,
    COALESCE(NULLIF((xpath('/artikal/mpcena/text()', art_xml))[1]::text, ''), '0')::numeric as mpcena,
    COALESCE((xpath('/artikal/nadgrupa/text()', art_xml))[1]::text, '') as nadgrupa,
    COALESCE((xpath('/artikal/grupa/text()', art_xml))[1]::text, '') as grupa,
    COALESCE((xpath('/artikal/proizvodjac/text()', art_xml))[1]::text, '') as proizvodjac,
    -- Reconstruct Uspon XML to include vendorId with correct case
    XMLELEMENT(NAME artikal,
        XMLELEMENT(NAME "vendorId", 1),
        (xpath('/artikal/sifra', art_xml))[1],
        (xpath('/artikal/barkod', art_xml))[1],
        (xpath('/artikal/naziv', art_xml))[1],
        (xpath('/artikal/mpcena', art_xml))[1],
        (xpath('/artikal/nadgrupa', art_xml))[1],
        (xpath('/artikal/grupa', art_xml))[1],
        (xpath('/artikal/proizvodjac', art_xml))[1],
        (xpath('/artikal/slike', art_xml))[1]
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
    COALESCE((xpath('/artikal/nadgrupa/text()', art_xml))[1]::text, '') as nadgrupa,
    COALESCE((xpath('/artikal/grupa/text()', art_xml))[1]::text, '') as grupa,
    COALESCE((xpath('/artikal/proizvodjac/text()', art_xml))[1]::text, '') as proizvodjac,
    -- Constructing new XML for Linkom with vendorId with correct case
    XMLELEMENT(NAME artikal,
        XMLELEMENT(NAME "vendorId", 2),
        XMLELEMENT(NAME sifra, (xpath('/artikal/sifra/text()', art_xml))[1]::text),
        XMLELEMENT(NAME barkod, (xpath('/artikal/barkod/text()', art_xml))[1]::text),
        XMLELEMENT(NAME naziv, (xpath('/artikal/naziv/text()', art_xml))[1]::text),
        XMLELEMENT(NAME mpcena, 
            CASE 
                WHEN COALESCE(NULLIF((xpath('/artikal/mpcena/text()', art_xml))[1]::text, ''), '0')::numeric > 0 
                THEN COALESCE(NULLIF((xpath('/artikal/mpcena/text()', art_xml))[1]::text, ''), '0')::numeric
                ELSE COALESCE(NULLIF((xpath('/artikal/cena/text()', art_xml))[1]::text, ''), '0')::numeric * 1.2
            END
        ),
        XMLELEMENT(NAME nadgrupa, (xpath('/artikal/nadgrupa/text()', art_xml))[1]::text),
        XMLELEMENT(NAME grupa, (xpath('/artikal/grupa/text()', art_xml))[1]::text),
        XMLELEMENT(NAME proizvodjac, (xpath('/artikal/proizvodjac/text()', art_xml))[1]::text),
        XMLELEMENT(NAME kolicina, (xpath('/artikal/kolicina/text()', art_xml))[1]::text),
        (xpath('/artikal/slike', art_xml))[1]
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
    COALESCE(split_part((xpath('/Article/classtitle/text()', art_xml))[1]::text, '\\', 2), '') as nadgrupa,
    COALESCE((xpath('/Article/classtitle/text()', art_xml))[1]::text, '') as grupa,
    COALESCE((xpath('/Article/articlebrand/text()', art_xml))[1]::text, '') as proizvodjac,
    -- Constructing new XML for Avtera with vendorId with correct case
    XMLELEMENT(NAME artikal,
        XMLELEMENT(NAME "vendorId", 3),
        XMLELEMENT(NAME sifra, (xpath('/Article/ident/text()', art_xml))[1]::text),
        XMLELEMENT(NAME barkod, (xpath('/Article/ean1/text()', art_xml))[1]::text),
        XMLELEMENT(NAME naziv, (xpath('/Article/title/text()', art_xml))[1]::text),
        XMLELEMENT(NAME mpcena, (COALESCE(NULLIF((xpath('/Article/b2cpricewotax/text()', art_xml))[1]::text, ''), '0')::numeric * 1.2)),
        XMLELEMENT(NAME nadgrupa, split_part((xpath('/Article/classtitle/text()', art_xml))[1]::text, '\\', 2)),
        XMLELEMENT(NAME grupa, (xpath('/Article/classtitle/text()', art_xml))[1]::text),
        XMLELEMENT(NAME proizvodjac, (xpath('/Article/articlebrand/text()', art_xml))[1]::text),
        XMLELEMENT(NAME slike, 
            XMLELEMENT(NAME slika, 
                trim(both ' ' from replace(replace((xpath('/Article/slikaVelika/text()', art_xml))[1]::text, '![CDATA[ ', ''), ' ]]', ''))
            )
        )
    )::text as original_xml
FROM (SELECT unnest(xpath('/xmlData/Article', xml_data)) as art_xml FROM vendor WHERE id = 3) t3;
