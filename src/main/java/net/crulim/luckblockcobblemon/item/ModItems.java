package net.crulim.luckblockcobblemon.item;

import net.crulim.luckblockcobblemon.LuckBlockCobblemon;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import static net.crulim.luckblockcobblemon.block.ModBlocks.OBSIDIAN_LEAF;

public class ModItems {




    private static Item registerItem(String name, Item item){
        return Registry.register(Registries.ITEM, Identifier.of(LuckBlockCobblemon.MOD_ID, name), item);
    }



    public static void registerModItems(){
        LuckBlockCobblemon.LOGGER.info("Registering Mod Items for"+ LuckBlockCobblemon.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries-> {


        });
    }


}
