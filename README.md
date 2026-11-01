<p align="center"><img src="design/logo/cindrix-scan-wordmark-on-dark.png" alt="Cindrix Scan" width="520"></p>

# Cindrix-Scan

A barcode scanner that adds your supplies to the Cindrix Ops inventory. Scan a can or box, set the quantity and use-by date, and send the batch to Ops.

| | |
|---|---|
| Android app ID | `com.cindrix.scan` |
| Platform | Android |
| Version | 0.5.0 |
| Status | Builds and passes its tests. Not yet tested on a phone. |

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

## Get the APK

Every push to `main` builds the app in GitHub Actions. Open the latest run on the Actions tab and download the `CindrixScan-android` artifact. It is a zip with one file in it, `CindrixScan-<version>-android.apk`. Install that on the phone.

Builds are signed with the keystore in `app/`, so each new build installs over the last one. That key is public and is only there to keep updates working. To sign with your own private key instead, add these repository secrets and the workflow uses them automatically: `SCAN_KEYSTORE_B64`, `SCAN_KEYSTORE_PASSWORD`, `SCAN_KEY_ALIAS`, `SCAN_KEY_PASSWORD`.

## Build notes

Kotlin, Jetpack Compose, CameraX and ZXing (Apache-2.0). Minimum Android 8.0 (API 26). The workflow also runs the unit tests for the import file writer.

## How it fits in the Cindrix line

The import file format lives in [Cindrix-Formats](https://github.com/Stormchyld2/Cindrix-Formats). Files travel by Cindrix AirLink or by copying onto the drive.

Design notes are in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## License

Not chosen yet.
