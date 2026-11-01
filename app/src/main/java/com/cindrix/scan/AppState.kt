package com.cindrix.scan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/** Everything the screens show, saved to disk on every change. */
class AppState(private val store: Store) {

    val batch = mutableStateListOf<BatchItem>()
    private val catalog = LinkedHashMap<String, CatalogEntry>()

    var callsign by mutableStateOf(store.callsign)
        private set

    var folderUri by mutableStateOf(store.folderUri)
        private set

    var lastLocation by mutableStateOf(store.lastLocation)
        private set

    /** Fast mode: a known item asks only for quantity and use-by date. Off by default. */
    var fastMode by mutableStateOf(store.fastMode)
        private set

    var soundOn by mutableStateOf(store.soundOn)
        private set

    var vibrateOn by mutableStateOf(store.vibrateOn)
        private set

    init {
        batch.addAll(store.loadBatch())
        catalog.putAll(store.loadCatalog())
    }

    fun updateCallsign(value: String) {
        callsign = Callsign.clean(value)
        store.callsign = callsign
    }

    fun setFolder(uri: String?) {
        folderUri = uri
        store.folderUri = uri
    }

    fun rememberLocation(value: String) {
        lastLocation = value
        store.lastLocation = value
    }

    fun updateFastMode(on: Boolean) {
        fastMode = on
        store.fastMode = on
    }

    fun updateSound(on: Boolean) {
        soundOn = on
        store.soundOn = on
    }

    fun updateVibrate(on: Boolean) {
        vibrateOn = on
        store.vibrateOn = on
    }

    fun source(): String = callsign.ifBlank { "Callsign" }

    fun lookup(barcode: String): CatalogEntry? = catalog[barcode]

    /** Remember what a barcode is, so the next scan fills in the name. */
    fun remember(item: BatchItem) {
        if (item.barcode.isEmpty()) return
        catalog[item.barcode] = CatalogEntry(item.barcode, item.name, item.category, item.unit, item.useBy)
        store.saveCatalog(catalog)
    }

    /** Adds the item, or adds to an identical line already in the batch. Returns true if merged. */
    fun addOrMerge(item: BatchItem): Boolean {
        val index = if (item.barcode.isEmpty()) {
            -1
        } else {
            batch.indexOfFirst {
                it.barcode == item.barcode &&
                    it.unit == item.unit &&
                    it.useBy == item.useBy &&
                    it.location == item.location
            }
        }
        return if (index >= 0) {
            val existing = batch[index]
            batch[index] = existing.copy(qty = existing.qty + item.qty)
            persist()
            true
        } else {
            batch.add(item)
            persist()
            false
        }
    }

    fun update(old: BatchItem, new: BatchItem) {
        val index = batch.indexOfFirst { it.scanId == old.scanId }
        if (index >= 0) {
            batch[index] = new
            persist()
        }
    }

    fun remove(item: BatchItem) {
        batch.removeAll { it.scanId == item.scanId }
        persist()
    }

    fun clear() {
        batch.clear()
        persist()
    }

    fun newItem(barcode: String): Pair<BatchItem, Boolean> {
        val known = if (barcode.isNotEmpty()) lookup(barcode) else null
        val item = BatchItem(
            scanId = UUID.randomUUID().toString(),
            name = known?.name ?: "",
            barcode = barcode,
            qty = 1.0,
            unit = known?.unit ?: "ea",
            useBy = "",
            category = known?.category ?: "",
            location = lastLocation,
            scannedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString(),
        )
        return Pair(item, known != null)
    }

    /** File name and text of the import file for the current batch. */
    fun exportFile(): Pair<String, String> {
        val name = InventoryCsv.fileName(source(), Instant.now())
        val text = InventoryCsv.build(batch.toList(), source())
        return Pair(name, text)
    }

    private fun persist() {
        store.saveBatch(batch.toList())
    }
}
