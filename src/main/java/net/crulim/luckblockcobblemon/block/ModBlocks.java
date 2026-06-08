package net.crulim.luckblockcobblemon.block;

import net.crulim.luckblockcobblemon.LuckBlockCobblemon;
import net.crulim.luckblockcobblemon.block.custom.LuckBlockVanilla;
import net.crulim.luckblockcobblemon.block.custom.PocketLuckBlock;
import net.crulim.luckblockcobblemon.config.LockedLuckyBlockTierConfig;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModBlocks {

    public static final Block LUCK_BLOCK_POCKET = registerBlock("luck_block_pocket",
            new LuckBlockPocket(defaultSettings()));

    public static final Block LUCK_BLOCK_VANILLA = registerBlock("luck_block_vanilla",
            new LuckBlockVanilla(defaultSettings()));

    public static final Block LUCK_BLOCK_POCKET_FIRE = registerBlock("luck_block_pocket_fire",
            new PocketLuckBlock(defaultSettings()));

    public static final Block LUCK_BLOCK_POCKET_WATER = registerBlock("luck_block_pocket_water",
            new PocketLuckBlock(defaultSettings()));

    public static final Block LUCK_BLOCK_POCKET_GRASS = registerBlock("luck_block_pocket_grass",
            new PocketLuckBlock(defaultSettings()));

    public static final Block LUCK_BLOCK_POCKET_GROUND = registerBlock("luck_block_pocket_ground",
            new PocketLuckBlock(defaultSettings()));

    public static final Block LUCK_BLOCK_POCKET_FLY = registerBlock("luck_block_pocket_fly",
            new PocketLuckBlock(defaultSettings()));

    public static final Block LUCK_BLOCK_POCKET_STEEL = registerBlock("luck_block_pocket_steel",
            new PocketLuckBlock(defaultSettings()));

    public static final Block LUCK_BLOCK_POCKET_ELETRIC = registerBlock("luck_block_pocket_eletric",
            new PocketLuckBlock(defaultSettings()));

    public static final Block LUCK_BLOCK_POCKET_FAIRY = registerBlock("luck_block_pocket_fairy",
            new PocketLuckBlock(defaultSettings()));

    public static final Map<Integer, Block> LOCKED_DEFAULT_BLOCKS = registerLockedDefaultBlocks();
    public static final Map<String, Map<Integer, Block>> LOCKED_THEMED_BLOCKS = registerLockedThemedBlocks();

    private static AbstractBlock.Settings defaultSettings() {
        return AbstractBlock.Settings.create().strength(0.5f).sounds(BlockSoundGroup.AMETHYST_BLOCK);
    }

    private static Map<Integer, Block> registerLockedDefaultBlocks() {
        Map<Integer, Block> blocks = new LinkedHashMap<>();
        for (int tier = 1; tier <= 10; tier++) {
            String name = "luck_block_pocket_locked_t" + tier;
            blocks.put(tier, registerBlock(name, new LuckBlockPocket(defaultSettings())));
        }
        return Collections.unmodifiableMap(blocks);
    }

    private static Map<String, Map<Integer, Block>> registerLockedThemedBlocks() {
        Map<String, Map<Integer, Block>> byType = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : LockedLuckyBlockTierConfig.getOfficialTypesToBaseBlockPaths().entrySet()) {
            String type = entry.getKey();
            String basePath = entry.getValue();
            if ("default".equals(type)) {
                continue;
            }

            Map<Integer, Block> byTier = new LinkedHashMap<>();
            for (int tier = 1; tier <= 10; tier++) {
                String name = basePath + "_locked_t" + tier;
                byTier.put(tier, registerBlock(name, new PocketLuckBlock(defaultSettings())));
            }
            byType.put(type, Collections.unmodifiableMap(byTier));
        }

        return Collections.unmodifiableMap(byType);
    }

    public static Block getLockedBlock(String type, int tier) {
        String canonicalType = LockedLuckyBlockTierConfig.canonicalizeType(type);
        if (canonicalType == null || !LockedLuckyBlockTierConfig.isValidTier(tier)) {
            return null;
        }

        if ("default".equals(canonicalType)) {
            return LOCKED_DEFAULT_BLOCKS.get(tier);
        }

        Map<Integer, Block> themedBlocks = LOCKED_THEMED_BLOCKS.get(canonicalType);
        return themedBlocks == null ? null : themedBlocks.get(tier);
    }

    public static List<Block> getAllLockedBlocks() {
        List<Block> blocks = new ArrayList<>(LOCKED_DEFAULT_BLOCKS.values());
        for (Map<Integer, Block> themedBlocks : LOCKED_THEMED_BLOCKS.values()) {
            blocks.addAll(themedBlocks.values());
        }
        return Collections.unmodifiableList(blocks);
    }

    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, Identifier.of(LuckBlockCobblemon.MOD_ID, name), block);
    }

    private static void registerBlockItem(String name, Block block) {
        Registry.register(Registries.ITEM, Identifier.of(LuckBlockCobblemon.MOD_ID, name), new BlockItem(block, new Item.Settings()));
    }

    public static void registerModBlocks() {
        LuckBlockCobblemon.LOGGER.info("Registering Mod Blocks for " + LuckBlockCobblemon.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
        });
    }
}
