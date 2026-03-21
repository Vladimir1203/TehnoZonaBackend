# TehnoZona Backend - Audit & Zapažanja

> Generisano: 2026-03-21
> Svrha: Pregled arhitekture, logike i propusta **prije** bilo kakvih ispravki.

---

## 0. STANJE PODATAKA - Broj artikala po dobavljaču

| Vendor ID | Naziv   | Artikala (lokalno) | Artikala (remote/produkcija) | Napomena |
|-----------|---------|-------------------|------------------------------|----------|
| 1         | uspon   | 3832              | 3833                         | OK |
| 2         | linkom  | 243               | 243                          | OK |
| 3         | avtera  | 1360              | 1360                         | OK |
| 4         | spektar | 2588              | 0 (vendor ne postoji!)       | Vendor dodat lokalno ali ne i na remote |

**Remote ukupno:** 5436 artikala (bez Spektra)
**Lokalno ukupno:** 8023 artikala (sa Spektrom)

Razlika: Spektar vendor postoji lokalno ali nije nikad inicijalizovan na produkciji.
Uzrok OOM crash-a na Render-u: dodavanje Spektra je triggerovalo 4 simultana XML fetcha iste noći,
a `FeedRefreshService.saveAndActivate()` čita cijeli XML kao `String` u memoriju (`Files.readString`),
što je pri 20MB+ feedovima (Uspon) rezultiralo OOM kada su sva 4 fetcha skoro istovremeno punila heap.

---

## 1. BAZA - VIEW `unified_artikli`

### 1.1 View referencira vendor_id = 4 koji ne postoji
`DatabaseConfig.java` gradi view sa `UNION ALL` za 4 vendora, ali u `vendor` tabeli postoje samo 3 (uspon, linkom, avtera). Četvrti blok (Spektar) se svaki put izvršava i vraća 0 rezultata. Dead code koji usporava svaki upit na view-u.

### 1.2 Avtera - broken encoding
XPath iz avtera XML-a vraća tekst sa iskvarenim UTF-8 karakterima:
```
POTROĎŻ˝NI MATERIJAL  →  trebalo bi: POTROŠNI MATERIJAL
SLUĎŻ˝ALICE           →  trebalo bi: SLUŠALICE
```
Uzrok: XML feed od avtera vjerovatno dolazi u Windows-1250 ili Latin-2 encoding, a čita se kao UTF-8.

### 1.3 Avtera - `nadgrupa` je uvijek prazna
View koristi:
```sql
split_part(classtitle, '\', 2)
```
`classtitle` format je npr. `TRUST\GAMING DODACI\SLUŠALICE`.
`split_part` sa `\` kao delimiterom vraća prazan string jer PostgreSQL tretira `\` drugačije u ovom kontekstu. Sve Avtera `nadgrupa` vrijednosti su prazne stringovi.

### 1.4 Avtera - `grupa` sadrži punu putanju umjesto samo grupe
Za istu kategoriju `TRUST\GAMING DODACI\SLUŠALICE`, `grupa` dobija cijeli string umjesto samo posljednjeg segmenta. Dakle `grupa` i `nadgrupa` mapiranja su pogrešna za vendora 3.

### 1.5 Duplikati barkodova između vendora
51 barkod se pojavljuje kod oba vendora 1 (uspon) i 3 (avtera). View nema `DISTINCT ON` ni ikakvu deduplikaciju - isti artikal se vraća dvaput. `SearchService` ima komentar o deduplikaciji ali nije konzistentno primijenjeno.

### 1.6 264 artikala bez barkoda, 95 sa cijenom 0
- Uspon: 194 bez barkoda
- Avtera: 70 bez barkoda, 95 sa `mpcena = 0`

Ovi artikli prolaze kroz sve upite i prikazuju se korisnicima bez filtriranja.

### 1.7 Uspon (vendor 1) - status ARCHIVED, nema ACTIVE
Zadnji fetch za uspon je 14.03, oba zapisa su `ARCHIVED`. To znači da nema `ACTIVE` zapisa za vendor 1 u `xml_feed_history`, ali `vendor.xml_data` još uvijek sadrži XML (stale). Logika `archiveCurrentActive` je možda ostavila bazu u nekonzistentnom stanju.

---

## 2. VIEW - ARHITEKTURA I PERFORMANSE

### 2.1 View parsira XML u realnom vremenu za svaki upit
`vendor.xml_data` je `xml` kolona koja čuva kompletan XML feed (npr. 3833 artikala za uspon). Svaki upit na `unified_artikli` radi `unnest(xpath(...))` nad tim XML-om u realnom vremenu. Nema indeksiranja, nema materijalizacije. Uz rast broja vendora i artikala, ovo postaje ozbiljan bottleneck.

**Preporuka:** Materijalizovani view ili zasebna tabela artikala koja se populiše pri fetch-u feeda.

### 2.2 `xml_feed_history` čuva kompletan XML
`xml_content` kolona u `xml_feed_history` drži raw XML za svaki fetch. Sa feedovima od 3-5k artikala, svaki uspješan fetch dodaje MB-ove. Čisti se samo na 2 najnovija zapisa po vendoru (`cleanupOldFeeds`), ali to je i dalje dosta podataka koji se rijetko koriste.

### 2.3 View nije ažuriran kada se dodaje novi vendor
`DatabaseConfig.java` kreira/ažurira view pri svakom startu aplikacije. Dodavanje novog vendora zahtijeva:
1. Unos u `vendor` tabelu
2. Ručno pisanje novog `UNION ALL` bloka u Java kodu
3. Restart aplikacije

Nema automatizacije ovog procesa.

---

## 3. LOGIKA POSLOVNIH PRAVILA

### 3.1 Hardkodirane kategorije u `VendorService`
`groupMap` u `VendorService.java` sadrži hardkodiranu mapu glavnih grupa i nadgrupa. Svaka promjena zahtijeva izmjenu koda i redeploy. Ove kategorije bi trebale biti u bazi ili konfiguracionom fajlu.

### 3.2 Hardkodirani glavni proizvođači
Lista "glavnih" brendova (BEKO, BOSCH, GORENJE...) je hardkodirana u servisu. Isto kao gore - nema fleksibilnosti bez code change.

### 3.3 Logika cijena za Avtera
```java
mpcena = b2cpricewotax * 1.2
```
PDV od 20% se direktno upisuje u view. Ako se PDV stopa promijeni ili ako neki avtera artikal ima drugačiji PDV, view mora biti ažuriran ručno. Nema polja za PDV stopu po vendoru.

### 3.4 Spektar vendor postoji u kodu ali ne u bazi
`FeedSchedulerService` ima `@Scheduled` cron za vendor_id=4 (Spektar) u 03:45. `DatabaseConfig` ima UNION ALL blok za vendor_id=4. Postoje sample XML fajlovi (`spektar_full.xml`, itd.). Ali `XmlDataInitializer` ne kreira Spektar vendor, i u `vendor` tabeli ga nema. Aplikacija tiho "radi" za nepostojeći vendor bez greške.

### 3.5 `getCena()` logika u `Artikal` modelu
```java
public BigDecimal getCena() {
    return mpcena != null && mpcena.compareTo(BigDecimal.ZERO) > 0 ? mpcena : webCena;
}
```
Fallback na `webCena` bez ikakvog logiranja. Korisnik ne vidi da je cijena možda netačna.

### 3.6 `FeaturedProduct` i `HomepageItem` - validFrom/validTo nisu obavezni
Moguće je kreirati featured product bez datuma isteka. Takvi zapisi ostaju "aktivni" zauvijek. Nema periodičnog čišćenja expired zapisa.

---

## 4. SIGURNOST

### 4.1 CredentialManager - Base64 + reverse nije enkripcija
```java
// Obfuskacija: reverse() + Base64.decode()
```
Ovo je security through obscurity. Ko god ima pristup compiled JAR-u može trivijalno deobfuskovati kredencijale. Trebalo bi koristiti environment varijable ili secrets manager.

### 4.2 Kredencijali vendora hard-kodirani u kodu
API kredencijali za Uspon, Linkom i Avtera su u `CredentialManager.java`. Svaka promjena lozinke zahtijeva code change i redeploy.

### 4.3 Email adrese hardkodirane
`vladimir12934@gmail.com` i `bratislav.2000@gmail.com` su hardkodirani u `EmailService`. Gmail app password je u `CredentialManager`.

### 4.4 CORS dozvoljava localhost u produkciji
`WebConfig` eksplicitno dozvoljava `http://localhost:4200`. U produkcijskom deployu ovo nije kritično ali je nepotrebno.

### 4.5 Admin feed API bez autentikacije
`POST /api/admin/feeds/refresh/{vendorId}` je dostupan bez ikakve autentikacije ili API ključa. Svako ko zna URL može triggerovati fetch.

---

## 5. KOD I ARHITEKTURA

### 5.1 `VendorService` je monolit - 946 linija
Jedan servis radi kategorije, artikle, pretragu, featured products, homepage items, i image caching. Teško za održavanje i testiranje.

### 5.2 `ProductRepository` čita iz JSON fajla
`ProductRepositoryImpl` učitava `products.json` iz classpath-a. Ovo je vjerovatno leftover od razvoja i nikad zamijenjeno pravim izvorom podataka.

### 5.3 Nema error handlinga za XML parsing failure
Ako `vendor.xml_data` sadrži malformiran XML (npr. zbog prekinutog downloada), `xpath()` u view-u može baciti exception koji ruši cijeli upit. Nema try-catch ni fallback.

### 5.4 `XmlDataInitializer` - upsert logikovana u raw SQL
```java
// INSERT INTO vendor... ON CONFLICT DO NOTHING
```
Ali se ovo poziva na svakom startu. Ako vendor postoji, tiho se ignoriše - OK. Ali `feed_source` se ne ažurira ako se promijeni URL ili cron.

### 5.5 Cron expression u `feed_source` tabeli nije korišten
`feed_source.cron_expression` kolona postoji (npr. `0 0 3 * * *`) ali `FeedSchedulerService` koristi hardkodirane `@Scheduled` anotacije u kodu. Kolona u bazi se ne čita nigdje. Promjena cron-a u bazi nema efekta.

### 5.6 `findDuplicateBarkodovi` query postoji ali se ne koristi
`VendorRepository` ima `findDuplicateBarkodovi` upit ali ga niko ne poziva. Mrtav kod.

### 5.7 Kategorije slika - keširanje na disku bez invalidacije
`CategoryImageService` preuzima i kešira slike po kategorijama na disk. Nema mehanizma za invalidaciju keša ako se slika promijeni ili vendor promijeni kategorije.

### 5.8 Paginacija nije konzistentna
Neki endpointi vraćaju `ProductPageResponse` sa paginacijom, drugi vraćaju `List<Artikal>` bez paginacije. Npr. `/api/vendors/{id}/artikli` vraća fiksno ograničen broj bez page/size parametara.

### 5.9 `@Transactional` nedostaje na kritičnim operacijama
Feed refresh radi više operacija: archive stari, insert novi, cleanup historije. Ako neka od ovih operacija failuje na sredini, baza ostaje u nekonzistentnom stanju. Nema `@Transactional` wrappera koji bi rollbackovao sve.

---

## 6. PODACI - KONZISTENTNOST

### 6.1 Vendor ID je `int` u view-u, `bigint` u tabeli
`unified_artikli` view vraća `vendor_id` kao `integer`, ali `vendor.id` je `bigint`. Ne pravi problem sada, ali je nekonzistentnost.

### 6.2 Nema unique constraint na `vendor.name`
Moguće je kreirati dva vendora sa istim imenom. `XmlDataInitializer` koristi `ON CONFLICT (id)` što znači da se name ne provjerava.

### 6.3 `featured_products` i `homepage_items` - vendor_id bez FK constraint?
Treba provjeriti da li postoji foreign key constraint na `vendor_id` u ovim tabelama ili je moguće kreirati featured product za nepostojeći vendor.

---

## 7. FEED HISTORY - STANJE U BAZI

Trenutno stanje (2026-03-21):
| Vendor | Status zadnjeg fetcha | Datum |
|--------|----------------------|-------|
| uspon  | ARCHIVED (nema ACTIVE!) | 2026-03-14 |
| linkom | ACTIVE | 2026-03-14 |
| avtera | ACTIVE | 2026-03-14 |

Uspon nema ACTIVE zapis - vjerovatno bug u arhiviranju.

---

## URAĐENO

### ✅ OOM fix - `FeedRefreshService.saveAndActivate()`
- **Problem:** `Files.readString()` učitavao cijeli XML (do 20MB) tri puta u Java heap
- **Fix:** XML se sada šalje u DB jednom kroz `PGobject`, a `vendor.xml_data` se ažurira SQL-om (`syncVendorXmlFromHistory`) unutar baze bez prolaska kroz Java heap
- **Dodatno:** `FeedSchedulerService` refaktorisan da svaki vendor ima odvojen `@Scheduled` sa razmakom 15 min - sprječava simultane fetchove

### ✅ Avtera slike - `![CDATA[` prefix i `&amp;` u URL-u
- **Problem:** Zbog broken encoding-a pri downloadu, `<![CDATA[` ostajalo kao `![CDATA[` u bazi. `&amp;` ostajao u URL-ovima.
- **Fix:** View (`DatabaseConfig.java`) sada stripuje i `![CDATA[` (bez `<`) i `&amp;` → `&` za Avtera slike

### ✅ Avtera encoding - Windows-1250 → UTF-8
- **Problem:** Avtera server šalje `Content-Type: text/html; charset=Windows-1250`, ali `downloadToTemp()` kopirao bytes bez konverzije → mojibake u bazi
- **Fix:** `downloadToTemp()` čita `Content-Type` header, detektuje charset, i konvertuje u UTF-8 pri downloadu. XML deklaracija se automatski ispravlja na `encoding="UTF-8"`
- **Napomena:** Stari podaci u bazi ostaju korumpirani dok se ne uradi novi fetch

### ✅ `insertVendor` refaktor
- Uklonjen XML parametar iz `insertVendor()` - vendor se inicijalizuje sa praznim XML-om, a podaci dolaze isključivo kroz feed refresh
- `ON CONFLICT DO UPDATE` zamijenjeno sa `DO NOTHING` - inicijalizator ne smije da briše XML podatke vendora pri restartu

### ✅ `unified_artikli` view - `chr(92)` fix za Avtera nadgrupa
- `split_part(..., '\\\\', 2)` zamijenjeno sa `split_part(..., chr(92), 2)` - ispravlja parsiranje `classtitle` putanje
- `nadgrupa` sada ispravno sadrži drugi segment putanje (npr. `PC Dodaci` umjesto praznog stringa)
- `grupa` sada sadrži punu putanju (npr. `Trust\PC Dodaci\Slušalice`)

### ✅ Spektar vendor dodat (ID=4)
- `XmlDataInitializer` dodan `initVendor(4L, "spektar", ...)` - vendor se inicijalizuje pri startu
- `FeedSchedulerService` refaktorisan: svaki vendor ima odvojen `@Scheduled`, Spektar u 03:45
- `unified_artikli` view već sadržavao UNION ALL blok za vendor 4, sada ima i podatke
- Na remote DB: vendor i feed_source za Spektar dodani direktno SQL-om

### ✅ `unified_artikli` view - filter artikala sa cijenom 0
- Svaki UNION ALL blok sada ima `WHERE mpcena > 0` (ili ekvivalent po vendoru)
- Eliminisano 95 Avtera artikala sa `mpcena = 0` koji su se prikazivali korisnicima

### ✅ `unified_artikli` view - ispravljen SQL (nedostajući SELECT)
- Avtera i Spektar blokovi izgubili `SELECT n as vendor_id,` tokom editovanja - vraćeno
- View sada kompajlira i radi za sva 4 vendora

### ⚠️ Avtera encoding - istraživanje
- Charset konverzija implementirana i radi (Windows-1250 → UTF-8 u `downloadToTemp()`)
- Međutim, mojibake ostaje jer Avtera server šalje `EF BF BD` (UTF-8 replacement char `\uFFFD`) umjesto stvarnih Windows-1250 bajtova za srpske karaktere
- **Uzrok je server-side**: Avtera feed sadrži `EF BF BD` gdje bi trebalo biti `0x9A` (š), `0xB9` (č) itd.
- Ovo je **bug na Avtera strani** - ne može se ispraviti client-side konverzijom. Treba kontaktirati Avtera.

---

## PRIORITETI ISPRAVKI (prijedlog)

### Hitno:
1. ~~Avtera `nadgrupa`/`grupa` parsing~~ ✅ Urađeno
2. ~~Avtera encoding problem~~ ⚠️ Server-side bug, nije rješivo bez Avtera saradnje
3. ~~Uspon ACTIVE status~~ ✅ Urađeno direktnim SQL na remote
4. Admin feed API - dodati autentikaciju

### Važno:
5. ~~Ukloniti dead kod za vendor_id=4 ili dodati Spektar vendor~~ ✅ Urađeno
6. ~~`@Transactional` na feed refresh operaciju~~ ✅ Urađeno (`saveAndActivate` je `@Transactional`)
7. Kredencijali u environment varijable
8. ~~Filtrirati artikle sa cijenom 0 iz prikaza~~ ✅ Urađeno (WHERE mpcena > 0 u view-u)

### Arhitekturno (dugoročno):
9. Materijalizovati `unified_artikli` view
10. Prebaciti hardkodirane kategorije i brendove u bazu
11. Razbiti `VendorService` na manje servise
12. Cron iz baze umjesto hardkodiranog u kodu
