package net.crulim.luckblockcobblemon.block;

import net.crulim.luckblockcobblemon.LuckBlockCobblemon;
import net.crulim.luckblockcobblemon.block.custom.LuckBlockVanilla;
import net.crulim.luckblockcobblemon.block.custom.PocketLuckBlock;
import net.crulim.luckblockcobblemon.config.LockedLuckyBlockTierConfig;
import net.crulim.luckblockcobblemon.registry.Registrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * All Lucky Blocks of the mod. The fields are filled by {@link #register(Registrar)},
 * which each loader calls during its block registration phase.
 */
public class ModBlocks {

    public static Block LUCK_BLOCK_POCKET;
    public static Block LUCK_BLOCK_VANILLA;
    public static Block LUCK_BLOCK_POCKET_FIRE;
    public static Block LUCK_BLOCK_POCKET_WATER;
    public static Block LUCK_BLOCK_POCKET_GRASS;
    public static Block LUCK_BLOCK_POCKET_GROUND;
    public static Block LUCK_BLOCK_POCKET_FLY;
    public static Block LUCK_BLOCK_POCKET_STEEL;
    public static Block LUCK_BLOCK_POCKET_ELETRIC;
    public static Block LUCK_BLOCK_POCKET_FAIRY;

    public static Map<Integer, Block> LOCKED_DEFAULT_BLOCKS = Collections.emptyMap();
    public static Map<String, Map<Integer, Block>> LOCKED_THEMED_BLOCKS = Collections.emptyMap();

    /** Every block registered by the mod, by path, in registration order (used to register the block items). */
    private static final Map<String, Block> ALL_BLOCKS = new LinkedHashMap<>();

    private static BlockBehaviour.Properties defaultSettings() {
        return BlockBehaviour.Properties.of().strength(0.5f).sound(SoundType.AMETHYST);
    }

    public static void register(Registrar registrar) {
        LuckBlockCobblemon.LOGGER.info("Registering Mod Blocks for " + LuckBlockCobblemon.MOD_ID);

        LUCK_BLOCK_POCKET = registerBlock(registrar, "luck_block_pocket", () -> new LuckBlockPocket(defaultSettings()));
        LUCK_BLOCK_VANILLA = registerBlock(registrar, "luck_block_vanilla", () -> new LuckBlockVanilla(defaultSettings()));
        LUCK_BLOCK_POCKET_FIRE = registerBlock(registrar, "luck_block_pocket_fire", () -> new PocketLuckBlock(defaultSettings()));
        LUCK_BLOCK_POCKET_WATER = registerBlock(registrar, "luck_block_pocket_water", () -> new PocketLuckBlock(defaultSettings()));
        LUCK_BLOCK_POCKET_GRASS = registerBlock(registrar, "luck_block_pocket_grass", () -> new PocketLuckBlock(defaultSettings()));
        LUCK_BLOCK_POCKET_GROUND = registerBlock(registrar, "luck_block_pocket_ground", () -> new PocketLuckBlock(defaultSettings()));
        LUCK_BLOCK_POCKET_FLY = registerBlock(registrar, "luck_block_pocket_fly", () -> new PocketLuckBlock(defaultSettings()));
        LUCK_BLOCK_POCKET_STEEL = registerBlock(registrar, "luck_block_pocket_steel", () -> new PocketLuckBlock(defaultSettings()));
        LUCK_BLOCK_POCKET_ELETRIC = registerBlock(registrar, "luck_block_pocket_eletric", () -> new PocketLuckBlock(defaultSettings()));
        LUCK_BLOCK_POCKET_FAIRY = registerBlock(registrar, "luck_block_pocket_fairy", () -> new PocketLuckBlock(defaultSettings()));

        LOCKED_DEFAULT_BLOCKS = registerLockedDefaultBlocks(registrar);
        LOCKED_THEMED_BLOCKS = registerLockedThemedBlocks(registrar);
    }

    /** Registers a BlockItem for every block. Must run after {@link #register(Registrar)}. */
    public static void registerBlockItems(Registrar registrar) {
        for (Map.Entry<String, Block> entry : ALL_BLOCKS.entrySet()) {
            Block block = entry.getValue();
            registrar.register(Registries.ITEM, entry.getKey(), () -> new BlockItem(block, new Item.Properties()));
        }
    }

    private static Map<Integer, Block> registerLockedDefaultBlocks(Registrar registrar) {
        Map<Integer, Block> blocks = new LinkedHashMap<>();
        for (int tier = 1; tier <= 10; tier++) {
            String name = "luck_block_pocket_locked_t" + tier;
            blocks.put(tier, registerBlock(registrar, name, () -> new LuckBlockPocket(defaultSettings())));
        }
        return Collections.unmodifiableMap(blocks);
    }

    private static Map<String, Map<Integer, Block>> registerLockedThemedBlocks(Registrar registrar) {
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
                byTier.put(tier, registerBlock(registrar, name, () -> new PocketLuckBlock(defaultSettings())));
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

    private static Block registerBlock(Registrar registrar, String name, Supplier<Block> factory) {
        Block block = registrar.register(Registries.BLOCK, name, factory);
        ALL_BLOCKS.put(name, block);
        return block;
    }
}
