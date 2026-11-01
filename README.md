# Cindrix-Scan

A barcode scanner that adds your supplies to the Cindrix Ops inventory. Scan a can or box, set the quantity and use-by date, and send the batch to Ops.

| | |
|---|---|
| Android app ID | `com.cindrix.scan` |
| Platform | Android |
| Status | Planning. No app code yet. |

## What it is

Typing a whole pantry into an inventory by hand is slow. Scan turns it into: point the camera, confirm the details, next item. When the batch is done, Scan writes one file and sends it to Cindrix Ops, which imports it on the Dashboard.

## Planned for the first version

- Scan barcodes with the camera, entirely on the phone
- Remember what each barcode is, so a repeat scan is one tap
- Set quantity, unit and use-by date for each item
- Add items with no barcode by hand
- Review the batch, then export it as a single file
- Send the file to Ops over Cindrix AirLink, or drop it on the drive

## Design rules

- **Camera permission only.** No network, no location.
- **One-way hand-off.** Scan writes an import file. Ops owns the inventory. Scan never changes it directly.
- **Works with no internet.** Product names come from your own catalog, not a lookup service.

## How it fits in the Cindrix line

The import file format lives in [Cindrix-Formats](https://github.com/Stormchyld2/Cindrix-Formats). Files travel by Cindrix AirLink or by copying onto the drive.

Design notes are in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## License

Not chosen yet.
