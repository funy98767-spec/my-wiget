package com.example.extension

/**
 * Reserved modular plugin architecture for future H-ecosystem widgets:
 * - HeChat Widget
 * - HTask Widget
 * - HNews Widget
 * - HAI Widget
 *
 * Future modules implement [HWidgetPlugin] and register into [HWidgetPluginRegistry]
 * without modifying core UI or database schemas.
 */
enum class ExtensionSlotId(
    val key: String,
    val displayName: String,
    val categoryLabel: String,
    val description: String,
    val capabilities: List<String>
) {
    HE_CHAT(
        key = "hechat_widget",
        displayName = "HeChat Widget",
        categoryLabel = "即時通訊模組",
        description = "預留即時對話氣泡、未讀訊息摘要與快速回覆桌面卡片插槽。",
        capabilities = listOf("未讀聯絡人列", "一鍵語音/文字回覆", "群組靜音狀態")
    ),
    H_TASK(
        key = "htask_widget",
        displayName = "HTask Widget",
        categoryLabel = "進階任務看板",
        description = "預留多專案看板、番茄鐘計時同步與團隊指派任務卡片插槽。",
        capabilities = listOf("看板分欄同步", "專注計時聯動", "優先級色標")
    ),
    H_NEWS(
        key = "hnews_widget",
        displayName = "HNews Widget",
        categoryLabel = "智慧新聞聚合",
        description = "預留多源 RSS 智慧排版、關鍵字訂閱與頭條輪播卡片插槽。",
        capabilities = listOf("多頻道訂閱", "關鍵字篩選", "自動離線快取")
    ),
    H_AI(
        key = "hai_widget",
        displayName = "HAI Widget",
        categoryLabel = "智慧中樞模組",
        description = "預留每日智慧簡報、日程建議與快速指令啟動器桌面插槽。",
        capabilities = listOf("晨間重點摘要", "情境快捷動作", "外部模型介面")
    )
}

data class ExtensionSlotPreview(
    val slotId: ExtensionSlotId,
    val version: String,
    val isMounted: Boolean,
    val headline: String,
    val detail: String,
    val statusBadge: String
)

interface HWidgetPlugin {
    val slotId: ExtensionSlotId
    val version: String
    val contractUri: String
    fun createPreview(isMounted: Boolean): ExtensionSlotPreview
}

object HWidgetPluginRegistry {
    private val plugins: Map<ExtensionSlotId, HWidgetPlugin> = mapOf(
        ExtensionSlotId.HE_CHAT to object : HWidgetPlugin {
            override val slotId = ExtensionSlotId.HE_CHAT
            override val version = "v1.0-slot"
            override val contractUri = "hwidget://plugin/hechat/v1"
            override fun createPreview(isMounted: Boolean) = ExtensionSlotPreview(
                slotId = slotId,
                version = version,
                isMounted = isMounted,
                headline = if (isMounted) "HeChat 已掛載就緒" else "HeChat 擴充插槽 (預留)",
                detail = "等待外部 HeChat 服務綁定 · 支援 2x2 / 4x2 訊息預覽",
                statusBadge = if (isMounted) "MOUNTED" else "RESERVED"
            )
        },
        ExtensionSlotId.H_TASK to object : HWidgetPlugin {
            override val slotId = ExtensionSlotId.H_TASK
            override val version = "v1.0-slot"
            override val contractUri = "hwidget://plugin/htask/v1"
            override fun createPreview(isMounted: Boolean) = ExtensionSlotPreview(
                slotId = slotId,
                version = version,
                isMounted = isMounted,
                headline = if (isMounted) "HTask 已掛載就緒" else "HTask 擴充插槽 (預留)",
                detail = "與本機待辦資料橋接 · 支援 Sprint 進度條與番茄鐘",
                statusBadge = if (isMounted) "MOUNTED" else "RESERVED"
            )
        },
        ExtensionSlotId.H_NEWS to object : HWidgetPlugin {
            override val slotId = ExtensionSlotId.H_NEWS
            override val version = "v1.0-slot"
            override val contractUri = "hwidget://plugin/hnews/v1"
            override fun createPreview(isMounted: Boolean) = ExtensionSlotPreview(
                slotId = slotId,
                version = version,
                isMounted = isMounted,
                headline = if (isMounted) "HNews 已掛載就緒" else "HNews 擴充插槽 (預留)",
                detail = "擴展 RSS 引擎 · 支援多頻道輪播與即時頭條推播",
                statusBadge = if (isMounted) "MOUNTED" else "RESERVED"
            )
        },
        ExtensionSlotId.H_AI to object : HWidgetPlugin {
            override val slotId = ExtensionSlotId.H_AI
            override val version = "v1.0-slot"
            override val contractUri = "hwidget://plugin/hai/v1"
            override fun createPreview(isMounted: Boolean) = ExtensionSlotPreview(
                slotId = slotId,
                version = version,
                isMounted = isMounted,
                headline = if (isMounted) "HAI 已掛載就緒" else "HAI 擴充插槽 (預留)",
                detail = "模組化智慧卡片介面 · 可直接掛載第三方或系統 AI 套件",
                statusBadge = if (isMounted) "MOUNTED" else "RESERVED"
            )
        }
    )

    fun allPlugins(): List<HWidgetPlugin> = plugins.values.toList()

    fun getPlugin(key: String?): HWidgetPlugin? =
        ExtensionSlotId.entries.firstOrNull { it.key == key }?.let { plugins[it] }
}
