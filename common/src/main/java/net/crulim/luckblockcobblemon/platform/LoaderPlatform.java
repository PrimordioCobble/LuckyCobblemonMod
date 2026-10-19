package net.crulim.luckblockcobblemon.platform;

import net.minecraft.world.item.CreativeModeTab;

import java.nio.file.Path;

/**
 * The few things that work differently on each mod loader.
 * Implemented in the fabric and neoforge modules.
 */
public interface LoaderPlatform {

    /** Loader name, used in logs ("fabric" or "neoforge"). */
    String loaderName();

    /** Global config folder of the game instance (the "config" directory). */
    Path configDir();

    /** Creative tab builder that integrates with the loader's tab system. */
    CreativeModeTab.Builder creativeTabBuilder();
}
