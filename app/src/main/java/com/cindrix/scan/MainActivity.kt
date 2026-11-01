package com.cindrix.scan

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/** Which screen is showing. */
sealed class Screen {
    object Home : Screen()
    object Scan : Screen()
    object Settings : Screen()
    data class Form(val item: BatchItem, val isNew: Boolean, val known: Boolean, val fromScan: Boolean) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        val store = Store(filesDir, getSharedPreferences("scan", Context.MODE_PRIVATE))
        val state = AppState(store)

        setContent {
            CindrixTheme {
                App(state)
            }
        }
    }
}

@Composable
fun App(state: AppState) {
    val context = LocalContext.current
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    fun toast(text: String) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            } catch (e: Exception) {
                // Some folders do not allow a lasting grant; saving may still work this session.
            }
            state.setFolder(uri.toString())
            toast("Folder set")
        }
    }

    val doSave: () -> Unit = {
        val folder = state.folderUri
        if (folder == null) {
            toast("Choose your Cindrix folder first, then tap Save again")
            pickFolder.launch(null)
        } else {
            val file = state.exportFile()
            val result = Exporter.saveToFolder(context, Uri.parse(folder), file.first, file.second)
            if (result.isSuccess) {
                savedMessage = "Saved " + state.batch.size + " items to inventory/" + (result.getOrNull() ?: file.first) +
                    ". Clear the batch now?"
            } else {
                toast("Could not save: " + (result.exceptionOrNull()?.message ?: "unknown error"))
            }
        }
    }

    val doSend: () -> Unit = {
        val file = state.exportFile()
        val result = Exporter.share(context, file.first, file.second)
        if (result.isFailure) {
            toast("Could not send: " + (result.exceptionOrNull()?.message ?: "unknown error"))
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        when (val current = screen) {
            Screen.Home -> HomeScreen(
                state = state,
                onScan = { screen = Screen.Scan },
                onManual = {
                    val made = state.newItem("")
                    screen = Screen.Form(made.first, isNew = true, known = false, fromScan = false)
                },
                onEdit = { item -> screen = Screen.Form(item, isNew = false, known = false, fromScan = false) },
                onSettings = { screen = Screen.Settings },
                onSave = doSave,
                onSend = doSend,
                onClear = { confirmClear = true },
            )

            Screen.Scan -> ScanScreen(
                onCode = { code ->
                    if (screen is Screen.Scan) {
                        val made = state.newItem(code)
                        screen = Screen.Form(made.first, isNew = true, known = made.second, fromScan = true)
                    }
                },
                onManual = {
                    val made = state.newItem("")
                    screen = Screen.Form(made.first, isNew = true, known = false, fromScan = true)
                },
                onDone = { screen = Screen.Home },
            )

            is Screen.Form -> FormScreen(
                initial = current.item,
                isNew = current.isNew,
                known = current.known,
                onSave = { item ->
                    if (current.isNew) {
                        val merged = state.addOrMerge(item)
                        toast(if (merged) "Added to the matching line" else "Added")
                    } else {
                        state.update(current.item, item)
                    }
                    state.remember(item)
                    state.setLastLocation(item.location)
                    screen = if (current.fromScan) Screen.Scan else Screen.Home
                },
                onDelete = if (current.isNew) null else ({
                    state.remove(current.item)
                    screen = Screen.Home
                }),
                onCancel = { screen = if (current.fromScan) Screen.Scan else Screen.Home },
            )

            Screen.Settings -> SettingsScreen(
                state = state,
                onPickFolder = { pickFolder.launch(null) },
                onBack = { screen = Screen.Home },
            )
        }
    }

    val message = savedMessage
    if (message != null) {
        AlertDialog(
            onDismissRequest = { savedMessage = null },
            title = { Text("Saved") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    state.clear()
                    savedMessage = null
                }) { Text("CLEAR BATCH") }
            },
            dismissButton = {
                TextButton(onClick = { savedMessage = null }) { Text("KEEP IT") }
            },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear the batch?") },
            text = { Text("This removes all " + state.batch.size + " lines that have not been exported.") },
            confirmButton = {
                TextButton(onClick = {
                    state.clear()
                    confirmClear = false
                }) { Text("CLEAR") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("CANCEL") }
            },
        )
    }
}
