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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.recepy.data.preferences.ThemeMode
import com.example.recepy.ui.screens.MainScreen
import com.example.recepy.ui.screens.RecipeDetailScreen
import com.example.recepy.ui.screens.SettingsScreen
import com.example.recepy.ui.screens.SplashScreen
import com.example.recepy.ui.theme.RecepyTheme
import com.example.recepy.viewmodel.MainViewModel
import com.example.recepy.viewmodel.RecipeDetailViewModel
import com.example.recepy.viewmodel.RecipeUiState
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

        handleAppIntent(intent)

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
                    var showSplash by remember { mutableStateOf(true) }

                    // בודקים מה להציג - את מסך הפתיחה או את האפליקציה המלאה
                    if (showSplash) {
                        SplashScreen(onNavigateToMain = { showSplash = false })
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
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("עדכון גרסה") },
        text = { Text("קיימת גרסה חדשה לאפליקציה. האם ברצונך לעדכן כעת?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("עדכן")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ביטול")
            }
        }
    )
}

@Composable
fun RecepyApp(
    mainViewModel: MainViewModel, detailViewModel: RecipeDetailViewModel,
    settingsViewModel: SettingsViewModel, keepScreenOn: Boolean, onKeepScreenOnToggle: () -> Unit
) {
    val navController = rememberNavController()
    val urlInput by mainViewModel.urlInput.collectAsState()
    val recipeUiState by mainViewModel.recipeUiState.collectAsState()
    val selectedRecipe by mainViewModel.selectedRecipe.collectAsState()
    val savedRecipes by mainViewModel.savedRecipes.collectAsState()
    val filteredRecipes by mainViewModel.filteredRecipes.collectAsState()
    val searchQuery by mainViewModel.searchQuery.collectAsState()
    val sortByAlpha by mainViewModel.sortByAlpha.collectAsState()
    val currentRecipe by detailViewModel.currentRecipe.collectAsState()
    val isSaving by detailViewModel.isSaving.collectAsState()
    val isSaved by detailViewModel.isSaved.collectAsState()
    val detailMessages by detailViewModel.messages.collectAsState(initial = "")
    val themeMode by settingsViewModel.themeMode.collectAsState()

    val homeSnackbarHostState = remember { SnackbarHostState() }
    val detailSnackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(recipeUiState) {
        when (recipeUiState) {
            is RecipeUiState.Success -> { navController.navigate("detail/-1"); mainViewModel.consumeUiState() }
            is RecipeUiState.Error -> { homeSnackbarHostState.showSnackbar((recipeUiState as RecipeUiState.Error).message); mainViewModel.consumeUiState() }
            else -> {}
        }
    }

    LaunchedEffect(detailMessages) { if (detailMessages.isNotBlank()) detailSnackbarHostState.showSnackbar(detailMessages) }

    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable("home") {
            MainScreen(
                urlInput = urlInput, onUrlChanged = mainViewModel::onUrlChanged,
                onExtractClick = mainViewModel::extractRecipe,
                onExtractFromText = { text, tags, image -> mainViewModel.extractRecipeFromText(text, tags, image) },
                onToggleFavorite = mainViewModel::toggleFavorite,
                isLoading = recipeUiState is RecipeUiState.Loading, savedRecipes = savedRecipes,
                filteredRecipes = filteredRecipes, searchQuery = searchQuery,
                onSearchQueryChanged = mainViewModel::onSearchQueryChanged,
                onSettingsClick = { navController.navigate("settings") },
                onSavedRecipeClick = { recipeId -> navController.navigate("detail/$recipeId") },
                onDeleteRecipe = mainViewModel::deleteRecipe, sortByAlpha = sortByAlpha,
                onSortToggle = mainViewModel::toggleSort, snackbarHostState = homeSnackbarHostState,
                viewModel = mainViewModel
            )
        }
        composable("settings") {
            val domainCounts by settingsViewModel.domainCounts.collectAsState()
            val appTheme by settingsViewModel.appTheme.collectAsState()
            val isImporting by mainViewModel.isImporting.collectAsState()
            val systemUpdateMessage by mainViewModel.systemUpdateMessage.collectAsState()
            val appUpdateMessage by mainViewModel.appUpdateMessage.collectAsState()
            val importMessage by mainViewModel.importMessage.collectAsState()
            val settingsSnackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(importMessage) {
                importMessage?.let {
                    settingsSnackbarHostState.showSnackbar(it)
                    mainViewModel.consumeImportMessage()
                }
            }

            val context = androidx.compose.ui.platform.LocalContext.current
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
                onCheckForUpdates = { mainViewModel.loadSeedRecipes() },
                onCheckAppUpdate = { mainViewModel.checkForAppUpdate(context) },
                snackbarHostState = settingsSnackbarHostState
            )
        }
        composable("detail/{recipeId}", arguments = listOf(navArgument("recipeId") { type = NavType.LongType })) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getLong("recipeId") ?: -1L
            LaunchedEffect(recipeId, selectedRecipe) { detailViewModel.loadRecipe(recipeId, if (recipeId == -1L) selectedRecipe else null) }
            RecipeDetailScreen(
                recipe = currentRecipe, isSaving = isSaving, isSaved = isSaved,
                onBack = { navController.popBackStack() }, onSave = detailViewModel::saveRecipe,
                onDelete = detailViewModel::deleteCurrentRecipe, snackbarHostState = detailSnackbarHostState,
                viewModel = mainViewModel
            )
        }
    }
}