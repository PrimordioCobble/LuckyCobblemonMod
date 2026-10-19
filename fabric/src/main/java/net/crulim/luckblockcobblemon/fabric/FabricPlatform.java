package net.crulim.luckblockcobblemon.fabric;

import net.crulim.luckblockcobblemon.platform.LoaderPlatform;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;

import java.nio.file.Path;

public final class FabricPlatform implements LoaderPlatform {
    @Override
    public String loaderName() {
        return "fabric";
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return FabricItemGroup.builder();
    }
}
