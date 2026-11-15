package com.example.ytdownloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DownloaderScreen()
                }
            }
        }
    }
}

data class FormatOption(val id: String, val label: String, val description: String)

val formatOptions = listOf(
    FormatOption("mp4", "MP4 (видео)", "Лучшее качество до 1080p, со звуком"),
    FormatOption("mp3", "MP3 (аудио)", "Только звук, высокое качество"),
    FormatOption("m4a", "M4A (аудио)", "Только звук, оригинальное качество"),
    FormatOption("wav", "WAV (аудио)", "Без потерь, большой размер")
)

@Composable
fun FormatPickerDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var selected by remember { mutableStateOf(formatOptions.first().id) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("В каком формате скачать?") },
        text = {
            Column(Modifier.selectableGroup()) {
                formatOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected == option.id,
                                onClick = { selected = option.id },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == option.id, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(option.label, style = MaterialTheme.typography.bodyLarge)
                            Text(option.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("Скачать") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun DownloaderScreen(viewModel: DownloaderViewModel = viewModel()) {
    var url by remember { mutableStateOf("") }
    var showFormatDialog by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val progress by viewModel.progress.collectAsState()
    val status by viewModel.status.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Скачать с YouTube", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Вставьте ссылку YouTube") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            trailingIcon = {
                IconButton(onClick = { clipboard.getText()?.text?.let { url = it } }) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Вставить")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { showFormatDialog = true },
            enabled = url.isNotBlank() && !isDownloading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isDownloading) "Скачивание..." else "Скачать")
        }
        if (isDownloading) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Text("${(progress * 100).toInt()}%")
        }
        status?.let {
            Card(Modifier.fillMaxWidth()) { Text(it, Modifier.padding(12.dp)) }
        }
    }

    if (showFormatDialog) {
        FormatPickerDialog(
            onDismiss = { showFormatDialog = false },
            onConfirm = { format ->
                showFormatDialog = false
                viewModel.download(url, format)
            }
        )
    }
}
