package com.example.recepy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.recepy.R
import com.example.recepy.data.repository.Recipe
import com.example.recepy.viewmodel.MainViewModel
import kotlinx.coroutines.launch

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
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchGroups()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.groups_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    val shoppingListItems by viewModel.shoppingList.collectAsState()
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
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(12.dp).size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LaunchedEffect(groups, groupRecipes) {
                            isRefreshing = false
                        }
                    } else {
                        IconButton(onClick = { 
                            isRefreshing = true
                            viewModel.fetchGroups()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                        }
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
                    Icon(Icons.Default.GroupAdd, contentDescription = stringResource(R.string.join_group))
                }
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_group))
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
                        stringResource(R.string.no_groups_joined),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { showJoinDialog = true }) {
                        Text(stringResource(R.string.join_existing_group))
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
                        isCreator = group["isCreator"] as? Boolean ?: false,
                        permissions = group["permissions"] as? Int ?: 2,
                        onAddRecipe = { recipe ->
                            viewModel.saveRecipe(recipe)
                        },
                        onLeaveGroup = {
                            viewModel.leaveGroup(id)
                        },
                        onDeleteGroup = {
                            viewModel.deleteGroup(id)
                        },
                        onDeleteRecipe = { recipe ->
                            viewModel.deleteGroupRecipe(id, recipe)
                        },
                        onShareRecipe = { recipe ->
                            viewModel.shareRecipeToGroup(recipe, id)
                        },
                        onUpdatePermissions = { newPerms ->
                            viewModel.updateGroupPermissions(group, newPerms)
                        },
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    if (showJoinDialog) {
        var groupName by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text(stringResource(R.string.join_group_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text(stringResource(R.string.group_name_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.password_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (groupName.isNotBlank() && password.isNotBlank()) {
                        viewModel.joinGroup(groupName, password)
                        showJoinDialog = false
                    }
                }) {
                    Text(stringResource(R.string.join_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
        LaunchedEffect(showJoinDialog) { }
    }

    if (showCreateDialog) {
        var groupName by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(stringResource(R.string.create_group_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text(stringResource(R.string.group_name_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.group_password_hint)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (groupName.isNotBlank() && password.isNotBlank()) {
                        viewModel.createGroup(groupName, password)
                        showCreateDialog = false
                    }
                }) {
                    Text(stringResource(R.string.create_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
        LaunchedEffect(showCreateDialog) { }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupCard(
    name: String,
    id: String,
    recipes: List<Recipe>,
    isCreator: Boolean,
    permissions: Int, // 0: View, 1: Add, 2: Full (Add & Delete)
    onAddRecipe: (Recipe) -> Unit,
    onLeaveGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
    onDeleteRecipe: (Recipe) -> Unit,
    onShareRecipe: (Recipe) -> Unit,
    onUpdatePermissions: (Int) -> Unit,
    viewModel: MainViewModel
) {
    var showLeaveConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddRecipeDialog by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }

    val canAdd = isCreator || permissions >= 1
    val canDelete = isCreator || permissions >= 2

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (isCreator) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.AdminPanelSettings, stringResource(R.string.admin_label), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text(stringResource(R.string.group_code_label, id), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                    if (isCreator) {
                        IconButton(onClick = { showPermissionsDialog = true }) {
                            Icon(Icons.Default.Security, contentDescription = stringResource(R.string.group_permissions_settings), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = { showLeaveConfirm = true }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = stringResource(R.string.leave_group), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isCreator) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = stringResource(R.string.delete_group), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (canAdd) {
                Button(
                    onClick = { showAddRecipeDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PostAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_recipe_to_group))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            
            if (recipes.isEmpty()) {
                Text(
                    stringResource(R.string.no_shared_recipes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Text(
                    stringResource(R.string.shared_recipes_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Column {
                    recipes.forEach { recipe ->
                        key(recipe.title + recipe.sourceUrl) {
                            SharedRecipeItem(
                                recipe = recipe,
                                canDelete = canDelete,
                                onAdd = { onAddRecipe(recipe) },
                                onDelete = { onDeleteRecipe(recipe) },
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddRecipeDialog) {
        AddRecipeToGroupDialog(
            onDismiss = { showAddRecipeDialog = false },
            onShare = onShareRecipe,
            viewModel = viewModel
        )
        LaunchedEffect(showAddRecipeDialog) { }
    }

    if (showPermissionsDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionsDialog = false },
            title = { Text(stringResource(R.string.group_permissions_settings)) },
            text = {
                Column {
                    PermissionOption(stringResource(R.string.perm_view_only), 0, permissions) { onUpdatePermissions(0); showPermissionsDialog = false }
                    PermissionOption(stringResource(R.string.perm_view_add), 1, permissions) { onUpdatePermissions(1); showPermissionsDialog = false }
                    PermissionOption(stringResource(R.string.perm_full_access), 2, permissions) { onUpdatePermissions(2); showPermissionsDialog = false }
                }
            },
            confirmButton = {}
        )
        LaunchedEffect(showPermissionsDialog) { }
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text(stringResource(R.string.leave_group_confirm_title)) },
            text = { Text(stringResource(R.string.leave_group_confirm_message, name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onLeaveGroup()
                        showLeaveConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.leave_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
        LaunchedEffect(showLeaveConfirm) { }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_group_confirm_title)) },
            text = { Text(stringResource(R.string.delete_group_confirm_message, name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteGroup()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_permanently))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
        LaunchedEffect(showDeleteConfirm) { }
    }
}

@Composable
fun SharedRecipeItem(recipe: Recipe, canDelete: Boolean, onAdd: () -> Unit, onDelete: () -> Unit, viewModel: MainViewModel) {

    val savedRecipes by viewModel.savedRecipes.collectAsState()
    val isAlreadyInLibrary = remember(recipe, savedRecipes) {
        savedRecipes.any { it.title.trim().lowercase() == recipe.title.trim().lowercase() }
    }
    
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (!recipe.imageUrl.isNullOrEmpty()) {
                    coil.compose.AsyncImage(
                        model = recipe.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(Icons.Default.RestaurantMenu, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(recipe.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            
            Row {
                if (canDelete) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove_from_group), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    }
                }

                if (!isAlreadyInLibrary) {
                    IconButton(
                        onClick = { 
                            onAdd()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = stringResource(R.string.add_to_my_library),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.padding(12.dp).size(24.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_shared_recipe_title)) },
            text = { Text(stringResource(R.string.delete_shared_recipe_message, recipe.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
        LaunchedEffect(showDeleteConfirm) { }
    }
}

@Composable
fun PermissionOption(text: String, value: Int, current: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = value == current, onClick = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
fun AddRecipeToGroupDialog(
    onDismiss: () -> Unit,
    onShare: (Recipe) -> Unit,
    viewModel: MainViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val savedRecipes by viewModel.savedRecipes.collectAsState()
    var urlText by remember { mutableStateOf("") }
    var freeText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_recipe_to_group_dialog)) },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                PrimaryScrollableTabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) { Text(stringResource(R.string.from_list), modifier = Modifier.padding(8.dp)) }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) { Text(stringResource(R.string.link_tab), modifier = Modifier.padding(8.dp)) }
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) { Text(stringResource(R.string.free_text_tab), modifier = Modifier.padding(8.dp)) }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                when (selectedTab) {
                    0 -> {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(savedRecipes) { recipe ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            onShare(recipe)
                                            onDismiss()
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.MenuBook, null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(recipe.title, maxLines = 1)
                                }
                            }
                        }
                    }
                    1 -> {
                        OutlinedTextField(
                            value = urlText,
                            onValueChange = { urlText = it },
                            label = { Text(stringResource(R.string.paste_recipe_link)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    2 -> {
                        OutlinedTextField(
                            value = freeText,
                            onValueChange = { freeText = it },
                            label = { Text(stringResource(R.string.paste_recipe_text_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTab != 0) {
                Button(onClick = {
                    if (selectedTab == 1 && urlText.isNotBlank()) {
                        // Extract and share
                        scope.launch {
                            val recipe = viewModel.extractRecipeSuspend(urlText)
                            if (recipe != null) onShare(recipe)
                        }
                        onDismiss()
                    } else if (selectedTab == 2 && freeText.isNotBlank()) {
                        // Parse and share
                        viewModel.extractRecipeFromText(freeText, emptyList(), null)
                        onDismiss()
                    }
                }) {
                    Text(stringResource(R.string.add_button))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
