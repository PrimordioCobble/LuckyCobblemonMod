package net.crulim.luckblockcobblemon.block;

import net.crulim.luckblockcobblemon.LuckBlockCobblemon;
import net.crulim.luckmod.block.CobbleLuckyBlock;
import net.crulim.luckmod.block.LuckClass;
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

public class ModBlocks {

    public static final Block LUCK_BASE = registerBlock("luck_base",
           new LuckClass(AbstractBlock.Settings.create().strength(0.5f).sounds(BlockSoundGroup.AMETHYST_BLOCK)));

    public static final Block LUCKY_BLOCK_COBBLE = registerBlock("lucky_block_cobble",
            new CobbleLuckyBlock());



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
