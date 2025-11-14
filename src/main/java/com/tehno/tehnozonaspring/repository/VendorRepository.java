package com.tehno.tehnozonaspring.repository;

import com.tehno.tehnozonaspring.model.Artikal;
import com.tehno.tehnozonaspring.model.FeaturedProduct;
import com.tehno.tehnozonaspring.model.Vendor;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {
    @Query(value = """
    SELECT array_to_string(xpath('/artikli/artikal[grupa="VIDEO KABLOVI"]/sifra/text()', xml_data), ', ')
    FROM vendor
    WHERE id = :id
""", nativeQuery = true)
    String findArtikliNaziviByGrupa(@Param("id") Long id);

    @Query(value = """

            SELECT array_to_string(array_agg(DISTINCT nadgrupa_text), ', ') AS nadgrupe
        FROM vendor,
        LATERAL (SELECT unnest(xpath('/artikli/artikal/nadgrupa/text()', xml_data))::TEXT AS nadgrupa_text) AS subquery
        WHERE id = :id
        """, nativeQuery = true)
    String findDistinctNadgrupeById(@Param("id") Long id);

    @Query(value = """
        SELECT array_to_string(array_agg(DISTINCT grupa_text), ', ') AS grupe 
        FROM vendor,
             LATERAL (
                SELECT unnest(
                           xpath(
                               concat('/artikli/artikal[nadgrupa/text()="', :nadgrupa, '"]/grupa/text()'), 
                               xml_data
                           )
                       )::TEXT AS grupa_text
             ) AS subquery
        WHERE id = :id
        """, nativeQuery = true)
    String findDistinctGroupsByNadgrupa(@Param("id") Long id, @Param("nadgrupa") String nadgrupa);

    @Query(value = """
    SELECT unnest(xpath('/artikli/artikal', xml_data))::TEXT AS artikal_xml
    FROM vendor
    WHERE id = :id
    LIMIT :limit
""", nativeQuery = true)
    List<String> findLimitedArtikliByVendorId(@Param("id") Long id, @Param("limit") int limit);

    @Query(value = """
    SELECT unnest(xpath(concat('/artikli/artikal[nadgrupa/text()="', :nadgrupa, '"]'), xml_data))::TEXT AS artikal_xml
    FROM vendor
    WHERE id = :id
""", nativeQuery = true)
    List<String> findArtikliByNadgrupa(@Param("id") Long id, @Param("nadgrupa") String nadgrupa);


    @Query(value = """
    SELECT DISTINCT grupa_text
    FROM vendor,
         LATERAL (
            SELECT unnest(
                       xpath('/artikli/artikal/grupa/text()', xml_data)
                   )::TEXT AS grupa_text
         ) AS subquery
""", nativeQuery = true)
    List<String> findAllGroups();

    @Query(value = """
    WITH artikli AS (
        SELECT 
            unnest(
                xpath(
                    concat(
                        '/artikli/artikal[', 
                        string_agg(concat('nadgrupa/text()="', nadgrupa, '"'), ' or '), 
                        ']'
                    ),
                    xml_data
                )
            )::TEXT AS artikal_xml
        FROM vendor,
        unnest(:nadgrupe) AS nadgrupa
        WHERE vendor.id = :vendorId
        GROUP BY vendor.id
    )
    SELECT DISTINCT artikal_xml
    FROM artikli;
    """, nativeQuery = true)
    List<String> findArtikliByGlavnaGrupa(
            @Param("vendorId") Long vendorId,
            @Param("nadgrupe") String[] nadgrupe
    );

    @Query(value = """
    SELECT unnest(
               xpath(
                   concat(
                       '/artikli/artikal[normalize-space(nadgrupa/text())="', :nadgrupa, '"]'
                   ),
                   xml_data
               )
           )::TEXT AS artikal_xml
    FROM vendor
    WHERE vendor.id = :vendorId
    """, nativeQuery = true)
    List<String> findArtikliByNadgrupaAndVendorId(
            @Param("vendorId") Long vendorId,
            @Param("nadgrupa") String nadgrupa
    );


    @Modifying
    @Transactional
    @Query(value = "INSERT INTO vendor (id, name, xml_data) VALUES (:id, :name, CAST(:xmlData AS xml))", nativeQuery = true)
    void insertVendor(@Param("id") Long id, @Param("name") String name, @Param("xmlData") String xmlData);

    @Query(value = "SELECT id FROM vendor", nativeQuery = true)
    List<Long> vratiSveIdjeve();

    // vrati sve proizvodjace za odredjenu glavnu grupu
    @Query(value = """
    WITH artikli AS (
        SELECT 
            unnest(
                xpath(
                    concat(
                        '/artikli/artikal[',
                        string_agg(concat('nadgrupa/text()="', nadgrupa, '"'), ' or '),
                        ']/proizvodjac/text()'
                    ),
                    xml_data
                )
            )::TEXT AS proizvodjac_text
        FROM vendor,
             unnest(:nadgrupe) AS nadgrupa
        WHERE vendor.id = :vendorId
        GROUP BY vendor.id
    )
    SELECT DISTINCT proizvodjac_text
    FROM artikli
    """, nativeQuery = true)
    List<String> findProizvodjaciByGlavnaGrupa(
            @Param("vendorId") Long vendorId,
            @Param("nadgrupe") String[] nadgrupe
    );

    @Query(value = """
    SELECT 
        UPPER(subquery.proizvodjac_text) as proizvodjac,
        COUNT(*) as broj_artikala
    FROM vendor,
    LATERAL (
        SELECT 
            unnest(xpath('/artikli/artikal/proizvodjac/text()', xml_data))::TEXT AS proizvodjac_text,
            unnest(xpath('/artikli/artikal/nadgrupa/text()', xml_data))::TEXT AS nadgrupa_text,
            NULLIF(unnest(xpath('/artikli/artikal/b2bcena/text()', xml_data))::TEXT, '')::NUMERIC AS cena_b2b
        ) AS subquery
    WHERE vendor.id = :vendorId
      AND subquery.nadgrupa_text = ANY(:nadgrupe)
      AND (cena_b2b IS NOT NULL)  -- Osigurava da NULL vrednosti ne prave problem
      AND (:minCena IS NULL OR cena_b2b >= :minCena)
      AND (:maxCena IS NULL OR cena_b2b <= :maxCena)
    GROUP BY subquery.proizvodjac_text
    ORDER BY proizvodjac
    """, nativeQuery = true)
    List<Object[]> findProizvodjaciWithCountByGlavnaGrupa(
            @Param("vendorId") Long vendorId,
            @Param("nadgrupe") String[] nadgrupe,
            @Param("minCena") Integer minCena,
            @Param("maxCena") Integer maxCena);



    @Query(value = """
        SELECT MAX(
            CASE 
                WHEN array_length(xpath('/artikli/artikal/b2bcena/text()', xml_data), 1) >= 1 
                THEN COALESCE(NULLIF((xpath('/artikli/artikal/b2bcena/text()', xml_data))[1]::TEXT, '')::NUMERIC, 0) 
                ELSE 0 
            END
        ) AS max_price
        FROM vendor 
        WHERE vendor.id = :vendorId
        """, nativeQuery = true)
    BigDecimal findMaxPriceByVendorId(@Param("vendorId") Long vendorId);

    @Query(value = """
    SELECT DISTINCT UPPER(subquery.proizvodjac_text) AS proizvodjac
    FROM vendor,
    LATERAL (
        SELECT unnest(xpath('/artikli/artikal/proizvodjac/text()', xml_data))::TEXT AS proizvodjac_text
    ) AS subquery
    ORDER BY proizvodjac
""", nativeQuery = true)
    List<String> findAllDistinctProizvodjaci();

    @Query(value = """
    SELECT unnest(xpath(
        concat('/artikli/artikal[proizvodjac/text()="', :proizvodjac, '"]'), xml_data
    ))::TEXT AS artikal_xml
    FROM vendor
    WHERE xml_data IS NOT NULL
""", nativeQuery = true)
    List<String> findArtikliByProizvodjac(@Param("proizvodjac") String proizvodjac);

    @Query(value = """
    SELECT unnest(xpath('/artikli/artikal', xml_data))::TEXT AS artikal_xml
    FROM vendor
    WHERE id = :vendorId
      AND EXISTS (
          SELECT 1
          FROM unnest(xpath('/artikli/artikal/grupa/text()', xml_data)) AS grupa_text
          WHERE grupa_text::TEXT = :grupa
      )
""", nativeQuery = true)
    List<String> findArtikliByVendorAndGrupa(
            @Param("vendorId") Long vendorId,
            @Param("grupa") String grupa
    );

    @Query(value = """
    WITH artikli AS (
        SELECT 
            subquery.proizvodjac_text AS proizvodjac,
            subquery.nadgrupa_text AS nadgrupa,
            cena_text::NUMERIC AS cena
        FROM vendor,
        LATERAL (
            SELECT 
                unnest(xpath('/artikli/artikal/proizvodjac/text()', xml_data))::TEXT AS proizvodjac_text,
                unnest(xpath('/artikli/artikal/nadgrupa/text()', xml_data))::TEXT AS nadgrupa_text,
                unnest(xpath('/artikli/artikal/b2bcena/text()', xml_data))::TEXT AS cena_text
        ) AS subquery
        WHERE vendor.id = :vendorId
          AND subquery.nadgrupa_text = :nadgrupa
    )
    SELECT 
        UPPER(proizvodjac) as proizvodjac,
        COUNT(*) as broj_artikala,
        array_agg(cena) AS cene
    FROM artikli
    GROUP BY proizvodjac
    ORDER BY proizvodjac
    """, nativeQuery = true)
    List<Object[]> findProizvodjaciWithCountByGlavnaGrupaAndNadgrupa(
            @Param("vendorId") Long vendorId,
            @Param("nadgrupa") String nadgrupa);

    @Query(value = """
    SELECT unnest(xpath('/artikli/artikal', xml_data))::TEXT AS artikal_xml
    FROM vendor
    WHERE id = :vendorId
""", nativeQuery = true)
    List<String> findAllArtikliXmlByVendorId(@Param("vendorId") Long vendorId);


    @Query(value = """
    SELECT unnest(
               xpath(
                   concat('/artikli/artikal[barkod/text()="', :barCode, '"]'),
                   xml_data
               )
           )::TEXT AS artikal_xml
    FROM vendor
    WHERE id = :vendorId
""", nativeQuery = true)
    List<String> getProductByArtikalBarCodeRaw(
            @Param("vendorId") Long vendorId,
            @Param("barCode") String barCode
    );

    @Modifying
    @Transactional
    @Query(value = """
    INSERT INTO featured_products 
        (barcode, vendor_id, feature_type, priority, valid_from, valid_to)
    VALUES 
        (:barcode, :vendorId, :featureType, :priority, :validFrom, :validTo)
""", nativeQuery = true)
    void insertFeaturedProduct(
            @Param("barcode") String barcode,
            @Param("vendorId") Long vendorId,
            @Param("featureType") String featureType,  // ovo je tvoj 'name' parametar
            @Param("priority") Integer priority,
            @Param("validFrom") LocalDateTime validFrom,
            @Param("validTo") LocalDateTime validTo
    );

}
