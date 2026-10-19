package net.crulim.luckblockcobblemon.neoforge;

import net.crulim.luckblockcobblemon.platform.LoaderPlatform;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public final class NeoForgePlatform implements LoaderPlatform {
    @Override
    public String loaderName() {
        return "neoforge";
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return CreativeModeTab.builder();
    }
}
