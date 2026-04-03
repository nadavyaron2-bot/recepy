package com.example.recepy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.recepy.R
import com.example.recepy.data.preferences.AppTheme
import com.example.recepy.data.preferences.ThemeMode
import com.example.recepy.ui.theme.BluePrimary
import com.example.recepy.ui.theme.GreenPrimary
import com.example.recepy.ui.theme.OrangePrimary
import com.example.recepy.ui.theme.PinkPrimary
import com.example.recepy.ui.theme.PurplePrimary
import com.example.recepy.viewmodel.MainViewModel

// מחלץ domain נקי מ-URL (ללא www. וללא path)
fun extractDomain(url: String): String {
    return try {
        java.net.URI(url).host?.removePrefix("www.") ?: url
    } catch (_: Exception) {
        url
    }
}

@Composable
fun ThemeMode.toLabel(): String = when (this) {
    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
    ThemeMode.DARK -> stringResource(R.string.theme_dark)
    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    themeMode: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    appTheme: AppTheme,
    onAppThemeSelected: (AppTheme) -> Unit,
    onBack: () -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnToggle: () -> Unit,
    domainCounts: Map<String, Int>,
    onDeleteByDomains: (Set<String>) -> Unit,
    onExportAll: () -> Unit = {},
    onImportFromFile: (android.net.Uri) -> Unit = {},
    isImporting: Boolean = false,
    systemUpdateMessage: String? = null,
    appUpdateMessage: String? = null,
    onCheckForUpdates: () -> Unit = {},
    onCheckAppUpdate: () -> Unit = {},
    downloadProgress: Float? = null,
    isDeveloper: Boolean = false,
    onDeveloperModeToggle: (Boolean) -> Unit = {},
    onReportBug: (String) -> Unit = {},
    snackbarHostState: SnackbarHostState,
    viewModel: MainViewModel
) {
    val showDeleteDialog = remember { mutableStateOf(false) }
    val selectedDomains = remember { mutableStateOf(setOf<String>()) }

    if (showDeleteDialog.value) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog.value = false },
            title = { Text(stringResource(R.string.delete_by_source_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    domainCounts.keys.sorted().forEach { domain ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val current = selectedDomains.value.toMutableSet()
                                    if (current.contains(domain)) current.remove(domain)
                                    else current.add(domain)
                                    selectedDomains.value = current
                                }
                        ) {
                            Checkbox(
                                checked = selectedDomains.value.contains(domain),
                                onCheckedChange = { checked ->
                                    val current = selectedDomains.value.toMutableSet()
                                    if (checked) current.add(domain)
                                    else current.remove(domain)
                                    selectedDomains.value = current
                                }
                            )
                            Text(
                                text = "$domain (${domainCounts[domain]})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteByDomains(selectedDomains.value)
                        showDeleteDialog.value = false
                    },
                    enabled = selectedDomains.value.isNotEmpty(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    val count = selectedDomains.value.sumOf { domainCounts[it] ?: 0 }
                    Text(stringResource(R.string.delete_confirm) + " ($count)")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog.value = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.appearance_title))
            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            // ── Updates ──────────────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.update_app_title),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.update_app_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (appUpdateMessage != null) {
                    Text(
                        text = appUpdateMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                if (downloadProgress != null) {
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    )
                }

                OutlinedButton(
                    onClick = { onCheckAppUpdate() },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.check_for_updates))
                }
            }

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            Text(
                text = stringResource(R.string.display_mode_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = mode == themeMode,
                        onClick = { onThemeSelected(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ThemeMode.entries.size
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(mode.toLabel())
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.theme_color_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppTheme.entries.forEach { theme ->
                    val color = when (theme) {
                        AppTheme.ORANGE -> OrangePrimary
                        AppTheme.BLUE -> BluePrimary
                        AppTheme.GREEN -> GreenPrimary
                        AppTheme.PINK -> PinkPrimary
                        AppTheme.PURPLE -> PurplePrimary
                    }
                    val isSelected = theme == appTheme
                    
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { onAppThemeSelected(theme) }
                            .padding(if (isSelected) 4.dp else 0.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.3f))
                            )
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            // ── Keep screen on ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.keep_screen_on_title),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.keep_screen_on_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = keepScreenOn,
                    onCheckedChange = { onKeepScreenOnToggle() }
                )
            }

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            // ── Data ──────────────────────────────────────────────────────────
            Text(stringResource(R.string.data_title))
            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            OutlinedButton(
                onClick = { onExportAll() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.export_recipes_title))
            }

            val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) onImportFromFile(uri)
            }

            OutlinedButton(
                onClick = { launcher.launch(arrayOf("application/json")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.import_recipes_title))
            }

            OutlinedButton(
                onClick = { showDeleteDialog.value = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.delete_by_source_title))
            }

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            // ── Developer Options ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.dev_mode_active))
                Switch(
                    checked = isDeveloper,
                    onCheckedChange = { onDeveloperModeToggle(it) }
                )
            }

            if (isDeveloper) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val bugText = remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = bugText.value,
                        onValueChange = { bugText.value = it },
                        label = { Text(stringResource(R.string.report_bug_suggestion_title)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = { 
                            onReportBug(bugText.value)
                            bugText.value = ""
                        },
                        modifier = Modifier.align(Alignment.End),
                        enabled = bugText.value.isNotBlank()
                    ) {
                        Text(stringResource(R.string.send_report_button))
                    }
                }
            }
        }
    }
}
