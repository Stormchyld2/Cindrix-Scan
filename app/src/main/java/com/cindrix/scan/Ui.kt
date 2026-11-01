package com.cindrix.scan

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// ---------------------------------------------------------------- shared pieces

@Composable
fun BrandHeader(subtitle: String) {
    Column {
        Row {
            Text("CINDRIX ", fontFamily = HeadingFont, fontSize = 26.sp, color = MaterialTheme.colorScheme.onBackground)
            Text("SCAN", fontFamily = HeadingFont, fontSize = 26.sp, color = MaterialTheme.colorScheme.primary)
        }
        Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun Btn(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    outlined: Boolean = false,
) {
    if (outlined) {
        OutlinedButton(onClick = onClick, modifier = modifier.height(52.dp), enabled = enabled, shape = Round) {
            Text(text, fontFamily = HeadingFont, fontSize = 15.sp)
        }
    } else {
        Button(onClick = onClick, modifier = modifier.height(52.dp), enabled = enabled, shape = Round) {
            Text(text, fontFamily = HeadingFont, fontSize = 15.sp)
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, fontFamily = HeadingFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun ChipRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option) },
            )
        }
    }
}

// ---------------------------------------------------------------- home

@Composable
fun HomeScreen(
    state: AppState,
    onScan: () -> Unit,
    onManual: () -> Unit,
    onEdit: (BatchItem) -> Unit,
    onSettings: () -> Unit,
    onSave: () -> Unit,
    onSend: () -> Unit,
    onClear: () -> Unit,
) {
    val hasItems = state.batch.isNotEmpty()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                BrandHeader(state.source() + "  |  " + state.batch.size + " in batch")
            }
            TextButton(onClick = onSettings) { Text("SETTINGS", fontFamily = HeadingFont) }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Btn("SCAN", onScan, Modifier.weight(1f))
            Btn("ADD BY HAND", onManual, Modifier.weight(1f), outlined = true)
        }
        Spacer(Modifier.height(12.dp))
        if (!hasItems) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "No items yet.\nTap SCAN and point the camera at a barcode.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.batch, key = { it.scanId }) { item ->
                    ItemRow(item) { onEdit(item) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Btn("SAVE TO FOLDER", onSave, Modifier.weight(1f), enabled = hasItems)
            Btn("SEND", onSend, Modifier.weight(1f), enabled = hasItems, outlined = true)
        }
        TextButton(onClick = onClear, enabled = hasItems, modifier = Modifier.align(Alignment.End)) {
            Text("CLEAR BATCH", fontFamily = HeadingFont)
        }
    }
}

@Composable
fun ItemRow(item: BatchItem, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        val bits = listOfNotNull(
            InventoryCsv.qtyText(item.qty) + " " + item.unit,
            if (item.useBy.isNotEmpty()) "use by " + item.useBy else null,
            if (item.category.isNotEmpty()) item.category else null,
            if (item.location.isNotEmpty()) item.location else null,
        )
        Text(bits.joinToString("  |  "), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------- scan

@Composable
fun ScanScreen(onCode: (String) -> Unit, onManual: () -> Unit, onDone: () -> Unit) {
    BackHandler(onBack = onDone)
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.CAMERA)
    }

    var torch by remember { mutableStateOf(false) }
    var hasTorch by remember { mutableStateOf(false) }

    if (!granted) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Scan needs the camera to read barcodes. Nothing is recorded or sent anywhere.",
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Btn("ALLOW CAMERA", { launcher.launch(Manifest.permission.CAMERA) }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Btn("ADD BY HAND", onManual, Modifier.fillMaxWidth(), outlined = true)
            Spacer(Modifier.height(8.dp))
            Btn("BACK", onDone, Modifier.fillMaxWidth(), outlined = true)
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        CameraView(torchOn = torch, onCode = onCode, onTorchSupport = { hasTorch = it })
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f)
                .height(140.dp)
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
        )
        Text(
            "Line the barcode up inside the box",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x99000000))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Btn("DONE", onDone, Modifier.weight(1f))
            Btn("BY HAND", onManual, Modifier.weight(1f), outlined = true)
            if (hasTorch) {
                Btn(if (torch) "LIGHT ON" else "LIGHT", { torch = !torch }, Modifier.weight(1f), outlined = true)
            }
        }
    }
}

// ---------------------------------------------------------------- item form

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UseByPicker(onDate: (String) -> Unit, onDismiss: () -> Unit) {
    val pickerState = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = pickerState.selectedDateMillis
                if (millis != null) {
                    onDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString())
                }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = pickerState)
    }
}

@Composable
fun FormScreen(
    initial: BatchItem,
    isNew: Boolean,
    known: Boolean,
    onSave: (BatchItem) -> Unit,
    onDelete: (() -> Unit)?,
    onCancel: () -> Unit,
) {
    BackHandler(onBack = onCancel)

    var name by remember(initial.scanId) { mutableStateOf(initial.name) }
    var qtyText by remember(initial.scanId) { mutableStateOf(InventoryCsv.qtyText(initial.qty)) }
    var unit by remember(initial.scanId) { mutableStateOf(initial.unit) }
    var useBy by remember(initial.scanId) { mutableStateOf(initial.useBy) }
    var category by remember(initial.scanId) { mutableStateOf(initial.category) }
    var location by remember(initial.scanId) { mutableStateOf(initial.location) }
    var picking by remember(initial.scanId) { mutableStateOf(false) }

    val qty: Double? = qtyText.toDoubleOrNull()
    val valid = name.isNotBlank() && qty != null && qty > 0.0

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BrandHeader(if (isNew) "New item" else "Edit item")

        if (initial.barcode.isNotEmpty()) {
            Text(
                "Barcode " + initial.barcode + if (known) "  |  known item" else "  |  new to the catalog",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Item name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Btn("-", {
                val current = qtyText.toDoubleOrNull() ?: 0.0
                qtyText = InventoryCsv.qtyText(if (current > 1.0) current - 1.0 else 0.0)
            }, Modifier.width(60.dp), outlined = true)
            OutlinedTextField(
                value = qtyText,
                onValueChange = { text -> qtyText = text.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Quantity") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            Btn("+", {
                val current = qtyText.toDoubleOrNull() ?: 0.0
                qtyText = InventoryCsv.qtyText(current + 1.0)
            }, Modifier.width(60.dp), outlined = true)
        }

        SectionLabel("UNIT")
        ChipRow(Units.all, unit) { unit = it }

        SectionLabel("USE BY: " + if (useBy.isEmpty()) "none" else useBy)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val today = LocalDate.now()
            FilterChip(selected = false, onClick = { useBy = "" }, label = { Text("None") })
            FilterChip(selected = false, onClick = { useBy = today.plusMonths(6).toString() }, label = { Text("+6 mo") })
            FilterChip(selected = false, onClick = { useBy = today.plusYears(1).toString() }, label = { Text("+1 yr") })
            FilterChip(selected = false, onClick = { useBy = today.plusYears(2).toString() }, label = { Text("+2 yr") })
            FilterChip(selected = false, onClick = { useBy = today.plusYears(5).toString() }, label = { Text("+5 yr") })
            FilterChip(selected = false, onClick = { picking = true }, label = { Text("Pick date") })
        }

        SectionLabel("CATEGORY (OPTIONAL)")
        ChipRow(Categories.all, category) { category = if (category == it) "" else it }

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Btn(
            "SAVE",
            {
                onSave(
                    initial.copy(
                        name = name.trim(),
                        qty = qty ?: 1.0,
                        unit = unit,
                        useBy = useBy,
                        category = category,
                        location = location.trim(),
                    )
                )
            },
            Modifier.fillMaxWidth(),
            enabled = valid,
        )
        Btn("CANCEL", onCancel, Modifier.fillMaxWidth(), outlined = true)
        if (onDelete != null) {
            Btn("DELETE THIS LINE", onDelete, Modifier.fillMaxWidth(), outlined = true)
        }
    }

    if (picking) {
        UseByPicker(onDate = { useBy = it }, onDismiss = { picking = false })
    }
}

// ---------------------------------------------------------------- settings

@Composable
fun SettingsScreen(state: AppState, onPickFolder: () -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    val folder = state.folderUri
    val folderLabel = if (folder == null) {
        "Not chosen yet"
    } else {
        (Uri.parse(folder).lastPathSegment ?: folder).replace(':', '/')
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BrandHeader("Settings")

        SectionLabel("DEVICE CALLSIGN")
        OutlinedTextField(
            value = state.callsign,
            onValueChange = { state.updateCallsign(it) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "This name goes into every export so Ops knows which phone the items came from.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Btn("NEW RANDOM CALLSIGN", { state.updateCallsign(Callsign.generate()) }, Modifier.fillMaxWidth(), outlined = true)

        SectionLabel("CINDRIX FOLDER")
        Text(folderLabel)
        Text(
            "Pick your Cindrix folder (for example Documents/Cindrix). Scan saves its files in an inventory folder inside it.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Btn("CHOOSE FOLDER", onPickFolder, Modifier.fillMaxWidth())

        Btn("BACK", onBack, Modifier.fillMaxWidth(), outlined = true)
        Text(
            "Cindrix Scan " + version,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
