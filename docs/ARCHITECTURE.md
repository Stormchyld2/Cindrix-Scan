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

## 8. Open questions

1. How should Ops treat a re-import when the quantity has since changed (add, replace, or ask)?
2. Which units are fixed and which can be typed?
3. Framework choice, and whether Scan reuses AirLink's transfer code or just hands the file to the AirLink app.
