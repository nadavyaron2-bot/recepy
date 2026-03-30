package com.example.recepy.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.example.recepy.data.local.AppDatabase
import com.example.recepy.data.local.RecipeEntity
import com.example.recepy.ui.theme.OrangePrimary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WidgetConfigActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WidgetConfigScreen(
                        context = this,
                        appWidgetId = appWidgetId,
                        onSave = { selectedType, selectedTheme, alpha, recipeId ->
                            saveWidgetConfig(selectedType, selectedTheme, alpha, recipeId)
                        }
                    )
                }
            }
        }
    }

    private fun saveWidgetConfig(type: String, theme: String, alpha: Float, recipeId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity)
                .getGlanceIdBy(appWidgetId)

            updateAppWidgetState(this@WidgetConfigActivity, glanceId) { prefs ->
                prefs[WidgetKeys.TYPE] = type
                prefs[WidgetKeys.THEME] = theme
                prefs[WidgetKeys.ALPHA] = alpha
                prefs[WidgetKeys.RECIPE_ID] = recipeId
            }
            
            RecepyWidget().update(this@WidgetConfigActivity, glanceId)

            withContext(Dispatchers.Main) {
                val resultValue = Intent().apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                setResult(Activity.RESULT_OK, resultValue)

                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
                finish()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(context: Context, appWidgetId: Int, onSave: (String, String, Float, Long) -> Unit) {
    var selectedType by remember { mutableStateOf("shopping") }
    var selectedTheme by remember { mutableStateOf("system") }
    var alpha by remember { mutableStateOf(0.8f) }
    var savedRecipeId by remember { mutableStateOf(-1L) }

    var recipes by remember { mutableStateOf<List<RecipeEntity>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    var selectedRecipe by remember { mutableStateOf<RecipeEntity?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            recipes = db.recipeDao().getAllRecipesOnce()
            
            if (recipes.isNotEmpty()) {
                selectedRecipe = if (savedRecipeId != -1L) {
                    recipes.find { it.id == savedRecipeId } ?: recipes.first()
                } else {
                    recipes.first()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "הגדרות הווידג'ט",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp, top = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("מה להציג בווידג'ט?", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceButton(
                        text = "רשימת קניות",
                        isSelected = selectedType == "shopping",
                        onClick = { selectedType = "shopping" },
                        modifier = Modifier.weight(1f)
                    )
                    ChoiceButton(
                        text = "מתכון ספציפי",
                        isSelected = selectedType == "recipe",
                        onClick = { selectedType = "recipe" },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (selectedType == "recipe") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("בחר מתכון:", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedRecipe?.title ?: "אין מתכונים",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            recipes.forEach { recipe ->
                                DropdownMenuItem(
                                    text = { Text(recipe.title) },
                                    onClick = {
                                        selectedRecipe = recipe
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("עיצוב הרקע:", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChoiceButton("מערכת", selectedTheme == "system", { selectedTheme = "system" }, Modifier.weight(1f))
                    ChoiceButton("בהיר", selectedTheme == "light", { selectedTheme = "light" }, Modifier.weight(1f))
                    ChoiceButton("כהה", selectedTheme == "dark", { selectedTheme = "dark" }, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("שקיפות הרקע: ${(alpha * 100).toInt()}%", fontWeight = FontWeight.SemiBold)
                Slider(
                    value = alpha,
                    onValueChange = { alpha = it },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val rId = if (selectedType == "recipe") selectedRecipe?.id ?: -1L else -1L
                onSave(selectedType, selectedTheme, alpha, rId)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("שמור הגדרות", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun ChoiceButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}
