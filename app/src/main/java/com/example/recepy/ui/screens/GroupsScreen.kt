package com.example.recepy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.recepy.data.repository.Recipe
import com.example.recepy.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    onBack: () -> Unit,
    viewModel: MainViewModel
) {
    val groups by viewModel.groups.collectAsState()
    val groupRecipes by viewModel.groupRecipes.collectAsState()
    
    var showJoinDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchGroups()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("קבוצות שיתוף") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזרה")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchGroups() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "רענן")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SmallFloatingActionButton(
                    onClick = { showJoinDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = "הצטרף לקבוצה")
                }
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "צור קבוצה")
                }
            }
        }
    ) { innerPadding ->
        if (groups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "עדיין לא הצטרפת לאף קבוצה",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { showJoinDialog = true }) {
                        Text("הצטרף לקבוצה קיימת")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp, top = 16.dp)
            ) {
                items(groups) { group ->
                    val id = group["id"] as String
                    val name = group["name"] as String
                    val recipes = groupRecipes[id] ?: emptyList()
                    
                    GroupCard(
                        name = name,
                        id = id,
                        recipes = recipes,
                        onAddRecipe = { recipe ->
                            viewModel.saveManualRecipe(
                                recipe.title,
                                recipe.ingredients.joinToString("\n"),
                                recipe.steps.joinToString("\n"),
                                recipe.tags,
                                recipe.imageUrl
                            )
                        },
                        onLeaveGroup = {
                            viewModel.leaveGroup(id)
                        },
                        onDeleteGroup = {
                            viewModel.deleteGroup(id)
                        },
                        onDeleteRecipe = { recipe ->
                            viewModel.deleteGroupRecipe(id, recipe)
                        }
                    )
                }
            }
        }
    }

    if (showJoinDialog) {
        var joinId by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("הצטרפות לקבוצה") },
            text = {
                OutlinedTextField(
                    value = joinId,
                    onValueChange = { joinId = it },
                    label = { Text("קוד קבוצה (6 ספרות)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (joinId.isNotBlank()) {
                        viewModel.joinGroup(joinId)
                        showJoinDialog = false
                    }
                }) {
                    Text("הצטרף")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text("ביטול")
                }
            }
        )
    }

    if (showCreateDialog) {
        var groupName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("יצירת קבוצה חדשה") },
            text = {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("שם הקבוצה") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (groupName.isNotBlank()) {
                        viewModel.createGroup(groupName)
                        showCreateDialog = false
                    }
                }) {
                    Text("צור")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("ביטול")
                }
            }
        )
    }
}

@Composable
fun GroupCard(
    name: String,
    id: String,
    recipes: List<Recipe>,
    onAddRecipe: (Recipe) -> Unit,
    onLeaveGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
    onDeleteRecipe: (Recipe) -> Unit
) {
    var showLeaveConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("קוד: $id", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                    IconButton(onClick = { showLeaveConfirm = true }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "עזוב קבוצה", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "מחק קבוצה", tint = MaterialTheme.colorScheme.error)
                    }
                    Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            
            if (recipes.isEmpty()) {
                Text(
                    "אין מתכונים משותפים בקבוצה זו עדיין",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Text(
                    "מתכונים משותפים:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Column {
                    recipes.forEach { recipe ->
                        key(recipe.title + recipe.sourceUrl) {
                            SharedRecipeItem(
                                recipe = recipe,
                                onAdd = { onAddRecipe(recipe) },
                                onDelete = { onDeleteRecipe(recipe) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("עזיבת קבוצה") },
            text = { Text("האם אתה בטוח שברצונך לעזוב את הקבוצה '$name'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onLeaveGroup()
                        showLeaveConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("עזוב")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) {
                    Text("ביטול")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("מחיקת קבוצה לצמיתות") },
            text = { Text("האם אתה בטוח שברצונך למחוק את הקבוצה '$name' לכל המשתמשים? פעולה זו אינה ניתנת לביטול.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteGroup()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("מחק לצמיתות")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("ביטול")
                }
            }
        )
    }
}

@Composable
fun SharedRecipeItem(recipe: Recipe, onAdd: () -> Unit, onDelete: () -> Unit) {

    var added by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.RestaurantMenu, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
            Text(recipe.title, style = MaterialTheme.typography.bodyLarge)
        }
        
        Row {
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "מחק מהקבוצה", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            }

            IconButton(
                onClick = { 
                    onAdd()
                    added = true
                },
                enabled = !added
            ) {
                Icon(
                    imageVector = if (added) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                    contentDescription = "הוסף לספרייה שלי",
                    tint = if (added) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("מחיקת מתכון משותף") },
            text = { Text("האם אתה בטוח שברצונך למחוק את '${recipe.title}' מהקבוצה?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("מחק")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("ביטול")
                }
            }
        )
    }
}
