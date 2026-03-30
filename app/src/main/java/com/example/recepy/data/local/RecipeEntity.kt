package com.example.recepy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "recipes",
    indices = [
        androidx.room.Index(value = ["title"]),
        androidx.room.Index(value = ["sourceUrl"])
    ]
)
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val imageUrl: String?,
    val ingredients: String,
    val steps: String,
    val sourceUrl: String,
    val dateAdded: Long,
    val isFavorite: Boolean = false,
    val tags: String? = "[]",
    val rating: Int = 0,           // חדש: דירוג 1-5
    val notes: String = "",        // חדש: הערות אישיות
    val lastCooked: Long = 0L      // חדש: מתי הוכן לאחרונה
)