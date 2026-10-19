package net.crulim.luckblockcobblemon.fabric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.crulim.luckblockcobblemon.LuckBlockCobblemon;
import net.crulim.luckblockcobblemon.config.LuckyBlockSettingsConfig;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;

/**
 * Fabric resource condition "luckblockcobblemon:config_enabled": loads a recipe/worldgen file only when
 * the given settings.json key is enabled.
 */
public record ConfigEnabledCondition(String key) implements ResourceCondition {
    public static final ResourceConditionType<ConfigEnabledCondition> TYPE = ResourceConditionType.create(
            ResourceLocation.fromNamespaceAndPath(LuckBlockCobblemon.MOD_ID, "config_enabled"),
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.STRING.fieldOf("key").forGetter(ConfigEnabledCondition::key)
            ).apply(instance, ConfigEnabledCondition::new))
    );

    @Override
    public ResourceConditionType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean test(HolderLookup.Provider registryLookup) {
        return LuckyBlockSettingsConfig.isEnabled(key);
    }
}