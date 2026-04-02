package com.example.recepy.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.recepy.R
import com.example.recepy.data.repository.Recipe
import com.example.recepy.viewmodel.MainViewModel
import com.example.recepy.viewmodel.RecipeUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onSavedRecipeClick: (Long) -> Unit,
    onDeleteRecipe: (Long) -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: MainViewModel = viewModel()
) {
    val filteredRecipes by viewModel.filteredRecipes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortByAlpha by viewModel.sortByAlpha.collectAsState()
    val urlInput by viewModel.urlInput.collectAsState()
    val recipeUiState by viewModel.recipeUiState.collectAsState()
    val savedRecipes by viewModel.savedRecipes.collectAsState()

    val onUrlChanged = viewModel::onUrlChanged
    val onSearchQueryChanged = viewModel::onSearchQueryChanged
    val onSortToggle = viewModel::toggleSort
    val onToggleFavorite = viewModel::toggleFavorite
    val onExtractClick = { viewModel.extractRecipe() }
    val onExtractFromText = viewModel::extractRecipeFromText
    val isLoading = recipeUiState is RecipeUiState.Loading

    LaunchedEffect(Unit) {
        viewModel.loadSeedRecipes()
    }

    var recipeToDelete by remember { mutableStateOf<Recipe?>(null) }
    fun updateRecipeToDelete(recipe: Recipe?) { recipeToDelete = recipe }

    val showPasteSheet by viewModel.showAddDialog.collectAsState()
    fun setShowPasteSheet(show: Boolean) = viewModel.setShowAddDialog(show)

    var isSearchFocused by remember { mutableStateOf(false) }
    fun updateSearchFocus(focused: Boolean) { isSearchFocused = focused }

    val density = LocalDensity.current
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0
    val compactSearchMode = isKeyboardOpen && isSearchFocused
    val noResults = savedRecipes.isNotEmpty() && filteredRecipes.isEmpty()

    val selectedTagFilters by viewModel.selectedTagFilters.collectAsState()
    val predefinedTags = viewModel.predefinedTags
    val lastCookedRecipe by viewModel.lastCookedRecipe.collectAsState()
    val searchByIngredients by viewModel.searchByIngredients.collectAsState()
    val shoppingListItems by viewModel.shoppingList.collectAsState()

    val lazyListState = rememberLazyListState()
    val isScrollingUp = remember {
        derivedStateOf {
            if (lazyListState.isScrollInProgress) {
                lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
            } else {
                true
            }
        }
    }
    val showScrollToTop by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 2
        }
    }
    val coroutineScope = rememberCoroutineScope()

    val shouldHideTopBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || (lazyListState.firstVisibleItemScrollOffset > 0 && !isScrollingUp.value)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AnimatedVisibility(
                visible = !compactSearchMode && !shouldHideTopBar,
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
                                    contentDescription = stringResource(id = R.string.shopping_list)
                                )
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showScrollToTop,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            lazyListState.animateScrollToItem(0)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(id = R.string.scroll_to_top))
                }
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
            AnimatedVisibility(
                visible = !compactSearchMode && isScrollingUp.value,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                        Text(text = stringResource(id = R.string.create_new_recipe_or_paste))
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
                            .onFocusChanged { updateSearchFocus(it.isFocused) },
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

//                if (suggestedMakoSearch != null) {
//                    Card(
//                        modifier = Modifier.fillMaxWidth(),
//                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
//                    ) {
//                        Column(modifier = Modifier.padding(12.dp)) {
//                            Text("לא נמצא מתכון מתאים באוסף שלך.", style = MaterialTheme.typography.bodyMedium)
//                            TextButton(onClick = {
//                                onUrlChanged(suggestedMakoSearch!!)
//                                onExtractClick()
//                            }) {
//                                Text("חפש וחלץ את \"$suggestedMakoSearch\" ממאקו")
//                            }
//                        }
//                    }
//                }

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
                                { Icon(imageVector = Icons.Filled.Done, contentDescription = stringResource(id = R.string.selected_indicator), modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                            } else null
                        )
                    }
                }

//                if (suggestedMakoSearch != null) {
//                    TextButton(
//                        onClick = {
//                            onUrlChanged(suggestedMakoSearch!!)
//                            onExtractClick()
//                        },
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Text("נסה לחפש את \"$suggestedMakoSearch\" במאקו")
//                    }
//                }

                if (noResults) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(id = R.string.no_results_for, searchQuery),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(id = R.string.search_on_web_suggestion),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = { viewModel.extractRecipe(searchQuery) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(id = R.string.auto_search_extract))
                        }
                    }
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        if (lastCookedRecipe != null && searchQuery.isBlank() && selectedTagFilters.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(id = R.string.recently_cooked),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                RecipeCardItem(
                                    recipe = lastCookedRecipe!!,
                                    onClick = onSavedRecipeClick,
                                    onToggleFavorite = onToggleFavorite,
                                    onLongClick = { updateRecipeToDelete(it) }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(id = R.string.all_recipes),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }

                        items(items = filteredRecipes, key = { if (it.id == -2L) it.title + it.sourceUrl else it.id.toString() }) { recipe ->
                            RecipeCardItem(
                                recipe = recipe,
                                onClick = onSavedRecipeClick,
                                onToggleFavorite = onToggleFavorite,
                                onLongClick = { updateRecipeToDelete(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPasteSheet) {
        AddRecipeSheet(
            onDismiss = { setShowPasteSheet(false) },
            onSaveManual = { title, ingredients, steps, tags, image, notes ->
                viewModel.saveManualRecipe(title, ingredients, steps, tags, image, notes)
                setShowPasteSheet(false)
            },
            onExtractFromText = { text, tags, image, notes ->
                onExtractFromText(text, tags, image, notes)
                setShowPasteSheet(false)
            }
        )
    }

    if (recipeToDelete != null) {
        AlertDialog(
            onDismissRequest = { updateRecipeToDelete(null) }, title = { Text(stringResource(id = R.string.delete_recipe_title)) }, text = { Text(stringResource(id = R.string.delete_recipe_confirmation)) },
            confirmButton = { TextButton(onClick = { onDeleteRecipe(recipeToDelete!!.id); updateRecipeToDelete(null) }) { Text(stringResource(id = R.string.delete), color = Color.Red) } },
            dismissButton = { TextButton(onClick = { updateRecipeToDelete(null) }) { Text(stringResource(id = R.string.cancel)) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipeSheet(
    onDismiss: () -> Unit,
    onSaveManual: (String, String, String, List<String>, String?, String) -> Unit,
    onExtractFromText: (String, List<String>, String?, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("") }
    var ingredients by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var recipeText by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var imageUrl by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            imageUrl = uri?.toString()
        }
    )

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val viewModel: MainViewModel = viewModel()
    val predefinedTags = viewModel.predefinedTags

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(id = R.string.add_recipe),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(id = R.string.manual_entry)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(id = R.string.text_paste)) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedTab == 0) {
                // Manual Entry
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(id = R.string.recipe_title_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = ingredients,
                    onValueChange = { ingredients = it },
                    label = { Text(stringResource(id = R.string.ingredients_edit_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = steps,
                    onValueChange = { steps = it },
                    label = { Text(stringResource(id = R.string.instructions_edit_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(id = R.string.notes_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            } else {
                // Text Paste
                OutlinedTextField(
                    value = recipeText,
                    onValueChange = { recipeText = it },
                    label = { Text(stringResource(id = R.string.recipe_text_label)) },
                    placeholder = { Text(stringResource(id = R.string.paste_recipe_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 8
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(id = R.string.notes_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Image Picker Section
            Text(text = stringResource(id = R.string.add_image), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (imageUrl != null) {
                Box(modifier = Modifier.size(120.dp)) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { imageUrl = null },
                        modifier = Modifier.align(Alignment.TopEnd).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape).size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(id = R.string.add_image_optional))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(text = stringResource(id = R.string.tags_optional), style = MaterialTheme.typography.titleMedium)
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                predefinedTags.forEach { tag ->
                    FilterChip(
                        selected = selectedTags.contains(tag),
                        onClick = {
                            selectedTags = if (selectedTags.contains(tag)) selectedTags - tag else selectedTags + tag
                        },
                        label = { Text(tag) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (selectedTab == 0) {
                        onSaveManual(title, ingredients, steps, selectedTags.toList(), imageUrl, notes)
                    } else {
                        onExtractFromText(recipeText, selectedTags.toList(), imageUrl, notes)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = if (selectedTab == 0) title.isNotBlank() else recipeText.isNotBlank()
            ) {
                Text(text = stringResource(id = if (selectedTab == 0) R.string.save_recipe_button else R.string.extract_recipe_from_text))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
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
                        if (recipe.id == -2L) {
                            Icon(Icons.Default.Groups, contentDescription = stringResource(id = R.string.shared_recipe), tint = MaterialTheme.colorScheme.primary)
                        } else {
                            Icon(
                                imageVector = if (recipe.isFavorite) Icons.Filled.Star else Icons.Default.StarBorder,
                                contentDescription = stringResource(id = R.string.favorite),
                                tint = if (recipe.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
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
