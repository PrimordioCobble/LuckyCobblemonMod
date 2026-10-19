package net.crulim.luckblockcobblemon.item;

import net.crulim.luckblockcobblemon.LuckBlockCobblemon;
import net.crulim.luckblockcobblemon.block.ModBlocks;
import net.crulim.luckblockcobblemon.platform.Platform;
import net.crulim.luckblockcobblemon.registry.Registrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class ModItemGroups {

    public static CreativeModeTab LUCK_BLOCKS_GROUP;

    public static void register(Registrar registrar) {
        LuckBlockCobblemon.LOGGER.info("Registering Item Groups for " + LuckBlockCobblemon.MOD_ID);

        LUCK_BLOCKS_GROUP = registrar.register(Registries.CREATIVE_MODE_TAB, "luckblocks", () -> Platform.get().creativeTabBuilder()
                .icon(() -> new ItemStack(ModBlocks.LUCK_BLOCK_POCKET))
                .title(Component.translatable("itemgroup.luckblockcobblemon.luckblocks"))
                .displayItems((displayContext, entries) -> {
                    entries.accept(ModBlocks.LUCK_BLOCK_VANILLA);
                    entries.accept(ModBlocks.LUCK_BLOCK_POCKET);
                    entries.accept(ModBlocks.LUCK_BLOCK_POCKET_FIRE);
                    entries.accept(ModBlocks.LUCK_BLOCK_POCKET_GRASS);
                    entries.accept(ModBlocks.LUCK_BLOCK_POCKET_GROUND);
                    entries.accept(ModBlocks.LUCK_BLOCK_POCKET_WATER);
                    entries.accept(ModBlocks.LUCK_BLOCK_POCKET_STEEL);
                    entries.accept(ModBlocks.LUCK_BLOCK_POCKET_FLY);
                    entries.accept(ModBlocks.LUCK_BLOCK_POCKET_FAIRY);
                    entries.accept(ModBlocks.LUCK_BLOCK_POCKET_ELETRIC);

                    for (Block block : ModBlocks.getAllLockedBlocks()) {
                        entries.accept(block);
                    }
                })
                .build());
    }
}
