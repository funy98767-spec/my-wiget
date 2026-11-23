package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.db.BlockJsonCodec
import com.example.data.db.HWidgetDatabase
import com.example.data.db.StoreInstallEntity
import com.example.data.db.WidgetEntity
import com.example.data.repository.StoreCatalog
import com.example.domain.model.BlockType
import com.example.domain.model.StoreWidgetPack
import com.example.ui.components.AuroraAmbientBackdrop
import com.example.ui.components.GlassSurfaceCard
import com.example.ui.theme.HWidgetTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HWidgetConfigureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setResult(RESULT_CANCELED)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            HWidgetTheme(themeMode = "SYSTEM", dynamicColor = true) {
                var widgets by remember { mutableStateOf<List<WidgetEntity>>(emptyList()) }
                val catalog = remember { StoreCatalog.allPacks() }
                var selectedTab by remember { mutableIntStateOf(0) } // 0 = 我的小工具, 1 = 全部 56 款成品
                var query by remember { mutableStateOf("") }
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    widgets = withContext(Dispatchers.IO) {
                        HWidgetDatabase.getInstance(applicationContext).widgetDao().getAllWidgetsOnce()
                    }
                }

                val filteredMyWidgets = remember(widgets, query) {
                    widgets.filter { w ->
                        query.isBlank() || w.title.contains(query, ignoreCase = true)
                    }
                }

                val filteredCatalog = remember(catalog, query) {
                    catalog.filter { pack ->
                        query.isBlank() ||
                            pack.title.contains(query, ignoreCase = true) ||
                            pack.description.contains(query, ignoreCase = true) ||
                            pack.category.labelZh.contains(query, ignoreCase = true)
                    }
                }

                fun bindAndFinish(widgetId: Long) {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            WidgetBindingStore.bind(applicationContext, appWidgetId, widgetId)
                            HWidgetDatabase.getInstance(applicationContext)
                                .widgetDao()
                                .markPinned(widgetId, pinned = true, appWidgetId = appWidgetId)
                            HWidgetUpdater.updateWidgetInstance(applicationContext, appWidgetId)
                        }
                        val resultValue = Intent().putExtra(
                            AppWidgetManager.EXTRA_APPWIDGET_ID,
                            appWidgetId
                        )
                        setResult(RESULT_OK, resultValue)
                        finish()
                    }
                }

                fun installCatalogAndFinish(pack: StoreWidgetPack) {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            val db = HWidgetDatabase.getInstance(applicationContext)
                            val now = System.currentTimeMillis()
                            val newId = db.widgetDao().insertWidget(
                                WidgetEntity(
                                    title = pack.title,
                                    widgetType = pack.category.name,
                                    themeStyle = pack.themeStyle.name,
                                    clockStyle = pack.clockStyle.name,
                                    fontStyle = pack.fontStyle.name,
                                    opacity = pack.opacity,
                                    blurRadius = pack.blurRadius,
                                    cornerRadius = pack.cornerRadius,
                                    accentColorHex = pack.accentColorHex,
                                    textColorHex = pack.textColorHex,
                                    backgroundPreset = pack.backgroundPreset.name,
                                    blocksJson = BlockJsonCodec.encode(pack.blocks),
                                    isPinnedToHome = true,
                                    appWidgetId = appWidgetId,
                                    sourceStoreId = pack.id,
                                    isCustom = false,
                                    createdAt = now,
                                    updatedAt = now
                                )
                            )
                            db.storeInstallDao().insertInstall(
                                StoreInstallEntity(
                                    storeItemId = pack.id,
                                    installedWidgetId = newId,
                                    installedAt = now
                                )
                            )
                            WidgetBindingStore.bind(applicationContext, appWidgetId, newId)
                            HWidgetUpdater.updateWidgetInstance(applicationContext, appWidgetId)
                        }
                        val resultValue = Intent().putExtra(
                            AppWidgetManager.EXTRA_APPWIDGET_ID,
                            appWidgetId
                        )
                        setResult(RESULT_OK, resultValue)
                        finish()
                    }
                }

                AuroraAmbientBackdrop(themeMode = "SYSTEM") {
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets.safeDrawing
                    ) { innerPadding ->
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    text = "選擇要放到桌面的 HWidget",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "每個桌面小工具位置皆獨立綁定，請選擇您要顯示的專屬 Widget。",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            item {
                                OutlinedTextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                    label = { Text("搜尋小工具名稱或類型...") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = selectedTab == 0,
                                        onClick = { selectedTab = 0 },
                                        label = { Text("我的小工具 (${widgets.size})") }
                                    )
                                    FilterChip(
                                        selected = selectedTab == 1,
                                        onClick = { selectedTab = 1 },
                                        label = { Text("商店成品 (${catalog.size})") }
                                    )
                                }
                            }

                            if (selectedTab == 0) {
                                items(filteredMyWidgets, key = { "my_${it.id}" }) { widget ->
                                    val blocks = remember(widget.blocksJson) {
                                        BlockJsonCodec.decode(widget.blocksJson).joinToString(" · ") { b ->
                                            runCatching { BlockType.valueOf(b.type).labelZh }.getOrDefault(b.type)
                                        }
                                    }
                                    GlassSurfaceCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { bindAndFinish(widget.id) }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = widget.title,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Surface(
                                                        shape = RoundedCornerShape(50),
                                                        color = if (widget.isCustom) {
                                                            MaterialTheme.colorScheme.tertiaryContainer
                                                        } else {
                                                            MaterialTheme.colorScheme.secondaryContainer
                                                        }
                                                    ) {
                                                        Text(
                                                            text = if (widget.isCustom) "自訂" else "已做好",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "${widget.themeStyle} · $blocks",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Button(onClick = { bindAndFinish(widget.id) }) {
                                                Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                                                Spacer(Modifier.width(4.dp))
                                                Text("選擇")
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(filteredCatalog, key = { "cat_${it.id}" }) { pack ->
                                    val blocks = remember(pack.blocks) {
                                        pack.blocks.joinToString(" · ") { b ->
                                            runCatching { BlockType.valueOf(b.type).labelZh }.getOrDefault(b.type)
                                        }
                                    }
                                    GlassSurfaceCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { installCatalogAndFinish(pack) }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = pack.title,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${pack.themeStyle.labelZh} · $blocks",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Button(onClick = { installCatalogAndFinish(pack) }) {
                                                Icon(Icons.Rounded.Download, contentDescription = null)
                                                Spacer(Modifier.width(4.dp))
                                                Text("加入")
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
