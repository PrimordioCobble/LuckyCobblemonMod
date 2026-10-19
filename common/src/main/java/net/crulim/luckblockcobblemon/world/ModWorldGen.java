package net.crulim.luckblockcobblemon.world;

import net.crulim.luckblockcobblemon.registry.Registrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class ModWorldGen {

    public static Feature<NoneFeatureConfiguration> POCKET_LUCKY_BLOCK_FEATURE;

    public static void register(Registrar registrar) {
        PocketLuckyBlockFeature feature = new PocketLuckyBlockFeature(NoneFeatureConfiguration.CODEC);
        registrar.register(Registries.FEATURE, "pocket_lucky_block", () -> feature);
        POCKET_LUCKY_BLOCK_FEATURE = feature;
    }
}
