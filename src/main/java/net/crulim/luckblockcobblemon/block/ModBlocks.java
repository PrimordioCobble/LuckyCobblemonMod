package net.crulim.luckblockcobblemon.block;
import net.minecraft.util.Identifier;
import net.crulim.luckblockcobblemon.LuckBlockCobblemon;
import net.crulim.luckblockcobblemon.block.custom.*;
import net.crulim.luckblockcobblemon.block.custom.LuckBlockVanilla;
import net.crulim.luckblockcobblemon.block.custom.PocketLuckBlock;
import net.crulim.luckblockcobblemon.item.ModItemGroups;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;





public class ModBlocks {

    //public static final Block LUCK_BASE = registerBlock("luck_base",new LuckClass(AbstractBlock.Settings.create().strength(0.5f).sounds(BlockSoundGroup.AMETHYST_BLOCK)));

    public static final Block LUCK_OBSIDIAN_TRAPDOOR = registerBlock("luck_obsidian",
            new CustomTrapdoorBlock(
                    BlockSetType.OAK,
                    AbstractBlock.Settings.create().strength(50.0f, 1200.0f).sounds(BlockSoundGroup.STONE).requiresTool().nonOpaque()
            )
    );

    public static final Block OBSIDIAN_LEAF = registerBlock(
            "obsidian_leaf",
            new ObsidianLeafBlock(
                    FabricBlockSettings.create()
                            .mapColor(MapColor.DARK_GREEN)
                            .strength(2.5f, 1200.0f)
                            .requiresTool()
                            .nonOpaque()
                            .sounds(BlockSoundGroup.CHERRY_LEAVES)
            )
    );




    // public static final Block LUCKY_BLOCK_COBBLE = registerBlock("lucky_block_cobble", new CobbleLuckyBlock());

    public static final Block LUCK_BLOCK_POCKET = registerBlock("luck_block_pocket",
            new LuckBlockPocket(AbstractBlock.Settings.create().strength(0.5f).sounds(BlockSoundGroup.AMETHYST_BLOCK)));

    // public static final Block LUCK_BLOCK_VANILLA = registerBlock("luck_block_vanilla", new LuckBlockVanilla(AbstractBlock.Settings.create().strength(0.5f).sounds(BlockSoundGroup.AMETHYST_BLOCK)));


    public static final Block LUCK_BLOCK_POCKET_FIRE = registerBlock("luck_block_pocket_fire",
            new PocketLuckBlock(AbstractBlock.Settings.create().strength(0.5f).sounds(BlockSoundGroup.AMETHYST_BLOCK)));

    public static final Block LUCK_BLOCK_POCKET_WATER = registerBlock("luck_block_pocket_water",
            new PocketLuckBlock(AbstractBlock.Settings.create().strength(0.5f).sounds(BlockSoundGroup.AMETHYST_BLOCK)));

    public static final Block LUCK_BLOCK_POCKET_GRASS = registerBlock("luck_block_pocket_grass",
            new PocketLuckBlock(AbstractBlock.Settings.create().strength(0.5f).sounds(BlockSoundGroup.AMETHYST_BLOCK)));

    public static final Block LUCK_BLOCK_POCKET_GROUND = registerBlock("luck_block_pocket_ground",
            new PocketLuckBlock(AbstractBlock.Settings.create().strength(0.5f).sounds(BlockSoundGroup.AMETHYST_BLOCK)));

    public static final Block LUCK_BLOCK_POCKET_FLY = registerBlock("luck_block_pocket_fly",
            new PocketLuckBlock(AbstractBlock.Settings.create().strength(0.5f).sounds(BlockSoundGroup.AMETHYST_BLOCK)));

    public static final Block LUCK_BLOCK_POCKET_STEEL = registerBlock("luck_block_pocket_steel",
            new PocketLuckBlock(AbstractBlock.Settings.create().strength(0.5f).sounds(BlockSoundGroup.AMETHYST_BLOCK)));

    public static final Block LUCK_BLOCK_POCKET_ELETRIC = registerBlock("luck_block_pocket_eletric",
            new PocketLuckBlock(AbstractBlock.Settings.create().strength(0.5f).sounds(BlockSoundGroup.AMETHYST_BLOCK)));

    public static final Block LUCK_BLOCK_POCKET_FAIRY = registerBlock("luck_block_pocket_fairy",
            new PocketLuckBlock(AbstractBlock.Settings.create().strength(0.5f).sounds(BlockSoundGroup.AMETHYST_BLOCK)));




    private  static Block registerBlock(String name, Block block){
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, Identifier.of(LuckBlockCobblemon.MOD_ID, name), block);
    }


    private static void registerBlockItem(String name, Block block){
       Registry.register(Registries.ITEM, Identifier.of(LuckBlockCobblemon.MOD_ID,name),new BlockItem(block, new Item.Settings()));
    }

    public static void registerModBlocks(){
        LuckBlockCobblemon.LOGGER.info(("Registering Mod Block for " + LuckBlockCobblemon.MOD_ID));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries ->{
            ;
        });
    }
}
