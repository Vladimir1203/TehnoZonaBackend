-- ============================================================
-- TEHNOZONA MIGRACIJA V2
-- Normalizovana artikal tabela + glavna_grupa_mapping
-- Pokretati JEDNOM rucno pre deploy-a Faze 2
-- ============================================================

-- ------------------------------------------------------------
-- 1. GLAVNA_GRUPA_MAPPING
--    Mapira nadgrupa → glavna_grupa
--    confirmed=false → ide u OSTALO, ceka rucno mapiranje
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS glavna_grupa_mapping (
    nadgrupa     TEXT PRIMARY KEY,
    glavna_grupa TEXT NOT NULL,
    confirmed    BOOLEAN NOT NULL DEFAULT true
);

-- Seed sa postojecim mappingom iz VendorService.groupMap
INSERT INTO glavna_grupa_mapping (nadgrupa, glavna_grupa) VALUES
    -- BELA TEHNIKA
    ('BELA TEHNIKA',                    'BELA TEHNIKA I KUĆNI APARATI'),
    ('MALI KUĆNI APARATI',              'BELA TEHNIKA I KUĆNI APARATI'),
    ('GREJANJE',                        'BELA TEHNIKA I KUĆNI APARATI'),
    ('HLADNJACI',                       'BELA TEHNIKA I KUĆNI APARATI'),
    ('KUĆNA BELA TEHNIKA',              'BELA TEHNIKA I KUĆNI APARATI'),
    ('MALI KUHINJSKI APARATI',          'BELA TEHNIKA I KUĆNI APARATI'),
    ('KLIMATIZACIJA I GREJANJE',        'BELA TEHNIKA I KUĆNI APARATI'),

    -- TV, FOTO, AUDIO
    ('AUDIO, HI-FI',                    'TV, FOTO, AUDIO I VIDEO'),
    ('TV, AUDIO, VIDEO',                'TV, FOTO, AUDIO I VIDEO'),
    ('FOTOAPARATI I KAMERE',            'TV, FOTO, AUDIO I VIDEO'),
    ('DIGITALNI SNIMAČI',               'TV, FOTO, AUDIO I VIDEO'),
    ('PROJEKTORI I OPREMA',             'TV, FOTO, AUDIO I VIDEO'),
    ('ZVUČNICI',                        'TV, FOTO, AUDIO I VIDEO'),
    ('SLUŠALICE I MIKROFONI',           'TV, FOTO, AUDIO I VIDEO'),
    ('KAMERE',                          'TV, FOTO, AUDIO I VIDEO'),
    ('TELEVIZORI',                      'TV, FOTO, AUDIO I VIDEO'),
    ('COMMERCIAL TV',                   'TV, FOTO, AUDIO I VIDEO'),
    ('HOTEL TV',                        'TV, FOTO, AUDIO I VIDEO'),
    ('AUDIO-VIDEO',                     'TV, FOTO, AUDIO I VIDEO'),

    -- RAČUNARI
    ('LAPTOP I TABLET RAČUNARI',        'RAČUNARI, KOMPONENTE I GAMING'),
    ('DESKTOP RAČUNARI',                'RAČUNARI, KOMPONENTE I GAMING'),
    ('SERVERI',                         'RAČUNARI, KOMPONENTE I GAMING'),
    ('PROCESORI',                       'RAČUNARI, KOMPONENTE I GAMING'),
    ('MATIČNE PLOČE',                   'RAČUNARI, KOMPONENTE I GAMING'),
    ('MEMORIJE',                        'RAČUNARI, KOMPONENTE I GAMING'),
    ('HARD DISKOVI',                    'RAČUNARI, KOMPONENTE I GAMING'),
    ('HDD RACK',                        'RAČUNARI, KOMPONENTE I GAMING'),
    ('GRAFIČKE KARTE',                  'RAČUNARI, KOMPONENTE I GAMING'),
    ('GAMING',                          'RAČUNARI, KOMPONENTE I GAMING'),
    ('RAČUNARI',                        'RAČUNARI, KOMPONENTE I GAMING'),
    ('RAČUNARSKE KOMPONENTE',           'RAČUNARI, KOMPONENTE I GAMING'),
    ('RAČUNARSKE PERIFERIJE',           'RAČUNARI, KOMPONENTE I GAMING'),
    ('PC KOZMETIKA',                    'RAČUNARI, KOMPONENTE I GAMING'),
    ('SOFTWARE',                        'RAČUNARI, KOMPONENTE I GAMING'),
    ('MICROSOFT',                       'RAČUNARI, KOMPONENTE I GAMING'),
    ('WIRELESS',                        'RAČUNARI, KOMPONENTE I GAMING'),
    ('OPTIČKI UREĐAJI',                 'RAČUNARI, KOMPONENTE I GAMING'),
    ('ČITAČI KARTICA',                  'RAČUNARI, KOMPONENTE I GAMING'),
    ('REKOVI I OPREMA',                 'RAČUNARI, KOMPONENTE I GAMING'),
    ('TASTATURE',                       'RAČUNARI, KOMPONENTE I GAMING'),
    ('FIBER',                           'RAČUNARI, KOMPONENTE I GAMING'),
    ('SSD DISKOVI',                     'RAČUNARI, KOMPONENTE I GAMING'),
    ('HDD DISKOVI',                     'RAČUNARI, KOMPONENTE I GAMING'),
    ('MONITORI',                        'RAČUNARI, KOMPONENTE I GAMING'),
    ('PC DODACI',                       'RAČUNARI, KOMPONENTE I GAMING'),
    ('GAMING DODACI',                   'RAČUNARI, KOMPONENTE I GAMING'),

    -- TELEFONI
    ('MOBILNI I FIKSNI TELEFONI',       'TELEFONI, TABLETI I OPREMA'),
    ('OPREMA ZA MOBILNE TELEFONE',      'TELEFONI, TABLETI I OPREMA'),
    ('OPREMA ZA LAPTOPOVE',             'TELEFONI, TABLETI I OPREMA'),
    ('OPREMA ZA TABLETE',               'TELEFONI, TABLETI I OPREMA'),
    ('OPREMA ZA TV',                    'TELEFONI, TABLETI I OPREMA'),
    ('MEMORIJSKE KARTICE I ČITAČI',     'TELEFONI, TABLETI I OPREMA'),
    ('USB FLASH I HDD',                 'TELEFONI, TABLETI I OPREMA'),
    ('USB KABLOVI',                     'TELEFONI, TABLETI I OPREMA'),
    ('USB ADAPTERI',                    'TELEFONI, TABLETI I OPREMA'),
    ('MREŽNA OPREMA',                   'TELEFONI, TABLETI I OPREMA'),
    ('FIKSNI TELEFONI',                 'TELEFONI, TABLETI I OPREMA'),
    ('USB FLASH',                       'TELEFONI, TABLETI I OPREMA'),
    ('MEMORIJSKA KARTICA',              'TELEFONI, TABLETI I OPREMA'),
    ('MOBILE DODACI',                   'TELEFONI, TABLETI I OPREMA'),
    ('PAMETNI UREĐAJI',                 'TELEFONI, TABLETI I OPREMA'),
    ('EXTERNI SSD',                     'TELEFONI, TABLETI I OPREMA'),

    -- SIGURNOST
    ('ALARMNI SISTEMI',                 'SIGURNOSNI I ALARMNI SISTEMI'),
    ('ALARMNI SISTEM PARADOX',          'SIGURNOSNI I ALARMNI SISTEMI'),
    ('ALARMNI SISTEM ELDES',            'SIGURNOSNI I ALARMNI SISTEMI'),
    ('VIDEO NADZOR I SIGURNOSNA OPREMA','SIGURNOSNI I ALARMNI SISTEMI'),
    ('OPREMA ZA VIDEO NADZOR',          'SIGURNOSNI I ALARMNI SISTEMI'),
    ('KUTIJE',                          'SIGURNOSNI I ALARMNI SISTEMI'),
    ('KANALICE',                        'SIGURNOSNI I ALARMNI SISTEMI'),
    ('UTIČNICE',                        'SIGURNOSNI I ALARMNI SISTEMI'),
    ('KONEKTORI I MODULI',              'SIGURNOSNI I ALARMNI SISTEMI'),
    ('VIDEO NADZOR I  SIGURNOSNA OPREMA','SIGURNOSNI I ALARMNI SISTEMI'),
    ('VIDEO NADZOR',                    'SIGURNOSNI I ALARMNI SISTEMI'),

    -- ALATI
    ('ALAT I BAŠTA',                    'ALATI I OPREMA ZA DOM'),
    ('BAŠTA',                           'ALATI I OPREMA ZA DOM'),
    ('LED RASVETA',                     'ALATI I OPREMA ZA DOM'),
    ('SVE ZA KUĆU',                     'ALATI I OPREMA ZA DOM'),
    ('BAŠTA I ALATI',                   'ALATI I OPREMA ZA DOM'),
    ('POSUĐE',                          'ALATI I OPREMA ZA DOM'),

    -- BATERIJE I KABLOVI
    ('BATERIJE I PUNJAČI',              'BATERIJE, PUNJAČI I KABLOVI'),
    ('KABLOVI',                         'BATERIJE, PUNJAČI I KABLOVI'),
    ('KABLOVI I ADAPTERI',              'BATERIJE, PUNJAČI I KABLOVI'),
    ('PCI ADAPTERI',                    'BATERIJE, PUNJAČI I KABLOVI'),
    ('PC KABLOVI',                      'BATERIJE, PUNJAČI I KABLOVI'),
    ('ADAPTERI',                        'BATERIJE, PUNJAČI I KABLOVI'),
    ('DODATNA OPREMA',                  'BATERIJE, PUNJAČI I KABLOVI'),

    -- FITNESS
    ('BICIKLE I FITNES',                'FITNESS I SPORT'),
    ('NEGA LICA I TELA',                'FITNESS I SPORT'),
    ('LEPOTA I ZDRAVLJE',               'FITNESS I SPORT'),

    -- KANCELARIJA
    ('KANCELARIJSKI MATERIJAL',         'KANCELARIJSKI I ŠKOLSKI MATERIJAL'),
    ('ŠKOLSKI PRIBOR',                  'KANCELARIJSKI I ŠKOLSKI MATERIJAL'),
    ('ŠTAMPAČI',                        'KANCELARIJSKI I ŠKOLSKI MATERIJAL'),
    ('TONERI',                          'KANCELARIJSKI I ŠKOLSKI MATERIJAL'),
    ('KERTRIDŽ',                        'KANCELARIJSKI I ŠKOLSKI MATERIJAL'),
    ('RIBONI',                          'KANCELARIJSKI I ŠKOLSKI MATERIJAL'),
    ('MASTILA',                         'KANCELARIJSKI I ŠKOLSKI MATERIJAL'),
    ('CD, DVD MEDIJI',                  'KANCELARIJSKI I ŠKOLSKI MATERIJAL'),
    ('SKENERI I FOTOKOPIRI',            'KANCELARIJSKI I ŠKOLSKI MATERIJAL'),
    ('POTROŠNI MATERIJAL',              'KANCELARIJSKI I ŠKOLSKI MATERIJAL'),
    ('ŠTAMPAČ',                         'KANCELARIJSKI I ŠKOLSKI MATERIJAL'),
    ('MULTIFUNKCIJSKI ŠTAMPAČ',         'KANCELARIJSKI I ŠKOLSKI MATERIJAL'),
    ('SKENER',                          'KANCELARIJSKI I ŠKOLSKI MATERIJAL'),
    ('KOPIR',                           'KANCELARIJSKI I ŠKOLSKI MATERIJAL'),
    ('BUBANJ',                          'KANCELARIJSKI I ŠKOLSKI MATERIJAL'),

    -- OSTALO
    ('OUTLET',                          'OSTALO I OUTLET'),
    ('RAZNO',                           'OSTALO I OUTLET'),
    ('REZERVNI DEO',                    'KANCELARIJSKI I ŠKOLSKI MATERIJAL'),
    ('DODATNA',                         'OSTALO I OUTLET'),
    ('POS OPREMA',                      'OSTALO I OUTLET'),

    -- KONZOLE (gaming)
    ('KONZOLE ZA IGRANJE',              'RAČUNARI, KOMPONENTE I GAMING'),
    ('KONZOLE I DODATNA OPREMA',        'RAČUNARI, KOMPONENTE I GAMING'),
    ('SMART TV BOX',                    'TV, FOTO, AUDIO I VIDEO'),
    ('ASPIRATORI',                      'BELA TEHNIKA I KUĆNI APARATI'),
    ('PR',                              'KANCELARIJSKI I ŠKOLSKI MATERIJAL')

ON CONFLICT (nadgrupa) DO NOTHING;

-- ------------------------------------------------------------
-- 2. ARTIKAL TABELA
--    Flat, indeksovana, bez XML parsiranja pri upitu
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS artikal (
    id               BIGSERIAL PRIMARY KEY,

    -- Identitet
    vendor_id        INTEGER   NOT NULL,
    vendor_sifra     TEXT      NOT NULL,
    barkod           TEXT,
    barkod_valid     BOOLEAN   NOT NULL DEFAULT false,

    -- Kategorije
    nadgrupa         TEXT,
    grupa            TEXT,
    glavna_grupa     TEXT,

    -- Osnovni podaci
    naziv            TEXT      NOT NULL,
    proizvodjac      TEXT,
    model            TEXT,
    jedinica_mere    TEXT,
    kolicina         TEXT,

    -- Cene
    mpcena           NUMERIC(12, 2),
    b2bcena          NUMERIC(12, 2),
    web_cena         NUMERIC(12, 2),
    pdv              INTEGER,
    valuta           TEXT,

    -- Sadrzaj
    opis             TEXT,
    deklaracija      TEXT,
    energetska_klasa      TEXT,
    energetska_klasa_link TEXT,
    energetska_klasa_pdf  TEXT,

    -- Slike i filteri
    slike            TEXT[]    NOT NULL DEFAULT '{}',
    filteri          JSONB     NOT NULL DEFAULT '[]',

    -- Audit
    updated_at       TIMESTAMP NOT NULL DEFAULT now(),

    -- Jedinstvenost: jedan SKU po vendoru
    CONSTRAINT uq_artikal_vendor_sifra UNIQUE (vendor_id, vendor_sifra)
);

-- ------------------------------------------------------------
-- 3. INDEKSI
-- ------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_artikal_barkod
    ON artikal (barkod) WHERE barkod IS NOT NULL AND barkod != '';

CREATE INDEX IF NOT EXISTS idx_artikal_nadgrupa
    ON artikal (nadgrupa);

CREATE INDEX IF NOT EXISTS idx_artikal_glavna_grupa
    ON artikal (glavna_grupa);

CREATE INDEX IF NOT EXISTS idx_artikal_grupa
    ON artikal (grupa);

CREATE INDEX IF NOT EXISTS idx_artikal_proizvodjac
    ON artikal (proizvodjac);

CREATE INDEX IF NOT EXISTS idx_artikal_mpcena
    ON artikal (mpcena);

CREATE INDEX IF NOT EXISTS idx_artikal_vendor_id
    ON artikal (vendor_id);

-- Full-text search (simple = radi bez srpskog rjecnika, podrska za dijakritiku)
CREATE INDEX IF NOT EXISTS idx_artikal_fts
    ON artikal USING GIN (
        to_tsvector('simple', COALESCE(naziv, '') || ' ' || COALESCE(proizvodjac, '') || ' ' || COALESCE(model, ''))
    );

-- ------------------------------------------------------------
-- 4. ARTIKAL_DEDUP VIEW
--    Ono sto front vidi: po barcode uzima najjeftinijeg vendora,
--    artikli bez barkoda se prikazuju svi (ne mogu se deduplikovati)
-- ------------------------------------------------------------
CREATE OR REPLACE VIEW artikal_dedup AS
    -- Sa validnim barkodom: pobeduje najjeftiniji
    SELECT * FROM (
        SELECT DISTINCT ON (barkod) *
        FROM artikal
        WHERE barkod IS NOT NULL
          AND barkod != ''
          AND barkod_valid = true
        ORDER BY barkod, mpcena ASC
    ) sa_barkodom

    UNION ALL

    -- Bez barkoda: prikazuj sve
    SELECT *
    FROM artikal
    WHERE barkod IS NULL
       OR barkod = ''
       OR barkod_valid = false;

-- ------------------------------------------------------------
-- Kraj migracije
-- Sledeci korak: implementirati ArticalImportService (Faza 2)
-- ------------------------------------------------------------
