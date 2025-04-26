package net.crulim.luckmod;

import net.crulim.luckmod.config.LuckConfig;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public class LuckMod implements ModInitializer {
    public static final String MOD_ID = "luckmod";

    @Override
    public void onInitialize() {
        LuckConfig.load();
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
