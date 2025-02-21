package com.tehno.tehnozonaspring.repository;

import com.tehno.tehnozonaspring.model.Vendor;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
    FROM artikli
    """, nativeQuery = true)
    List<String> findArtikliByGlavnaGrupaAndNadgrupa(
            @Param("vendorId") Long vendorId,
            @Param("nadgrupe") String[] nadgrupe
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
        SELECT unnest(xpath('/artikli/artikal/proizvodjac/text()', xml_data))::TEXT AS proizvodjac_text,
               unnest(xpath('/artikli/artikal/nadgrupa/text()', xml_data))::TEXT AS nadgrupa_text
    ) AS subquery
    WHERE vendor.id = :vendorId
      AND subquery.nadgrupa_text = ANY(:nadgrupe)
    GROUP BY subquery.proizvodjac_text
    ORDER BY proizvodjac
    """, nativeQuery = true)
    List<Object[]> findProizvodjaciWithCountByGlavnaGrupa(@Param("vendorId") Long vendorId, @Param("nadgrupe") String[] nadgrupe);

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

}
