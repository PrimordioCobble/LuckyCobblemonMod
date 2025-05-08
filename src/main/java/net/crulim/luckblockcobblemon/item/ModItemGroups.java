package net.crulim.luckblockcobblemon.item;
import net.crulim.luckblockcobblemon.LuckBlockCobblemon;
import net.crulim.luckblockcobblemon.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;



public class ModItemGroups {


    public static final ItemGroup LUCK_BLOCKS_GROUP = Registry.register(Registries.ITEM_GROUP, Identifier.of(LuckBlockCobblemon.MOD_ID, "luckblocks"),
            FabricItemGroup.builder().icon(() -> new ItemStack(ModBlocks.LUCK_BLOCK_POCKET)).displayName(Text.translatable("itemgroup.luckblockcobblemon.luckblocks"))
                    .entries((displayContext, entries) -> {
                       // entries.add(ModBlocks.LUCK_BASE);
                       // entries.add(ModBlocks.LUCKY_BLOCK_COBBLE);
                        entries.add(ModBlocks.LUCK_BLOCK_VANILLA);
                        entries.add(ModBlocks.LUCK_BLOCK_POCKET);
                        entries.add(ModBlocks.LUCK_BLOCK_POCKET_FIRE);
                        entries.add(ModBlocks.LUCK_BLOCK_POCKET_GRASS);
                        entries.add(ModBlocks.LUCK_BLOCK_POCKET_GROUND);
                        entries.add(ModBlocks.LUCK_BLOCK_POCKET_WATER);
                        entries.add(ModBlocks.LUCK_BLOCK_POCKET_STEEL);
                        entries.add(ModBlocks.LUCK_BLOCK_POCKET_FLY);
                        entries.add(ModBlocks.LUCK_BLOCK_POCKET_FAIRY);
                        entries.add(ModBlocks.LUCK_BLOCK_POCKET_ELETRIC);

                    })
                    .build());

    public static void  registerItemGroups(){
        LuckBlockCobblemon.LOGGER.info("Registering Item Groups for " + LuckBlockCobblemon.MOD_ID);

    }

}