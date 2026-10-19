package net.crulim.luckblockcobblemon.neoforge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.crulim.luckblockcobblemon.config.LuckyBlockSettingsConfig;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * NeoForge condition "luckblockcobblemon:config_enabled": loads a recipe/worldgen file only when
 * the given settings.json key is enabled. Same JSON fields as the Fabric version.
 */
public record ConfigEnabledCondition(String key) implements ICondition {
    public static final MapCodec<ConfigEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("key").forGetter(ConfigEnabledCondition::key)
    ).apply(instance, ConfigEnabledCondition::new));

    @Override
    public boolean test(IContext context) {
        return LuckyBlockSettingsConfig.isEnabled(key);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
