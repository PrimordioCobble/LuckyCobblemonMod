package net.crulim.luckblockcobblemon.neoforge;

import com.mojang.serialization.MapCodec;
import net.crulim.luckblockcobblemon.LuckBlockCobblemon;
import net.crulim.luckblockcobblemon.handler.AlphaLuckEvent;
import net.crulim.luckblockcobblemon.platform.Platform;
import net.crulim.luckblockcobblemon.registry.ModRegistration;
import net.crulim.luckblockcobblemon.registry.Registrar;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.function.Supplier;

/**
 * NeoForge entrypoint: wires the common code to NeoForge events.
 * Worldgen for the natural Lucky Blocks comes from data/luckblockcobblemon/neoforge/biome_modifier.
 */
@Mod(LuckBlockCobblemon.MOD_ID)
public final class LuckBlockCobblemonNeoForge {

    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, LuckBlockCobblemon.MOD_ID);

    static {
        CONDITIONS.register("config_enabled", () -> ConfigEnabledCondition.CODEC);
    }

    public LuckBlockCobblemonNeoForge(IEventBus modBus) {
        Platform.set(new NeoForgePlatform());
        LuckBlockCobblemon.init();

        CONDITIONS.register(modBus);
        modBus.addListener(LuckBlockCobblemonNeoForge::onRegister);

        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> LuckBlockCobblemon.registerCommands(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((ServerStartingEvent event) -> LuckBlockCobblemon.onServerStarting());
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> AlphaLuckEvent.onServerStopping());
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> AlphaLuckEvent.onServerTick(event.getServer()));
    }

    /** NeoForge only accepts registrations inside RegisterEvent, one registry at a time. */
    private static void onRegister(RegisterEvent event) {
        Registrar registrar = new Registrar() {
            @Override
            public <T> T register(ResourceKey<? extends Registry<T>> registry, String name, Supplier<T> factory) {
                T value = factory.get();
                event.register(registry, LuckBlockCobblemon.id(name), () -> value);
                return value;
            }
        };

        run(event, Registries.STRUCTURE_PROCESSOR, () -> ModRegistration.structureProcessors(registrar));
        run(event, Registries.FEATURE, () -> ModRegistration.features(registrar));
        run(event, Registries.BLOCK, () -> ModRegistration.blocks(registrar));
        run(event, Registries.ITEM, () -> ModRegistration.items(registrar));
        run(event, Registries.CREATIVE_MODE_TAB, () -> ModRegistration.creativeTabs(registrar));
    }

    private static void run(RegisterEvent event, ResourceKey<? extends Registry<?>> key, Runnable action) {
        if (event.getRegistryKey().equals(key)) {
            action.run();
        }
    }
}
