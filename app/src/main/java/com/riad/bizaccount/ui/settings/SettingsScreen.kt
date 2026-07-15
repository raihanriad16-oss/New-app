package com.riad.bizaccount.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onManageCategories: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var businessName by remember(settings.businessName) { mutableStateOf(settings.businessName) }
    var currency by remember(settings.currencySymbol) { mutableStateOf(settings.currencySymbol) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showRestartNotice by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val msg = when (event) {
                SettingsEvent.BackupSuccess -> "ব্যাকআপ সফল হয়েছে"
                SettingsEvent.BackupFailure -> "ব্যাকআপ ব্যর্থ হয়েছে"
                SettingsEvent.RestoreSuccess -> { showRestartNotice = true; "পুনরুদ্ধার সফল হয়েছে" }
                SettingsEvent.RestoreFailure -> "পুনরুদ্ধার ব্যর্থ হয়েছে"
            }
            scope.launch { snackbarHostState.showSnackbar(msg) }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("সেটিংস") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(shape = MaterialTheme.shapes.large, elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("ব্যবসার তথ্য", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it; viewModel.updateBusinessName(it) },
                        label = { Text("ব্যবসার নাম") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it; viewModel.updateCurrencySymbol(it) },
                        label = { Text("মুদ্রা প্রতীক") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Card(shape = MaterialTheme.shapes.large, elevation = CardDefaults.cardElevation(2.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ডার্ক মোড", style = MaterialTheme.typography.titleMedium)
                    Switch(checked = settings.darkMode, onCheckedChange = { viewModel.updateDarkMode(it) })
                }
            }

            Card(shape = MaterialTheme.shapes.large, elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("ব্যাকআপ ও পুনরুদ্ধার", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { viewModel.backupNow() }, modifier = Modifier.fillMaxWidth()) { Text("এখনই ব্যাকআপ নিন") }
                    OutlinedButton(onClick = { showRestoreDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("ব্যাকআপ পুনরুদ্ধার করুন") }
                }
            }

            OutlinedButton(onClick = onManageCategories, modifier = Modifier.fillMaxWidth()) { Text("ক্যাটাগরি পরিচালনা করুন") }
        }
    }

    if (showRestoreDialog) {
        val backups = remember { viewModel.listBackups() }
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("ব্যাকআপ নির্বাচন করুন") },
            text = {
                if (backups.isEmpty()) {
                    Text("কোনো ব্যাকআপ পাওয়া যায়নি")
                } else {
                    Column {
                        backups.forEach { file ->
                            TextButton(onClick = {
                                viewModel.restore(file)
                                showRestoreDialog = false
                            }) {
                                Text(formatBackupName(file))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showRestoreDialog = false }) { Text("বাতিল") } }
        )
    }

    if (showRestartNotice) {
        AlertDialog(
            onDismissRequest = { showRestartNotice = false },
            title = { Text("রিস্টার্ট প্রয়োজন") },
            text = { Text("পরিবর্তন কার্যকর করতে অ্যাপটি বন্ধ করে আবার চালু করুন।") },
            confirmButton = { TextButton(onClick = { showRestartNotice = false }) { Text("ঠিক আছে") } }
        )
    }
}

private fun formatBackupName(file: File): String {
    val name = file.nameWithoutExtension.removePrefix("bizaccount_backup_")
    return runCatching {
        val parsed = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).parse(name)
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(parsed!!)
    }.getOrDefault(file.name)
}
