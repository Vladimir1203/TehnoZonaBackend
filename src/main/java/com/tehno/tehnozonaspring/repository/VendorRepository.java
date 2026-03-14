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
            @Param("nadgrupe") String[] nadgrupe);

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
            @Param("nadgrupa") String nadgrupa);

    @Modifying
    @Transactional
    @Query(value = """
                INSERT INTO vendor (id, name, xml_data)
                VALUES (:id, :name, CAST(:xmlData AS xml))
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name,
                    xml_data = EXCLUDED.xml_data
            """, nativeQuery = true)
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
            @Param("nadgrupe") String[] nadgrupe);

    @Query(value = """
            SELECT
                UPPER(subquery.proizvodjac_text) as proizvodjac,
                COUNT(*) as broj_artikala
            FROM vendor,
            LATERAL (
                SELECT
                    unnest(xpath('/artikli/artikal/proizvodjac/text()', xml_data))::TEXT AS proizvodjac_text,
                    unnest(xpath('/artikli/artikal/nadgrupa/text()', xml_data))::TEXT AS nadgrupa_text,
                    unnest(xpath('/artikli/artikal/mpcena/text()', xml_data))::TEXT AS cena_text,
                    NULLIF(unnest(xpath('/artikli/artikal/mpcena/text()', xml_data))::TEXT, '')::NUMERIC AS cena_mp
                ) AS subquery
            WHERE vendor.id = :vendorId
              AND subquery.nadgrupa_text = ANY(:nadgrupe)
              AND (cena_mp IS NOT NULL)  -- Osigurava da NULL vrednosti ne prave problem
              AND (:minCena IS NULL OR cena_mp >= :minCena)
              AND (:maxCena IS NULL OR cena_mp <= :maxCena)
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
                    WHEN array_length(xpath('/artikli/artikal/mpcena/text()', xml_data), 1) >= 1
                    THEN COALESCE(NULLIF((xpath('/artikli/artikal/mpcena/text()', xml_data))[1]::TEXT, '')::NUMERIC, 0)
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
            @Param("grupa") String grupa);

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
                    unnest(xpath('/artikli/artikal/mpcena/text()', xml_data))::TEXT AS cena_text
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
                SELECT original_xml
                FROM (
                    SELECT DISTINCT ON (barkod) *
                    FROM unified_artikli
                    WHERE barkod IS NOT NULL AND barkod != '' AND mpcena > 0
                    AND nadgrupa = ANY(:nadgrupe)
                    ORDER BY barkod, mpcena ASC
                ) as best_deals
            """, nativeQuery = true)
    List<String> findUnifiedArtikliByGlavnaGrupa(@Param("nadgrupe") String[] nadgrupe);

    @Query(value = """
                SELECT original_xml
                FROM (
                    SELECT DISTINCT ON (barkod) *
                    FROM unified_artikli
                    WHERE barkod IS NOT NULL AND barkod != '' AND mpcena > 0
                    AND nadgrupa = :nadgrupa
                    ORDER BY barkod, mpcena ASC
                ) as best_deals
            """, nativeQuery = true)
    List<String> findUnifiedArtikliByNadgrupa(@Param("nadgrupa") String nadgrupa);

    @Query(value = """
                SELECT original_xml
                FROM unified_artikli
                WHERE UPPER(proizvodjac) = UPPER(:brand)
                ORDER BY mpcena ASC
            """, nativeQuery = true)
    List<String> findUnifiedArtikliByBrand(@Param("brand") String brand);

    @Query(value = """
                SELECT
                    UPPER(proizvodjac) as proizvodjac,
                    COUNT(*) as broj_artikala,
                    array_agg(mpcena) AS cene
                FROM (
                    SELECT DISTINCT ON (barkod) *
                    FROM unified_artikli
                    WHERE nadgrupa = :nadgrupa AND mpcena > 0
                    ORDER BY barkod, mpcena ASC
                ) as best_deals
                GROUP BY proizvodjac
                ORDER BY proizvodjac
            """, nativeQuery = true)
    List<Object[]> findUnifiedProizvodjaciWithCountByNadgrupa(@Param("nadgrupa") String nadgrupa);

    @Query(value = """
                SELECT barkod
                FROM unified_artikli
                WHERE barkod IS NOT NULL AND barkod != ''
                GROUP BY barkod
                HAVING count(DISTINCT vendor_id) > 1
            """, nativeQuery = true)
    List<String> findDuplicateBarkodovi();

    @Query(value = """
                SELECT original_xml
                FROM unified_artikli
                WHERE nadgrupa = :nadgrupa AND grupa = :grupa
                ORDER BY mpcena ASC
            """, nativeQuery = true)
    List<String> findUnifiedArtikliByNadgrupaAndGrupa(@Param("nadgrupa") String nadgrupa, @Param("grupa") String grupa);

    @Query(value = """
                SELECT MAX(mpcena) FROM unified_artikli
            """, nativeQuery = true)
    BigDecimal findUnifiedMaxPrice();

    @Query(value = """
                SELECT DISTINCT UPPER(proizvodjac)
                FROM unified_artikli
                WHERE nadgrupa IN :nadgrupe
                ORDER BY 1
            """, nativeQuery = true)
    List<String> findUnifiedProizvodjaciByGlavnaGrupa(@Param("nadgrupe") List<String> nadgrupe);

    @Query(value = """
                SELECT original_xml
                FROM unified_artikli
                WHERE barkod = :barkod
                ORDER BY mpcena ASC
            """, nativeQuery = true)
    List<String> findArtikliByBarkod(@Param("barkod") String barkod);

    @Query(value = """
                SELECT original_xml
                FROM unified_artikli
                WHERE barkod = :barkod
                ORDER BY mpcena ASC
                LIMIT 1
            """, nativeQuery = true)
    String findUnifiedArtikalByBarkod(@Param("barkod") String barkod);

    @Query(value = """
                SELECT original_xml
                FROM (
                    SELECT DISTINCT ON (barkod) *
                    FROM unified_artikli
                    WHERE barkod IS NOT NULL AND barkod != '' AND mpcena > 0
                    OR ( (barkod IS NULL OR barkod = '') AND mpcena > 0 )
                    ORDER BY barkod, mpcena ASC
                ) as best_deals
            """, nativeQuery = true)
    List<String> findUnifiedArtikliXml();

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
            @Param("barCode") String barCode);

    @Modifying
    @Transactional
    @Query(value = """
                INSERT INTO featured_products
                    (barcode, vendor_id, feature_type, priority, valid_from, valid_to,
                     item_type, subtitle, button_text, button_route, glavna_grupa, nadgrupa, grupa,
                     brand_name, custom_name, custom_image_url)
                VALUES
                    (:barcode, :vendorId, :featureType, :priority, :validFrom, :validTo,
                     :itemType, :subtitle, :buttonText, :buttonRoute, :glavnaGrupa, :nadgrupa, :grupa,
                     :brandName, :customName, :customImageUrl)
            """, nativeQuery = true)
    void insertFeaturedProduct(
            @Param("barcode") String barcode,
            @Param("vendorId") Long vendorId,
            @Param("featureType") String featureType,
            @Param("priority") Integer priority,
            @Param("validFrom") LocalDateTime validFrom,
            @Param("validTo") LocalDateTime validTo,
            @Param("itemType") String itemType,
            @Param("subtitle") String subtitle,
            @Param("buttonText") String buttonText,
            @Param("buttonRoute") String buttonRoute,
            @Param("glavnaGrupa") String glavnaGrupa,
            @Param("nadgrupa") String nadgrupa,
            @Param("grupa") String grupa,
            @Param("brandName") String brandName,
            @Param("customName") String customName,
            @Param("customImageUrl") String customImageUrl);

}
