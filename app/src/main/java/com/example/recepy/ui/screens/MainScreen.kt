package com.example.recepy.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.recepy.R
import com.example.recepy.data.repository.Recipe
import com.example.recepy.viewmodel.MainViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    urlInput: String,
    onUrlChanged: (String) -> Unit,
    onExtractClick: () -> Unit,
    onExtractFromText: (String, List<String>, String?) -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
    isLoading: Boolean,
    savedRecipes: List<Recipe>,
    filteredRecipes: List<Recipe>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onSavedRecipeClick: (Long) -> Unit,
    onDeleteRecipe: (Long) -> Unit,
    sortByAlpha: Boolean,
    onSortToggle: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadSeedRecipes()
    }

    var recipeToDelete by remember { mutableStateOf<Recipe?>(null) }
    val showPasteSheet by viewModel.showAddDialog.collectAsState()
    fun setShowPasteSheet(show: Boolean) = viewModel.setShowAddDialog(show)

    val showShoppingList by viewModel.showShoppingList.collectAsState()

    var isSearchFocused by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0
    val compactSearchMode = isKeyboardOpen && isSearchFocused
    val noResults = savedRecipes.isNotEmpty() && filteredRecipes.isEmpty()

    val selectedTagFilters by viewModel.selectedTagFilters.collectAsState()
    val predefinedTags = viewModel.predefinedTags
    val lastCookedRecipe by viewModel.lastCookedRecipe.collectAsState()
    val searchByIngredients by viewModel.searchByIngredients.collectAsState()
    val shoppingListItems by viewModel.shoppingList.collectAsState()
    val context = LocalContext.current

    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    var useManualInput by remember { mutableStateOf(prefs.getBoolean("use_manual_input", true)) }

    Scaffold(
        modifier = modifier,
        topBar = {
            AnimatedVisibility(
                visible = !compactSearchMode,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
            ) {
                CenterAlignedTopAppBar(
                    title = { Text(text = stringResource(id = R.string.top_bar_title)) },
                    actions = {
                        IconButton(onClick = { viewModel.setShowShoppingList(true) }) {
                            BadgedBox(
                                badge = { if (shoppingListItems.isNotEmpty()) Badge { Text(shoppingListItems.size.toString()) } }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "רשימת קניות"
                                )
                            }
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(id = R.string.settings_content_desc))
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!compactSearchMode) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = onUrlChanged,
                    label = { Text(text = stringResource(id = R.string.url_label)) },
                    placeholder = { Text(text = stringResource(id = R.string.url_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )

                OutlinedButton(
                    onClick = { setShowPasteSheet(true) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "יצירת מתכון חדש / הדבקה מטקסט")
                }

                Button(
                    onClick = onExtractClick,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(text = stringResource(id = R.string.loading_recipe))
                    } else {
                        Text(text = stringResource(id = R.string.extract_recipe))
                    }
                }
            }

            if (savedRecipes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(id = R.string.no_saved_recipes))
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { isSearchFocused = it.isFocused },
                        singleLine = true,
                        placeholder = {
                            AnimatedContent(
                                targetState = searchByIngredients,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(220, delayMillis = 90)) togetherWith
                                            fadeOut(animationSpec = tween(90))
                                }, label = "SearchPlaceholder"
                            ) { targetSearchByIngredients ->
                                Text(text = if (targetSearchByIngredients) stringResource(id = R.string.search_by_ingredients) else stringResource(id = R.string.search_saved_recipes))
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChanged("") }) { Icon(Icons.Default.Clear, null) }
                                }
                                IconButton(onClick = { viewModel.toggleSearchMode() }) {
                                    AnimatedContent(
                                        targetState = searchByIngredients,
                                        transitionSpec = {
                                            (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
                                        }, label = "SearchModeIcon"
                                    ) { targetSearchByIngredients ->
                                        Icon(
                                            imageVector = if (targetSearchByIngredients) Icons.Default.Kitchen else Icons.AutoMirrored.Filled.MenuBook,
                                            contentDescription = stringResource(id = R.string.search_by_ingredients),
                                            tint = if (targetSearchByIngredients) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    )
                    IconButton(onClick = onSortToggle) {
                        Icon(if (sortByAlpha) Icons.Default.SortByAlpha else Icons.Default.AccessTime, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(predefinedTags) { tag ->
                        FilterChip(
                            selected = selectedTagFilters.contains(tag),
                            onClick = { viewModel.toggleTagFilter(tag) },
                            label = { Text(tag) },
                            leadingIcon = if (selectedTagFilters.contains(tag)) {
                                { Icon(imageVector = Icons.Filled.Done, contentDescription = "Selected", modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                            } else null
                        )
                    }
                }

                if (noResults) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(id = R.string.no_search_results))
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        if (lastCookedRecipe != null && searchQuery.isBlank() && selectedTagFilters.isEmpty()) {
                            item {
                                Text(
                                    text = "הכנתי לאחרונה 👨‍🍳",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                RecipeCardItem(
                                    recipe = lastCookedRecipe!!,
                                    onClick = onSavedRecipeClick,
                                    onToggleFavorite = onToggleFavorite,
                                    onLongClick = { recipeToDelete = it }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "כל המתכונים",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }

                        items(items = filteredRecipes, key = { it.id }) { recipe ->
                            RecipeCardItem(
                                recipe = recipe,
                                onClick = onSavedRecipeClick,
                                onToggleFavorite = onToggleFavorite,
                                onLongClick = { recipeToDelete = it }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showShoppingList) {
        ModalBottomSheet(onDismissRequest = { viewModel.setShowShoppingList(false) }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("רשימת קניות 🛒", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (shoppingListItems.isNotEmpty()) {
                            IconButton(onClick = {
                                val shareText = "🛒 *רשימת קניות :*\n\n" +
                                        shoppingListItems.filter { !it.isChecked }
                                            .groupBy { it.recipeName ?: "מצרכים כלליים" }
                                            .map { (recipe, items) ->
                                                "📍 *$recipe*:\n" + items.joinToString("\n") { "• ${it.name}" }
                                            }.joinToString("\n\n")

                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "שתף רשימה", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        if (shoppingListItems.isNotEmpty()) {
                            if (shoppingListItems.any { it.isChecked }) {
                                TextButton(onClick = { viewModel.clearCheckedShoppingItems() }) {
                                    Text("מחק מסומנים", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            TextButton(onClick = { viewModel.clearShoppingList() }) {
                                Text("נקה הכל", color = Color.Red)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (shoppingListItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("רשימת הקניות ריקה", color = MaterialTheme.colorScheme.onSurface)
                    }
                } else {
                    // מפצלים למצרכים שלא נקנו ולמצרכים שנקנו
                    val uncheckedGroups = shoppingListItems.filter { !it.isChecked }.groupBy { it.recipeName ?: "מצרכים כלליים" }
                    val checkedGroups = shoppingListItems.filter { it.isChecked }.groupBy { it.recipeName ?: "מצרכים כלליים" }

                    LazyColumn(modifier = Modifier.fillMaxHeight(0.8f)) {

                        // קודם מציגים את כל המצרכים שלא סומנו (לפי מתכונים)
                        uncheckedGroups.forEach { (recipeName, recipeItems) ->
                            item(key = "header_unchecked_$recipeName") {
                                Text(
                                    text = recipeName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(top = 16.dp, bottom = 4.dp)
                                        .animateItem(
                                            placementSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
                                        )
                                )
                            }

                            items(recipeItems, key = { it.id }) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem(
                                            placementSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
                                        )
                                        .clickable { viewModel.toggleShoppingItem(item.id) }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = item.isChecked,
                                        onCheckedChange = { viewModel.toggleShoppingItem(item.id) }
                                    )
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                                    )
                                    IconButton(onClick = { viewModel.removeShoppingItem(item.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "מחק", tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }

                        // עכשיו מציגים בתחתית הרשימה את כל המצרכים שכן סומנו
                        if (checkedGroups.isNotEmpty()) {
                            item(key = "divider_checked") {
                                HorizontalDivider(
                                    modifier = Modifier
                                        .padding(vertical = 16.dp)
                                        .animateItem(
                                            placementSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
                                        )
                                )
                            }

                            checkedGroups.forEach { (recipeName, recipeItems) ->
                                item(key = "header_checked_$recipeName") {
                                    Text(
                                        text = "$recipeName (נקנה)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .padding(top = 8.dp, bottom = 4.dp)
                                            .animateItem(
                                                placementSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
                                            )
                                    )
                                }

                                items(recipeItems, key = { "checked_${it.id}" }) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateItem(
                                                placementSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
                                            )
                                            .clickable { viewModel.toggleShoppingItem(item.id) }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = item.isChecked,
                                            onCheckedChange = { viewModel.toggleShoppingItem(item.id) },
                                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                        )
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            textDecoration = TextDecoration.LineThrough,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f).padding(start = 8.dp)
                                        )
                                        IconButton(onClick = { viewModel.removeShoppingItem(item.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "מחק", tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPasteSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { setShowPasteSheet(false) }, sheetState = sheetState) {
            var manualTitle by rememberSaveable { mutableStateOf("") }
            var manualIngredients by rememberSaveable { mutableStateOf("") }
            var manualSteps by rememberSaveable { mutableStateOf("") }
            var pastedRecipeText by rememberSaveable { mutableStateOf("") }
            var selectedSheetTags by remember { mutableStateOf(setOf<String>()) }
            val scrollState = rememberScrollState()
            var manualImageUrl by rememberSaveable { mutableStateOf<String?>(null) }
            val context = LocalContext.current
            val photoPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia(),
                onResult = { uri ->
                    if (uri != null) {
                        try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: SecurityException) { e.printStackTrace() }
                        manualImageUrl = uri.toString()
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("הוספת מתכון", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val manualButtonModifier = Modifier.weight(1f)
                    if (useManualInput) {
                        Button(onClick = { }, modifier = manualButtonModifier) { Text("הזנה ידנית") }
                    } else {
                        OutlinedButton(
                            onClick = {
                                useManualInput = true
                                prefs.edit { putBoolean("use_manual_input", true) }
                            },
                            modifier = manualButtonModifier
                        ) { Text("הזנה ידנית") }
                    }
                    val pasteButtonModifier = Modifier.weight(1f)
                    if (!useManualInput) {
                        Button(onClick = { }, modifier = pasteButtonModifier) { Text("הדבקת טקסט") }
                    } else {
                        OutlinedButton(
                            onClick = {
                                useManualInput = false
                                prefs.edit { putBoolean("use_manual_input", false) }
                            },
                            modifier = pasteButtonModifier
                        ) { Text("הדבקת טקסט") }
                    }
                }
                if (manualImageUrl != null) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                        AsyncImage(model = manualImageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)))
                        IconButton(onClick = { manualImageUrl = null }, modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(alpha=0.5f), CircleShape)) { Icon(Icons.Default.Close, tint=Color.White, contentDescription=null) }
                    }
                } else {
                    OutlinedButton(onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.AddPhotoAlternate, contentDescription=null); Spacer(modifier=Modifier.width(8.dp)); Text("הוסף תמונה למתכון (אופציונלי)") }
                }
                Text("תגיות (אופציונלי):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(predefinedTags) { tag -> FilterChip(selected = selectedSheetTags.contains(tag), onClick = { selectedSheetTags = if (selectedSheetTags.contains(tag)) selectedSheetTags - tag else selectedSheetTags + tag }, label = { Text(tag) }) }
                }

                if (useManualInput) {
                    OutlinedTextField(value = manualTitle, onValueChange = { manualTitle = it }, label = { Text("כותרת המתכון") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = manualIngredients, onValueChange = { manualIngredients = it }, label = { Text("מצרכים (שורה לכל מצרך)") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5)
                    OutlinedTextField(value = manualSteps, onValueChange = { manualSteps = it }, label = { Text("הוראות הכנה (שורה לכל שלב)") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5)
                    Button(onClick = { viewModel.saveManualRecipe(manualTitle, manualIngredients, manualSteps, selectedSheetTags.toList(), manualImageUrl); setShowPasteSheet(false) }, modifier = Modifier.fillMaxWidth(), enabled = manualTitle.isNotBlank() || manualIngredients.isNotBlank()) { Icon(Icons.Default.Save, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text(text = "שמור מתכון") }
                } else {
                    OutlinedTextField(value = pastedRecipeText, onValueChange = { pastedRecipeText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("טקסט המתכון") }, minLines = 6, maxLines = 10)
                    Button(onClick = { onExtractFromText(pastedRecipeText, selectedSheetTags.toList(), manualImageUrl); pastedRecipeText = ""; setShowPasteSheet(false) }, enabled = pastedRecipeText.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("חלץ מתכון מטקסט") }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (recipeToDelete != null) {
        AlertDialog(
            onDismissRequest = { recipeToDelete = null }, title = { Text("מחיקת מתכון") }, text = { Text("בטוח שברצונך למחוק?") },
            confirmButton = { TextButton(onClick = { onDeleteRecipe(recipeToDelete!!.id); recipeToDelete = null }) { Text("מחק", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { recipeToDelete = null }) { Text("בטל") } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecipeCardItem(
    recipe: Recipe,
    onClick: (Long) -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
    onLongClick: (Recipe) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onClick(recipe.id) },
                onLongClick = { onLongClick(recipe) }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (recipe.isFavorite) 6.dp else 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (recipe.isFavorite) MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.3f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!recipe.imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = recipe.imageUrl,
                    contentDescription = stringResource(id = R.string.recipe_image_desc),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(32.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = recipe.title.takeIf { it.isNotBlank() } ?: stringResource(id = R.string.fallback_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onToggleFavorite(recipe) },
                        modifier = Modifier.size(24.dp).padding(start = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (recipe.isFavorite) Icons.Filled.Star else Icons.Default.StarBorder,
                            contentDescription = "מועדף",
                            tint = if (recipe.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                if (recipe.rating > 0) {
                    Row {
                        for (i in 1..5) {
                            Icon(
                                imageVector = if (i <= recipe.rating) Icons.Filled.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (i <= recipe.rating) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (recipe.tags.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(recipe.tags) { tag ->
                            SuggestionChip(onClick = { }, label = { Text(tag, style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}
