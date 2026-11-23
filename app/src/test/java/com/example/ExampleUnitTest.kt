package com.example

import com.example.data.db.BlockJsonCodec
import com.example.domain.model.BlockType
import com.example.domain.model.WidgetBlock
import com.example.extension.ExtensionSlotId
import com.example.extension.HWidgetPluginRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun compositeWidgetBlocksSerializeAndDeserialize() {
        val blocks = listOf(
            WidgetBlock(id = "t1", type = BlockType.TIME.name, orderIndex = 0, spanFullWidth = true),
            WidgetBlock(id = "b1", type = BlockType.BATTERY.name, orderIndex = 1),
            WidgetBlock(id = "w1", type = BlockType.WEATHER.name, orderIndex = 2)
        )
        val encoded = BlockJsonCodec.encode(blocks)
        val decoded = BlockJsonCodec.decode(encoded)
        assertEquals(3, decoded.size)
        assertEquals(BlockType.TIME.name, decoded[0].type)
        assertEquals(BlockType.BATTERY.name, decoded[1].type)
        assertEquals(BlockType.WEATHER.name, decoded[2].type)
    }

    @Test
    fun extensionRegistryExposesAllReservedHEcosystemSlots() {
        val plugins = HWidgetPluginRegistry.allPlugins()
        assertEquals(4, plugins.size)
        val keys = plugins.map { it.slotId }.toSet()
        assertTrue(ExtensionSlotId.HE_CHAT in keys)
        assertTrue(ExtensionSlotId.H_TASK in keys)
        assertTrue(ExtensionSlotId.H_NEWS in keys)
        assertTrue(ExtensionSlotId.H_AI in keys)
        assertNotNull(HWidgetPluginRegistry.getPlugin("hai_widget"))
    }

    @Test
    fun newWidgetToolBlocksSerializeAndDeserialize() {
        val newTypes = listOf(
            BlockType.POMODORO,
            BlockType.WATER,
            BlockType.HABIT,
            BlockType.SYSTEM_STATS,
            BlockType.QUOTE,
            BlockType.QUICK_LAUNCH
        )
        val blocks = newTypes.mapIndexed { idx, t ->
            WidgetBlock(id = "n_$idx", type = t.name, orderIndex = idx, spanFullWidth = true)
        }
        val decoded = BlockJsonCodec.decode(BlockJsonCodec.encode(blocks))
        assertEquals(6, decoded.size)
        assertEquals(newTypes.map { it.name }, decoded.map { it.type })
    }

    @Test
    fun storeCatalogHasOver50UniqueNonDuplicateWidgets() {
        val packs = com.example.data.repository.StoreCatalog.allPacks()
        assertTrue("Expected at least 50 unique widgets, got ${packs.size}", packs.size >= 50)
        assertEquals(packs.size, packs.map { it.id }.toSet().size)
        assertEquals(packs.size, packs.map { it.title }.toSet().size)
        assertTrue(BlockType.entries.size >= 25)
    }
}
