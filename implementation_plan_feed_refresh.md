# Technical Implementation Plan: Reliable Feed Refresh System

## 1. Overview
This document outlines the design for a robust, automated feed refresh system tailored for **TehnoZona**. The system will automate the current manual process of loading XML files (like `TehnoZona-uspon.txt`) into the PostgreSQL `vendor` table.

## 2. Project-Specific Context
*   **Existing Model**: Use the `Vendor` entity (ID 1 for 'Uspon', ID 2 for 'Linkom', etc.).
*   **Current Storage**: Native PostgreSQL `xml` column in the `vendor` table.
*   **XPath Usage**: Highly dependent on the `<artikli>/artikal` structure found in `TehnoZona-uspon.txt`.

## 3. Database Design Refinement

### 3.1 `feed_source` (Configuration)
This table will store where to download the XML from and which XSD to use.
| Field | Type | Example |
| :--- | :--- | :--- |
| `id` | BIGINT | 1 |
| `vendor_id` | BIGINT | 1 (Foreign key to `vendor.id`) |
| `endpoint_url` | VARCHAR | `http://api.provider.com/feed.xml` |
| `xsd_path` | VARCHAR | `schemas/uspon_v1.xsd` |
| `cron_expression` | VARCHAR | `0 0 3 * * *` (3 AM daily) |

### 3.2 `xml_feed_history` (Audit & Staging)
To prevent corrupting the `vendor` table, we store downloads here first.
| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | SERIAL | Primary Key |
| `vendor_id` | BIGINT | |
| `xml_content` | XML | Staged XML data |
| `status` | VARCHAR | `PENDING`, `ACTIVE`, `FAILED`, `ARCHIVED` |
| `hash_sum` | VARCHAR | SHA-256 to detect if provider content changed |
| `error_message` | TEXT | Log if validation fails |

## 4. Feed Refresh Flow (The "TehnoZona" Way)

### 4.1 Step-by-Step Logic
1.  **Scheduled Trigger**: `FeedSchedulerService` runs tasks based on `cron_expression`.
2.  **Streaming Download**: `FeedDownloadService` pipes the external URL directly to a temporary file (`temp_uspon.xml`). 
    *   *Rationale*: `TehnoZona-uspon.txt` is 14MB+. Reading it into a `StringBuilder` (as seen in `XmlDataInitializer`) causes spikes in memory.
3.  **Hash Check**: Calculate SHA-256 of the temp file. If it matches the latest `ACTIVE` entry in `xml_feed_history`, skip insertion.
4.  **Validation**:
    *   **XSD**: Check against `uspon.xsd`.
    *   **Business Check**: Ensure `count(//artikal) > 0` and `<prod_cena>` (or `<mpcena>`) exists.
5.  **Staging Insert**: Insert into `xml_feed_history` with status `PENDING`.
6.  **Atomic Activation**:
    *   `UPDATE xml_feed_history SET status = 'ARCHIVED' WHERE vendor_id = 1 AND status = 'ACTIVE'`
    *   `UPDATE xml_feed_history SET status = 'ACTIVE' WHERE id = :new_id`
    *   `UPDATE vendor SET xml_data = :new_xml WHERE id = 1` (Sync with main table used by XPath queries).

## 5. Development & Testing Support

### 5.1 Immediate Manual Trigger
To avoid waiting 24h, we will implement a `FeedController`:
*   `POST /api/admin/feeds/refresh/{vendorId}`
*   This endpoint calls the same service logic as the scheduler, allowing "On-Demand" testing.

### 5.2 Logging & Visibility
*   Expose a `GET /api/admin/feeds/history` to see the status of the last 10 refreshes.
*   Integrate `SLF4J` logging with file persistence for debugging network issues.

## 6. Service Implementation Strategy

### 6.1 `FeedDownloadService`
*   Use `RestTemplate` with `ResponseExtractor` to write `InputStream` directly to `File`.
*   **Safety**: Implement a 30-second timeout.

### 6.2 `FeedValidationService`
*   Use `javax.xml.validation.Schema`.
*   **Specific for Uspon**: Validate that the root is `<artikli>` and each `<artikal>` has a `<sifra>`.

### 6.3 `FeedNotificationService`
*   Configured to use `vladimir12934@gmail.com` as seen in `application.properties`.
*   Send notification only on **Failure** or **Significant Schema Change**.

## 7. Immediate Action Items
1.  **Create XSD**: Extract a schema from the current `TehnoZona-uspon.txt`.
2.  **Entity Mapping**: Create `FeedSource` and `XmlFeedHistory` JPA entities.
3.  **Manual Refresh Endpoint**: Priority #1 for developer testing.
4.  **Streaming Persistence**: Refactor `XmlDataInitializer` logic to use streams instead of `StringBuilder` for the actual implementation.

