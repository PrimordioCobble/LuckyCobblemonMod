package net.crulim.luckblockcobblemon.world;

import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.crulim.luckblockcobblemon.LuckBlockCobblemon; // Seu MOD_ID
import net.minecraft.world.gen.feature.Feature;

public class ModWorldGen {

    public static Feature<DefaultFeatureConfig> POCKET_LUCKY_BLOCK_FEATURE;

    public static void registerFeatures() {
        POCKET_LUCKY_BLOCK_FEATURE = Registry.register(
                Registries.FEATURE,
                Identifier.of(LuckBlockCobblemon.MOD_ID, "pocket_lucky_block"),
                new PocketLuckyBlockFeature(DefaultFeatureConfig.CODEC)
        );
    }

    public static void init() {
        registerFeatures();
    }
}
