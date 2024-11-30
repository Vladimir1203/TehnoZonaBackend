package com.tehno.tehnozonaspring.repository;

import com.tehno.tehnozonaspring.model.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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


}
