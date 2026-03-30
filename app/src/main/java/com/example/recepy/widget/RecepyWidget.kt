package com.example.recepy.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.recepy.MainActivity
import com.example.recepy.data.local.AppDatabase
import com.example.recepy.data.repository.Recipe
import com.example.recepy.data.repository.RecipeRepository
import com.example.recepy.data.local.ShoppingItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val ITEM_ID_KEY = ActionParameters.Key<String>("itemId")

// מפתחות עבור ה-State של הווידג'ט
object WidgetKeys {
    val TYPE = stringPreferencesKey("widget_type")
    val THEME = stringPreferencesKey("widget_theme")
    val ALPHA = floatPreferencesKey("widget_alpha")
    val RECIPE_ID = longPreferencesKey("widget_recipe_id")
}

class RecepyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RecepyWidget()
}

class RecepyWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context.applicationContext)
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)

        provideContent {
            val prefs = currentState<Preferences>()
            
            val widgetType = prefs[WidgetKeys.TYPE] ?: "shopping"
            val theme = prefs[WidgetKeys.THEME] ?: "system"
            val alpha = prefs[WidgetKeys.ALPHA] ?: 0.8f
            val recipeId = prefs[WidgetKeys.RECIPE_ID] ?: -1L

            val shoppingItems by db.shoppingDao().getAllItemsFlow().collectAsState(initial = emptyList<ShoppingItem>())
            
            WidgetContent(context, appWidgetId, widgetType, recipeId, theme, alpha, shoppingItems)
        }
    }

    @Composable
    private fun WidgetContent(
        context: Context,
        appWidgetId: Int,
        widgetType: String,
        recipeId: Long,
        theme: String,
        alpha: Float,
        shoppingItems: List<ShoppingItem>
    ) {
        val isSystemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val isDark = when (theme) {
            "light" -> false
            "dark" -> true
            else -> isSystemDark
        }

        val baseColor = if (isDark) Color.Black else Color.White
        val textColor = if (isDark) Color.White else Color.Black
        val backgroundColor = baseColor.copy(alpha = alpha)

        val mainAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_tab", if (widgetType == "shopping") "shopping" else "home")
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(backgroundColor)
                .clickable(actionStartActivity(mainAppIntent))
                .padding(12.dp)
        ) {
            HeaderRow(context, appWidgetId, textColor, widgetType)

            if (widgetType == "shopping") {
                ShoppingListContent(textColor, shoppingItems)
            } else {
                RecipeContent(context, recipeId, textColor)
            }
        }
    }

    @Composable
    private fun HeaderRow(context: Context, appWidgetId: Int, textColor: Color, widgetType: String) {
        val configIntent = Intent(context, WidgetConfigActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }

        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (widgetType == "shopping") "רשימת קניות 🛒" else "מתכון 🍲",
                style = TextStyle(color = ColorProvider(textColor), fontSize = 18.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = "⚙️",
                style = TextStyle(fontSize = 18.sp),
                modifier = GlanceModifier.clickable(actionStartActivity(configIntent)).padding(start = 8.dp)
            )
        }
    }

    @Composable
    private fun ShoppingListContent(textColor: Color, items: List<ShoppingItem>) {
        if (items.isEmpty()) {
            Text("הרשימה ריקה! 🛒", style = TextStyle(color = ColorProvider(textColor), fontSize = 16.sp))
        } else {
            val uncheckedItems = items.filter { !it.isChecked }
            val checkedItems = items.filter { it.isChecked }

            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                if (uncheckedItems.isNotEmpty()) {
                    val grouped = uncheckedItems.groupBy { it.recipeName ?: "מצרכים כלליים" }
                    grouped.forEach { (name, groupItems) ->
                        item {
                            Text(
                                text = name,
                                style = TextStyle(color = ColorProvider(textColor), fontSize = 15.sp, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline),
                                modifier = GlanceModifier.padding(top = 10.dp, bottom = 4.dp)
                            )
                        }
                        items(groupItems) { item -> ShoppingItemRow(item, textColor) }
                    }
                }

                if (checkedItems.isNotEmpty()) {
                    item {
                        Text(
                            text = "בוצעו ✅",
                            style = TextStyle(color = ColorProvider(textColor), fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            modifier = GlanceModifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                    }
                    items(checkedItems) { item -> ShoppingItemRow(item, textColor) }
                }
            }
        }
    }

    @Composable
    private fun ShoppingItemRow(item: ShoppingItem, textColor: Color) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CheckBox(
                checked = item.isChecked,
                onCheckedChange = actionRunCallback<ToggleItemAction>(actionParametersOf(ITEM_ID_KEY to item.id.toString())),
                modifier = GlanceModifier.padding(end = 8.dp)
            )
            Text(
                text = item.name,
                style = TextStyle(
                    color = ColorProvider(textColor),
                    fontSize = 14.sp,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                ),
                modifier = GlanceModifier.fillMaxWidth()
            )
        }
    }

    @Composable
    private fun RecipeContent(context: Context, recipeId: Long, textColor: Color) {
        val db = AppDatabase.getDatabase(context.applicationContext)
        val repository = RecipeRepository(db.recipeDao())
        // שימוש ב-collectAsState כדי לטעון את המתכון בצורה ריאקטיבית
        val recipes by repository.observeSavedRecipes().collectAsState(initial = emptyList<Recipe>())
        val recipe = recipes.find { it.id == recipeId }

        if (recipe == null) {
            Text("בחר מתכון בהגדרות 🍲", style = TextStyle(color = ColorProvider(textColor), fontSize = 14.sp))
            return
        }

        Text(
            text = recipe.title,
            style = TextStyle(color = ColorProvider(textColor), fontSize = 16.sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.padding(bottom = 8.dp)
        )

        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            items(recipe.ingredients) { ingredient ->
                Text(
                    text = "• $ingredient",
                    style = TextStyle(color = ColorProvider(textColor), fontSize = 14.sp),
                    modifier = GlanceModifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

class ToggleItemAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val itemId = parameters[ITEM_ID_KEY]?.toLongOrNull() ?: return
        withContext(Dispatchers.IO) {
            AppDatabase.getDatabase(context.applicationContext).shoppingDao().toggleItemChecked(itemId)
        }
    }
}
