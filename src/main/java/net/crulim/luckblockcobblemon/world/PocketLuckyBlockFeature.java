package net.crulim.luckblockcobblemon.world;

import com.mojang.serialization.Codec;
import net.crulim.luckblockcobblemon.LuckBlockCobblemon;
import net.crulim.luckblockcobblemon.block.ModBlocks;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.Random;

public class PocketLuckyBlockFeature extends Feature<DefaultFeatureConfig> {
    public PocketLuckyBlockFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        WorldAccess world = context.getWorld();
        Random random = new Random();

        for (int i = 0; i < 16; i++) {
            int x = context.getOrigin().getX() + random.nextInt(16);
            int z = context.getOrigin().getZ() + random.nextInt(16);

            // Pega o topo da coluna para superfície do mundo
            int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1;
            BlockPos pos = new BlockPos(x, y + 1, z); // ar acima do solo
            BlockPos below = pos.down();

            // Checa se em cima é ar e abaixo não é folha, água, gelo, lava, etc.
            if (
                    world.isAir(pos)
                            && world.getBlockState(below).isOpaque()
                            && !world.getBlockState(below).isIn(BlockTags.LEAVES)
                            && !world.getBlockState(below).isOf(Blocks.WATER)
                            && !world.getBlockState(below).isOf(Blocks.LAVA)
                            && !world.getBlockState(below).isOf(Blocks.ICE)
                            && !world.getBlockState(below).isOf(Blocks.PACKED_ICE)
                            && !world.getBlockState(below).isOf(Blocks.BLUE_ICE)
            ) {
                // Troque para seu bloco depois de testar
                world.setBlockState(pos, ModBlocks.LUCK_BLOCK_POCKET.getDefaultState(), 3);
                return true;
            }
        }
        return false;
    }
}
