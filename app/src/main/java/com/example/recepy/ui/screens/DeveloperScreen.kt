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
            TopAppBar(
                title = { Text("ארגז כלים למפתחים") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזרה")
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
            Text("ברוך הבא למצב מפתח!", style = MaterialTheme.typography.headlineSmall)
            
            DeveloperCard(
                title = "באגים שדווחו",
                icon = Icons.Default.BugReport,
                items = reportedBugs,
                emptyText = "אין דיווחי באגים חדשים",
                onDeleteItem = { viewModel.removeReportedBug(it) }
            )

            DeveloperCard(
                title = "מתכונים מבוקשים (חיפושים ללא תוצאות)",
                icon = Icons.Default.Lightbulb,
                items = suggestedRecipes,
                emptyText = "אין בקשות חדשות",
                onDeleteItem = { viewModel.removeSuggestedRecipe(it) }
            )
            
            DeveloperCard(
                title = "באגים בטיפול",
                icon = Icons.Default.BugReport,
                items = listOf(
                    "pluralization של מספרים מורכבים (כוס וחצי) - שופר",
                    "זיהוי כותרות מאתרים עם מבנה HTML לא סטנדרטי - בבדיקה"
                )
            )

            DeveloperCard(
                title = "מתכונים מתוכננים",
                icon = Icons.Default.Info,
                items = listOf(
                    "עוגת ביסקוויטים קלאסית",
                    "שקשוקה בלקנית",
                    "פסטה ברוטב ורוד"
                )
            )

            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                "כלים מתקדמים (DB Inspector, Logs) יתווספו בגרסאות הבאות.",
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
                                    contentDescription = "מחיקה",
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
