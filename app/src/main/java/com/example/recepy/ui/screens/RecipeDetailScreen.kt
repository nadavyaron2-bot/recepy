package com.example.recepy.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.layout as modifierLayout
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.recepy.R
import com.example.recepy.RecipeTimerManager
import com.example.recepy.data.repository.Recipe
import com.example.recepy.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipe: Recipe?,
    isSaving: Boolean,
    isSaved: Boolean,
    onBack: () -> Unit,
    onSave: (Recipe) -> Unit,
    onDelete: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    LaunchedEffect(showDeleteDialog) {}

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val savedToHistoryMsg = stringResource(R.string.saved_to_history)
    val ingredientsAddedToCartMsg = stringResource(R.string.ingredients_added_to_cart)
    val allIngredientsCheckedMsg = stringResource(R.string.all_ingredients_checked)
    val ratingSavedMsg = stringResource(R.string.rating_saved)
    val notesSavedMsg = stringResource(R.string.notes_saved)
    val overlayPermissionRequestMsg = stringResource(R.string.overlay_permission_request)

    val predefinedTags = viewModel.predefinedTags

    var isEditing by remember(isSaved) { mutableStateOf(!isSaved) }
    LaunchedEffect(isEditing) {}
    var quantityMultiplier by remember { mutableFloatStateOf(1f) }

    var editedTitle by remember(recipe?.id) { mutableStateOf(recipe?.title ?: "") }
    var editedImageUrl by remember(recipe?.id) { mutableStateOf(recipe?.imageUrl) }
    var editedIngredients by remember(recipe?.id) { mutableStateOf(recipe?.ingredients?.joinToString("\n") ?: "") }
    var editedSteps by remember(recipe?.id) { mutableStateOf(recipe?.steps?.joinToString("\n") ?: "") }
    var editedTags by remember(recipe?.id) { mutableStateOf(recipe?.tags?.toSet() ?: emptySet()) }

    // משיכת סטייט המצרכים המסומנים מה-ViewModel כדי שיישאר גם אם יוצאים מהמסך
    val checkedIngredientsMap by viewModel.checkedIngredientsMap.collectAsState()
    val recipeIdForMap = recipe?.id ?: -1L
    val checkedIngredients = checkedIngredientsMap[recipeIdForMap] ?: emptySet()

    val timerSeconds by RecipeTimerManager.timeRemaining.collectAsState()
    val isTimerRunning by RecipeTimerManager.isRunning.collectAsState()

    val shareGraphicsLayer = rememberGraphicsLayer()
    var isShareCardReady by remember(recipe?.id) { mutableStateOf(false) }

    var showCookingMode by remember { mutableStateOf(false) }
    LaunchedEffect(showCookingMode) {}

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var showGroupShareDialog by remember { mutableStateOf(false) }
    LaunchedEffect(showGroupShareDialog) {}
    val groups by viewModel.groups.collectAsState()

    if (showGroupShareDialog) {
        AlertDialog(
            onDismissRequest = { showGroupShareDialog = false },
            title = { Text(stringResource(R.string.share_to_group)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (groups.isEmpty()) {
                        Text(stringResource(R.string.no_groups_yet))
                    } else {
                        groups.forEach { group ->
                            val groupName = group["name"] as String
                            val successMsg = stringResource(R.string.shared_to_group_success, groupName)
                            Button(
                                onClick = {
                                    recipe?.let { viewModel.shareRecipeToGroup(it, group["id"] as String) }
                                    showGroupShareDialog = false
                                    Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Group, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(groupName)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showGroupShareDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
            Scaffold(
                modifier = modifier,
                topBar = {
                    if (!isEditing && recipe != null) {
                        HeaderSection(
                            title = editedTitle,
                            imageUrl = editedImageUrl,
                            isEditing = isEditing,
                            onTitleChange = { editedTitle = it },
                            onImageUrlChange = { editedImageUrl = it },
                            onBack = onBack,
                            onShare = {
                                shareRecipeText(context, buildShareText(context, recipe.copy(
                                    title = editedTitle,
                                    ingredients = editedIngredients.split("\n").map { it.trim() }.filter { it.isNotBlank() },
                                    steps = editedSteps.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                                )))
                            },
                            onShareImage = {
                                if (isShareCardReady) {
                                    viewModel.viewModelScope.launch {
                                        val bitmap = shareGraphicsLayer.toImageBitmap().asAndroidBitmap()
                                        shareRecipeImage(context, bitmap, recipe.title, recipe.sourceUrl)
                                    }
                                } else {
                                    Toast.makeText(context, R.string.preparing_image, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onEditStart = { isEditing = true },
                            onCookingModeStart = { showCookingMode = true },
                            onShareToGroup = { showGroupShareDialog = true },
                            onOpenSource = { uriHandler.openUri(it) },
                            sourceUrl = recipe.sourceUrl,
                            scrollBehavior = scrollBehavior
                        )
                    }
                },
                bottomBar = {
                    Column {
                        AnimatedVisibility(
                            visible = timerSeconds > 0 && !isEditing,
                            enter = slideInVertically { it },
                            exit = slideOutVertically { it }
                        ) {
                            BottomAppBar(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                tonalElevation = 8.dp
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Timer, contentDescription = stringResource(R.string.timer_active_desc))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        val minutes = timerSeconds / 60
                                        val seconds = timerSeconds % 60
                                        Text(
                                            text = stringResource(R.string.timer_format, minutes, seconds),
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(onClick = { RecipeTimerManager.isRunning.value = !isTimerRunning }) {
                                            Icon(
                                                imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = stringResource(R.string.play_pause_desc)
                                            )
                                        }
                                        IconButton(onClick = { RecipeTimerManager.stopTimer() }) {
                                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel_timer_desc))
                                        }
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = isEditing,
                            enter = slideInVertically { it },
                            exit = slideOutVertically { it }
                        ) {
                            BottomAppBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 16.dp
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            isEditing = false
                                            editedTitle = recipe?.title ?: ""
                                            editedImageUrl = recipe?.imageUrl
                                            editedIngredients = recipe?.ingredients?.joinToString("\n") ?: ""
                                            editedSteps = recipe?.steps?.joinToString("\n") ?: ""
                                            editedTags = recipe?.tags?.toSet() ?: emptySet()
                                        },
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.cancel))
                                    }

                                    Button(
                                        onClick = {
                                            val updatedRecipe = recipe?.copy(
                                                title = editedTitle,
                                                imageUrl = editedImageUrl,
                                                ingredients = editedIngredients.split("\n").map { it.trim() }.filter { it.isNotBlank() },
                                                steps = editedSteps.split("\n").map { it.trim() }.filter { it.isNotBlank() },
                                                tags = editedTags.toList()
                                            )
                                            if (updatedRecipe != null) {
                                                onSave(updatedRecipe)
                                            }
                                            isEditing = false
                                        },
                                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.save_changes))
                                    }
                                }
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (!isEditing) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                if (isSaved) showDeleteDialog = true else recipe?.let { onSave(it) }
                            },
                            expanded = true,
                            icon = { Icon(if (isSaved) Icons.Default.Delete else Icons.Default.Save, null) },
                            text = {
                                Text(if (isSaving) stringResource(id = R.string.saving) else if (isSaved) stringResource(id = R.string.delete) else stringResource(id = R.string.save_recipe))
                            },
                            containerColor = if (isSaved) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary,
                            contentColor = if (isSaved) Color.White else MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(bottom = if (timerSeconds > 0) 80.dp else 12.dp)
                        )
                    }
                },
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
            ) { innerPadding ->
                if (recipe == null) {
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(id = R.string.detail_not_found))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                    if (isSaved) {
                        item {
                            Button(
                                onClick = {
                                    val cookedRecipe = recipe.copy(lastCooked = System.currentTimeMillis())
                                    onSave(cookedRecipe)
                                    Toast.makeText(context, savedToHistoryMsg, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.Restaurant, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.finish_cooking_button), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                        if (isEditing) {
                            item {
                                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                                    Text(stringResource(R.string.tags_label), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        items(predefinedTags) { tag ->
                                            FilterChip(
                                                selected = editedTags.contains(tag),
                                                onClick = {
                                                    editedTags = if (editedTags.contains(tag)) editedTags - tag else editedTags + tag
                                                },
                                                label = { Text(tag) }
                                            )
                                        }
                                    }
                                }
                            }
                            item {
                                OutlinedTextField(
                                    value = editedIngredients,
                                    onValueChange = { editedIngredients = it },
                                    label = { Text(stringResource(R.string.ingredients_edit_label)) },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                    minLines = 5
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = editedSteps,
                                    onValueChange = { editedSteps = it },
                                    label = { Text(stringResource(R.string.instructions_edit_label)) },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                    minLines = 6
                                )
                            }
                        } else {
                            if (recipe.tags.isNotEmpty()) {
                                item {
                                    LazyRow(
                                        modifier = Modifier.padding(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(recipe.tags) { tag ->
                                            AssistChip(onClick = {}, label = { Text(tag) })
                                        }
                                    }
                                }
                            }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.ingredients_title),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    SingleChoiceSegmentedButtonRow {
                                        val options = listOf(0.5f to "½", 1f to "1x", 2f to "2x", 3f to "3x")
                                        options.forEachIndexed { index, option ->
                                            SegmentedButton(
                                                selected = quantityMultiplier == option.first,
                                                onClick = { quantityMultiplier = option.first },
                                                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                                            ) { Text(option.second) }
                                        }
                                    }
                                }
                            }

                            if (recipe.ingredients.isEmpty()) {
                                item { Text(text = stringResource(id = R.string.no_ingredients), modifier = Modifier.padding(horizontal = 20.dp)) }
                            } else {
                                itemsIndexed(recipe.ingredients) { index, ingredient ->
                                    val isChecked = checkedIngredients.contains(index)
                                    val scaledIngredient = scaleNumbersInText(ingredient, quantityMultiplier)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.toggleIngredientCheck(recipeIdForMap, index)
                                            }
                                            .padding(horizontal = 20.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = scaledIngredient,
                                            style = MaterialTheme.typography.bodyLarge,
                                            textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                            }

                            // הוספה לעגלה - מסנן את מה שכבר סומן
                            if (recipe.ingredients.isNotEmpty()) {
                                item {
                                    Button(
                                        onClick = {
                                            val ingredientsToAdd = recipe.ingredients.filterIndexed { index, _ ->
                                                !checkedIngredients.contains(index)
                                            }

                                            if (ingredientsToAdd.isNotEmpty()) {
                                                viewModel.addIngredientsToCart(ingredientsToAdd, quantityMultiplier, recipe.title)
                                                Toast.makeText(context, ingredientsAddedToCartMsg, Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, allIngredientsCheckedMsg, Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.add_missing_to_cart), fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }

                            item {
                                Text(
                                    text = stringResource(id = R.string.instructions_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                                )
                            }

                            if (recipe.steps.isEmpty()) {
                                item { Text(text = stringResource(id = R.string.no_steps), modifier = Modifier.padding(horizontal = 20.dp)) }
                            } else {
                                // שלבים רגילים, ללא סימון ומחיקה
                                itemsIndexed(recipe.steps) { index, step ->
                                    val extractedTime = extractTimerMinutes(step)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "${index + 1}. ",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = step,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )

                                            if (extractedTime != null) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                OutlinedButton(
                                                    onClick = {
                                                        if (!Settings.canDrawOverlays(context)) {
                                                            Toast.makeText(context, overlayPermissionRequestMsg, Toast.LENGTH_LONG).show()
                                                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())
                                                            context.startActivity(intent)
                                                        } else {
                                                            RecipeTimerManager.startTimer(context, extractedTime * 60, recipe.title)
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(32.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = MaterialTheme.colorScheme.primary
                                                    )
                                                ) {
                                                    Icon(Icons.Default.Timer, contentDescription = "Timer", modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(stringResource(R.string.start_timer_minutes, extractedTime), style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (!isEditing && isSaved) {
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp, modifier = Modifier.padding(horizontal = 20.dp))
                                Spacer(modifier = Modifier.height(16.dp))

                                    Column(modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                                    Text(
                                        text = stringResource(R.string.rating_notes_title),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                        for (i in 1..5) {
                                            Icon(
                                                imageVector = if (i <= recipe.rating) Icons.Filled.Star else Icons.Default.StarBorder,
                                                contentDescription = null,
                                                tint = if (i <= recipe.rating) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clickable {
                                                        onSave(recipe.copy(rating = i))
                                                        Toast.makeText(context, ratingSavedMsg, Toast.LENGTH_SHORT).show()
                                                    }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    var localNotes by remember(recipe.notes) { mutableStateOf(recipe.notes) }

                                    OutlinedTextField(
                                        value = localNotes,
                                        onValueChange = { localNotes = it },
                                        label = { Text(stringResource(R.string.notes_hint)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 3
                                    )

                                    AnimatedVisibility(visible = localNotes != recipe.notes) {
                                        Button(
                                            onClick = {
                                                onSave(recipe.copy(notes = localNotes))
                                                Toast.makeText(context, notesSavedMsg, Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Icon(Icons.Default.Save, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stringResource(R.string.save_notes))
                                        }
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(140.dp)) }
                    }
                }

                // Hidden Share Card for "Share as Image"
                if (recipe != null) {
                    Box(
                        modifier = Modifier
                            .modifierLayout { measurable: Measurable, _: Constraints ->
                                val placeable = measurable.measure(Constraints())
                                layout(placeable.width, placeable.height) {
                                    placeable.place(-5000, -5000)
                                }
                            }
                            .drawWithContent {
                                shareGraphicsLayer.record {
                                    this@drawWithContent.drawContent()
                                }
                            }
                    ) {
                        RecipeShareCard(
                            recipe = recipe,
                            modifier = Modifier.width(600.dp),
                            onImageReady = { isShareCardReady = it }
                        )
                    }
                }
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text(text = stringResource(id = R.string.delete_recipe_title)) },
                    text = { Text(text = stringResource(id = R.string.delete_recipe_message)) },
                    confirmButton = {
                        TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text(text = stringResource(id = R.string.delete), color = Color.Red) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) { Text(text = stringResource(id = R.string.cancel)) }
                    }
                )
            }

            if (showCookingMode && recipe != null) {
                CookingModeScreen(
                    recipe = recipe,
                    onDismiss = { showCookingMode = false }
                )
            }
        }
    }
}

@Composable
fun CookingModeScreen(
    recipe: Recipe,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    val coroutineScope = rememberCoroutineScope()
    
    // שמירה על מסך דולק במצב בישול
    DisposableEffect(view) {
        val originalKeepScreenOn = view.keepScreenOn
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = originalKeepScreenOn
        }
    }

    // מעקב אחר שלבים שהושלמו
    var completedSteps by remember { mutableStateOf(setOf<Int>()) }
    
    // מצב הטיימר הגלובלי
    val timerSeconds by RecipeTimerManager.timeRemaining.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(
                        text = recipe.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.cooking_mode_active),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                if (timerSeconds > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.clickable { 
                            // אולי להוסיף פעולה ללחיצה על הטיימר
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Timer, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            val mins = timerSeconds / 60
                            val secs = timerSeconds % 60
                            Text(
                                stringResource(R.string.timer_format, mins, secs),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Progress Bar
            val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { recipe.steps.size + 1 })
            val progress = (pagerState.currentPage.toFloat() / (recipe.steps.size)).coerceIn(0f, 1f)
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Box(modifier = Modifier.weight(1f)) {
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    pageSpacing = 16.dp,
                    userScrollEnabled = true
                ) { page ->
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                            if (page == 0) {
                                // עמוד מצרכים
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ShoppingCart, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            stringResource(R.string.what_is_needed),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    LazyColumn(modifier = Modifier.weight(1f)) {
                                        items(recipe.ingredients) { ingredient ->
                                            Row(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text("•", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = ingredient,
                                                    style = MaterialTheme.typography.titleLarge,
                                                    lineHeight = 32.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // עמודי שלבים
                                val stepIndex = page - 1
                                val isDone = completedSteps.contains(stepIndex)
                                
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            stringResource(R.string.step_x_of_y, page, recipe.steps.size),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        
                                        FilterChip(
                                            selected = isDone,
                                            onClick = {
                                                completedSteps = if (isDone) completedSteps - stepIndex else completedSteps + stepIndex
                                            },
                                            label = { Text(if (isDone) stringResource(R.string.done_label) else stringResource(R.string.mark_as_done)) },
                                            leadingIcon = if (isDone) {
                                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                            } else null
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Box(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = recipe.steps[stepIndex],
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Medium,
                                            lineHeight = 42.sp,
                                            modifier = Modifier
                                                .verticalScroll(rememberScrollState())
                                                .fillMaxWidth(),
                                            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                            color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    
                                    val timerMins = extractTimerMinutes(recipe.steps[stepIndex])
                                    if (timerMins != null) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { RecipeTimerManager.startTimer(context, timerMins * 60, recipe.title) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Icon(Icons.Default.Timer, null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stringResource(R.string.start_timer_minutes, timerMins))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Navigation Buttons (Floating)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (pagerState.currentPage > 0) {
                        LargeNavigationButton(
                            icon = Icons.Default.ChevronRight,
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                            }
                        )
                    } else {
                        Spacer(modifier = Modifier.width(64.dp))
                    }

                    if (pagerState.currentPage < recipe.steps.size) {
                        LargeNavigationButton(
                            icon = Icons.Default.ChevronLeft,
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                        )
                    } else {
                        // כפתור סיום
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.height(64.dp),
                            shape = RoundedCornerShape(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Celebration, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.finished_cooking_success), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Pager Indicators
            Row(
                Modifier
                    .height(40.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(recipe.steps.size + 1) { iteration ->
                    val color = if (pagerState.currentPage == iteration) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(if (pagerState.currentPage == iteration) 10.dp else 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LargeNavigationButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
        contentColor = MaterialTheme.colorScheme.onPrimary,
        tonalElevation = 6.dp,
        modifier = Modifier.size(64.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, modifier = Modifier.size(36.dp))
        }
    }
}


fun scaleNumbersInText(text: String, multiplier: Float): String {
    if (multiplier == 1f) return text
    val regex = Regex("""\b(\d+(\.\d+)?)\b""")
    return regex.replace(text) { matchResult ->
        val num = matchResult.value.toFloatOrNull() ?: return@replace matchResult.value
        val scaled = num * multiplier
        if (scaled % 1.0f == 0f) scaled.toInt().toString() else String.format(Locale.US, "%.1f", scaled)
    }
}

fun extractTimerMinutes(text: String): Int? {
    val minutesRegex = Regex("""(\d+)\s*(דקות|minutes)""", RegexOption.IGNORE_CASE)
    val hoursRegex = Regex("""(\d+(\.\d+)?)\s*(שעות|hours)""", RegexOption.IGNORE_CASE)

    val minMatch = minutesRegex.find(text)
    if (minMatch != null) return minMatch.groupValues[1].toIntOrNull()

    val hourMatch = hoursRegex.find(text)
    if (hourMatch != null) {
        val hours = hourMatch.groupValues[1].toFloatOrNull()
        if (hours != null) return (hours * 60).roundToInt()
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderSection(
    title: String, imageUrl: String?, sourceUrl: String?, isEditing: Boolean,
    onTitleChange: (String) -> Unit, onImageUrlChange: (String?) -> Unit,
    onBack: () -> Unit, onShare: () -> Unit, onShareImage: () -> Unit,
    onEditStart: () -> Unit, onCookingModeStart: () -> Unit,
    onShareToGroup: () -> Unit,
    onOpenSource: (String) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    val context = LocalContext.current
    var showShareOptions by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: SecurityException) { e.printStackTrace() }
                onImageUrlChange(uri.toString())
            }
        }
    )

    // חישוב שקיפות התמונה בהתאם לגלילה
    val alpha = scrollBehavior?.let {
        1f - it.state.collapsedFraction
    } ?: 1f

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (scrollBehavior != null) (280.dp * alpha).coerceAtLeast(0.dp) else 280.dp)
                .graphicsLayer { this.alpha = alpha }
        ) {
            if (!imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (isEditing) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                    Row(modifier = Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        IconButton(onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.change_image_desc), tint = Color.White) }
                        IconButton(onClick = { onImageUrlChange(null) }, modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove_image_desc), tint = Color.White) }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    if (isEditing) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)) { Icon(Icons.Default.AddPhotoAlternate, contentDescription = stringResource(R.string.add_image), tint = MaterialTheme.colorScheme.onPrimary) }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.add_image), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Text(text = stringResource(id = R.string.no_image), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            
            // גרדיאנט שחור רק כשיש תמונה
            if (!imageUrl.isNullOrEmpty()) {
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent, Color.Black.copy(alpha = 0.7f)), 0f, Float.POSITIVE_INFINITY)))
            }
        }

        val topBarContainerColor = if (alpha < 0.1f) MaterialTheme.colorScheme.surface else Color.Transparent
        val contentColor = if (alpha < 0.1f) MaterialTheme.colorScheme.onSurface else Color.White

        TopAppBar(
            title = {
                if (alpha < 0.5f || isEditing) {
                    if (isEditing) {
                        OutlinedTextField(
                            value = title, onValueChange = onTitleChange, label = { Text(stringResource(R.string.recipe_title_label)) },
                            modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            singleLine = true
                        )
                    } else {
                        Text(
                            text = title.takeIf { it.isNotBlank() } ?: stringResource(id = R.string.fallback_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.back),
                        tint = contentColor
                    )
                }
            },
            actions = {
                if (!isEditing) {
                    IconButton(onClick = onCookingModeStart) { Icon(Icons.Default.RestaurantMenu, stringResource(R.string.cooking_mode_desc), tint = contentColor) }
                    IconButton(onClick = onEditStart) { Icon(Icons.Default.Edit, stringResource(R.string.edit_recipe_desc), tint = contentColor) }
                    if (sourceUrl?.startsWith("http", ignoreCase = true) == true) IconButton(onClick = { onOpenSource(sourceUrl) }) { Icon(Icons.Default.OpenInBrowser, stringResource(R.string.open_url_desc), tint = contentColor) }
                    IconButton(onClick = { showShareOptions = true }) { Icon(Icons.Default.Share, stringResource(id = R.string.share_recipe), tint = contentColor) }
                    
                    DropdownMenu(expanded = showShareOptions, onDismissRequest = { showShareOptions = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.share_as_text)) },
                            onClick = { onShare(); showShareOptions = false },
                            leadingIcon = { Icon(Icons.Default.TextFields, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.share_as_image)) },
                            onClick = { onShareImage(); showShareOptions = false },
                            leadingIcon = { Icon(Icons.Default.Image, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.share_to_group)) },
                            onClick = { onShareToGroup(); showShareOptions = false },
                            leadingIcon = { Icon(Icons.Default.Group, null) }
                        )
                    }
                }
            },
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = topBarContainerColor,
                scrolledContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // כותרת גדולה שמופיעה כשהתמונה גלויה
        if (!isEditing && alpha >= 0.5f) {
            Text(
                text = title.takeIf { it.isNotBlank() } ?: stringResource(id = R.string.fallback_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (!imageUrl.isNullOrEmpty()) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .graphicsLayer { this.alpha = (alpha - 0.5f) * 2f }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
    }
}

fun shareRecipeImage(context: Context, bitmap: Bitmap, title: String, sourceUrl: String) {
    val file = File(context.cacheDir, "shared_recipe.png")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    
    val sourcePart = if (sourceUrl.startsWith("http", ignoreCase = true)) "\n" + context.getString(R.string.source_prefix, sourceUrl) else ""
    val shareBody = "🍽️ $title$sourcePart\n" + context.getString(R.string.shared_from_app_branding)

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, shareBody)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_recipe_chooser_title)))
}

fun buildShareText(context: Context, recipe: Recipe): String {
    val ingredientsText = recipe.ingredients.joinToString(separator = "\n") { "• $it" }
    val stepsText = recipe.steps.mapIndexed { index, step -> "${index + 1}. $step" }.joinToString(separator = "\n")
    
    val sourcePart = if (recipe.sourceUrl.startsWith("http", ignoreCase = true)) {
        "\n" + context.getString(R.string.source_prefix, recipe.sourceUrl) + "\n"
    } else ""

    val ingredientsTitle = context.getString(R.string.ingredients_title)
    val instructionsTitle = context.getString(R.string.instructions_title)

    return "🍽️ ${recipe.title}\n$sourcePart\n$ingredientsTitle:\n$ingredientsText\n\n$instructionsTitle:\n$stepsText"
}

fun shareRecipeText(context: Context, shareText: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) }
    context.startActivity(Intent.createChooser(sendIntent, null))
}

@Composable
fun RecipeShareCard(recipe: Recipe, modifier: Modifier = Modifier, onImageReady: (Boolean) -> Unit = {}) {
    val hasImage = !recipe.imageUrl.isNullOrEmpty()

    if (!hasImage) {
        LaunchedEffect(recipe.id) {
            onImageReady(true)
        }
    }

    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            // Header Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(vertical = 24.dp, horizontal = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = recipe.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left Column: Image & Ingredients
                Column(modifier = Modifier.weight(0.4f)) {
                    if (hasImage) {
                        AsyncImage(
                            model = recipe.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            onSuccess = { onImageReady(true) },
                            onError = { onImageReady(true) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.ingredients_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    recipe.ingredients.forEach { ingredient ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(6.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = ingredient,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                // Right Column: Instructions
                Column(modifier = Modifier.weight(0.6f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.instructions_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    recipe.steps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black,
                                modifier = Modifier.weight(1f),
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Branding & Source at the bottom of the right column
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        if (recipe.sourceUrl.startsWith("http", ignoreCase = true)) {
                            Text(
                                text = recipe.sourceUrl,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                maxLines = 1,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        Text(
                            text = "Recepy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
