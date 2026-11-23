package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.WidgetBlock
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Entity(tableName = "installed_widgets")
data class WidgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val widgetType: String,
    val themeStyle: String = "GLASS",
    val clockStyle: String = "DIGITAL",
    val fontStyle: String = "SANS",
    val opacity: Float = 0.88f,
    val blurRadius: Float = 14f,
    val cornerRadius: Float = 24f,
    val accentColorHex: Long = 0xFF4F46E5L,
    val textColorHex: Long = 0xFFFFFFFFL,
    val backgroundPreset: String = "AURORA",
    val backgroundImageUri: String? = null,
    val animationsEnabled: Boolean = true,
    val refreshIntervalMinutes: Int = 30,
    val blocksJson: String = "[]",
    val isPinnedToHome: Boolean = false,
    val appWidgetId: Int = -1,
    val sourceStoreId: String? = null,
    val isCustom: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val dueAtMillis: Long? = null,
    val reminderEnabled: Boolean = true,
    val priority: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val colorKey: String = "YELLOW",
    val isPinned: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "countdowns")
data class CountdownEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String = "EXAM",
    val targetDateMillis: Long,
    val reminderEnabled: Boolean = true,
    val accentHex: Long = 0xFF4F46E5L,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "rss_sources")
data class RssSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val feedUrl: String,
    val autoUpdateMinutes: Int = 30,
    val isActive: Boolean = true,
    val lastFetchedAt: Long = 0L
)

@Entity(tableName = "news_articles")
data class NewsArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: Long,
    val sourceName: String,
    val title: String,
    val summary: String,
    val link: String,
    val publishedAtText: String,
    val fetchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "store_installs")
data class StoreInstallEntity(
    @PrimaryKey val storeItemId: String,
    val installedWidgetId: Long,
    val installedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val streakDays: Int = 1,
    val checkedToday: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

object BlockJsonCodec {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val listType = Types.newParameterizedType(List::class.java, WidgetBlock::class.java)
    private val adapter = moshi.adapter<List<WidgetBlock>>(listType)

    fun encode(blocks: List<WidgetBlock>): String =
        runCatching { adapter.toJson(blocks) }.getOrDefault("[]")

    fun decode(json: String): List<WidgetBlock> =
        runCatching { adapter.fromJson(json).orEmpty() }.getOrDefault(emptyList())
}
