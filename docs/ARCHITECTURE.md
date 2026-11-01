# Cindrix-Scan: Architecture

Status: draft, 2026-11-01. Nothing is built. Items marked **proposal** are suggestions to confirm.

## 1. Goal

Get supplies into the Cindrix Ops inventory quickly and without internet, by scanning and sending one file.

## 2. Flow

```
  camera -> barcode decode -> catalog lookup -> details (qty, unit, use-by)
                                   |                        |
                           unknown? ask name          add to batch
                                                            |
                                    review batch -> export import file
                                                            |
                                          AirLink / copy to drive
                                                            |
                                         Ops Dashboard imports it
```

## 3. Components

- **Scanner.** Decodes barcodes from the camera feed on the phone. **Proposal:** a permissively licensed decoder such as ZXing (Apache-2.0). Avoid copyleft code, and avoid SDKs that depend on Google Play services, so the app works on any phone with no internet.
- **Catalog.** A small local table of barcode to name, category and default unit. It starts empty and fills as the person names new items. A repeat scan of a known barcode needs no typing.
- **Batch.** The list of items scanned in this session, editable before export. Held on the phone so a crash or a dead battery loses nothing.
- **Exporter.** Writes the batch as the inventory import CSV defined in Cindrix-Formats.
- **Sender.** Hands the file to AirLink, or saves it into the shared Cindrix folder under `inventory/`.

## 4. Data

Each batch line carries:

| Field | Notes |
|---|---|
| `scan_id` | Random ID per line, so importing the same file twice does not double the stock |
| `item` | Name from the catalog or typed |
| `barcode` | Empty for items added by hand |
| `qty`, `unit` | Quantity and unit (can, box, kg, litre and so on) |
| `use_by` | `YYYY-MM-DD`, or empty |
| `category`, `location` | Optional |
| `scanned_at` | UTC time |
| `source` | Callsign of the phone |

`scan_id` is not in the draft Formats spec yet and needs to be added there.

## 5. Behaviour to decide

- Scanning the same barcode twice in one batch: add to the quantity and confirm, rather than create a duplicate line.
- Ops merges imported lines into existing items using the barcode when there is one, otherwise the name.
- Undo for the last scan, and a way to edit or delete any line before export.

## 6. Permissions

- Camera, requested on first scan.
- Storage access only to the shared Cindrix folder.
- No network, no location, no contacts.

## 7. Optional later

- Import a public product database into the catalog while online once. Open Food Facts is a candidate, but its licence has attribution and share-alike terms that need checking first.
- A read-only copy of current stock exported from Ops, so Scan can show what is already on hand.

## 8. Decided while building (2026-11-01)

- **Framework:** native Kotlin with Jetpack Compose, CameraX and ZXing. Small, no Flutter, no Google Play services.
- **Sending:** Scan does not contain transfer code. "Send" opens the Android share sheet, and AirLink (or any other app) takes the file from there.
- **Saving:** "Save to folder" writes into an `inventory/` folder inside the Cindrix folder the person picks once.
- **Units:** a fixed list (ea, can, jar, bottle, bag, box, pack, lb, oz, kg, g, L, gal).
- **Categories:** a fixed starter list (Food, Water, Medical, Hygiene, Fuel, Tools, Power, Other). These need matching to the categories Ops uses.
- **Duplicates:** scanning the same barcode with the same unit, use-by date and location adds to the existing line.
- **Feedback:** a successful scan beeps and vibrates. Both can be switched off in Settings; the beep follows the phone's silent mode.
- **Fast mode:** off by default. For a barcode already in the catalog it shows a small popup (quantity, use-by chips, ADD / ADD and another date / EDIT ALL / CANCEL) instead of the full form. Unknown barcodes and manual entry always use the full form.
- **Quantity:** one line per purchase, so several identical items bought together are one entry with one use-by date.
- **Look:** AirLink palette and heading font, with its own logo ("Flame in the frame", sources in `design/logo/`).
- **File name:** `CindrixScan-<version>-android.apk`.
- **Add or take out:** scanning a known barcode always opens a small popup with ADD STOCK / TAKE OUT and a quantity. Take-out lines go in the file with `action=remove`. Fast mode only decides whether the use-by date starts as "same as last time".
- **File format v2:** adds an `action` column (`add` or `remove`) after `scan_id`. Ops still reads v1.
- **Outbox:** SEND TO OPS saves the batch as a file in the app's outbox and empties the batch, so nothing is lost. Files stay there until written into the Ops folder (`inbox/inventory/` inside the folder picked in Settings). A button also hands a file to Cindrix AirLink directly when it is installed. Ops holds the file in its own inbox until the person accepts it (Ops docs/39).
- **Automatic folder (0.4.1):** on Android 10 and newer, lists are written to `Documents/Cindrix/inbox/inventory/` through MediaStore, which needs no permission and no picker. Choosing a folder in Settings is an optional override. Older phones (Android 8 and 9) still need a folder chosen.
- **In stock (0.5.0):** a read-only tab built from the stock list Ops publishes (`outbox/stock-list.csv`, format `#cindrix-stock-list v1`). Scan never edits it. Only the device hosting the server changes the inventory; Scan sends changes back as add/take-out files. The quick popup shows "On hand" from the list, adjusted by the batch not yet sent (files already in the outbox are not counted yet). The list is loaded with a file picker for now.
- **Signing:** a stable keystore is committed so builds install over each other. It is public. Real release secrets can replace it without code changes.

## 9. Open questions

1. How should Ops treat a re-import when the quantity has since changed (add, replace, or ask)?
2. Match Scan's categories to the ones Cindrix Ops uses.
3. Does the first version read barcodes well in dim pantries and on curved cans? Needs testing on the Note10 and Note20.
