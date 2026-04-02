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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.recepy.viewmodel.MainViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.recepy.R
import com.example.recepy.data.preferences.AppTheme
import com.example.recepy.data.preferences.ThemeMode
import com.example.recepy.ui.theme.BluePrimary
import com.example.recepy.ui.theme.GreenPrimary
import com.example.recepy.ui.theme.OrangePrimary
import com.example.recepy.ui.theme.PinkPrimary
import com.example.recepy.ui.theme.PurplePrimary

// מחלץ domain נקי מ-URL (ללא www. וללא path)
fun extractDomain(url: String): String {
    return try {
        java.net.URI(url).host?.removePrefix("www.") ?: url
    } catch (_: Exception) {
        url
    }
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
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    viewModel: MainViewModel
) {
    val shoppingListItems by viewModel.shoppingList.collectAsState()
    val showDeleteDialog = remember { mutableStateOf(false) }
    val selectedDomains = remember { mutableStateOf<Set<String>>(emptySet()) }
    
    // Developer Mode Logic
    val devClickCount = remember { mutableIntStateOf(0) }
    val lastClickTime = remember { mutableLongStateOf(0L) }
    val showDevLoginDialog = remember { mutableStateOf(false) }
    val devUsername = remember { mutableStateOf("") }
    val devPassword = remember { mutableStateOf("") }
    val loginError = remember { mutableStateOf(false) }

    val allDomains = domainCounts.keys.sorted()

    // ── Developer Login Dialog ─────────────────────────────────────────────
    if (showDevLoginDialog.value) {
        AlertDialog(
            onDismissRequest = { showDevLoginDialog.value = false },
            title = { Text(stringResource(R.string.dev_login_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.dev_login_message))
                    OutlinedTextField(
                        value = devUsername.value,
                        onValueChange = { devUsername.value = it; loginError.value = false },
                        label = { Text(stringResource(R.string.username_label)) },
                        isError = loginError.value
                    )
                    OutlinedTextField(
                        value = devPassword.value,
                        onValueChange = { devPassword.value = it; loginError.value = false },
                        label = { Text(stringResource(R.string.password_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = loginError.value
                    )
                    if (loginError.value) {
                        Text(
                            stringResource(R.string.login_error),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (devUsername.value == "admin" && devPassword.value == "recepy2024") {
                        onDeveloperModeToggle(true)
                        showDevLoginDialog.value = false
                    } else {
                        loginError.value = true
                    }
                }) {
                    Text(stringResource(R.string.login_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDevLoginDialog.value = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // ── Delete dialog ─────────────────────────────────────────────────────────
    if (showDeleteDialog.value) {
        val allSelected = selectedDomains.value.size == allDomains.size

        AlertDialog(
            onDismissRequest = {
                showDeleteDialog.value = false
            },
            title = { Text(stringResource(R.string.delete_by_source_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text(
                        text = stringResource(R.string.delete_by_source_message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // "בחר הכל"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.select_all),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            val total = domainCounts.values.sum()
                            Text(
                                text = stringResource(R.string.recipes_count, total),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Checkbox(
                            checked = allSelected,
                            onCheckedChange = { checked ->
                                selectedDomains.value = if (checked) allDomains.toSet() else emptySet()
                            }
                        )
                    }

                    HorizontalDivider()

                    // שורה לכל domain — פעם אחת בלבד
                    allDomains.forEach { domain ->
                        val count   = domainCounts[domain] ?: 0
                        val checked = domain in selectedDomains.value

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = domain,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = stringResource(R.string.recipes_count, count),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    selectedDomains.value = if (isChecked)
                                        selectedDomains.value + domain
                                    else
                                        selectedDomains.value - domain
                                }
                            )
                        }

                        HorizontalDivider()
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

    // ── Screen ────────────────────────────────────────────────────────────────
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setShowShoppingList(true) }) {
                        BadgedBox(
                            badge = { if (shoppingListItems.isNotEmpty()) Badge { Text(shoppingListItems.size.toString()) } }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = stringResource(id = R.string.shopping_list)
                            )
                        }
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.delete_all_title),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.delete_all_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = { showDeleteDialog.value = true },
                    enabled = domainCounts.isNotEmpty(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete_all_button))
                }
            }

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            // ── Export/Import ─────────────────────────────────────────────────
            Text(stringResource(R.string.export_import_title))
            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            // Export
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.export_recipes_title),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.export_recipes_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = onExportAll,
                    enabled = domainCounts.isNotEmpty()
                ) {
                    Text(stringResource(R.string.export_button))
                }
            }

            // Import
            val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
            ) { uri ->
                if (uri != null) onImportFromFile(uri)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.import_recipes_title),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.import_recipes_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = { filePickerLauncher.launch("application/json") }
                ) {
                    Text(stringResource(R.string.import_button))
                }
            }

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            // ── Updates ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.update_system_recipes_title),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.update_system_recipes_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = onCheckForUpdates,
                    enabled = !isImporting
                ) {
                    Text(stringResource(R.string.check_for_updates))
                }
            }

            // הודעת סטטוס קטנה מתחת לכפתור
            systemUpdateMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            // ── App Update ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.update_app_title),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.update_app_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = onCheckAppUpdate
                ) {
                    Text(stringResource(R.string.check_for_updates))
                }
            }

            // הודעת סטטוס קטנה מתחת לכפתור
            appUpdateMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            downloadProgress?.let { progress ->
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            // ── Bug Report / Suggestion ──────────────────────────────────────────
            val reportContent = remember { mutableStateOf("") }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.report_bug_suggestion_title),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.report_bug_suggestion_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = reportContent.value,
                    onValueChange = { reportContent.value = it },
                    label = { Text(stringResource(R.string.bug_report_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                OutlinedButton(
                    onClick = {
                        if (reportContent.value.isNotBlank()) {
                            onReportBug(reportContent.value)
                            reportContent.value = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                    enabled = reportContent.value.isNotBlank()
                ) {
                    Text(stringResource(R.string.send_report_button))
                }
            }
            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            // ── App Info ──────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val totalRecipes = domainCounts.values.sum()
                Text(
                    text = stringResource(R.string.total_recipes_count, totalRecipes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                val context = LocalContext.current
                val packageInfo = remember {
                    runCatching {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                        } else {
                            @Suppress("DEPRECATION")
                            context.packageManager.getPackageInfo(context.packageName, 0)
                        }
                    }.getOrNull()
                }
                val versionName = packageInfo?.versionName ?: "1.0.0"
                
                Text(
                    text = stringResource(R.string.version_label, versionName),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastClickTime.longValue > 2000) {
                            devClickCount.intValue = 1
                        } else {
                            devClickCount.intValue++
                        }
                        lastClickTime.longValue = currentTime
                        
                        if (devClickCount.intValue >= 8 && !isDeveloper) {
                            showDevLoginDialog.value = true
                            devClickCount.intValue = 0
                        }
                    }
                )

                if (isDeveloper) {
                    Text(
                        text = stringResource(R.string.dev_mode_active),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    TextButton(onClick = { onDeveloperModeToggle(false) }) {
                        Text(stringResource(R.string.disable_dev_mode), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeMode.toLabel(): String {
    return when (this) {
        ThemeMode.LIGHT  -> stringResource(R.string.theme_light)
        ThemeMode.DARK   -> stringResource(R.string.theme_dark)
        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
    }
}