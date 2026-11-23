package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetDao {
    @Query("SELECT * FROM installed_widgets ORDER BY updatedAt DESC")
    fun observeAllWidgets(): Flow<List<WidgetEntity>>

    @Query("SELECT * FROM installed_widgets ORDER BY updatedAt DESC")
    suspend fun getAllWidgetsOnce(): List<WidgetEntity>

    @Query("SELECT * FROM installed_widgets WHERE id = :id LIMIT 1")
    suspend fun getWidgetById(id: Long): WidgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWidget(widget: WidgetEntity): Long

    @Update
    suspend fun updateWidget(widget: WidgetEntity)

    @Query("DELETE FROM installed_widgets WHERE id = :id")
    suspend fun deleteWidgetById(id: Long)

    @Query("SELECT * FROM installed_widgets WHERE appWidgetId = :appWidgetId LIMIT 1")
    suspend fun getWidgetByAppWidgetId(appWidgetId: Int): WidgetEntity?

    @Query("UPDATE installed_widgets SET isPinnedToHome = :pinned, appWidgetId = :appWidgetId WHERE id = :id")
    suspend fun markPinned(id: Long, pinned: Boolean, appWidgetId: Int)

    @Query("UPDATE installed_widgets SET isPinnedToHome = 0, appWidgetId = -1 WHERE appWidgetId = :appWidgetId")
    suspend fun clearPinnedByAppWidgetId(appWidgetId: Int)
}

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY isCompleted ASC, createdAt DESC")
    fun observeAllTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE isCompleted = 0 ORDER BY createdAt DESC LIMIT 5")
    suspend fun getActiveTodosOnce(): List<TodoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity): Long

    @Update
    suspend fun updateTodo(todo: TodoEntity)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteTodoById(id: Long)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC LIMIT 1")
    suspend fun getTopNoteOnce(): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)
}

@Dao
interface CountdownDao {
    @Query("SELECT * FROM countdowns ORDER BY targetDateMillis ASC")
    fun observeAllCountdowns(): Flow<List<CountdownEntity>>

    @Query("SELECT * FROM countdowns ORDER BY targetDateMillis ASC LIMIT 3")
    suspend fun getUpcomingCountdownsOnce(): List<CountdownEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCountdown(countdown: CountdownEntity): Long

    @Update
    suspend fun updateCountdown(countdown: CountdownEntity)

    @Query("DELETE FROM countdowns WHERE id = :id")
    suspend fun deleteCountdownById(id: Long)
}

@Dao
interface NewsDao {
    @Query("SELECT * FROM rss_sources ORDER BY id ASC")
    fun observeRssSources(): Flow<List<RssSourceEntity>>

    @Query("SELECT * FROM rss_sources WHERE isActive = 1")
    suspend fun getActiveSourcesOnce(): List<RssSourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: RssSourceEntity): Long

    @Update
    suspend fun updateSource(source: RssSourceEntity)

    @Query("DELETE FROM rss_sources WHERE id = :id")
    suspend fun deleteSourceById(id: Long)

    @Query("SELECT * FROM news_articles ORDER BY fetchedAt DESC LIMIT 30")
    fun observeArticles(): Flow<List<NewsArticleEntity>>

    @Query("SELECT * FROM news_articles ORDER BY fetchedAt DESC LIMIT 5")
    suspend fun getTopArticlesOnce(): List<NewsArticleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<NewsArticleEntity>)

    @Query("DELETE FROM news_articles WHERE sourceId = :sourceId")
    suspend fun deleteArticlesBySource(sourceId: Long)
}

@Dao
interface StoreInstallDao {
    @Query("SELECT * FROM store_installs")
    fun observeInstalls(): Flow<List<StoreInstallEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstall(install: StoreInstallEntity)

    @Query("DELETE FROM store_installs WHERE installedWidgetId = :widgetId")
    suspend fun deleteByWidgetId(widgetId: Long)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY checkedToday ASC, id ASC")
    fun observeAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY checkedToday ASC, id ASC")
    suspend fun getAllHabitsOnce(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: Long)
}
