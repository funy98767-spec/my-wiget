package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.domain.model.BlockType
import com.example.domain.model.WidgetBlock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Database(
    entities = [
        WidgetEntity::class,
        TodoEntity::class,
        NoteEntity::class,
        CountdownEntity::class,
        RssSourceEntity::class,
        NewsArticleEntity::class,
        StoreInstallEntity::class,
        HabitEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class HWidgetDatabase : RoomDatabase() {
    abstract fun widgetDao(): WidgetDao
    abstract fun todoDao(): TodoDao
    abstract fun noteDao(): NoteDao
    abstract fun countdownDao(): CountdownDao
    abstract fun newsDao(): NewsDao
    abstract fun storeInstallDao(): StoreInstallDao
    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile
        private var INSTANCE: HWidgetDatabase? = null

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE installed_widgets ADD COLUMN isCustom INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS habits (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        streakDays INTEGER NOT NULL,
                        checkedToday INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): HWidgetDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): HWidgetDatabase {
            lateinit var db: HWidgetDatabase
            db = Room.databaseBuilder(
                appContext,
                HWidgetDatabase::class.java,
                "hwidget_platform.db"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(object : Callback() {
                    override fun onCreate(supportDb: SupportSQLiteDatabase) {
                        super.onCreate(supportDb)
                        CoroutineScope(Dispatchers.IO).launch {
                            seedInitialData(db)
                        }
                    }
                })
                .build()
            return db
        }

        private suspend fun seedInitialData(db: HWidgetDatabase) {
            val now = System.currentTimeMillis()
            val dayMs = TimeUnit.DAYS.toMillis(1)

            // Seed Ready-Made Widgets (已經做好的)
            val compositeId = db.widgetDao().insertWidget(
                WidgetEntity(
                    title = "極光晨間儀表板",
                    widgetType = "COMPOSITE",
                    themeStyle = "GLASS",
                    clockStyle = "DIGITAL",
                    fontStyle = "SPACE",
                    opacity = 0.88f,
                    blurRadius = 16f,
                    cornerRadius = 26f,
                    accentColorHex = 0xFF22D3EEL,
                    textColorHex = 0xFFFFFFFFL,
                    backgroundPreset = "AURORA",
                    animationsEnabled = true,
                    refreshIntervalMinutes = 30,
                    blocksJson = BlockJsonCodec.encode(
                        listOf(
                            WidgetBlock(id = "b1", type = BlockType.TIME.name, orderIndex = 0, spanFullWidth = true),
                            WidgetBlock(id = "b2", type = BlockType.BATTERY.name, orderIndex = 1),
                            WidgetBlock(id = "b3", type = BlockType.WEATHER.name, orderIndex = 2)
                        )
                    ),
                    sourceStoreId = "official_aurora_combo",
                    isCustom = false
                )
            )
            db.storeInstallDao().insertInstall(
                StoreInstallEntity(storeItemId = "official_aurora_combo", installedWidgetId = compositeId)
            )

            db.widgetDao().insertWidget(
                WidgetEntity(
                    title = "復古翻頁時鐘",
                    widgetType = "CLOCK",
                    themeStyle = "DARK",
                    clockStyle = "FLIP",
                    fontStyle = "SPACE",
                    opacity = 0.94f,
                    blurRadius = 8f,
                    cornerRadius = 24f,
                    accentColorHex = 0xFF818CF8L,
                    textColorHex = 0xFFF8FAFC,
                    backgroundPreset = "NONE",
                    blocksJson = BlockJsonCodec.encode(
                        listOf(WidgetBlock(id = "b_flip", type = BlockType.TIME.name, orderIndex = 0, spanFullWidth = true))
                    ),
                    sourceStoreId = "official_flip_zen",
                    isCustom = false
                )
            )

            db.widgetDao().insertWidget(
                WidgetEntity(
                    title = "霓虹像素時鐘",
                    widgetType = "CLOCK",
                    themeStyle = "PIXEL",
                    clockStyle = "PIXEL",
                    fontStyle = "PIXEL",
                    opacity = 0.95f,
                    blurRadius = 0f,
                    cornerRadius = 14f,
                    accentColorHex = 0xFF00F5D4L,
                    textColorHex = 0xFF00F5D4L,
                    backgroundPreset = "CYBERPUNK",
                    blocksJson = BlockJsonCodec.encode(
                        listOf(WidgetBlock(id = "b_px", type = BlockType.TIME.name, orderIndex = 0, spanFullWidth = true))
                    ),
                    isCustom = false
                )
            )

            // Seed a Custom Widget example (自訂的)
            db.widgetDao().insertWidget(
                WidgetEntity(
                    title = "我的日常自訂看板",
                    widgetType = "COMPOSITE",
                    themeStyle = "MATERIAL",
                    clockStyle = "DIGITAL",
                    fontStyle = "SANS",
                    opacity = 0.92f,
                    blurRadius = 10f,
                    cornerRadius = 24f,
                    accentColorHex = 0xFF4F46E5L,
                    textColorHex = 0xFF0F172A,
                    backgroundPreset = "NONE",
                    blocksJson = BlockJsonCodec.encode(
                        listOf(
                            WidgetBlock(id = "c_todo", type = BlockType.TODO.name, orderIndex = 0, spanFullWidth = true),
                            WidgetBlock(id = "c_cd", type = BlockType.COUNTDOWN.name, orderIndex = 1),
                            WidgetBlock(id = "c_stp", type = BlockType.STEPS.name, orderIndex = 2)
                        )
                    ),
                    isCustom = true
                )
            )

            // Seed Todos
            db.todoDao().insertTodo(
                TodoEntity(
                    title = "完成 HWidget 桌面排版設計",
                    isCompleted = false,
                    dueAtMillis = now + TimeUnit.HOURS.toMillis(4),
                    reminderEnabled = true,
                    priority = 2
                )
            )
            db.todoDao().insertTodo(
                TodoEntity(
                    title = "補充水分與 8,000 步健走目標",
                    isCompleted = false,
                    dueAtMillis = now + TimeUnit.HOURS.toMillis(8),
                    reminderEnabled = true,
                    priority = 1
                )
            )
            db.todoDao().insertTodo(
                TodoEntity(
                    title = "檢查 RSS 新聞訂閱來源",
                    isCompleted = true,
                    dueAtMillis = null,
                    reminderEnabled = false,
                    priority = 0
                )
            )

            // Seed Sticky Notes (Yellow, Blue, Pink, Green)
            db.noteDao().insertNote(
                NoteEntity(
                    title = "本週靈感便籤",
                    content = "把時間 + 電量 + 天氣組合成玻璃擬態橫幅，放在首頁最上方最順眼！",
                    colorKey = "YELLOW",
                    isPinned = true
                )
            )
            db.noteDao().insertNote(
                NoteEntity(
                    title = "採買清單",
                    content = "手沖咖啡豆、藍芽鍵盤電池、Type-C 編織線",
                    colorKey = "BLUE",
                    isPinned = false
                )
            )
            db.noteDao().insertNote(
                NoteEntity(
                    title = "設計筆記",
                    content = "圓角 24dp 搭配 14dp 模糊與 88% 透明度在深色桌布最協調。",
                    colorKey = "PINK",
                    isPinned = false
                )
            )
            db.noteDao().insertNote(
                NoteEntity(
                    title = "運動計畫",
                    content = "週三晚間河濱慢跑 5 公里，記得同步步數小工具。",
                    colorKey = "GREEN",
                    isPinned = false
                )
            )

            // Seed Countdowns (Exam, Birthday, Travel)
            db.countdownDao().insertCountdown(
                CountdownEntity(
                    title = "重要考試 / 高考衝刺",
                    category = "EXAM",
                    targetDateMillis = now + dayMs * 42,
                    reminderEnabled = true,
                    accentHex = 0xFFF43F5EL
                )
            )
            db.countdownDao().insertCountdown(
                CountdownEntity(
                    title = "好友生日派對",
                    category = "BIRTHDAY",
                    targetDateMillis = now + dayMs * 11,
                    reminderEnabled = true,
                    accentHex = 0xFFF59E0BL
                )
            )
            db.countdownDao().insertCountdown(
                CountdownEntity(
                    title = "東京秋季自由行",
                    category = "TRAVEL",
                    targetDateMillis = now + dayMs * 28,
                    reminderEnabled = true,
                    accentHex = 0xFF06B6D4L
                )
            )

            // Seed RSS Sources
            val hnrssId = db.newsDao().insertSource(
                RssSourceEntity(
                    name = "Hacker News Frontpage",
                    feedUrl = "https://hnrss.org/frontpage",
                    autoUpdateMinutes = 30,
                    isActive = true
                )
            )
            db.newsDao().insertSource(
                RssSourceEntity(
                    name = "Android Developers Blog",
                    feedUrl = "https://feeds.feedburner.com/blogspot/hsDu",
                    autoUpdateMinutes = 60,
                    isActive = true
                )
            )
            db.newsDao().insertArticles(
                listOf(
                    NewsArticleEntity(
                        sourceId = hnrssId,
                        sourceName = "Hacker News",
                        title = "點擊右上角同步按鈕即可即時抓取最新 RSS 頭條",
                        summary = "HWidget 內建即時 XML RSS/Atom 解析引擎，支援自訂新聞來源與自動更新排程。",
                        link = "https://news.ycombinator.com",
                        publishedAtText = "即時就緒"
                    )
                )
            )

            // Seed Habits
            db.habitDao().insertHabit(HabitEntity(title = "早起喝溫水 300ml", streakDays = 7, checkedToday = true))
            db.habitDao().insertHabit(HabitEntity(title = "閱讀專業書 20 分鐘", streakDays = 4, checkedToday = false))
            db.habitDao().insertHabit(HabitEntity(title = "伸展或慢跑運動", streakDays = 5, checkedToday = false))
        }
    }
}
