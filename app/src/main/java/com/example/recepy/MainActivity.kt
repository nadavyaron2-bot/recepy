package com.example.recepy

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import com.example.recepy.data.preferences.ThemeMode
import com.example.recepy.ui.screens.GroupsScreen
import com.example.recepy.ui.screens.MainScreen
import com.example.recepy.ui.screens.RecipeDetailScreen
import com.example.recepy.ui.screens.SettingsScreen
import com.example.recepy.ui.screens.DeveloperScreen
import com.example.recepy.ui.screens.SplashScreen
import com.example.recepy.viewmodel.RecipeUiState
import com.example.recepy.ui.theme.RecepyTheme
import com.example.recepy.viewmodel.MainViewModel
import com.example.recepy.viewmodel.RecipeDetailViewModel
import com.example.recepy.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val detailViewModel: RecipeDetailViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val prefs by lazy { getSharedPreferences("settings_prefs", MODE_PRIVATE) }
    private val keepScreenOn = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {

        // 1. קריאה למסך הטעינה המובנה של אנדרואיד (מונע מסך לבן בהתחלה)
        installSplashScreen()

        super.onCreate(savedInstanceState)
        keepScreenOn.value = prefs.getBoolean("keep_screen_on", true)
        enableEdgeToEdge()

        if (keepScreenOn.value) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        if (savedInstanceState == null) {
            handleAppIntent(intent)
        }

        setContent {

            val themeMode by settingsViewModel.themeMode.collectAsState()
            val appTheme by settingsViewModel.appTheme.collectAsState()
            val showUpdateDialog by mainViewModel.showUpdateDialog.collectAsState()
            val updateDownloadUrl by mainViewModel.updateDownloadUrl.collectAsState()
            
            val useDarkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            LaunchedEffect(keepScreenOn.value) {
                if (keepScreenOn.value) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !useDarkTheme

            RecepyTheme(darkTheme = useDarkTheme, appTheme = appTheme) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

                    // משתנה שזוכר אם אנחנו עדיין במסך הפתיחה (Compose)
                    // משתמשים ב-rememberSaveable כדי שזה יישמר בסיבוב מסך
                    var showSplash by rememberSaveable { mutableStateOf(true) }

                    // בודקים מה להציג - את מסך הפתיחה או את האפליקציה המלאה
                    if (showSplash) {
                        SplashScreen(onNavigateToMain = { 
                            showSplash = false 
                            mainViewModel.checkForAppUpdate(this@MainActivity)
                        })
                    } else {
        if (showUpdateDialog && updateDownloadUrl != null) {
            UpdateDialog(
                onDismiss = { mainViewModel.dismissUpdateDialog() },
                onConfirm = {
                    mainViewModel.installUpdate(this@MainActivity, updateDownloadUrl!!)
                    mainViewModel.dismissUpdateDialog()
                }
            )
        }
                        RecepyApp(
                            mainViewModel = mainViewModel, detailViewModel = detailViewModel,
                            settingsViewModel = settingsViewModel, keepScreenOn = keepScreenOn.value,
                            onKeepScreenOnToggle = {
                                val newValue = !keepScreenOn.value
                                keepScreenOn.value = newValue
                                prefs.edit { putBoolean("keep_screen_on", newValue) }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAppIntent(intent)
    }

    private fun handleAppIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND) {
            if (intent.type == "text/plain") {
                mainViewModel.handleSharedText(intent.getStringExtra(Intent.EXTRA_TEXT))
            } else if (intent.type == "application/json") {
                val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM) as? android.net.Uri
                }
                uri?.let { mainViewModel.importRecipesFromUri(this, it) }
            }
        } else if (intent.action == Intent.ACTION_VIEW && intent.type == "application/json") {
            intent.data?.let { mainViewModel.importRecipesFromUri(this, it) }
        }

        val openAction = intent.getStringExtra("open_action")
        if (openAction == "add_recipe") {
            mainViewModel.setShowAddDialog(true)
            intent.removeExtra("open_action")
        }

        val openTab = intent.getStringExtra("open_tab")
        if (openTab == "shopping") {
            mainViewModel.setShowShoppingList(true)
            intent.removeExtra("open_tab")
        } else if (openTab == "home") {
             mainViewModel.setShowShoppingList(false)
             intent.removeExtra("open_tab")
        }
    }
}

@Composable
fun UpdateDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_dialog_title)) },
        text = { Text(stringResource(R.string.update_dialog_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.update_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecepyApp(
    mainViewModel: MainViewModel, detailViewModel: RecipeDetailViewModel,
    settingsViewModel: SettingsViewModel, keepScreenOn: Boolean, onKeepScreenOnToggle: () -> Unit
) {
    val navController = rememberNavController()
    val recipeUiState by mainViewModel.recipeUiState.collectAsState()
    val selectedRecipe by mainViewModel.selectedRecipe.collectAsState()
    val currentRecipe by detailViewModel.currentRecipe.collectAsState()
    val isSaving by detailViewModel.isSaving.collectAsState()
    val isSaved by detailViewModel.isSaved.collectAsState()
    val detailMessages by detailViewModel.messages.collectAsState(initial = "")
    val themeMode by settingsViewModel.themeMode.collectAsState()
    val isDeveloper by settingsViewModel.isDeveloper.collectAsState()

    val homeSnackbarHostState = remember { SnackbarHostState() }
    val detailSnackbarHostState = remember { SnackbarHostState() }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf("home", "groups", "settings", "developer")
    val showShoppingList by mainViewModel.showShoppingList.collectAsState()
    val shoppingListItems by mainViewModel.shoppingList.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(recipeUiState) {
        when (recipeUiState) {
            is RecipeUiState.Success -> { navController.navigate("detail/-1"); mainViewModel.consumeUiState() }
            is RecipeUiState.Error -> { homeSnackbarHostState.showSnackbar((recipeUiState as RecipeUiState.Error).message); mainViewModel.consumeUiState() }
            else -> {}
        }
    }

    LaunchedEffect(detailMessages) { if (detailMessages.isNotBlank()) detailSnackbarHostState.showSnackbar(detailMessages) }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.nav_home)) },
                        label = { Text(stringResource(R.string.nav_home)) },
                        selected = currentRoute == "home",
                        onClick = {
                            if (currentRoute != "home") {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Group, contentDescription = stringResource(R.string.nav_groups)) },
                        label = { Text(stringResource(R.string.nav_groups)) },
                        selected = currentRoute == "groups",
                        onClick = {
                            if (currentRoute != "groups") {
                                navController.navigate("groups") {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings)) },
                        label = { Text(stringResource(R.string.nav_settings)) },
                        selected = currentRoute == "settings",
                        onClick = {
                            if (currentRoute != "settings") {
                                navController.navigate("settings") {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                    if (isDeveloper) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Build, contentDescription = stringResource(R.string.nav_developer)) },
                            label = { Text(stringResource(R.string.nav_developer)) },
                            selected = currentRoute == "developer",
                            onClick = {
                                if (currentRoute != "developer") {
                                    navController.navigate("developer") {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.fillMaxSize(),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                composable("home") {
                    MainScreen(
                        onSavedRecipeClick = { recipeId -> navController.navigate("detail/$recipeId") },
                        onDeleteRecipe = mainViewModel::deleteRecipe,
                        snackbarHostState = homeSnackbarHostState,
                        viewModel = mainViewModel
                    )
                }
                composable("developer") {
                    DeveloperScreen(onBack = { navController.popBackStack() }, viewModel = mainViewModel)
                }
                composable("groups") {
                    GroupsScreen(onBack = { navController.popBackStack() }, viewModel = mainViewModel)
                }
                composable("settings") {
                    val domainCounts by settingsViewModel.domainCounts.collectAsState()
                    val appTheme by settingsViewModel.appTheme.collectAsState()
                    val isImporting by mainViewModel.isImporting.collectAsState()
                    val systemUpdateMessage by mainViewModel.systemUpdateMessage.collectAsState()
                    val appUpdateMessage by mainViewModel.appUpdateMessage.collectAsState()
                    val downloadProgress by mainViewModel.downloadProgress.collectAsState()
                    val importMessage by mainViewModel.importMessage.collectAsState()
                    val settingsSnackbarHostState = remember { SnackbarHostState() }
                    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

                    LaunchedEffect(importMessage) {
                        importMessage?.let {
                            settingsSnackbarHostState.showSnackbar(it)
                            mainViewModel.consumeImportMessage()
                        }
                    }

                    val context = LocalContext.current
                    SettingsScreen(
                        themeMode = themeMode,
                        onThemeSelected = settingsViewModel::updateThemeMode,
                        appTheme = appTheme,
                        onAppThemeSelected = settingsViewModel::updateAppTheme,
                        keepScreenOn = keepScreenOn,
                        onKeepScreenOnToggle = onKeepScreenOnToggle,
                        onBack = { navController.popBackStack() },
                        domainCounts = domainCounts,
                        onDeleteByDomains = { settingsViewModel.deleteByDomains(it) },
                        onExportAll = { mainViewModel.exportAllRecipes(context) },
                        onImportFromFile = { mainViewModel.importRecipesFromUri(context, it) },
                        isImporting = isImporting,
                        systemUpdateMessage = systemUpdateMessage,
                        appUpdateMessage = appUpdateMessage,
                        onCheckForUpdates = { },
                        onCheckAppUpdate = { mainViewModel.checkForAppUpdate(context) },
                        downloadProgress = downloadProgress,
                        isDeveloper = isDeveloper,
                        onDeveloperModeToggle = { settingsViewModel.setDeveloperMode(it) },
                        onReportBug = { bug ->
                            mainViewModel.reportBug(bug)
                            coroutineScope.launch {
                                settingsSnackbarHostState.showSnackbar("הדיווח נשלח, תודה!")
                            }
                        },
                        snackbarHostState = settingsSnackbarHostState,
                        viewModel = mainViewModel
                    )
                }
                composable("detail/{recipeId}", arguments = listOf(navArgument("recipeId") { type = NavType.LongType })) { backStackEntry ->
                    val recipeId = backStackEntry.arguments?.getLong("recipeId") ?: -1L
                    val groupRecipes by mainViewModel.groupRecipes.collectAsState()
                    LaunchedEffect(recipeId, selectedRecipe, groupRecipes) {
                        detailViewModel.loadRecipe(recipeId, if (recipeId == -1L || recipeId == -2L) selectedRecipe else null, groupRecipes)
                    }
                    RecipeDetailScreen(
                        recipe = currentRecipe, isSaving = isSaving, isSaved = isSaved,
                        onBack = { navController.popBackStack() }, onSave = detailViewModel::saveRecipe,
                        onDelete = detailViewModel::deleteCurrentRecipe, snackbarHostState = detailSnackbarHostState,
                        viewModel = mainViewModel
                    )
                }
            }

            if (showShoppingList) {
                ModalBottomSheet(onDismissRequest = { mainViewModel.setShowShoppingList(false) }) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(id = R.string.shopping_list_with_icon), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (shoppingListItems.isNotEmpty()) {
                                    val shareHeader = stringResource(id = R.string.shopping_list_share_header)
                                    val sectionHeader = stringResource(id = R.string.shopping_list_share_recipe_section)
                                    val itemFormat = stringResource(id = R.string.shopping_list_share_item)
                                    val generalItemsLabel = stringResource(id = R.string.general_items)

                                    IconButton(onClick = {
                                        val shareText = shareHeader +
                                                shoppingListItems.filter { !it.isChecked }
                                                    .groupBy { it.recipeName ?: generalItemsLabel }
                                                    .map { (recipe, items) ->
                                                        sectionHeader.format(recipe) + items.joinToString("\n") { itemFormat.format(it.name) }
                                                    }.joinToString("\n\n")

                                        val sendIntent: Intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, null)
                                        context.startActivity(shareIntent)
                                    }) {
                                        Icon(Icons.Default.Share, contentDescription = stringResource(id = R.string.share_list), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                if (shoppingListItems.isNotEmpty()) {
                                    if (shoppingListItems.any { it.isChecked }) {
                                        TextButton(onClick = { mainViewModel.clearCheckedShoppingItems() }) {
                                            Text(stringResource(id = R.string.delete_checked), color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    TextButton(onClick = { mainViewModel.clearShoppingList() }) {
                                        Text(stringResource(id = R.string.clear_all), color = Color.Red)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (shoppingListItems.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(stringResource(id = R.string.shopping_list_empty), color = MaterialTheme.colorScheme.onSurface)
                            }
                        } else {
                            val generalItemsLabel = stringResource(id = R.string.general_items)
                            // מפצלים למצרכים שלא נקנו ולמצרכים שנקנו
                            val uncheckedGroups = shoppingListItems.filter { !it.isChecked }.groupBy { it.recipeName ?: generalItemsLabel }
                            val checkedGroups = shoppingListItems.filter { it.isChecked }.groupBy { it.recipeName ?: generalItemsLabel }

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
                                                .clickable { mainViewModel.toggleShoppingItem(item.id) }
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = item.isChecked,
                                                onCheckedChange = { mainViewModel.toggleShoppingItem(item.id) }
                                            )
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f).padding(start = 8.dp)
                                            )
                                            IconButton(onClick = { mainViewModel.removeShoppingItem(item.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete_item), tint = MaterialTheme.colorScheme.onSurface)
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
                                                text = "$recipeName " + stringResource(id = R.string.bought_suffix),
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
                                                    .clickable { mainViewModel.toggleShoppingItem(item.id) }
                                                    .padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = item.isChecked,
                                                    onCheckedChange = { mainViewModel.toggleShoppingItem(item.id) },
                                                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                                )
                                                Text(
                                                    text = item.name,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    textDecoration = TextDecoration.LineThrough,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                                                )
                                                IconButton(onClick = { mainViewModel.removeShoppingItem(item.id) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete_item), tint = MaterialTheme.colorScheme.onSurface)
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
        }
    }
}