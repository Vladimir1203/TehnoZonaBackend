# TehnoZona Backend — Plan optimizacije arhitekture

> Datum: 2026-03-21
> Svrha: Detaljan plan refaktorisanja koji treba da se implementira u narednom periodu.
> Ovaj dokument je namijenjen za nastavak rada u novom chat kontekstu.

---

## 1. KONTEKST — KAKO APLIKACIJA SADA RADI

### Stack
- Spring Boot 3.3.5 / Java 17 / PostgreSQL / Maven
- Deploy: Render (free tier, 512MB RAM)
- Frontend: Angular, URL: `https://www.tehnozona.rs`
- Backend: `https://tehnozonabackend.onrender.com`

### Tok podataka (trenutno)
```
Noću (cron):
  HTTP fetch → temp file → SHA256 hash check → XSD validate
    → INSERT xml_feed_history (status=ACTIVE)
    → UPDATE vendor.xml_data (kopija unutar DB, nema Java heap)
    → archive stari ACTIVE → cleanup > 2 zapisa po vendoru

Danju (svaki API poziv):
  SELECT FROM unified_artikli
    → unified_artikli je VIEW koji radi xpath() + unnest() nad vendor.xml_data u realnom vremenu
    → Java deserijalizuje XML string → Artikal objekat
    → filtriranje, sortiranje, paginacija u Javi (!)
```

### Vendori
| ID | Naziv   | Artikala | Feed format | Posebnosti |
|----|---------|----------|-------------|------------|
| 1  | uspon   | ~3833    | XML `/artikli/artikal` | Windows-1250? Ne, UTF-8 |
| 2  | linkom  | ~243     | XML `/artikli/artikal` | Samo kablovi/mrežna oprema |
| 3  | avtera  | ~1360    | XML `/xmlData/Article` | Windows-1250 → konverzija urađena; ali server šalje EF BF BD (replacement chars) za srpska slova — bug na Avtera strani, ne može se popraviti |
| 4  | spektar | ~2588    | XML `/products/product` | CDATA u svim fieldovima; price = mpcena (nema posebne b2b cijene) |

### Cron raspored (staggered, sprječava simultane fetchove)
- Uspon: 03:00
- Linkom: 03:15
- Avtera: 03:30
- Spektar: 03:45

---

## 2. TRENUTNI PROBLEMI

### 2.1 Performance — XML parsing u realnom vremenu
**Ovo je glavni problem.** `unified_artikli` VIEW:
```sql
SELECT unnest(xpath('/artikli/artikal', xml_data)) as art_xml FROM vendor WHERE id = 1
```
Za svaki API poziv PostgreSQL prolazi kroz ~3833 XML elemenata, parsira xpath na svakom,
kastuje u text, vraća Java-i koja to opet parsira u `Artikal` objekte.

Nema indeksa. Nema keša. Svaki poziv = brute force nad celim XML-om.

### 2.2 Filtriranje i sortiranje u Javi
`VendorService.java` (946 linija!) dohvata LISTU artikala iz baze, pa filtrira u Javi:
```java
artikli.stream()
  .filter(a -> a.getMpcena() >= minCena)
  .filter(a -> proizvodjaci.contains(a.getProizvodjac()))
  .sorted(...)
  .skip(page * size)
  .limit(size)
  .collect(...)
```
Za 3833 artikala, svaki poziv sa filterom prolazi kroz sve.

### 2.3 Kategorije su hardkodirane u Java kodu
`VendorService.java` sadrži:
```java
private final Map<String, List<String>> groupMap = Map.of(
    "BELA TEHNIKA I KUĆNI APARATI", List.of(
        "BELA TEHNIKA", "MALI KUĆNI APARATI", "GREJANJE", ...
    ),
    "RAČUNARI, KOMPONENTE I GAMING", List.of(...),
    ...
    // 10 glavnih grupa, svaka sa listom vendor-raw nadgrupa
);
```
Svaka izmjena kategorija = code change + redeploy.

### 2.4 Normalizacija nadgrupa ne postoji
Svaki vendor koristi svoje nazive. Trenutna mapa mapira vendor nadgrupe na glavne grupe,
ali ne normalizuje same nadgrupe — korisnik vidi različite nazive za istu stvar:
- `MALI KUĆNI APARATI` (vendor 1) = `MALI KUHINJSKI APARATI` (vendor 3) = `MALI KUCNI APARATI` (vendor 3, bez dijakritika)
- `KUĆNA BELA TEHNIKA` (vendor 1) + `BELA TEHNIKA` (vendor 1 + vendor 3) — dvije nadgrupe za istu kategoriju
- `RAČUNARSKE PERIFERIJE` (vendor 1) = `RACUNARSKE PERIFERIJE` (vendor 2, bez dijakritika) = `PERIFERIJE` (vendor 4)

Vendor 4 (Spektar) je poseban problem — ima ~150 nadgrupa koje su zapravo GRUPE,
ne nadgrupe (npr. `APARATI ZA KROFNE`, `LEDOMATI`, `KETLERI` — svaka sa 2-10 artikala).

### 2.5 Duplikati barkodova između vendora
~51 barkod postoji i kod vendor 1 (uspon) i vendor 3 (avtera).
VIEW vraća isti artikal dvaput. Nema prioritetizacije ni dedup logike.

### 2.6 VendorService monolit
946 linija. Radi: kategorije, artikle, pretragu, featured products, homepage items,
image caching, filtriranje, paginaciju. Nemoguće testirati pojedinačne dijelove.

### 2.7 Frontend koristi vendorId=0
Frontend hardkoduje `vendorId=0` za sve pozive. Backend mapuje 0 na "sve vendore".
Ovo je relikt dizajna koji nema smisla sa 4 vendora.

---

## 3. PREDLOŽENA NOVA ARHITEKTURA

### 3.1 Centralna `artikal` tabela (zamjena za VIEW)

```sql
CREATE TABLE artikal (
    id              BIGSERIAL PRIMARY KEY,
    vendor_id       BIGINT NOT NULL REFERENCES vendor(id),
    sifra           VARCHAR(100),
    barkod          VARCHAR(100),          -- UNIQUE (jedan artikal po barkodu, vendor prioritet)
    naziv           VARCHAR(500) NOT NULL,
    naziv_tsv       TSVECTOR,              -- full-text search index
    mpcena          NUMERIC(12,4) NOT NULL CHECK (mpcena > 0),
    web_cena        NUMERIC(12,4),
    b2b_cena        NUMERIC(12,4),
    pdv             INTEGER DEFAULT 20,
    valuta          VARCHAR(10) DEFAULT 'RSD',
    kolicina        NUMERIC(12,4),
    jedinica_mere   VARCHAR(50),
    model           VARCHAR(200),
    opis            TEXT,
    deklaracija     TEXT,
    slike           TEXT[],               -- PostgreSQL array URL-ova
    filteri         JSONB,                -- {ime: string, vrednost: string}[]
    nadgrupa_id     BIGINT REFERENCES nadgrupa(id),
    glavna_grupa_id BIGINT REFERENCES glavna_grupa(id),  -- denormalizovano za brzinu
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indeksi
CREATE UNIQUE INDEX artikal_barkod_idx ON artikal(barkod) WHERE barkod IS NOT NULL AND barkod != '';
CREATE INDEX artikal_nadgrupa_idx ON artikal(nadgrupa_id);
CREATE INDEX artikal_glavna_grupa_idx ON artikal(glavna_grupa_id);
CREATE INDEX artikal_proizvodjac_idx ON artikal(proizvodjac);
CREATE INDEX artikal_mpcena_idx ON artikal(mpcena);
CREATE INDEX artikal_tsv_idx ON artikal USING GIN(naziv_tsv);
```

Vendor prioritet za dedup barkodova: **Uspon (1) > Linkom (2) > Avtera (3) > Spektar (4)**
Ako Uspon i Avtera imaju isti barkod, Uspon "pobjeđuje" — UPDATE samo ako vendor_id <= postojeći.

### 3.2 Hijerarhija kategorija

```sql
CREATE TABLE glavna_grupa (
    id       BIGSERIAL PRIMARY KEY,
    naziv    VARCHAR(200) UNIQUE NOT NULL,
    redosled INTEGER DEFAULT 0
);

CREATE TABLE nadgrupa (
    id              BIGSERIAL PRIMARY KEY,
    naziv           VARCHAR(200) NOT NULL,
    glavna_grupa_id BIGINT NOT NULL REFERENCES glavna_grupa(id),
    redosled        INTEGER DEFAULT 0,
    UNIQUE (naziv, glavna_grupa_id)
);

CREATE TABLE grupa (
    id          BIGSERIAL PRIMARY KEY,
    naziv       VARCHAR(200) NOT NULL,
    nadgrupa_id BIGINT NOT NULL REFERENCES nadgrupa(id),
    UNIQUE (naziv, nadgrupa_id)
);
```

Početne vrijednosti `glavna_grupa` (8 kanonskih, iz trenutnog koda):
1. BELA TEHNIKA I KUĆNI APARATI
2. RAČUNARI, KOMPONENTE I GAMING
3. TELEFONI, TABLETI I OPREMA
4. TV, FOTO, AUDIO I VIDEO
5. BATERIJE, PUNJAČI I KABLOVI
6. ALATI I OPREMA ZA DOM
7. FITNESS I SPORT
8. SIGURNOSNI I ALARMNI SISTEMI
9. KANCELARIJSKI I ŠKOLSKI MATERIJAL
10. OSTALO

### 3.3 Mapiranje vendor raw → interna nadgrupa

Ovo je KEY tabela — bez nje normalizacija ne radi.

```sql
CREATE TABLE nadgrupa_mapiranje (
    id                  BIGSERIAL PRIMARY KEY,
    vendor_id           BIGINT NOT NULL REFERENCES vendor(id),
    vendor_nadgrupa_raw VARCHAR(300) NOT NULL,  -- tačno onako kako dolazi iz XML-a
    nadgrupa_id         BIGINT REFERENCES nadgrupa(id),  -- NULL = OSTALO
    UNIQUE (vendor_id, vendor_nadgrupa_raw)
);
```

**Inicijalno punjenje:** Izvesti se iz trenutnog `groupMap` u `VendorService.java`.
Svaka stavka u listi po glavnoj grupi → INSERT u `nadgrupa_mapiranje`.
Npr:
```sql
INSERT INTO nadgrupa_mapiranje (vendor_id, vendor_nadgrupa_raw, nadgrupa_id)
VALUES
  (1, 'BELA TEHNIKA', (SELECT id FROM nadgrupa WHERE naziv='BELA TEHNIKA')),
  (1, 'KUĆNA BELA TEHNIKA', (SELECT id FROM nadgrupa WHERE naziv='BELA TEHNIKA')),  -- mapira na istu!
  (3, 'MALI KUHINJSKI APARATI', (SELECT id FROM nadgrupa WHERE naziv='MALI KUĆNI APARATI')),
  (3, 'MALI KUCNI APARATI', (SELECT id FROM nadgrupa WHERE naziv='MALI KUĆNI APARATI')),
  ...
```

### 3.4 Nepoznate nadgrupe (admin workflow)

```sql
CREATE TABLE nepoznata_nadgrupa (
    id                  BIGSERIAL PRIMARY KEY,
    vendor_id           BIGINT NOT NULL REFERENCES vendor(id),
    vendor_nadgrupa_raw VARCHAR(300) NOT NULL,
    broj_artikala       INTEGER DEFAULT 0,
    status              VARCHAR(20) DEFAULT 'PENDING',  -- PENDING | RESOLVED
    prvi_put_vidjeno    TIMESTAMP DEFAULT NOW(),
    poslednji_put_vidjeno TIMESTAMP DEFAULT NOW(),
    UNIQUE (vendor_id, vendor_nadgrupa_raw)
);
```

Workflow:
1. Noćni job parsira artikal, gleda u `nadgrupa_mapiranje`
2. Nema mapiranja → artikal ide u OSTALO + INSERT/UPDATE u `nepoznata_nadgrupa`
3. Email se šalje: "5 novih nepoznatih nadgrupa, idite na /admin/kategorije"
4. Admin UI prikazuje listu `PENDING` nepoznatih nadgrupa sa brojem artikala
5. Admin odabira postojeću nadgrupu ili kreira novu → INSERT u `nadgrupa_mapiranje`
6. Trigger (ili scheduled job) ažurira sve artikle iz OSTALO koji imaju taj raw naziv
7. `nepoznata_nadgrupa.status` → RESOLVED

---

## 4. NOĆNI JOB — NOVA LOGIKA

Trenutni `FeedRefreshService.saveAndActivate()` radi samo:
```
archive → INSERT xml_feed_history → UPDATE vendor.xml_data → cleanup
```

Nakon refaktora dodaje se korak **ParseAndSync**:

```
archive → INSERT xml_feed_history → UPDATE vendor.xml_data → cleanup
    → parseAndSyncArtikli(vendorId)          ← NOVO
```

`parseAndSyncArtikli(vendorId)`:
1. Čita `vendor.xml_data` za dati vendor (XML je već u bazi)
2. Parsira xpath u Java `Artikal` objekte (isti kod koji već postoji u VIEW-u, samo u Javi)
3. Za svaki artikal:
   a. Lookup `nadgrupa_mapiranje` → nađi `nadgrupa_id`
   b. Ako nema → `nadgrupa_id = OSTALO_ID`, INSERT/UPDATE `nepoznata_nadgrupa`
   c. `INSERT INTO artikal (...) ON CONFLICT (barkod) DO UPDATE SET ... WHERE artikal.vendor_id >= EXCLUDED.vendor_id`
      (vendor prioritet: niži ID = viši prioritet, ne overwrite-ujemo bolji vendor)
4. Briše stare artikle ovog vendora koji više nisu u feedu:
   `DELETE FROM artikal WHERE vendor_id = ? AND updated_at < job_start_time`
5. Ažurira `naziv_tsv`:
   `UPDATE artikal SET naziv_tsv = to_tsvector('simple', naziv) WHERE vendor_id = ?`
6. Ako ima nepoznatih nadgrupa → email notifikacija
7. Log: "Vendor X: inserted=Y, updated=Z, deleted=W, unknown_nadgrupe=V"

**Zašto 'simple' a ne 'serbian' za tsvector?**
PostgreSQL nema ugrađeni Serbian dictionary. 'simple' radi lowercase + tokenize što je dovoljno
za pretragu po nazivu. Alternativa je instalirati `pg_trgm` za fuzzy search.

---

## 5. API ENDPOINTI — NOVI vs STARI

Front koristi `vendorId=0` za sve. Backend treba da mapuje 0 → sve vendore.

### Novi endpointi (direktno iz `artikal` tabele):

```
GET /api/v2/artikli?glavnaGrupa=X&nadgrupa=Y&grupa=Z
    &minCena=&maxCena=&proizvodjaci=A,B&sort=mpcena_asc
    &page=0&size=20
→ SELECT * FROM artikal WHERE nadgrupa_id IN (...)
  AND mpcena BETWEEN ? AND ?
  AND proizvodjac = ANY(?)
  ORDER BY mpcena ASC
  LIMIT 20 OFFSET 0

GET /api/v2/artikli/search?q=laptop&page=0&size=20
→ SELECT * FROM artikal
  WHERE naziv_tsv @@ plainto_tsquery('simple', ?)
  ORDER BY ts_rank(naziv_tsv, query) DESC
  LIMIT 20 OFFSET 0

GET /api/v2/artikli/{barkod}
→ SELECT * FROM artikal WHERE barkod = ?  (PK lookup, <1ms)

GET /api/v2/kategorije
→ SELECT gg.naziv, n.naziv, g.naziv FROM glavna_grupa gg
  JOIN nadgrupa n ON n.glavna_grupa_id = gg.id
  LEFT JOIN grupa g ON g.nadgrupa_id = n.id
  ORDER BY gg.redosled, n.redosled

GET /api/v2/artikli/filteri?nadgrupa=X
→ SELECT proizvodjac, count(*) FROM artikal
  WHERE nadgrupa_id = ? GROUP BY proizvodjac ORDER BY count DESC
```

### Stari endpointi:
Ostaju netaknuti, označe se `@Deprecated` komentarom, ne diraju se dok front ne migrira.
Frontend sada koristi:
- `GET /api/vendors/{vendorId}/artikli-paginated`
- `GET /api/vendors/{vendorId}/search`
- `GET /api/vendors/glavneGrupe`
- `GET /api/vendors/menu-structure`
- itd. (vidi AUDIT.md sekcija 5)

---

## 6. REDOSLED IMPLEMENTACIJE

### Faza 1 — Baza (prerequisit za sve)
1. CREATE TABLE `glavna_grupa`, `nadgrupa`, `grupa`
2. INSERT inicijalne vrijednosti iz `groupMap` u VendorService.java
3. CREATE TABLE `nadgrupa_mapiranje`
4. INSERT inicijalno mapiranje (izvesti iz groupMap)
5. CREATE TABLE `artikal`
6. CREATE TABLE `nepoznata_nadgrupa`

### Faza 2 — Noćni job
7. Implementirati `ParseAndSyncService.parseAndSyncArtikli(vendorId)`
   - XML parsing logika (xpath) — može se izvući iz `DatabaseConfig.java` (SQL xpath → Java xpath)
   - UPSERT u `artikal` tabelu sa vendor prioritetom
   - Detekcija nepoznatih nadgrupa
8. Pozvati iz `FeedRefreshService.saveAndActivate()` nakon sync
9. Email notifikacija za nepoznate nadgrupe
10. Ručno pokrenuti za sve 4 vendora da se tabela inicijalno napuni

### Faza 3 — Novi API
11. `ArtikalRepository` — JPA repository nad `artikal` tabelom
12. `ArtikalService` — nova klasa, ne diraj VendorService
13. `ArtikalController` — `/api/v2/artikli` endpointi
14. Testirati da novi endpointi vraćaju iste podatke kao stari

### Faza 4 — Admin kategorije UI
15. `NepoznataNadgrupaController` — GET lista, POST mapiranje
16. Admin UI akcija (Angular) za pregled i mapiranje nepoznatih nadgrupa
17. Scheduled job koji proces-ira OSTALO artikle nakon novog mapiranja

### Faza 5 — Migracija fronta (opcionalno, dugoročno)
18. Promijeniti Angular services da koriste `/api/v2/` endpointe
19. Ukloniti stare endpointe

---

## 7. DETALJI PARSIRANJA PO VENDORU

Ovo je ključna informacija za implementaciju `ParseAndSyncService`.
Svaki vendor ima drugačiju XML strukturu.

### Vendor 1 — Uspon
```
Root: /artikli/artikal
sifra:        //sifra/text()
barkod:       //barkod/text()
naziv:        //naziv/text()
nadgrupa:     //nadgrupa/text()  → UPPER(TRIM(...))
grupa:        //grupa/text()     → UPPER(TRIM(...))
proizvodjac:  //proizvodjac/text()
mpcena:       //mpcena/text() ako > 0, else //cena/text() * 1.2
b2bcena:      //b2bcena/text() ili //cena/text()
kolicina:     //kolicina/text() ili //stock/text()
jedinica_mere://jedinica_mere/text() ili //jm/text()
opis:         //opis/node() + //opis_dugi/node() + //karakteristike/node() + ...
slike:        //slike//slika/text() + //slika/text() + //picture/text() + //image/text()
FILTER:       mpcena > 0 OR cena > 0
```

### Vendor 2 — Linkom
```
Identičan format kao Vendor 1 (isti supplier sistem)
Root: /artikli/artikal
FILTER: mpcena > 0 OR cena > 0
```

### Vendor 3 — Avtera
```
Root: /xmlData/Article
sifra:        //ident/text()
barkod:       //ean1/text()
naziv:        //title/text()
nadgrupa:     split_part(//classtitle/text(), '\', 2)  → drugi segment putanje
grupa:        //classtitle/text()                      → puna putanja npr. "Trust\PC Dodaci\Slušalice"
proizvodjac:  //articlebrand/text()
mpcena:       //b2cpricewotax/text() * 1.2  (PDV uvijek 20%)
b2bcena:      //price/text()
kolicina:     //stock/text()
jedinica_mere://unit/text()
opis:         //description/node() + //longdescription/node() + //opis/node() + //karakteristike/node()
slike:        //slikaVelika/text() + //dodatneSlike//*/text()
              (strip: '<![CDATA[', '![CDATA[', ']]>', ']]', '&amp;'→'&')
FILTER:       b2cpricewotax > 0
NAPOMENA:     Encoding bug na Avtera strani — srpski karakteri dolaze kao EF BF BD (U+FFFD replacement char).
              Charset konverzija implementirana u downloadToTemp() ali ne može popraviti već izgubljene karaktere.
```

### Vendor 4 — Spektar
```
Root: /products/product
sifra:        //code/text()     (strip CDATA)
barkod:       //ean/text()      (strip CDATA)
naziv:        //name/text()     (strip CDATA)
nadgrupa:     //category/text() (strip CDATA) — ovo je GRUBUSAN naziv, previše granularno!
grupa:        //category/text() (ista vrijednost za nadgrupu i grupu!)
proizvodjac:  //manufacturer/text() (strip CDATA)
model:        //model/text()    (strip CDATA)
mpcena:       //price/text()    (strip CDATA)
b2bcena:      //price/text()    (ista cijena)
kolicina:     //stock/text()
valuta:       //currency/text()
deklaracija:  //declaration/text() (strip CDATA)
opis:         //description/node()
slike:        //images//*/text() (strip CDATA)
FILTER:       price > 0
NAPOMENA:     Spektar nema nadgrupe — ima ~150 kategorija koje su zapravo grupe/podgrupe.
              Sve moraju biti mapirane u nadgrupa_mapiranje tabelu.
              CDATA stripping potreban na gotovo svakom polju.
```

---

## 8. ŠABLON ZA PARSIRANJE (Java pseudo-kod)

```java
// U ParseAndSyncService.java
public void parseAndSyncArtikli(Long vendorId) {
    String xml = vendorRepository.getXmlData(vendorId);
    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = builder.parse(new InputSource(new StringReader(xml)));
    XPath xpath = XPathFactory.newInstance().newXPath();

    String rootExpr = switch (vendorId.intValue()) {
        case 1, 2 -> "/artikli/artikal";
        case 3    -> "/xmlData/Article";
        case 4    -> "/products/product";
        default   -> throw new IllegalArgumentException("Unknown vendor: " + vendorId);
    };

    NodeList nodes = (NodeList) xpath.evaluate(rootExpr, doc, XPathConstants.NODESET);
    LocalDateTime jobStart = LocalDateTime.now();

    for (int i = 0; i < nodes.getLength(); i++) {
        Node artNode = nodes.item(i);
        ArtikalEntity artikal = parseArtikalByVendor(vendorId, artNode, xpath);
        if (artikal == null) continue; // filtriran (mpcena = 0)

        // Lookup nadgrupa mapiranje
        Long nadgrupaId = nadgrupaMapiranjeRepo.findNadgrupaId(vendorId, artikal.getRawNadgrupa());
        if (nadgrupaId == null) {
            nadgrupaId = OSTALO_NADGRUPA_ID;
            nepoznataNadgrupaRepo.upsert(vendorId, artikal.getRawNadgrupa());
        }
        artikal.setNadgrupaId(nadgrupaId);
        artikal.setGlavnaGrupaId(nadgrupaRepo.findGlavnaGrupaId(nadgrupaId));

        artikalRepo.upsertWithVendorPriority(artikal);
    }

    // Obriši artikle koji više nisu u feedu
    artikalRepo.deleteByVendorAndUpdatedBefore(vendorId, jobStart);

    // Refresh tsvector
    jdbcTemplate.update("UPDATE artikal SET naziv_tsv = to_tsvector('simple', naziv) WHERE vendor_id = ?", vendorId);

    // Email za nepoznate nadgrupe
    List<NepoznataNadgrupa> pending = nepoznataNadgrupaRepo.findPending(vendorId);
    if (!pending.isEmpty()) {
        emailService.sendUnknownNadgrupeNotification(vendorId, pending);
    }
}
```

---

## 9. UPSERT SA VENDOR PRIORITETOM

Ključni SQL za dedup barkodova:

```sql
INSERT INTO artikal (vendor_id, sifra, barkod, naziv, mpcena, nadgrupa_id, ...)
VALUES (?, ?, ?, ?, ?, ?, ...)
ON CONFLICT (barkod) DO UPDATE SET
    vendor_id       = EXCLUDED.vendor_id,
    naziv           = EXCLUDED.naziv,
    mpcena          = EXCLUDED.mpcena,
    nadgrupa_id     = EXCLUDED.nadgrupa_id,
    updated_at      = NOW()
WHERE artikal.vendor_id >= EXCLUDED.vendor_id;
-- Condition: samo update-uj ako novi vendor ima isti ili bolji prioritet
-- (niži vendor_id = viši prioritet: 1=Uspon > 2=Linkom > 3=Avtera > 4=Spektar)
```

Za artikle bez barkoda (264 artikala, uglavnom Uspon) — koristiti `vendor_id + sifra` kao alternativni unique key.

---

## 10. ADMIN KATEGORIJE — UI FLOW

### Backend endpoint:
```
GET  /api/admin/kategorije/nepoznate
     → lista nepoznata_nadgrupa WHERE status='PENDING', sa vendor nazivom i brojem artikala

POST /api/admin/kategorije/mapiranje
     body: { vendorId, vendorNadgrupaRaw, targetNadgrupaId }
     → INSERT nadgrupa_mapiranje
     → UPDATE artikal SET nadgrupa_id=targetNadgrupaId
       WHERE vendor_id=vendorId
       AND id IN (SELECT artikal_id FROM ... WHERE raw_nadgrupa = vendorNadgrupaRaw)
     → UPDATE nepoznata_nadgrupa SET status='RESOLVED' WHERE ...
     → 200 OK { "ažuriranoArtikala": N }

GET  /api/admin/kategorije/nadgrupe
     → lista svih internih nadgrupa sa glavnom grupom (za dropdown)
```

### Frontend admin UI:
Tabela sa kolonama: Vendor | Raw naziv | Broj artikala | Mapiraj u →
Za svaki red: dropdown sa internim nadgrupama grupisan po glavnoj grupi + dugme "Sačuvaj"
Nakon sačuvaj: red nestaje iz tabele (status=RESOLVED)

---

## 11. ŠTA SE NE MIJENJA

- `vendor` tabela — ostaje
- `xml_feed_history` — ostaje (audit log, raw XML backup)
- `vendor.xml_data` — ostaje (source of truth za reprocessing)
- `feed_source` tabela — ostaje
- `FeedRefreshService.downloadToTemp()` — ostaje
- `FeedRefreshService.validateXml()` — ostaje
- `FeedRefreshService.saveAndActivate()` — ostaje, samo se dodaje poziv na ParseAndSyncService
- Svi postojeći endpointi — ostaju (deprecated, ali rade)
- `AdminAuthFilter` (X-Admin-Key) — ostaje i važi za nove admin endpointe
- `FeedSchedulerService` staggered cron — ostaje
- `DatabaseConfig.java` unified_artikli VIEW — može ostati kao backward compat, ili se ukloni kad stari endpointi odu

---

## 12. MIGRACIJA PODATAKA

Kada se implementira Faza 1-2:
1. Pokrenuti `parseAndSyncArtikli` za sve 4 vendora ručno (admin endpoint)
2. Verifikovati: `SELECT count(*) FROM artikal` treba biti ~8000
3. Verifikovati: `SELECT count(*) FROM nepoznata_nadgrupa WHERE status='PENDING'` — to je backlog za mapiranje
4. Admin prolazi kroz nepoznate nadgrupe i mapira ih
5. Frontend testirati na novim `/api/v2/` endpointima dok stari rade paralelno

---

## 13. NAPOMENE ZA IMPLEMENTACIJU

### Render free tier ograničenja
- 512MB RAM — ovo je razlog zašto smo prešli na PGobject + DB-internal copy
- CPU throttling — parseAndSync za 8000 artikala treba da bude batch (ne sve odjednom)
  Preporuka: procesovati u batch-evima od 500, sa kratkim pauzama
- PostgreSQL na Render free tier: 1GB storage limit

### Encoding
- Avtera: encoding problem je server-side (EF BF BD u XML-u). Charset konverzija je urađena
  ali ne može da oporavi već izgubljene karaktere. Kontaktirati Avtera za ispravku.
- Spektar: CDATA stripping mora biti u Java parseru (strip `<![CDATA[`, `![CDATA[`, `]]>`, `]]`)

### Testovi
- Postojeći `FeedRefreshServiceTest` (3 testa) treba proširiti sa testovima za `ParseAndSyncService`
- Test: vendor 1 XML → 3832 artikala u tabeli
- Test: duplikat barkod između vendor 1 i 3 → vendor 1 pobjeđuje
- Test: nepoznata nadgrupa → INSERT u nepoznata_nadgrupa

### Bitne klase za čitanje prije implementacije
- `VendorService.java` (946 linija) — sadrži groupMap i svu poslovnu logiku
- `DatabaseConfig.java` — sadrži SQL xpath logiku za sve 4 vendora (izvor za Java xpath implementaciju)
- `FeedRefreshService.java` — saveAndActivate(), downloadToTemp()
- `VendorRepository.java` — syncVendorXmlFromHistory(), findSampleImageForNadgrupa()
- `SearchController.java` i `VendorController.java` — svi postojeći endpointi
