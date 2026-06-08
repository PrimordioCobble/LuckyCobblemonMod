package net.crulim.luckblockcobblemon.world;

import com.mojang.serialization.Codec;
import net.crulim.luckblockcobblemon.LuckBlockCobblemon;
import net.crulim.luckblockcobblemon.block.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class PocketLuckyBlockFeature extends Feature<DefaultFeatureConfig> {
    public PocketLuckyBlockFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        WorldAccess world = context.getWorld();
        Random random = context.getRandom();

        for (int i = 0; i < 16; i++) {
            int x = context.getOrigin().getX() + random.nextInt(16);
            int z = context.getOrigin().getZ() + random.nextInt(16);

            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y <= world.getBottomY()) {
                continue;
            }

            BlockPos pos = new BlockPos(x, y, z);
            BlockPos below = pos.down();

            if (!canPlaceAt(world, pos, below)) {
                continue;
            }

            Block blockToPlace = getBlockForBiome(world.getBiome(pos));
            world.setBlockState(pos, blockToPlace.getDefaultState(), 3);

            LuckBlockCobblemon.LOGGER.info(
                    "[LuckyBlock Worldgen] Spawned {} at {} {} {}",
                    net.minecraft.registry.Registries.BLOCK.getId(blockToPlace),
                    pos.getX(), pos.getY(), pos.getZ()
            );

            return true;
        }

        return false;
    }

    private static boolean canPlaceAt(WorldAccess world, BlockPos pos, BlockPos below) {
        return world.isAir(pos)
                && world.isAir(pos.up())
                && world.isSkyVisible(pos)
                && world.getBlockState(below).isOpaque()
                && !world.getBlockState(below).isIn(BlockTags.LEAVES)
                && !world.getBlockState(below).isIn(BlockTags.LOGS)
                && !world.getBlockState(below).isOf(Blocks.WATER)
                && !world.getBlockState(below).isOf(Blocks.LAVA)
                && !world.getBlockState(below).isOf(Blocks.ICE)
                && !world.getBlockState(below).isOf(Blocks.PACKED_ICE)
                && !world.getBlockState(below).isOf(Blocks.BLUE_ICE)
                && !world.getBlockState(pos.down()).isOf(Blocks.TALL_GRASS)
                && !world.getBlockState(pos.down()).isOf(Blocks.LARGE_FERN);
    }

    private static Block getBlockForBiome(RegistryEntry<Biome> biome) {
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

    private static boolean isWaterBiome(RegistryEntry<Biome> biome) {
        return biome.matchesKey(BiomeKeys.OCEAN)
                || biome.matchesKey(BiomeKeys.DEEP_OCEAN)
                || biome.matchesKey(BiomeKeys.COLD_OCEAN)
                || biome.matchesKey(BiomeKeys.DEEP_COLD_OCEAN)
                || biome.matchesKey(BiomeKeys.LUKEWARM_OCEAN)
                || biome.matchesKey(BiomeKeys.DEEP_LUKEWARM_OCEAN)
                || biome.matchesKey(BiomeKeys.WARM_OCEAN)
                || biome.matchesKey(BiomeKeys.FROZEN_OCEAN)
                || biome.matchesKey(BiomeKeys.DEEP_FROZEN_OCEAN)
                || biome.matchesKey(BiomeKeys.RIVER)
                || biome.matchesKey(BiomeKeys.FROZEN_RIVER)
                || biome.matchesKey(BiomeKeys.BEACH)
                || biome.matchesKey(BiomeKeys.SNOWY_BEACH);
    }

    private static boolean isFireBiome(RegistryEntry<Biome> biome) {
        return biome.matchesKey(BiomeKeys.DESERT)
                || biome.matchesKey(BiomeKeys.BADLANDS)
                || biome.matchesKey(BiomeKeys.ERODED_BADLANDS)
                || biome.matchesKey(BiomeKeys.WOODED_BADLANDS);
    }

    private static boolean isGrassBiome(RegistryEntry<Biome> biome) {
        return biome.matchesKey(BiomeKeys.FOREST)
                || biome.matchesKey(BiomeKeys.FLOWER_FOREST)
                || biome.matchesKey(BiomeKeys.BIRCH_FOREST)
                || biome.matchesKey(BiomeKeys.OLD_GROWTH_BIRCH_FOREST)
                || biome.matchesKey(BiomeKeys.JUNGLE)
                || biome.matchesKey(BiomeKeys.SPARSE_JUNGLE)
                || biome.matchesKey(BiomeKeys.BAMBOO_JUNGLE)
                || biome.matchesKey(BiomeKeys.SWAMP)
                || biome.matchesKey(BiomeKeys.MANGROVE_SWAMP);
    }

    private static boolean isGroundBiome(RegistryEntry<Biome> biome) {
        return biome.matchesKey(BiomeKeys.PLAINS)
                || biome.matchesKey(BiomeKeys.SUNFLOWER_PLAINS)
                || biome.matchesKey(BiomeKeys.SAVANNA)
                || biome.matchesKey(BiomeKeys.SAVANNA_PLATEAU);
    }

    private static boolean isFlyingBiome(RegistryEntry<Biome> biome) {
        return biome.matchesKey(BiomeKeys.MEADOW)
                || biome.matchesKey(BiomeKeys.GROVE)
                || biome.matchesKey(BiomeKeys.SNOWY_SLOPES)
                || biome.matchesKey(BiomeKeys.FROZEN_PEAKS)
                || biome.matchesKey(BiomeKeys.WINDSWEPT_HILLS)
                || biome.matchesKey(BiomeKeys.WINDSWEPT_FOREST);
    }

    private static boolean isSteelBiome(RegistryEntry<Biome> biome) {
        return biome.matchesKey(BiomeKeys.STONY_PEAKS)
                || biome.matchesKey(BiomeKeys.JAGGED_PEAKS)
                || biome.matchesKey(BiomeKeys.STONY_SHORE)
                || biome.matchesKey(BiomeKeys.WINDSWEPT_GRAVELLY_HILLS);
    }

    private static boolean isElectricBiome(RegistryEntry<Biome> biome) {
        return biome.matchesKey(BiomeKeys.TAIGA)
                || biome.matchesKey(BiomeKeys.SNOWY_TAIGA)
                || biome.matchesKey(BiomeKeys.OLD_GROWTH_PINE_TAIGA)
                || biome.matchesKey(BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA)
                || biome.matchesKey(BiomeKeys.DARK_FOREST);
    }

    private static boolean isFairyBiome(RegistryEntry<Biome> biome) {
        return biome.matchesKey(BiomeKeys.CHERRY_GROVE)
                || biome.matchesKey(BiomeKeys.FLOWER_FOREST)
                || biome.matchesKey(BiomeKeys.MEADOW);
    }
}