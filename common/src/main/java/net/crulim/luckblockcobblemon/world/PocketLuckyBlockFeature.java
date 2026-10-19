package net.crulim.luckblockcobblemon.world;

import com.mojang.serialization.Codec;
import net.crulim.luckblockcobblemon.LuckBlockCobblemon;
import net.crulim.luckblockcobblemon.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class PocketLuckyBlockFeature extends Feature<NoneFeatureConfiguration> {
    public PocketLuckyBlockFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        LevelAccessor world = context.level();
        RandomSource random = context.random();

        for (int i = 0; i < 16; i++) {
            int x = context.origin().getX() + random.nextInt(16);
            int z = context.origin().getZ() + random.nextInt(16);

            int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y <= world.getMinBuildHeight()) {
                continue;
            }

            BlockPos pos = new BlockPos(x, y, z);
            BlockPos below = pos.below();

            if (!canPlaceAt(world, pos, below)) {
                continue;
            }

            Block blockToPlace = getBlockForBiome(world.getBiome(pos));
            world.setBlock(pos, blockToPlace.defaultBlockState(), 3);

            LuckBlockCobblemon.LOGGER.info(
                    "[LuckyBlock Worldgen] Spawned {} at {} {} {}",
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(blockToPlace),
                    pos.getX(), pos.getY(), pos.getZ()
            );

            return true;
        }

        return false;
    }

    private static boolean canPlaceAt(LevelAccessor world, BlockPos pos, BlockPos below) {
        return world.isEmptyBlock(pos)
                && world.isEmptyBlock(pos.above())
                && world.canSeeSky(pos)
                && world.getBlockState(below).canOcclude()
                && !world.getBlockState(below).is(BlockTags.LEAVES)
                && !world.getBlockState(below).is(BlockTags.LOGS)
                && !world.getBlockState(below).is(Blocks.WATER)
                && !world.getBlockState(below).is(Blocks.LAVA)
                && !world.getBlockState(below).is(Blocks.ICE)
                && !world.getBlockState(below).is(Blocks.PACKED_ICE)
                && !world.getBlockState(below).is(Blocks.BLUE_ICE)
                && !world.getBlockState(pos.below()).is(Blocks.TALL_GRASS)
                && !world.getBlockState(pos.below()).is(Blocks.LARGE_FERN);
    }

    private static Block getBlockForBiome(Holder<Biome> biome) {
        if (isWaterBiome(biome)) {
            return ModBlocks.LUCK_BLOCK_POCKET_WATER;
        }
        if (isFireBiome(biome)) {
            return ModBlocks.LUCK_BLOCK_POCKET_FIRE;
        }
        if (isFairyBiome(biome)) {
            return ModBlocks.LUCK_BLOCK_POCKET_FAIRY;
        }
        if (isFlyingBiome(biome)) {
            return ModBlocks.LUCK_BLOCK_POCKET_FLY;
        }
        if (isSteelBiome(biome)) {
            return ModBlocks.LUCK_BLOCK_POCKET_STEEL;
        }
        if (isElectricBiome(biome)) {
            return ModBlocks.LUCK_BLOCK_POCKET_ELETRIC;
        }
        if (isGrassBiome(biome)) {
            return ModBlocks.LUCK_BLOCK_POCKET_GRASS;
        }
        if (isGroundBiome(biome)) {
            return ModBlocks.LUCK_BLOCK_POCKET_GROUND;
        }
        return ModBlocks.LUCK_BLOCK_POCKET;
    }

    private static boolean isWaterBiome(Holder<Biome> biome) {
        return biome.is(Biomes.OCEAN)
                || biome.is(Biomes.DEEP_OCEAN)
                || biome.is(Biomes.COLD_OCEAN)
                || biome.is(Biomes.DEEP_COLD_OCEAN)
                || biome.is(Biomes.LUKEWARM_OCEAN)
                || biome.is(Biomes.DEEP_LUKEWARM_OCEAN)
                || biome.is(Biomes.WARM_OCEAN)
                || biome.is(Biomes.FROZEN_OCEAN)
                || biome.is(Biomes.DEEP_FROZEN_OCEAN)
                || biome.is(Biomes.RIVER)
                || biome.is(Biomes.FROZEN_RIVER)
                || biome.is(Biomes.BEACH)
                || biome.is(Biomes.SNOWY_BEACH);
    }

    private static boolean isFireBiome(Holder<Biome> biome) {
        return biome.is(Biomes.DESERT)
                || biome.is(Biomes.BADLANDS)
                || biome.is(Biomes.ERODED_BADLANDS)
                || biome.is(Biomes.WOODED_BADLANDS);
    }

    private static boolean isGrassBiome(Holder<Biome> biome) {
        return biome.is(Biomes.FOREST)
                || biome.is(Biomes.FLOWER_FOREST)
                || biome.is(Biomes.BIRCH_FOREST)
                || biome.is(Biomes.OLD_GROWTH_BIRCH_FOREST)
                || biome.is(Biomes.JUNGLE)
                || biome.is(Biomes.SPARSE_JUNGLE)
                || biome.is(Biomes.BAMBOO_JUNGLE)
                || biome.is(Biomes.SWAMP)
                || biome.is(Biomes.MANGROVE_SWAMP);
    }

    private static boolean isGroundBiome(Holder<Biome> biome) {
        return biome.is(Biomes.PLAINS)
                || biome.is(Biomes.SUNFLOWER_PLAINS)
                || biome.is(Biomes.SAVANNA)
                || biome.is(Biomes.SAVANNA_PLATEAU);
    }

    private static boolean isFlyingBiome(Holder<Biome> biome) {
        return biome.is(Biomes.MEADOW)
                || biome.is(Biomes.GROVE)
                || biome.is(Biomes.SNOWY_SLOPES)
                || biome.is(Biomes.FROZEN_PEAKS)
                || biome.is(Biomes.WINDSWEPT_HILLS)
                || biome.is(Biomes.WINDSWEPT_FOREST);
    }

    private static boolean isSteelBiome(Holder<Biome> biome) {
        return biome.is(Biomes.STONY_PEAKS)
                || biome.is(Biomes.JAGGED_PEAKS)
                || biome.is(Biomes.STONY_SHORE)
                || biome.is(Biomes.WINDSWEPT_GRAVELLY_HILLS);
    }

    private static boolean isElectricBiome(Holder<Biome> biome) {
        return biome.is(Biomes.TAIGA)
                || biome.is(Biomes.SNOWY_TAIGA)
                || biome.is(Biomes.OLD_GROWTH_PINE_TAIGA)
                || biome.is(Biomes.OLD_GROWTH_SPRUCE_TAIGA)
                || biome.is(Biomes.DARK_FOREST);
    }

    private static boolean isFairyBiome(Holder<Biome> biome) {
        return biome.is(Biomes.CHERRY_GROVE)
                || biome.is(Biomes.FLOWER_FOREST)
                || biome.is(Biomes.MEADOW);
    }
}