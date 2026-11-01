package com.cindrix.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class InventoryCsvTest {

    private fun item(name: String = "Beans", qty: Double = 2.0) = BatchItem(
        scanId = "id-1",
        name = name,
        barcode = "012345678905",
        qty = qty,
        unit = "can",
        useBy = "2027-05-01",
        category = "Food",
        location = "Pantry",
        scannedAt = "2026-11-01T15:04:05Z",
    )

    @Test
    fun startsWithMarkerAndHeader() {
        val lines = InventoryCsv.build(emptyList(), "Falcon-27").lines()
        assertEquals("#cindrix-inventory-import v1", lines[0])
        assertEquals("scan_id,item,barcode,qty,unit,use_by,category,location,scanned_at,source", lines[1])
    }

    @Test
    fun writesOneRowPerItem() {
        val lines = InventoryCsv.build(listOf(item()), "Falcon-27").lines()
        assertEquals("id-1,Beans,012345678905,2,can,2027-05-01,Food,Pantry,2026-11-01T15:04:05Z,Falcon-27", lines[2])
    }

    @Test
    fun quotesCommasAndQuotes() {
        val lines = InventoryCsv.build(listOf(item(name = "Soup, \"tomato\"")), "Falcon-27").lines()
        assertTrue(lines[2].startsWith("id-1,\"Soup, \"\"tomato\"\"\",012345678905,"))
    }

    @Test
    fun wholeQuantitiesHaveNoDecimalPoint() {
        assertEquals("2", InventoryCsv.qtyText(2.0))
        assertEquals("2.5", InventoryCsv.qtyText(2.5))
    }

    @Test
    fun fileNameUsesUtcStamp() {
        val name = InventoryCsv.fileName("Falcon-27", Instant.parse("2026-11-01T15:04:05Z"))
        assertEquals("inventory-Falcon-27-20261101-150405.csv", name)
    }

    @Test
    fun fileNameReplacesUnsafeCharacters() {
        val name = InventoryCsv.fileName("my phone/1", Instant.parse("2026-11-01T15:04:05Z"))
        assertEquals("inventory-my_phone_1-20261101-150405.csv", name)
    }

    @Test
    fun generatedCallsignLooksRight() {
        assertTrue(Regex("[A-Za-z]+-[0-9]{2}").matches(Callsign.generate()))
    }
}
