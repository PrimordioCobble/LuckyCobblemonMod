package net.crulim.luckblockcobblemon.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.function.Supplier;

/**
 * Registers one object in a game registry, the way the current loader requires.
 * Fabric registers immediately; NeoForge registers inside its RegisterEvent.
 */
@FunctionalInterface
public interface Registrar {
    <T> T register(ResourceKey<? extends Registry<T>> registry, String name, Supplier<T> factory);
}
