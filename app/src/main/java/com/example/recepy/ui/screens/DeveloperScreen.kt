package com.example.recepy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
//import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recepy.viewmodel.MainViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

import androidx.compose.ui.res.stringResource
import com.example.recepy.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreen(
    onBack: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.fetchDeveloperData()
    }
    val suggestedRecipes by viewModel.suggestedRecipesForDev.collectAsState()
    val reportedBugs by viewModel.reportedBugs.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.developer_tools_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_desc))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.welcome_dev_mode), style = MaterialTheme.typography.headlineSmall)
            
            DeveloperCard(
                title = stringResource(R.string.reported_bugs_title),
                icon = Icons.Default.BugReport,
                items = reportedBugs,
                emptyText = stringResource(R.string.no_new_bugs),
                onDeleteItem = { viewModel.removeReportedBug(it) }
            )

            DeveloperCard(
                title = stringResource(R.string.suggested_recipes_title),
                icon = Icons.Default.Lightbulb,
                items = suggestedRecipes,
                emptyText = stringResource(R.string.no_new_requests),
                onDeleteItem = { viewModel.removeSuggestedRecipe(it) }
            )
            
            DeveloperCard(
                title = stringResource(R.string.bugs_in_progress_title),
                icon = Icons.Default.BugReport,
                items = listOf(
                    "pluralization של מספרים מורכבים (כוס וחצי) - שופר",
                    "זיהוי כותרות מאתרים עם מבנה HTML לא סטנדרטי - בבדיקה"
                )
            )

            DeveloperCard(
                title = stringResource(R.string.planned_recipes_title),
                icon = Icons.Default.Info,
                items = listOf(
                    "עוגת ביסקוויטים קלאסית",
                    "שקשוקה בלקנית",
                    "פסטה ברוטב ורוד"
                )
            )

            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                stringResource(R.string.advanced_tools_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DeveloperCard(
    title: String,
    icon: ImageVector,
    items: List<String>,
    emptyText: String = "",
    onDeleteItem: ((String) -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (items.isEmpty() && emptyText.isNotEmpty()) {
                Text("• $emptyText", style = MaterialTheme.typography.bodyMedium)
            } else {
                items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("• $item", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        if (onDeleteItem != null) {
                            IconButton(
                                onClick = { onDeleteItem(item) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete_desc),
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
