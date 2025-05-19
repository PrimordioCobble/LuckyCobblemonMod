package net.crulim.luckblockcobblemon;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;
import net.crulim.luckblockcobblemon.block.ModBlocks;

@Environment(EnvType.CLIENT)
public class LuckBlockCobblemonClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.OBSIDIAN_LEAF, RenderLayer.getCutout());
    }
}
